package tn.esprit.spring.clinicalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * ICD-10 diagnosis entity linked to ConsultationRecord.
 */
@Entity
@Table(name = "icd10_diagnoses", indexes = {
    @Index(name = "idx_consultation_id", columnList = "consultation_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ICD10Diagnosis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;
    
    @Column(name = "icd10_code", nullable = false, length = 10)
    private String icd10Code;
    
    @Column(name = "diagnosis_label", nullable = false)
    private String diagnosisLabel;
    
    @Column(name = "is_primary")
    private Boolean isPrimary;  // true = primary diagnosis, false = secondary/comorbidity
    
    @Column(columnDefinition = "TEXT")
    private String notes;
}
