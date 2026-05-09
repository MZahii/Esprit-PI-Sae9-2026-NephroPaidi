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
import tn.esprit.spring.communicationservice.domain.enums.SenderRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "message_replies")
public class MessageReply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private String senderKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderRole senderRole;

    @Column(nullable = false, length = 2000)
    private String replyText;

    @Column(nullable = false)
    private Instant createdAt;
}
