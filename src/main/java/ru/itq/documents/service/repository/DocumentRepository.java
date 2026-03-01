package ru.itq.documents.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itq.documents.service.entity.Document;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findByStatusOrderByUpdatedAtAsc(Document.DocumentStatus status);

    @Query(value = "SELECT id FROM documents WHERE status = :status ORDER BY updated_at ASC LIMIT :limit", nativeQuery = true)
    List<Long> findIdsByStatusOrderByUpdatedAtAsc(
            @Param("status") String status,
            @Param("limit") int limit);

    List<Document> findByIdInAndStatus(List<Long> ids, Document.DocumentStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE documents SET status = :newStatus, updated_at = CURRENT_TIMESTAMP, version = version + 1 " +
            "WHERE id = :id AND version = :version AND status = :oldStatus", nativeQuery = true)
    int updateStatusOptimistic(@Param("id") Long id,
                               @Param("oldStatus") String oldStatus,
                               @Param("newStatus") String newStatus,
                               @Param("version") Integer version);

    Optional<Document> findByIdAndStatus(Long id, Document.DocumentStatus status);

    @Query(value = "SELECT nextval('document_number_seq')", nativeQuery = true)
    Long getNextDocumentNumber();
}

