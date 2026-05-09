package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Trend Analysis Service - Analyzes eGFR trends and changes
 * 
 * Detects:
 * - Rapid decline (> 20% decrease)
 * - Moderate decline (5-20% decrease)
 * - Stability (-5% to +5%)
 * - Improvement (> 5% increase)
 * - Rapid improvement (> 20% increase - may indicate lab error)
 * 
 * @author Clinical Service Team
 */
@Slf4j
@Service
public class TrendAnalysisService {

    private static final double RAPID_DECLINE_THRESHOLD = -20.0;
    private static final double DECLINE_THRESHOLD = -5.0;
    private static final double STABILITY_LOWER = -5.0;
    private static final double STABILITY_UPPER = 5.0;
    private static final double IMPROVEMENT_THRESHOLD = 5.0;
    private static final double RAPID_IMPROVEMENT_THRESHOLD = 20.0;

    /**
     * Analyze eGFR trend between current and previous values
     * 
     * @param currentEgfr Current eGFR value (mL/min/1.73m²)
     * @param previousEgfr Previous eGFR value (mL/min/1.73m²)
     * @return TrendData with analysis results
     */
    public TrendData analyzeTrend(double currentEgfr, Double previousEgfr) {
        if (previousEgfr == null || previousEgfr <= 0) {
            log.debug("No previous eGFR available for trend analysis");
            return TrendData.builder()
                    .currentEgfr(currentEgfr)
                    .previousEgfr(null)
                    .absoluteChange(null)
                    .percentChange(null)
                    .trendStatus(EgfrTrend.NO_PREVIOUS_DATA)
                    .isAbnormal(false)
                    .requiresAlert(false)
                    .recommendation("Baseline established. Monitor at next visit.")
                    .build();
        }

        double absoluteChange = currentEgfr - previousEgfr;
        double percentChange = (absoluteChange / previousEgfr) * 100.0;

        EgfrTrend trend = determineTrend(percentChange);
        boolean isAbnormal = isAbnormalTrend(trend);
        boolean requiresAlert = requiresAlert(trend, absoluteChange, percentChange);

        String recommendation = generateRecommendation(trend, percentChange, absoluteChange);

        log.info("Trend Analysis - Previous: {} → Current: {} | Change: {:.1f}% | Trend: {}", 
                previousEgfr, currentEgfr, percentChange, trend);

        return TrendData.builder()
                .currentEgfr(currentEgfr)
                .previousEgfr(previousEgfr)
                .absoluteChange(Math.round(absoluteChange * 10.0) / 10.0)
                .percentChange(Math.round(percentChange * 10.0) / 10.0)
                .trendStatus(trend)
                .isAbnormal(isAbnormal)
                .requiresAlert(requiresAlert)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Determine trend status based on percent change
     */
    private EgfrTrend determineTrend(double percentChange) {
        if (percentChange < RAPID_DECLINE_THRESHOLD) {
            return EgfrTrend.RAPID_DECLINE;
        } else if (percentChange < DECLINE_THRESHOLD) {
            return EgfrTrend.DECLINING;
        } else if (percentChange >= STABILITY_LOWER && percentChange <= STABILITY_UPPER) {
            return EgfrTrend.STABLE;
        } else if (percentChange <= IMPROVEMENT_THRESHOLD) {
            return EgfrTrend.IMPROVING;
        } else if (percentChange >= RAPID_IMPROVEMENT_THRESHOLD) {
            return EgfrTrend.RAPID_IMPROVEMENT;
        } else {
            return EgfrTrend.STABLE;
        }
    }

    /**
     * Check if trend is abnormal (requires clinical attention)
     */
    private boolean isAbnormalTrend(EgfrTrend trend) {
        return trend == EgfrTrend.RAPID_DECLINE || 
               trend == EgfrTrend.RAPID_IMPROVEMENT;
    }

    /**
     * Check if trend requires alert/notification
     */
    private boolean requiresAlert(EgfrTrend trend, double absoluteChange, double percentChange) {
        return trend == EgfrTrend.RAPID_DECLINE ||
               trend == EgfrTrend.RAPID_IMPROVEMENT ||
               percentChange < DECLINE_THRESHOLD;
    }

    /**
     * Generate clinical recommendation based on trend
     */
    private String generateRecommendation(EgfrTrend trend, double percentChange, double absoluteChange) {
        return switch (trend) {
            case RAPID_DECLINE -> 
                    String.format("⚠️ ALERT: Rapid eGFR decline (%.1f%%). Recommend urgent nephrology review. " +
                                  "Consider checking for AKI, medication effects, or lab error.",
                                  percentChange);
            
            case DECLINING ->
                    String.format("eGFR declining (%.1f%%). Schedule nephrology follow-up. " +
                                  "Review medication list and blood pressure control.",
                                  percentChange);
            
            case STABLE ->
                    "eGFR stable. Continue current management. Routine follow-up scheduled.";
            
            case IMPROVING ->
                    String.format("eGFR improving (%.1f%%). Positive response to treatment. " +
                                  "Continue current regimen.",
                                  percentChange);
            
            case RAPID_IMPROVEMENT ->
                    String.format("Rapid eGFR improvement (%.1f%%). Verify lab accuracy. " +
                                  "Previous result may have been erroneous.",
                                  percentChange);
            
            case NO_PREVIOUS_DATA ->
                    "Baseline eGFR established. Monitor at next visit.";
        };
    }

    /**
     * Calculate estimated time to CKD Stage 5 (end-stage renal disease)
     * if current decline rate continues
     */
    public Integer estimateTimeToESRDMonths(double currentEgfr, Double previousEgfr, 
                                           long monthsSincePrevious) {
        if (previousEgfr == null || previousEgfr <= 0 || monthsSincePrevious <= 0) {
            return null;
        }

        if (currentEgfr >= 15) {
            // Not at ESRD yet
            double eGFRDeclinePerMonth = (currentEgfr - previousEgfr) / monthsSincePrevious;
            
            if (eGFRDeclinePerMonth >= 0) {
                // Stable or improving - no ESRD timeline
                return null;
            }

            double monthsToESRD = (15.0 - currentEgfr) / Math.abs(eGFRDeclinePerMonth);
            return (int) Math.round(monthsToESRD);
        } else {
            // Already at ESRD
            return 0;
        }
    }

    // ============================================================================
    // Data Classes
    // ============================================================================

    @Getter
    @Builder
    public static class TrendData {
        private Double currentEgfr;
        private Double previousEgfr;
        private Double absoluteChange;      // current - previous
        private Double percentChange;       // (change / previous) × 100
        private EgfrTrend trendStatus;      // STABLE, DECLINING, RAPID_DECLINE, etc
        private boolean isAbnormal;         // Requires clinical attention
        private boolean requiresAlert;      // Should notify doctor
        private String recommendation;      // Clinical action recommended

        /**
         * Get severity level (1-5) for UI color coding
         */
        public int getSeverityLevel() {
            return switch (trendStatus) {
                case RAPID_DECLINE -> 5;    // Red
                case DECLINING -> 4;        // Orange
                case NO_PREVIOUS_DATA, STABLE -> 3;  // Yellow/Blue
                case IMPROVING -> 2;        // Green
                case RAPID_IMPROVEMENT -> 4; // Orange (verify)
            };
        }

        /**
         * Get CSS class for Bootstrap styling
         */
        public String getCssClass() {
            return switch (trendStatus) {
                case RAPID_DECLINE -> "danger";
                case DECLINING -> "warning";
                case STABLE -> "info";
                case IMPROVING -> "success";
                case RAPID_IMPROVEMENT -> "warning";
                case NO_PREVIOUS_DATA -> "secondary";
            };
        }

        /**
         * Get icon emoji for UI display
         */
        public String getIcon() {
            return switch (trendStatus) {
                case RAPID_DECLINE -> "📉"; // Red down arrow
                case DECLINING -> "↘️";     // Down arrow
                case STABLE -> "➡️";       // Right arrow
                case IMPROVING -> "↗️";    // Up arrow
                case RAPID_IMPROVEMENT -> "📈"; // Up arrow
                case NO_PREVIOUS_DATA -> "❓";   // Question mark
            };
        }
    }

    /**
     * eGFR Trend Status Enum
     */
    public enum EgfrTrend {
        NO_PREVIOUS_DATA("No previous eGFR available"),
        STABLE("eGFR stable (-5% to +5%)"),
        DECLINING("eGFR declining (-5% to -20%)"),
        RAPID_DECLINE("eGFR rapidly declining (< -20%)"),
        IMPROVING("eGFR improving (+5%)"),
        RAPID_IMPROVEMENT("eGFR rapidly improving (> +20%)");

        private final String description;

        EgfrTrend(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
