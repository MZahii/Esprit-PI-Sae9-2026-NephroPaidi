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
import tn.esprit.spring.communicationservice.domain.enums.MessageQueue;
import tn.esprit.spring.communicationservice.domain.enums.MessageStatus;
import tn.esprit.spring.communicationservice.domain.enums.MessageType;
import tn.esprit.spring.communicationservice.domain.enums.PriorityLevel;
import tn.esprit.spring.communicationservice.domain.enums.StaffRole;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "follow_up_messages")
public class FollowUpMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String guardianKeycloakId;

    private String assignedDoctorKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageQueue queue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    private String assignedToUserKeycloakId;

    @Enumerated(EnumType.STRING)
    private StaffRole assignedToRole;

    @Column(length = 120)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String messageText;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    private Instant closedAt;
}
