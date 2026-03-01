package ru.itq.documents.service.worker;

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

@Component
@Slf4j
@ConditionalOnProperty(name = "app.workers.enabled", havingValue = "true", matchIfMissing = true)
public class SubmitWorker extends AbstractDocumentWorker {

    private static final String WORKER_NAME = "submit-worker";

    public SubmitWorker(DocumentService documentService,
                       DocumentRepository documentRepository,
                       WorkersConfig config) {
        super(documentService, documentRepository, config);
    }

    @Scheduled(fixedDelayString = "${app.workers.submit-interval}")
    public void processDraftDocuments() {
        processBatch(WORKER_NAME, Document.DocumentStatus.DRAFT.name(), documentService::submitBatch);
    }
}
