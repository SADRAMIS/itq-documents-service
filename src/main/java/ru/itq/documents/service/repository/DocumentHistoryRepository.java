package ru.itq.documents.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itq.documents.service.entity.DocumentHistory;

import java.util.List;

@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {
    List<DocumentHistory> findByDocumentIdOrderByActionTimeAsc(Long documentId);
}

