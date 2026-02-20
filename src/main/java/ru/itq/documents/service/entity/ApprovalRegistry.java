package ru.itq.documents.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "approval_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRegistry {
    @Id
    @Column(name = "document_id")
    private Long documentId;  // Внешний ключ к documents.id

    @CreationTimestamp
    @Column(name = "approved_at", nullable = false)
    private ZonedDateTime approvedAt;

    @Column(name = "approver_id", nullable = false)
    private String approverId;
}

