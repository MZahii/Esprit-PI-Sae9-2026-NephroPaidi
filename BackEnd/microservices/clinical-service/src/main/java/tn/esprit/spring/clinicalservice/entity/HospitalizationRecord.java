package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hospitalization record entity with neonatal and pediatric sections.
 * Contains embedded sections for respiratory, cardiac, neurological, infectious, and auditory-vision findings.
 */
@Entity
@Table(name = "hospitalization_records", indexes = {
    @Index(name = "idx_hosp_patient_id", columnList = "patient_id"),
    @Index(name = "idx_hosp_admission_date", columnList = "admission_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HospitalizationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, name = "patient_id")
    private UUID patientId;
    
    @Column(nullable = false, name = "admission_date")
    private LocalDateTime admissionDate;
    
    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;
    
    @Column(columnDefinition = "TEXT")
    private String admissionReason;
    
    // ============= NEONATAL DATA =============
    @Embedded
    private NeonatalData neonatalData;
    
    // ============= RESPIRATORY SECTION (gated) =============
    @Column(name = "has_respiratory_pathology")
    private Boolean hasRespiratoryPathology;
    
    @Embedded
    private RespiratorySection respiratorySection;
    
    // ============= CARDIAC SECTION (gated) =============
    @Column(name = "has_cardiac_pathology")
    private Boolean hasCardiacPathology;
    
    @Embedded
    private CardiacSection cardiacSection;
    
    // ============= NEUROLOGICAL SECTION =============
    @Embedded
    private NeurologicalSection neurologicalSection;
    
    // ============= INFECTIOUS SECTION =============
    @Embedded
    private InfectiousSection infectiousSection;
    
    // ============= AUDITORY & VISION SECTION =============
    @Embedded
    private AuditoryVisionSection auditoryVisionSection;
    
    // ============= EMBEDDED PEDIATRIC NEPHROLOGY RECORD =============
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "eGFR", column = @Column(name = "hosp_nephro_egfr")),
        @AttributeOverride(name = "schwartz_k", column = @Column(name = "hosp_nephro_k")),
        @AttributeOverride(name = "ckdStage", column = @Column(name = "hosp_nephro_ckd_stage"))
    })
    private PediatricNephrologyRecord nephologyRecord;
    
    // ============= AUDIT FIELDS =============
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
