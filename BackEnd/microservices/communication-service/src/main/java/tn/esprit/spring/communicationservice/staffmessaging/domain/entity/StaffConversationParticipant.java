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

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "staff_conversation_participants")
public class StaffConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private StaffConversation conversation;

    @Column(nullable = false, length = 255)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private InternalStaffRole userRole;

    @Column(length = 255)
    private String displayName;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant lastReadAt;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "is_muted", nullable = false)
    private boolean muted;
}
