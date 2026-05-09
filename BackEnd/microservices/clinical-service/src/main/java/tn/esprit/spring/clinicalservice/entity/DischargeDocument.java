package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Discharge document entity with 5 mandatory HAS sections.
 * HAS Sections: 1=admission reason, 2=medical summary, 3=technical acts, 4=medications, 5=follow-up
 */
@Entity
@Table(name = "discharge_documents", indexes = {
    @Index(name = "idx_discharge_patient_id", columnList = "patient_id"),
    @Index(name = "idx_discharge_date", columnList = "discharge_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DischargeDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, name = "patient_id")
    private UUID patientId;
    
    @Column(nullable = false, name = "discharge_date")
    private LocalDateTime dischargeDate;
    
    // ============= 5 MANDATORY HAS SECTIONS =============
    
    // Section 1: Admission Reason (required)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String admissionReason;
    
    // Section 2: Medical Summary (required)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String medicalSummary;
    
    // Section 3: Technical Acts (@OneToMany, required)
    // Persisted separately
    
    // Section 4: Medications At Discharge (@OneToMany, required)
    // Persisted separately
    
    // Section 5: Follow-up Plan (required)
    @Embedded
    private FollowUpPlan followUpPlan;
    
    // ============= MEDICO-ADMINISTRATIVE FIELDS =============
    
    @Enumerated(EnumType.STRING)
    @Column(name = "discharge_destination")
    private DischargeDestination dischargeDestination;
    
    @Column(name = "discharge_weight_g")
    private Integer dischargeWeight_g;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "crh_document_status")
    private CRHDocumentStatus crhDocumentStatus;  // COMPLETE, PARTIAL_PENDING_8_DAYS
    
    @Column(name = "guardian_consent_for_dmp")
    private Boolean guardianConsentForDMP;  // Digital Medical Program consent
    
    @Column(name = "redactor_id")
    private UUID redactorId;
    
    @Column(name = "redaction_date")
    private LocalDateTime redactionDate;
    
    @Column(columnDefinition = "TEXT")
    private String distributionList;  // e.g., "GP, Pediatrician, Nephrology"
    
    @Column(name = "document_valid_as_crh")
    private Boolean documentValidAsCRH;  // Is this document valid as Compte Rendu d'Hospitalisation?
    
    // ============= AUDIT FIELDS =============
    
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;
    
    @Column(name = "finalized")
    private Boolean finalized = false;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
