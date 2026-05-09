package tn.esprit.spring.communicationservice.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.domain.enums.MessageAuditAction;
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "message_audit_logs")
public class MessageAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private String actorKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageAuditAction action;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;
}
