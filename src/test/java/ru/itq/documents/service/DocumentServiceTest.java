package ru.itq.documents.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.itq.documents.service.dto.*;
import ru.itq.documents.service.entity.Document;
import ru.itq.documents.service.entity.DocumentHistory;
import ru.itq.documents.service.repository.ApprovalRegistryRepository;
import ru.itq.documents.service.repository.DocumentHistoryRepository;
import ru.itq.documents.service.repository.DocumentRepository;
import ru.itq.documents.service.service.DocumentService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.itq.documents.service.entity.Document.DocumentStatus.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentHistoryRepository historyRepository;

    @Autowired
    private ApprovalRegistryRepository approvalRegistryRepository;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setAuthor("Test Author");
        request.setTitle("Test Document");
        request.setContent("Test Content");
        testDocument = documentService.create(request, "test-initiator");
    }

    @Test
    void testCreateDocument() {
        assertNotNull(testDocument.getId());
        assertNotNull(testDocument.getNumber());
        assertTrue(testDocument.getNumber().startsWith("DOC-"));
        assertEquals(DRAFT, testDocument.getStatus());
        assertEquals("Test Author", testDocument.getAuthor());
        assertEquals("Test Document", testDocument.getTitle());
    }

    @Test
    void testSubmitBatch() {
        Long docId = testDocument.getId();
        BatchRequest request = new BatchRequest();
        request.setIds(List.of(docId));
        request.setInitiator("test-user");
        request.setComment("Test submit");

        List<BatchResult> results = documentService.submitBatch(request);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(docId, results.get(0).getId());

        Document updated = documentRepository.findById(docId).orElseThrow();
        assertEquals(SUBMITTED, updated.getStatus());

        List<DocumentHistory> history = historyRepository.findByDocumentIdOrderByActionTimeAsc(docId);
        assertEquals(1, history.size());
        assertEquals(ActionTypeDto.SUBMIT, history.get(0).getAction());
        assertEquals("test-user", history.get(0).getInitiator());
    }

    @Test
    void testApproveBatch() {
        // Сначала переводим в SUBMITTED
        Long docId = testDocument.getId();
        documentRepository.updateStatusOptimistic(docId, DRAFT.name(), SUBMITTED.name(), testDocument.getVersion());

        BatchRequest request = new BatchRequest();
        request.setIds(List.of(docId));
        request.setInitiator("test-approver");
        request.setComment("Test approve");

        List<BatchResult> results = documentService.approveBatch(request);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());

        Document updated = documentRepository.findById(docId).orElseThrow();
        assertEquals(APPROVED, updated.getStatus());

        // Проверяем запись в реестре
        assertTrue(approvalRegistryRepository.findById(docId).isPresent());

        // Проверяем историю
        List<DocumentHistory> history = historyRepository.findByDocumentIdOrderByActionTimeAsc(docId);
        assertEquals(1, history.size());
        assertEquals(ActionTypeDto.APPROVE, history.get(0).getAction());
    }

    @Test
    void testApproveBatchWithPartialResults() {
        // Создаем еще один документ
        CreateDocumentRequest request2 = new CreateDocumentRequest();
        request2.setAuthor("Author 2");
        request2.setTitle("Document 2");
        request2.setContent("Content 2");
        Document doc2 = documentService.create(request2, "test");

        // Переводим оба в SUBMITTED
        Long docId1 = testDocument.getId();
        Long docId2 = doc2.getId();
        documentRepository.updateStatusOptimistic(docId1, DRAFT.name(), SUBMITTED.name(), testDocument.getVersion());
        documentRepository.updateStatusOptimistic(docId2, DRAFT.name(), SUBMITTED.name(), doc2.getVersion());

        // Утверждаем оба
        BatchRequest approveRequest = new BatchRequest();
        approveRequest.setIds(List.of(docId1, docId2));
        approveRequest.setInitiator("test-approver");

        List<BatchResult> results = documentService.approveBatch(approveRequest);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(BatchResult::isSuccess));
    }

    @Test
    void testApproveBatchNotFound() {
        BatchRequest request = new BatchRequest();
        request.setIds(List.of(99999L));
        request.setInitiator("test-user");

        List<BatchResult> results = documentService.approveBatch(request);

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals("NOT_FOUND", results.get(0).getErrorCode());
    }

    @Test
    void testApproveBatchConflict() {
        Long docId = testDocument.getId();
        documentRepository.updateStatusOptimistic(docId, DRAFT.name(), SUBMITTED.name(), testDocument.getVersion());

        BatchRequest request = new BatchRequest();
        request.setIds(List.of(docId));
        request.setInitiator("test-user");

        // Первое утверждение должно пройти
        List<BatchResult> results1 = documentService.approveBatch(request);
        assertTrue(results1.get(0).isSuccess());

        // Второе утверждение должно вернуть конфликт
        List<BatchResult> results2 = documentService.approveBatch(request);
        assertFalse(results2.get(0).isSuccess());
        assertEquals("NOT_FOUND", results2.get(0).getErrorCode()); // Документ уже в APPROVED
    }
}

