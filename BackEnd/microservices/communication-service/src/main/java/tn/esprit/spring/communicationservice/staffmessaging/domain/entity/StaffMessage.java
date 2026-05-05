package tn.esprit.spring.communicationservice.staffmessaging.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.communicationservice.staffmessaging.domain.InternalStaffRole;
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffMessageType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "staff_messages")
public class StaffMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private StaffConversation conversation;

    @Column(nullable = false, length = 255)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private InternalStaffRole senderRole;

    @Column(nullable = false, length = 255)
    private String senderDisplayName;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StaffMessageType messageType;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant editedAt;

    private Instant deletedAt;
}
