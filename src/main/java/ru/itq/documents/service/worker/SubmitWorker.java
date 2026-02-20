package ru.itq.documents.service.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itq.documents.service.config.WorkersConfig;
import ru.itq.documents.service.dto.BatchRequest;
import ru.itq.documents.service.entity.Document;
import ru.itq.documents.service.repository.DocumentRepository;
import ru.itq.documents.service.service.DocumentService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.workers.enabled", havingValue = "true", matchIfMissing = true)
public class SubmitWorker {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final WorkersConfig config;

    @Scheduled(fixedDelayString = "${app.workers.submit-interval}")
    public void processDraftDocuments() {
        if (!config.isEnabled()) {
            return;
        }

        try {
            List<Long> draftIds = documentRepository
                    .findIdsByStatusOrderByUpdatedAtAsc(Document.DocumentStatus.DRAFT, config.getBatchSize());

            if (draftIds.isEmpty()) {
                log.debug("No DRAFT documents found for processing");
                return;
            }

            log.info("SubmitWorker: Found {} DRAFT documents, processing batch of {}", 
                    draftIds.size(), Math.min(draftIds.size(), config.getBatchSize()));

            BatchRequest request = new BatchRequest();
            request.setIds(draftIds.stream().limit(config.getBatchSize()).collect(Collectors.toList()));
            request.setInitiator("submit-worker");
            request.setComment("Auto-submit by background worker");

            long startTime = System.currentTimeMillis();
            var results = documentService.submitBatch(request);
            long duration = System.currentTimeMillis() - startTime;

            long successCount = results.stream().filter(r -> r.isSuccess()).count();
            long failedCount = results.size() - successCount;

            log.info("SubmitWorker: Processed {} documents in {}ms. Success: {}, Failed: {}", 
                    results.size(), duration, successCount, failedCount);

        } catch (Exception e) {
            log.error("SubmitWorker: Error processing DRAFT documents", e);
        }
    }
}
