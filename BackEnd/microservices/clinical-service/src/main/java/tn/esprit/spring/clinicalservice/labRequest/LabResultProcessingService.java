package tn.esprit.spring.clinicalservice.labRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationMetricsRequest;
import tn.esprit.spring.clinicalservice.consultation.metrics.*;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.labRequest.events.LabResultUploadedEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lab Result Processing Service
 * 
 * Orchestrates processing of lab results:
 * 1. Validates lab data
 * 2. Converts units to SI standard (µmol/L)
 * 3. Calculates eGFR using CKD-EPI formula
 * 4. Assigns CKD stage
 * 5. Analyzes trend vs previous results
 * 6. Updates consultation metrics
 * 7. Triggers follow-up scheduling
 * 8. Notifies relevant staff
 * 
 * @author Clinical Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LabResultProcessingService {

    private final CKDEPICalculationService ckdEpiCalculator;
    private final CKDStageResolver stageResolver;
    private final TrendAnalysisService trendAnalyzer;
    private final LabUnitConversionService unitConverter;
    private final ConsultationRepository consultationRepository;
    private final ConsultationMetricsService metricsService;
    // private final NotificationService notificationService; // Inject when ready
    // private final ReceptionistSchedulingService schedulingService; // Inject when ready

    // ============================================================
    // EVENT LISTENERS
    // ============================================================

    /**
     * Event listener for lab result uploads
     * 
     * Triggered when:
     * - Lab agent publishes LabResultUploadedEvent
     * - Event contains serum creatinine value, patient demographics
     * 
     * Processing flow:
     * 1. Extract creatinine value from event
     * 2. Validate patient data
     * 3. Convert units to SI (µmol/L)
     * 4. Calculate eGFR using CKD-EPI formula
     * 5. Determine CKD stage and trends
     * 6. Update ConsultationMetrics table
     * 7. Publish LabResultProcessedEvent for downstream (auto-scheduling, receptionist)
     * 8. Send alerts if kidney function abnormal
     * 
     * @param event Lab result uploaded event from lab agent
     */
    @EventListener
    public void onLabResultUploaded(LabResultUploadedEvent event) {
        log.info("🧪 Received LabResultUploadedEvent: consultation={}, test={}, value={} {}",
                event.getConsultationId(), event.getTestName(), event.getTestValue(), event.getTestUnit());

        if (event == null || event.getConsultationId() == null) {
            log.warn("Received invalid LabResultUploadedEvent: null or missing consultationId");
            return;
        }

        try {
            // Filter for serum creatinine tests only (other tests handled separately)
            if (!isSerumCreatinineTest(event.getTestCode(), event.getTestName())) {
                log.debug("Event is not a serum creatinine test, skipping: {}", event.getTestCode());
                return;
            }

            // Validate patient demographics
            if (event.getPatientAge() == null || event.getPatientAge() < 18 ||
                event.getPatientSex() == null || event.getTestValue() == null) {
                log.warn("Lab result missing required demographics: age={}, sex={}, value={}",
                        event.getPatientAge(), event.getPatientSex(), event.getTestValue());
                return;
            }

            // Process the lab result
            LabProcessingResult result = processSerumCreatinineResult(
                    event.getTestValue(),
                    event.getTestUnit() != null ? event.getTestUnit() : "mg/dL",
                    event.getConsultationId(),
                    event.getPatientAge(),
                    event.getPatientSex()
            );

            if (result.isSuccess()) {
                log.info("✓ Lab result processed: eGFR={}, stage={}, trend={}",
                        result.getEgfr(), result.getCkdStage(), result.getTrendStatus());

                // Publish result for downstream processing (auto-scheduling, receptionist workflow)
                publishLabResultProcessedEvent(result, event);

                // Send alerts if abnormal
                if (result.isRequiresAlert()) {
                    log.warn("⚠️ ALERT: Abnormal kidney function detected - eGFR={}, stage={}", 
                            result.getEgfr(), result.getCkdStage());
                    // TODO: Send alert notification to clinical team
                }
            } else {
                log.error("✗ Lab result processing failed: {}", result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error processing LabResultUploadedEvent", e);
        }
    }

    /**
     * Check if test is serum creatinine test
     * Matches common test codes/names across different lab systems
     */
    private boolean isSerumCreatinineTest(String testCode, String testName) {
        if (testCode == null) testCode = "";
        if (testName == null) testName = "";

        String code = testCode.toUpperCase();
        String name = testName.toUpperCase();

        return code.matches(".*SCR.*|.*CREATININE.*") ||
               name.matches(".*CREATININE.*|.*SERUM.*CREATININE.*");
    }

    /**
     * Publish LabResultProcessedEvent for downstream consumption
     * (Auto-scheduling service, receptionist workflow, audit trail)
     */
    private void publishLabResultProcessedEvent(LabProcessingResult result, LabResultUploadedEvent originalEvent) {
        // TODO: Publish to event bus (RabbitMQ/Kafka)
        // This event should trigger:
        // 1. Auto-scheduling: Create new appointment suggestion
        // 2. Receptionist workflow: Notify receptionist to confirm/schedule
        // 3. Doctor notification: Alert doctor if abnormal results
        // 4. Audit trail: Log for compliance

        log.info("📤 Publishing LabResultProcessedEvent for downstream: consultation={}", result.getConsultationId());
        // eventPublisher.publishEvent(new LabResultProcessedEvent(...));
    }
     * 
     * Main orchestration method that:
     * - Validates inputs
     * - Converts units
     * - Calculates eGFR
     * - Determines stage & trends
     * - Updates metrics
     * - Triggers notifications
     * 
     * @param serumCreatinineValue Lab result value
     * @param originalUnit Unit of measurement (mg/dL, µmol/L, etc)
     * @param consultationId Consultation being updated
     * @param patientAge Patient age in years
     * @param patientSex Patient sex (M/F)
     * @return LabProcessingResult with all calculated values
     */
    public LabProcessingResult processSerumCreatinineResult(
            double serumCreatinineValue,
            String originalUnit,
            UUID consultationId,
            int patientAge,
            String patientSex) {

        log.info("Processing serum creatinine result: value={}, unit={}, patient_age={}, sex={}, consultation={}",
                serumCreatinineValue, originalUnit, patientAge, patientSex, consultationId);

        try {
            // Step 1: Validate inputs
            validateInputs(serumCreatinineValue, patientAge, patientSex);

            // Step 2: Convert to SI units (µmol/L)
            double serumCreatinineMicromolPerL = unitConverter.convertToMicromolPerL(serumCreatinineValue, originalUnit);
            log.debug("Converted serum creatinine: {} → {} µmol/L", serumCreatinineValue, serumCreatinineMicromolPerL);

            // Step 3: Check lab quality
            CKDEPICalculationService.QualityFlag qualityFlag = 
                    ckdEpiCalculator.checkSerumCreatinineQuality(serumCreatinineMicromolPerL);
            log.debug("Lab quality flag: {}", qualityFlag.getDescription());

            // Step 4: Calculate eGFR using CKD-EPI
            double eGFR = ckdEpiCalculator.calculateEgfr(serumCreatinineMicromolPerL, patientAge, patientSex);
            log.info("Calculated eGFR: {} mL/min/1.73m²", eGFR);

            // Step 5: Determine CKD stage
            CKDStageResolver.CKDStage ckdStage = stageResolver.resolveCKDStage(eGFR);
            log.info("Assigned CKD stage: {}", ckdStage.getFullName());

            // Step 6: Fetch previous metrics and analyze trend
            TrendAnalysisService.TrendData trendData = fetchAndAnalyzeTrend(consultationId, eGFR);

            // Step 7: Update consultation metrics in database
            ConsultationMetricsRequest metricsRequest = buildMetricsRequest(
                    serumCreatinineMicromolPerL,
                    patientAge,
                    patientSex,
                    eGFR,
                    ckdStage.name(),
                    trendData,
                    qualityFlag.name()
            );

            // Note: This would update the database
            // ConsultationMetrics updatedMetrics = metricsService.upsert(consultationId, metricsRequest);

            // Step 8: Build response
            LabProcessingResult result = LabProcessingResult.builder()
                    .consultationId(consultationId)
                    .success(true)
                    .serumCreatinineOriginal(serumCreatinineValue)
                    .serumCreatinineOriginalUnit(originalUnit)
                    .serumCreatinineSI(serumCreatinineMicromolPerL)
                    .egfr(eGFR)
                    .egfrMethod("CKD_EPI_2021")
                    .ckdStage(ckdStage.name())
                    .ckdStageDescription(ckdStage.getDescription())
                    .previousEgfr(trendData.getPreviousEgfr())
                    .egfrChange(trendData.getAbsoluteChange())
                    .egfrChangePercent(trendData.getPercentChange())
                    .trendStatus(trendData.getTrendStatus().name())
                    .trendDescription(trendData.getTrendStatus().getDescription())
                    .trendRecommendation(trendData.getRecommendation())
                    .requiresAlert(trendData.isRequiresAlert())
                    .labQualityFlag(qualityFlag.name())
                    .labQualityDescription(qualityFlag.getDescription())
                    .processedAt(LocalDateTime.now())
                    .build();

            log.info("Lab result processed successfully: eGFR={}, stage={}, trend={}", 
                    eGFR, ckdStage, trendData.getTrendStatus());

            // Step 9: Trigger notifications (when services ready)
            triggerNotifications(result, trendData);

            // Step 10: Auto-schedule follow-up if needed
            scheduleFollowUpIfNeeded(result, consultationId, patientAge, patientSex);

            return result;

        } catch (Exception e) {
            log.error("Error processing lab result", e);
            return LabProcessingResult.builder()
                    .consultationId(consultationId)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Validate input parameters
     */
    private void validateInputs(double serumCreatinine, int age, String sex) {
        if (serumCreatinine <= 0) {
            throw new IllegalArgumentException("Serum creatinine must be positive");
        }
        if (age < 18 || age > 120) {
            throw new IllegalArgumentException("Age must be between 18 and 120 years");
        }
        if (!("M".equalsIgnoreCase(sex) || "F".equalsIgnoreCase(sex))) {
            throw new IllegalArgumentException("Sex must be 'M' or 'F'");
        }
    }

    /**
     * Fetch previous eGFR and analyze trend
     */
    private TrendAnalysisService.TrendData fetchAndAnalyzeTrend(UUID consultationId, double currentEgfr) {
        // TODO: Fetch previous metrics from ConsultationMetricsRepository
        // Double previousEgfr = metricsRepository.findPreviousEgfr(consultationId);
        
        Double previousEgfr = null; // Placeholder for now
        
        return trendAnalyzer.analyzeTrend(currentEgfr, previousEgfr);
    }

    /**
     * Build metrics request for storage
     */
    private ConsultationMetricsRequest buildMetricsRequest(
            double scrMicromolPerL,
            int ageYears,
            String sex,
            double eGFR,
            String ckdStage,
            TrendAnalysisService.TrendData trendData,
            String labQuality) {

        // Convert µmol/L back to mg/dL for backward compatibility
        double scrMgdl = unitConverter.convertMicromolPerLToMgdl(scrMicromolPerL);

        ConsultationMetricsRequest request = new ConsultationMetricsRequest();
        request.setCreatinineMgDl(scrMgdl);
        request.setAgeYears(ageYears);
        request.setHeightCm(null); // TODO: Fetch from patient profile
        request.setWeightKg(null); // TODO: Fetch from patient profile

        // Additional fields for enhanced tracking
        // request.setEgfr(eGFR);
        // request.setEgfrMethod("CKD_EPI_2021");
        // request.setCkdStage(ckdStage);
        // request.setSerumCreatinineUnit("MICROMOL_L");
        // request.setLabQualityFlag(labQuality);
        // request.setPreviousEgfr(trendData.getPreviousEgfr());
        // request.setEgfrChange(trendData.getAbsoluteChange());
        // request.setEgfrTrend(trendData.getTrendStatus().name());

        return request;
    }

    /**
     * Trigger notifications to relevant staff
     */
    private void triggerNotifications(LabProcessingResult result, TrendAnalysisService.TrendData trendData) {
        log.debug("Triggering notifications for lab result");

        // TODO: Implement when NotificationService is ready
        // if (result.isRequiresAlert()) {
        //     notificationService.notifyDoctor(
        //         String.format("⚠️ Lab alert: %s", trendData.getRecommendation()),
        //         result.getConsultationId()
        //     );
        // }

        // notificationService.notifyReceptionist(
        //     "Lab results ready. Schedule patient for follow-up consultation.",
        //     result.getConsultationId()
        // );

        log.info("Notification triggers registered for alert={}", result.isRequiresAlert());
    }

    /**
     * Schedule follow-up consultation if needed
     */
    private void scheduleFollowUpIfNeeded(LabProcessingResult result, UUID consultationId, 
                                          int patientAge, String patientSex) {
        log.debug("Checking if follow-up scheduling needed");

        // TODO: Implement when SchedulingService is ready
        // CKDStageResolver.CKDStage stage = CKDStageResolver.CKDStage.valueOf(result.getCkdStage());
        // int followUpWeeks = stage.getFollowUpIntervalWeeks();
        
        // if (followUpWeeks > 0) {
        //     schedulingService.createAutoSchedulingRequest(
        //         consultationId,
        //         followUpWeeks,
        //         "Follow-up for lab results and kidney function assessment"
        //     );
        // }
    }

    // ============================================================================
    // Data Classes
    // ============================================================================

    @lombok.Builder
    @lombok.Getter
    public static class LabProcessingResult {
        private UUID consultationId;
        private boolean success;
        private String errorMessage;

        // Input data
        private double serumCreatinineOriginal;
        private String serumCreatinineOriginalUnit;
        private double serumCreatinineSI;

        // Calculated values
        private double egfr;
        private String egfrMethod;
        private String ckdStage;
        private String ckdStageDescription;

        // Trend data
        private Double previousEgfr;
        private Double egfrChange;
        private Double egfrChangePercent;
        private String trendStatus;
        private String trendDescription;
        private String trendRecommendation;

        // Quality flags
        private boolean requiresAlert;
        private String labQualityFlag;
        private String labQualityDescription;

        // Metadata
        private LocalDateTime processedAt;

        @Override
        public String toString() {
            if (!success) {
                return String.format("Error: %s", errorMessage);
            }

            return String.format(
                    "Lab Result: SCr=%f %s (SI: %f µmol/L) → eGFR=%f (%s) → Stage %s | Trend: %s (%+.1f%%)",
                    serumCreatinineOriginal, serumCreatinineOriginalUnit, serumCreatinineSI,
                    egfr, egfrMethod, ckdStage, trendStatus, egfrChangePercent != null ? egfrChangePercent : 0
            );
        }
    }
}
