package ru.itq.documents.service.entity;

import jakarta.persistence.*;
import ru.itq.documents.service.dto.ActionTypeDto;

import java.time.ZonedDateTime;

@Entity
@Table(name = "document_history")
public class DocumentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, length = 100)
    private String initiator;

    @Column(name = "action_time", nullable = false)
    private ZonedDateTime actionTime = ZonedDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionTypeDto action; // SUBMIT, APPROVE

    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public ZonedDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(ZonedDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public ActionTypeDto getAction() {
        return action;
    }

    public void setAction(ActionTypeDto action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

