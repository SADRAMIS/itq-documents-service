package ru.itq.documents.service.worker;

import lombok.extern.slf4j.Slf4j;
import ru.itq.documents.service.config.WorkersConfig;
import ru.itq.documents.service.dto.BatchRequest;
import ru.itq.documents.service.dto.BatchResult;
import ru.itq.documents.service.repository.DocumentRepository;
import ru.itq.documents.service.service.DocumentService;

import java.util.List;
import java.util.function.Function;

/**
 * Базовый класс для воркеров обработки документов.
 * Устраняет дублирование кода между SubmitWorker и ApproveWorker.
 */
@Slf4j
abstract class AbstractDocumentWorker {

    protected final DocumentService documentService;
    protected final DocumentRepository documentRepository;
    protected final WorkersConfig config;

    protected AbstractDocumentWorker(DocumentService documentService,
                                    DocumentRepository documentRepository,
                                    WorkersConfig config) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.config = config;
    }

    protected void processBatch(String workerName, String statusName,
                                Function<BatchRequest, List<BatchResult>> processFn) {
        if (!config.isEnabled()) {
            return;
        }

        try {
            List<Long> ids = documentRepository
                    .findIdsByStatusOrderByUpdatedAtAsc(statusName, config.getBatchSize());

            if (ids.isEmpty()) {
                log.debug("{}: No documents found for processing", workerName);
                return;
            }

            List<Long> batchIds = ids.stream().limit(config.getBatchSize()).toList();
            log.info("{}: Found {} documents, processing batch of {}", workerName, ids.size(), batchIds.size());

            BatchRequest request = new BatchRequest();
            request.setIds(batchIds);
            request.setInitiator(workerName);
            request.setComment("Auto-process by " + workerName);

            long startTime = System.currentTimeMillis();
            List<BatchResult> results = processFn.apply(request);
            long duration = System.currentTimeMillis() - startTime;

            long successCount = results.stream().filter(BatchResult::isSuccess).count();
            long failedCount = results.size() - successCount;

            log.info("{}: Processed {} documents in {}ms. Success: {}, Failed: {}",
                    workerName, results.size(), duration, successCount, failedCount);

        } catch (Exception e) {
            log.error("{}: Error processing documents - will retry on next run", workerName, e);
            throw e;
        }
    }
}
