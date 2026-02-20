package ru.itq.documents.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itq.documents.service.entity.ApprovalRegistry;

@Repository
public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {
}
