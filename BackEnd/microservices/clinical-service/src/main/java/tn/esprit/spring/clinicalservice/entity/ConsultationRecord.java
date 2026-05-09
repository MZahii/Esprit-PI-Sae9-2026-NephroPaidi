package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.clinicalservice.enums.ConsultationType;
import tn.esprit.spring.clinicalservice.enums.AdmissionMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consultation record entity with embedded vitals, SOAP notes, pediatric nephrology findings, and allergies.
 */
@Entity
@Table(name = "consultation_records", indexes = {
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_consultation_date", columnList = "consultation_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, name = "patient_id")
    private UUID patientId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "consultation_type")
    private ConsultationType consultationType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "admission_mode")
    private AdmissionMode admissionMode;
    
    @Column(name = "referring_physician_id")
    private UUID referringPhysicianId;
    
    @Column(nullable = false, name = "consultation_date")
    private LocalDateTime consultationDate;
    
    @Column(name = "attending_physician_id")
    private UUID attendingPhysicianId;
    
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    // ============= EMBEDDED VITAL SIGNS =============
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "weight_kg", column = @Column(name = "vital_weight_kg")),
        @AttributeOverride(name = "height_cm", column = @Column(name = "vital_height_cm")),
        @AttributeOverride(name = "headCircumference_cm", column = @Column(name = "vital_head_circumference_cm")),
        @AttributeOverride(name = "bpSystolic_mmHg", column = @Column(name = "vital_bp_systolic")),
        @AttributeOverride(name = "bpDiastolic_mmHg", column = @Column(name = "vital_bp_diastolic")),
        @AttributeOverride(name = "heartRate_bpm", column = @Column(name = "vital_heart_rate")),
        @AttributeOverride(name = "respiratoryRate_bpm", column = @Column(name = "vital_respiratory_rate")),
        @AttributeOverride(name = "temperature_celsius", column = @Column(name = "vital_temperature")),
        @AttributeOverride(name = "oxygen_saturation_pct", column = @Column(name = "vital_oxygen_saturation"))
    })
    private VitalSigns vitalSigns;
    
    // ============= EMBEDDED SOAP NOTES =============
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "subjectiveSOAP", column = @Column(name = "soap_subjective")),
        @AttributeOverride(name = "objectiveSOAP", column = @Column(name = "soap_objective")),
        @AttributeOverride(name = "assessmentSOAP", column = @Column(name = "soap_assessment")),
        @AttributeOverride(name = "planSOAP", column = @Column(name = "soap_plan"))
    })
    private SOAPNote soapNote;
    
    // ============= EMBEDDED PEDIATRIC NEPHROLOGY RECORD =============
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "eGFR", column = @Column(name = "nephro_egfr")),
        @AttributeOverride(name = "schwartz_k", column = @Column(name = "nephro_k")),
        @AttributeOverride(name = "ckdStage", column = @Column(name = "nephro_ckd_stage")),
        @AttributeOverride(name = "ckdCause", column = @Column(name = "nephro_ckd_cause"))
    })
    private PediatricNephrologyRecord nephologyRecord;
    
    // ============= ELEMENT COLLECTION: ALLERGIES =============
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "consultation_allergies", joinColumns = @JoinColumn(name = "consultation_id"))
    private List<AllergyEntry> allergies = new ArrayList<>();
    
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
