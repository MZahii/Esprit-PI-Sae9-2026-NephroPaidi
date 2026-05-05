package tn.esprit.spring.clinicalservice.followup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.spring.clinicalservice.followup.DoctorFollowUpStatus;
import tn.esprit.spring.clinicalservice.followup.FollowUpOffsetUnit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_follow_up_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorFollowUpRequest {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "consultation_id", nullable = false, columnDefinition = "uuid")
    private UUID consultationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false, columnDefinition = "uuid")
    private UUID doctorId;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(name = "offset_amount", nullable = false)
    private Integer offsetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "offset_unit", nullable = false, length = 16)
    private FollowUpOffsetUnit offsetUnit;

    @Column(name = "computed_return_date", nullable = false)
    private LocalDate computedReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DoctorFollowUpStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
