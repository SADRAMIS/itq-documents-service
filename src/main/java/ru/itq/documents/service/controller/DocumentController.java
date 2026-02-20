package ru.itq.documents.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itq.documents.service.dto.*;
import ru.itq.documents.service.entity.Document;
import ru.itq.documents.service.entity.DocumentHistory;
import ru.itq.documents.service.service.DocumentService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentDto> create(@Valid @RequestBody CreateDocumentRequest request,
                                               @RequestHeader(value = "X-Initiator", defaultValue = "system") String initiator) {
        log.info("Creating document with title: {} by {}", request.getTitle(), initiator);
        Document document = documentService.create(request, initiator);
        return ResponseEntity.ok(toDto(document));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentWithHistoryDto> getById(@PathVariable Long id,
                                                          @RequestParam(value = "withHistory", defaultValue = "false") boolean withHistory) {
        Document document = documentService.findById(id)
                .orElseThrow(() -> new ru.itq.documents.service.exception.NotFoundException(id));
        
        DocumentWithHistoryDto dto = DocumentWithHistoryDto.builder()
                .document(toDto(document))
                .build();
        
        if (withHistory) {
            List<DocumentHistory> history = documentService.getHistory(id);
            dto.setHistory(history.stream()
                    .map(this::toHistoryDto)
                    .collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<DocumentDto>> getByIds(@RequestBody List<Long> ids) {
        List<Document> documents = documentService.findByIds(ids);
        List<DocumentDto> dtos = documents.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/submit")
    public ResponseEntity<List<BatchResult>> submitBatch(@Valid @RequestBody BatchRequest request) {
        log.info("Submit batch request for {} documents by {}", request.getIds().size(), request.getInitiator());
        validateBatchSize(request.getIds());
        List<BatchResult> results = documentService.submitBatch(request);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/approve")
    public ResponseEntity<List<BatchResult>> approveBatch(@Valid @RequestBody BatchRequest request) {
        log.info("Approve batch request for {} documents by {}", request.getIds().size(), request.getInitiator());
        validateBatchSize(request.getIds());
        List<BatchResult> results = documentService.approveBatch(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DocumentDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        ZonedDateTime from = dateFrom != null ? ZonedDateTime.parse(dateFrom) : null;
        ZonedDateTime to = dateTo != null ? ZonedDateTime.parse(dateTo) : null;
        
        Specification<Document> spec = documentService.buildSearchSpecification(status, author, from, to);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Document> documents = documentService.search(spec, pageable);
        Page<DocumentDto> dtos = documents.map(this::toDto);
        
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/concurrent-approve-test")
    public ResponseEntity<ConcurrentApproveResult> testConcurrentApprove(
            @RequestParam Long documentId,
            @RequestParam int threads,
            @RequestParam int attempts,
            @RequestHeader(value = "X-Initiator", defaultValue = "test") String initiator) {
        
        log.info("Testing concurrent approve for document {} with {} threads, {} attempts each", 
                documentId, threads, attempts);
        
        ConcurrentApproveResult result = documentService.testConcurrentApprove(documentId, threads, attempts, initiator);
        return ResponseEntity.ok(result);
    }

    private void validateBatchSize(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("IDs list cannot be empty");
        }
        if (ids.size() > 1000) {
            throw new IllegalArgumentException("Batch size cannot exceed 1000");
        }
    }

    private DocumentDto toDto(Document document) {
        return DocumentDto.builder()
                .id(document.getId())
                .number(document.getNumber())
                .author(document.getAuthor())
                .title(document.getTitle())
                .status(DocumentStatusDto.valueOf(document.getStatus().name()))
                .createdAt(document.getCreatedAt() != null ? document.getCreatedAt().toLocalDateTime() : null)
                .updatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().toLocalDateTime() : null)
                .build();
    }

    private DocumentHistoryDto toHistoryDto(DocumentHistory history) {
        return DocumentHistoryDto.builder()
                .id(history.getId())
                .initiator(history.getInitiator())
                .actionTime(history.getActionTime() != null ? history.getActionTime().toLocalDateTime() : null)
                .action(history.getAction())
                .comment(history.getComment())
                .build();
    }
}
