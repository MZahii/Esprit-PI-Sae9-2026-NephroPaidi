package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.EntryType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medical dossier entry for longitudinal patient timeline.
 * Tracks consultations, lab results, hospitalizations, procedures, discharge events, and reports.
 */
@Entity
@Table(name = "medical_dossier_entries", indexes = {
    @Index(name = "idx_medical_dossier_patient_id", columnList = "patient_id"),
    @Index(name = "idx_medical_dossier_legacy_patient_uuid", columnList = "legacy_patient_uuid"),
    @Index(name = "idx_medical_dossier_created_at", columnList = "created_at"),
    @Index(name = "idx_medical_dossier_entry_type", columnList = "entry_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalDossierEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "legacy_patient_uuid")
    private UUID legacyPatientUuid;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "entry_type")
    private EntryType entryType;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "source_service_id")
    private UUID sourceServiceId;  // which microservice published this entry
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
