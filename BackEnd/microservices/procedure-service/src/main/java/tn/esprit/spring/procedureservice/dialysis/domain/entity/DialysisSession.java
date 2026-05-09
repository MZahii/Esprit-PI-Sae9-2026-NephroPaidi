package tn.esprit.spring.procedureservice.dialysis.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dialysis_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DialysisSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private DialysisPlan plan;

    // External references are stored as text in the current schema.
    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "consultation_id")
    private String consultationId;

    @Column(name = "appointment_id")
    private String appointmentId;

    private LocalDateTime sessionDate;
    private String notes;
    
    // Idempotency key for duplicate request detection
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
