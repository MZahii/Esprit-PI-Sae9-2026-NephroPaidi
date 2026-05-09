package tn.esprit.spring.clinicalservice.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultation_outcome")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationOutcome {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "consultation_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID consultationId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "diagnosis", columnDefinition = "text")
    private String diagnosis;

    @Column(name = "prescriptions", columnDefinition = "text")
    private String prescriptions;

    @Column(name = "lab_requests", columnDefinition = "text")
    private String labRequests;

    @Column(name = "treatment_plan", columnDefinition = "text")
    private String treatmentPlan;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
