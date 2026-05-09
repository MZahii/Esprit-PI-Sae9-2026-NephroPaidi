package tn.esprit.spring.communicationservice.staffmessaging.domain.entity;

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
import tn.esprit.spring.communicationservice.staffmessaging.domain.StaffConversationType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "staff_conversations")
public class StaffConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StaffConversationType type;

    @Column(length = 255)
    private String title;

    @Column(length = 512, unique = true)
    private String directConversationKey;

    @Column(nullable = false, length = 255)
    private String createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant lastMessageAt;
}
