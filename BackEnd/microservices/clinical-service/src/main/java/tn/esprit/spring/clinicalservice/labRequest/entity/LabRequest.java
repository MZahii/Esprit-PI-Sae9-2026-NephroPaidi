package tn.esprit.spring.clinicalservice.labRequest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequest {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "doctor_id", nullable = false, columnDefinition = "uuid")
    private UUID doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "consultation_id", columnDefinition = "uuid")
    private UUID consultationId;

    @Column(name = "test_type", nullable = false, length = 500)
    private String testType;

    @Column(name = "test_items_json", columnDefinition = "TEXT")
    private String testItemsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LabUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LabStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (urgency == null) urgency = LabUrgency.ROUTINE;
        if (status == null) status = LabStatus.PENDING;
    }

    public enum LabUrgency {
        ROUTINE, URGENT, STAT
    }

    public enum LabStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
