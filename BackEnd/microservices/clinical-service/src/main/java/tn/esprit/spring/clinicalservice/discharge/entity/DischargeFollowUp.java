package tn.esprit.spring.clinicalservice.discharge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discharge_follow_ups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DischargeFollowUp {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false, columnDefinition = "uuid")
    private UUID doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FollowUpStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = FollowUpStatus.ACTIVE;
    }

    public enum FollowUpStatus {
        ACTIVE, COMPLETED, CANCELLED
    }
}
