package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.Getter;
import org.springframework.stereotype.Service;

/**
 * CKD Stage Resolver - Determines CKD stage based on eGFR value
 * 
 * CKD Stages per KDIGO 2021:
 * - Stage 1: eGFR > 90 mL/min/1.73m² (normal)
 * - Stage 2: eGFR 60-89 mL/min/1.73m² (mild decrease)
 * - Stage 3a: eGFR 45-59 mL/min/1.73m² (mild-moderate decrease)
 * - Stage 3b: eGFR 30-44 mL/min/1.73m² (moderate-severe decrease)
 * - Stage 4: eGFR 15-29 mL/min/1.73m² (severe decrease)
 * - Stage 5: eGFR < 15 mL/min/1.73m² (kidney failure, RRT needed)
 * 
 * @author Clinical Service Team
 */
@Service
public class CKDStageResolver {

    /**
     * Determine CKD stage based on eGFR value
     * 
     * @param eGFR eGFR value in mL/min/1.73m²
     * @return CKDStage enum with stage and clinical details
     */
    public CKDStage resolveCKDStage(double eGFR) {
        if (eGFR > 90) {
            return CKDStage.STAGE_1;
        } else if (eGFR >= 60) {
            return CKDStage.STAGE_2;
        } else if (eGFR >= 45) {
            return CKDStage.STAGE_3A;
        } else if (eGFR >= 30) {
            return CKDStage.STAGE_3B;
        } else if (eGFR >= 15) {
            return CKDStage.STAGE_4;
        } else {
            return CKDStage.STAGE_5;
        }
    }

    /**
     * Check if CKD stage indicates need for specialist referral
     */
    public boolean requiresNephrologistReferral(CKDStage stage) {
        return stage.ordinal() >= CKDStage.STAGE_3A.ordinal();
    }

    /**
     * Check if CKD stage requires dialysis consideration
     */
    public boolean dialysisRequired(CKDStage stage) {
        return stage == CKDStage.STAGE_4 || stage == CKDStage.STAGE_5;
    }

    @Getter
    public enum CKDStage {
        STAGE_1("> 90", "Normal or high", "No action needed", 1),
        STAGE_2("60-89", "Mild decrease", "Monitor kidney function, manage cardiovascular risk", 2),
        STAGE_3A("45-59", "Mild to moderate decrease", "Nephrology referral recommended", 3),
        STAGE_3B("30-44", "Moderate to severe decrease", "Nephrology referral recommended", 4),
        STAGE_4("15-29", "Severe decrease", "Nephrology referral required", 5),
        STAGE_5("< 15", "Kidney failure (RRT)", "Renal replacement therapy (dialysis/transplant)", 6);

        private final String eGFRRange;
        private final String description;
        private final String clinicalRecommendation;
        private final int severity; // 1-6, higher = more severe

        CKDStage(String eGFRRange, String description, String clinicalRecommendation, int severity) {
            this.eGFRRange = eGFRRange;
            this.description = description;
            this.clinicalRecommendation = clinicalRecommendation;
            this.severity = severity;
        }

        /**
         * Get full stage name with eGFR range
         */
        public String getFullName() {
            return String.format("CKD Stage %d: %s (eGFR %s mL/min/1.73m²)", 
                    severity, description, eGFRRange);
        }

        /**
         * Get CSS class for frontend styling (bootstrap alert/badge)
         */
        public String getCssClass() {
            return switch (this) {
                case STAGE_1, STAGE_2 -> "success";
                case STAGE_3A, STAGE_3B -> "warning";
                case STAGE_4, STAGE_5 -> "danger";
            };
        }

        /**
         * Get color code for UI display
         */
        public String getColorHex() {
            return switch (this) {
                case STAGE_1, STAGE_2 -> "#28a745"; // green
                case STAGE_3A, STAGE_3B -> "#ffc107"; // amber
                case STAGE_4, STAGE_5 -> "#dc3545"; // red
            };
        }

        /**
         * Get recommended follow-up interval in weeks
         */
        public int getFollowUpIntervalWeeks() {
            return switch (this) {
                case STAGE_1, STAGE_2 -> 52; // Annual
                case STAGE_3A, STAGE_3B -> 12; // Quarterly (3 months)
                case STAGE_4 -> 6; // Every 6 weeks
                case STAGE_5 -> 2; // Every 2 weeks or more frequent
            };
        }

        /**
         * Check if stage progression from current to new stage is concerning
         */
        public static boolean isRapidProgression(CKDStage previous, CKDStage current) {
            if (previous == null) return false;
            // Progression of 2+ stages or any progression to Stage 4/5
            int stageDifference = current.severity - previous.severity;
            return stageDifference >= 2 || current == STAGE_4 || current == STAGE_5;
        }
    }
}
