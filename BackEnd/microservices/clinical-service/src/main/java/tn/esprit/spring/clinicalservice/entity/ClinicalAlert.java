package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.AlertSeverity;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clinical alert entity.
 * Tracks alerts for vigilance, nephrotoxic drugs, electrolyte imbalances, etc.
 */
@Entity
@Table(name = "clinical_alerts", indexes = {
    @Index(name = "idx_alert_patient_id", columnList = "patient_id"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_resolved", columnList = "resolved")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, name = "patient_id")
    private UUID patientId;
    
    @Column(nullable = false)
    private String alertType;  // e.g., "VIGILANCE", "NEPHROTOXIC_DRUG", "ELECTROLYTE_IMBALANCE"
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;  // ROUTINE, WARNING, URGENT
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(columnDefinition = "TEXT")
    private String details;  // e.g., drug name, lab value
    
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "resolved")
    private Boolean resolved = false;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
