package tn.esprit.spring.clinicalservice.consultation.metrics;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation Metrics Entity - Enhanced CKD-EPI Support
 * 
 * Stores kidney function metrics with:
 * - CKD-EPI 2021 formula calculations (European standard)
 * - Both SI units (µmol/L) and legacy units (mg/dL)
 * - Trend analysis data (previous eGFR, change percentage)
 * - Quality scoring and audit trail
 * - CKD stage classification and alerts
 */
@Entity
@Table(name = "consultation_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMetrics {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private UUID consultationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "creatinine_mg_dl")
    private Double creatinineMgDl;

    @Column(name = "creatinine_umol")
    private Double creatinineUmol;

    @Column(name = "serum_creatinine_unit")
    private String serumCreatinineUnit;  // "MG_DL" or "MICROMOL_L"

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "age_years")
    private Integer ageYears;

    @Column(name = "egfr")
    private Double egfr;

    @Column(name = "ckdepi_egfr")
    private Double ckdEpiEgfr;

    @Column(name = "egfr_formula_used")
    private String egfrFormulaUsed;  // "CKD_EPI_2021" or "COCKCROFT_GAULT"

    @Column(name = "previous_egfr")
    private Double previousEgfr;

    @Column(name = "egfr_change")
    private Double egfrChange;

    @Column(name = "egfr_change_percent")
    private Double egfrChangePercent;

    @Column(name = "egfr_trend")
    private String egfrTrend;  // "STABLE", "IMPROVING", "DECLINING", "RAPIDLY_DECLINING"

    @Column(name = "egfr_quality_indicator")
    private String egfrQualityIndicator;  // "HIGH_QUALITY", "MEDIUM_QUALITY", "LOW_QUALITY"

    @Column(name = "egfr_last_updated_at")
    private LocalDateTime egfrLastUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ckd_stage")
    private CkdStage ckdStage;

    @Column(name = "alert_low_egfr")
    private Boolean alertLowEgfr;

    @Column(name = "alert_rapid_decline")
    private Boolean alertRapidDecline;

    @Column(name = "alert_message", columnDefinition = "text")
    private String alertMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
