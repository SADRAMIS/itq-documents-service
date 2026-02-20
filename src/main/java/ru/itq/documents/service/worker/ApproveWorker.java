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
public class ApproveWorker {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final WorkersConfig config;

    @Scheduled(fixedDelayString = "${app.workers.approve-interval}")
    public void processSubmittedDocuments() {
        if (!config.isEnabled()) {
            return;
        }

        try {
            List<Long> submittedIds = documentRepository
                    .findIdsByStatusOrderByUpdatedAtAsc(Document.DocumentStatus.SUBMITTED, config.getBatchSize());

            if (submittedIds.isEmpty()) {
                log.debug("No SUBMITTED documents found for processing");
                return;
            }

            log.info("ApproveWorker: Found {} SUBMITTED documents, processing batch of {}", 
                    submittedIds.size(), Math.min(submittedIds.size(), config.getBatchSize()));

            BatchRequest request = new BatchRequest();
            request.setIds(submittedIds.stream().limit(config.getBatchSize()).collect(Collectors.toList()));
            request.setInitiator("approve-worker");
            request.setComment("Auto-approve by background worker");

            long startTime = System.currentTimeMillis();
            var results = documentService.approveBatch(request);
            long duration = System.currentTimeMillis() - startTime;

            long successCount = results.stream().filter(r -> r.isSuccess()).count();
            long failedCount = results.size() - successCount;

            log.info("ApproveWorker: Processed {} documents in {}ms. Success: {}, Failed: {}", 
                    results.size(), duration, successCount, failedCount);

        } catch (Exception e) {
            log.error("ApproveWorker: Error processing SUBMITTED documents", e);
        }
    }
}
