package ru.itq.documents.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itq.documents.service.dto.*;
import ru.itq.documents.service.entity.ApprovalRegistry;
import ru.itq.documents.service.entity.Document;
import ru.itq.documents.service.entity.DocumentHistory;
import ru.itq.documents.service.exception.NotFoundException;
import ru.itq.documents.service.exception.RegistryErrorException;
import ru.itq.documents.service.repository.ApprovalRegistryRepository;
import ru.itq.documents.service.repository.DocumentHistoryRepository;
import ru.itq.documents.service.repository.DocumentRepository;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import ru.itq.documents.service.dto.ConcurrentApproveResult;

import static ru.itq.documents.service.entity.Document.DocumentStatus.APPROVED;
import static ru.itq.documents.service.entity.Document.DocumentStatus.DRAFT;
import static ru.itq.documents.service.entity.Document.DocumentStatus.SUBMITTED;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DocumentService {

    private static final String NUMBER_PREFIX = "DOC-";

    private final DocumentRepository repository;
    private final DocumentHistoryRepository historyRepository;
    private final ApprovalRegistryRepository approvalRegistryRepository;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/documents}")
    private String datasourceUrl;

    @Transactional
    public Document create(CreateDocumentRequest request, String initiator) {
        String number = generateDocumentNumber();
        Document document = Document.builder()
                .number(number)
                .author(request.getAuthor())
                .title(request.getTitle())
                .content(request.getContent())
                .status(DRAFT)
                .build();
        Document saved = repository.save(document);
        log.info("Created document {} with number {} by {}", saved.getId(), number, initiator);
        return saved;
    }

    private String generateDocumentNumber() {
        try {
            // Используем последовательность из БД для генерации уникального номера
            if (datasourceUrl.contains("h2") || datasourceUrl.contains("H2")) {
                // Для H2 используем fallback
                long timestamp = System.currentTimeMillis();
                long counter = timestamp % 100000000;
                return String.format("%s%08d", NUMBER_PREFIX, counter);
            } else {
                // Для PostgreSQL используем последовательность
                Long sequenceValue = repository.getNextDocumentNumber();
                return String.format("%s%08d", NUMBER_PREFIX, sequenceValue);
            }
        } catch (Exception e) {
            // Fallback для любых ошибок
            log.warn("Failed to get sequence value, using fallback: {}", e.getMessage());
            long timestamp = System.currentTimeMillis();
            return String.format("%s%08d", NUMBER_PREFIX, timestamp % 100000000);
        }
    }

    public Optional<Document> findById(Long id) {
        return repository.findById(id);
    }

    public List<Document> findByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    public List<DocumentHistory> getHistory(Long documentId) {
        return historyRepository.findByDocumentIdOrderByActionTimeAsc(documentId);
    }

    public Page<Document> search(Specification<Document> spec, Pageable pageable) {
        return repository.findAll(spec, pageable);
    }

    @Transactional
    public List<BatchResult> submitBatch(BatchRequest request) {
        log.info("Processing submit batch for {} documents by {}", request.getIds().size(), request.getInitiator());
        return processBatch(request.getIds(), DRAFT, SUBMITTED, ActionTypeDto.SUBMIT, request.getInitiator(), request.getComment());
    }

    @Transactional
    public List<BatchResult> approveBatch(BatchRequest request) {
        log.info("Processing approve batch for {} documents by {}", request.getIds().size(), request.getInitiator());
        List<BatchResult> results = new ArrayList<>();

        for (Long id : request.getIds()) {
            try {
                BatchResult result = approveSingle(id, request.getInitiator(), request.getComment());
                results.add(result);
            } catch (Exception e) {
                log.error("Error approving document {}: {}", id, e.getMessage());
                results.add(BatchResult.builder()
                        .id(id)
                        .success(false)
                        .errorCode("ERROR")
                        .message(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    private BatchResult approveSingle(Long id, String initiator, String comment) {
        Optional<Document> docOpt = repository.findByIdAndStatus(id, SUBMITTED);
        
        if (docOpt.isEmpty()) {
            return BatchResult.builder()
                    .id(id)
                    .success(false)
                    .errorCode("NOT_FOUND")
                    .message("Document not found or not in SUBMITTED status")
                    .build();
        }

        Document doc = docOpt.get();
        int currentVersion = doc.getVersion();
        int updated = repository.updateStatusOptimistic(id, SUBMITTED.name(), APPROVED.name(), currentVersion);
        
        if (updated == 0) {
            return BatchResult.builder()
                    .id(id)
                    .success(false)
                    .errorCode("CONFLICT")
                    .message("Concurrent modification detected")
                    .build();
        }
        
        // Сохраняем историю
        saveHistory(id, ActionTypeDto.APPROVE, initiator, comment);

        // Пытаемся записать в реестр
        try {
            ApprovalRegistry registry = ApprovalRegistry.builder()
                    .documentId(id)
                    .approverId(initiator)
                    .build();
            approvalRegistryRepository.save(registry);
            log.info("Successfully approved document {} by {}", id, initiator);
            return BatchResult.builder()
                    .id(id)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Failed to save approval registry for document {}: {}", id, e.getMessage());
            // Откатываем изменение статуса (версия уже увеличилась на 1)
            repository.updateStatusOptimistic(id, APPROVED.name(), SUBMITTED.name(), currentVersion + 1);
            throw new RegistryErrorException("Failed to save approval registry: " + e.getMessage());
        }
    }

    private List<BatchResult> processBatch(List<Long> ids, Document.DocumentStatus fromStatus, 
                                           Document.DocumentStatus toStatus, ActionTypeDto actionType,
                                           String initiator, String comment) {
        List<BatchResult> results = new ArrayList<>();

        for (Long id : ids) {
            Optional<Document> docOpt = repository.findByIdAndStatus(id, fromStatus);
            
            if (docOpt.isEmpty()) {
                results.add(BatchResult.builder()
                        .id(id)
                        .success(false)
                        .errorCode("NOT_FOUND")
                        .message("Document not found or not in " + fromStatus + " status")
                        .build());
                continue;
            }

            Document doc = docOpt.get();
            int currentVersion = doc.getVersion();
            int updated = repository.updateStatusOptimistic(id, fromStatus.name(), toStatus.name(), currentVersion);
            
            if (updated == 0) {
                results.add(BatchResult.builder()
                        .id(id)
                        .success(false)
                        .errorCode("CONFLICT")
                        .message("Concurrent modification detected")
                        .build());
                continue;
            }

            // Сохраняем историю
            saveHistory(id, actionType, initiator, comment);

            results.add(BatchResult.builder()
                    .id(id)
                    .success(true)
                    .build());
        }

        return results;
    }

    private void saveHistory(Long documentId, ActionTypeDto action, String initiator, String comment) {
        Document document = repository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        DocumentHistory history = new DocumentHistory();
        history.setDocument(document);
        history.setAction(action);
        history.setInitiator(initiator);
        history.setActionTime(ZonedDateTime.now());
        history.setComment(comment);
        historyRepository.save(history);
    }

    public ConcurrentApproveResult testConcurrentApprove(Long documentId, int threads, int attempts, String initiator) {
        // Убеждаемся, что документ в статусе SUBMITTED
        Document doc = repository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        if (doc.getStatus() != SUBMITTED) {
            // Переводим в SUBMITTED для теста
            repository.updateStatusOptimistic(documentId, doc.getStatus().name(), SUBMITTED.name(), doc.getVersion());
            // Обновляем документ для получения актуальной версии
            doc = repository.findById(documentId).orElseThrow();
        }

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong conflictCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                for (int j = 0; j < attempts; j++) {
                    try {
                        BatchRequest request = new BatchRequest();
                        request.setIds(List.of(documentId));
                        request.setInitiator(initiator + "-thread" + threadNum + "-attempt" + j);
                        request.setComment("Concurrent test");
                        
                        List<BatchResult> results = approveBatch(request);
                        BatchResult result = results.get(0);
                        
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else if ("CONFLICT".equals(result.getErrorCode())) {
                            conflictCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("Error in concurrent test thread {} attempt {}: {}", threadNum, j, e.getMessage());
                    }
                }
            });
            threadList.add(thread);
            thread.start();
        }

        // Ждем завершения всех потоков
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Получаем финальное состояние
        Document finalDoc = repository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        long registryCount = approvalRegistryRepository.findAll().stream()
                .filter(r -> r.getDocumentId().equals(documentId))
                .count();

        return ConcurrentApproveResult.builder()
                .documentId(documentId)
                .successCount((int) successCount.get())
                .conflictCount((int) conflictCount.get())
                .errorCount((int) errorCount.get())
                .finalStatus(finalDoc.getStatus())
                .registryEntriesCount((int) registryCount)
                .build();
    }

    public Specification<Document> buildSearchSpecification(String status, String author, 
                                                           ZonedDateTime dateFrom, ZonedDateTime dateTo) {
        Specification<Document> spec = (root, query, cb) -> cb.conjunction();
        
        if (status != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("status"), Document.DocumentStatus.valueOf(status)));
        }
        if (author != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("author"), author));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
        }
        
        return spec;
    }
}
