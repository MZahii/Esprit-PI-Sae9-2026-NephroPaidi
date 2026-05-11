package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.ai.dto.AiLabAnalysisResponse;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationMetricsRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationMetricsResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consultation Metrics Service - European CKD-EPI Formula
 * 
 * Enhanced with:
 * - CKD-EPI 2021 formula (European standard)
 * - SI units support (µmol/L for serum creatinine)
 * - Unit conversion (mg/dL → µmol/L)
 * - Trend analysis (current vs previous eGFR)
 * - Quality scoring
 * - Comprehensive audit trail
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationMetricsService {

    private final ConsultationMetricsRepository metricsRepository;
    private final ConsultationRepository consultationRepository;
    private final CKDEPICalculationService ckdEpiCalculator;
    private final PediatricEgfrCalculationService pediatricEgfrCalculator;
    private final CKDStageResolver stageResolver;
    private final TrendAnalysisService trendAnalyzer;
    private final LabUnitConversionService unitConverter;

    public ConsultationMetricsResponse upsert(UUID consultationId, UUID doctorId, ConsultationMetricsRequest request) {
        Consultation consultation = requireConsultation(consultationId, doctorId);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        ConsultationMetrics metrics = metricsRepository.findByConsultationId(consultationId)
                .orElseGet(() -> ConsultationMetrics.builder()
                        .consultationId(consultationId)
                        .patientId(consultation.getPatientId())
                        .build());

        try {
            metrics.setPatientId(consultation.getPatientId());
            metrics.setHeightCm(request.getHeightCm());
            metrics.setWeightKg(request.getWeightKg());
            metrics.setAgeYears(request.getAgeYears());
            metrics.setPatientSex(request.getSex());
            metrics.setSystolicBpMmHg(request.getSystolicBpMmHg());
            metrics.setDiastolicBpMmHg(request.getDiastolicBpMmHg());
            metrics.setHeartRateBpm(request.getHeartRateBpm());
            metrics.setRespiratoryRateBpm(request.getRespiratoryRateBpm());
            metrics.setTemperatureC(request.getTemperatureC());
            metrics.setOxygenSaturationPct(request.getOxygenSaturationPct());

            // ============================================================
            // Pediatric-first eGFR calculation
            // - Schwartz for pediatric patients (<18)
            // - CKD-EPI 2021 for adult patients
            // ============================================================

            // Step 1: Convert serum creatinine to SI units (µmol/L)
            Double creatinineMicromolPerL = null;
            if (request.getCreatinineMgDl() != null && request.getCreatinineMgDl() > 0) {
                // Auto-detect and convert to SI units
                creatinineMicromolPerL = unitConverter.convertToMicromolPerL(request.getCreatinineMgDl(), null);
                metrics.setCreatinineMgDl(request.getCreatinineMgDl());
                metrics.setCreatinineUmol(creatinineMicromolPerL);
                metrics.setSerumCreatinineUnit("MICROMOL_L");
                log.debug("Converted creatinine to SI units: {} µmol/L", creatinineMicromolPerL);
            }

            // Step 2: Calculate eGFR using CKD-EPI formula
            Double egfr = null;
            String qualityIndicator = "LOW_QUALITY";

            boolean hasEnoughPatientContext = creatinineMicromolPerL != null
                    && metrics.getAgeYears() != null
                    && metrics.getAgeYears() > 0
                    && (metrics.getAgeYears() < 18 || request.getSex() != null);

            if (hasEnoughPatientContext) {

                // Check serum creatinine quality
                CKDEPICalculationService.QualityFlag scrQuality = 
                        ckdEpiCalculator.checkSerumCreatinineQuality(creatinineMicromolPerL);

                boolean pediatricCase = metrics.getAgeYears() < 18;
                String formulaUsed = pediatricCase ? "SCHWARTZ_BEDSIDE" : "CKD_EPI_2021";

                if (pediatricCase) {
                    egfr = pediatricEgfrCalculator.calculateEgfr(
                            metrics.getHeightCm(),
                            metrics.getCreatinineMgDl(),
                            metrics.getAgeYears()
                    );
                    metrics.setCkdEpiEgfr(null);
                } else {
                    egfr = ckdEpiCalculator.calculateEgfr(
                            creatinineMicromolPerL,
                            metrics.getAgeYears(),
                            request.getSex()
                    );
                    metrics.setCkdEpiEgfr(egfr);
                }

                if (egfr != null) {
                    metrics.setEgfr(egfr);
                    metrics.setEgfrFormulaUsed(formulaUsed);

                    // Determine CKD stage using CKDStageResolver
                    CKDStageResolver.CKDStage ckdStageResolver = stageResolver.resolveCKDStage(egfr);
                    // Convert CKDStageResolver.CKDStage to CkdStage entity enum
                    CkdStage ckdStageEntity = convertToCkdStageEnum(ckdStageResolver);
                    metrics.setCkdStage(ckdStageEntity);

                    log.info("{} calculated: eGFR={} mL/min/1.73m², Stage={}", formulaUsed, egfr, ckdStageEntity);
                }

                // Step 3: Analyze trend vs previous eGFR
                ConsultationMetrics previous = metricsRepository
                        .findTopByPatientIdAndConsultationIdNotOrderByCreatedAtDesc(
                                consultation.getPatientId(), consultationId)
                        .orElse(null);

                TrendAnalysisService.TrendData trendData = null;
                if (previous != null && previous.getEgfr() != null) {
                    trendData = trendAnalyzer.analyzeTrend(egfr, previous.getEgfr());
                    metrics.setPreviousEgfr(previous.getEgfr());
                    metrics.setEgfrChange(trendData.getAbsoluteChange());
                    metrics.setEgfrChangePercent(trendData.getPercentChange());
                    metrics.setEgfrTrend(trendData.getTrendStatus().name());

                    log.info("Trend analysis: {} (change: {}%)", 
                            trendData.getTrendStatus(), trendData.getPercentChange());
                }

                // Step 4: Calculate quality indicator
                qualityIndicator = calculateQualityIndicator(
                        creatinineMicromolPerL != null,
                        metrics.getAgeYears() != null,
                        request.getSex() != null,
                        metrics.getHeightCm() != null,
                        pediatricCase,
                        previous != null && previous.getEgfr() != null,
                        scrQuality
                );
                metrics.setEgfrQualityIndicator(qualityIndicator);

                // Step 5: Set alerts and recommendations
                boolean lowEgfr = egfr != null && egfr < 60.0;
                boolean rapidDecline = trendData != null && trendData.isAbnormal();

                metrics.setAlertLowEgfr(lowEgfr);
                metrics.setAlertRapidDecline(rapidDecline);
                metrics.setAlertMessage(buildAlertMessage(lowEgfr, rapidDecline, egfr, trendData, metrics.getEgfrFormulaUsed()));
                metrics.setEgfrLastUpdatedAt(LocalDateTime.now());
            }

            ConsultationMetrics saved = metricsRepository.save(metrics);
            return toResponse(saved);

        } catch (Exception e) {
            log.error("Error calculating metrics for consultation {}: {}", consultationId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error calculating metrics: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ConsultationMetricsResponse get(UUID consultationId, UUID doctorId) {
        Consultation consultation = requireConsultation(consultationId, doctorId);
        ConsultationMetrics metrics = metricsRepository.findByConsultationId(consultation.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Metrics not found"));
        return toResponse(metrics);
    }

    public ConsultationMetrics applyAiLabAnalysis(UUID consultationId, Long patientId, String sourceFileName,
                                                  AiLabAnalysisResponse aiResponse) {
        ConsultationMetrics metrics = metricsRepository.findByConsultationId(consultationId)
                .orElseGet(() -> ConsultationMetrics.builder()
                        .consultationId(consultationId)
                        .patientId(patientId)
                        .build());

        metrics.setPatientId(patientId);
        if (aiResponse.getExtractedCreatinineMgDl() != null) {
            metrics.setCreatinineMgDl(aiResponse.getExtractedCreatinineMgDl());
        }
        if (aiResponse.getExtractedCreatinineUmolL() != null) {
            metrics.setCreatinineUmol(aiResponse.getExtractedCreatinineUmolL());
            metrics.setSerumCreatinineUnit("MICROMOL_L");
        }

        metrics.setAiRecommendation(aiResponse.getRecommendation());
        metrics.setAiConfidence(aiResponse.getConfidence());
        metrics.setAiSummary(aiResponse.getSummary());
        metrics.setAiRequiresReview(Boolean.TRUE.equals(aiResponse.getRequiresDoctorReview()));
        metrics.setAiSourceFileName(sourceFileName);
        metrics.setAiUpdatedAt(LocalDateTime.now());

        if (metrics.getAgeYears() != null && metrics.getCreatinineMgDl() != null) {
            boolean pediatricCase = metrics.getAgeYears() < 18;
            Double egfr = pediatricCase
                    ? pediatricEgfrCalculator.calculateEgfr(metrics.getHeightCm(), metrics.getCreatinineMgDl(), metrics.getAgeYears())
                    : ckdEpiCalculator.calculateEgfr(metrics.getCreatinineMgDl(), metrics.getAgeYears(), metrics.getPatientSex(), true);

            if (egfr != null) {
                metrics.setEgfr(egfr);
                metrics.setEgfrFormulaUsed(pediatricCase ? "SCHWARTZ_BEDSIDE" : "CKD_EPI_2021");
                if (!pediatricCase) {
                    metrics.setCkdEpiEgfr(egfr);
                }
                metrics.setCkdStage(convertToCkdStageEnum(stageResolver.resolveCKDStage(egfr)));
                metrics.setEgfrQualityIndicator(pediatricCase
                        ? (metrics.getHeightCm() != null ? "HIGH_QUALITY" : "MEDIUM_QUALITY")
                        : "MEDIUM_QUALITY");
                metrics.setAlertLowEgfr(egfr < 60.0);
                metrics.setEgfrLastUpdatedAt(LocalDateTime.now());
                metrics.setAlertMessage(buildAlertMessage(
                        egfr < 60.0,
                        false,
                        egfr,
                        null,
                        metrics.getEgfrFormulaUsed()
                ));
            }
        }

        return metricsRepository.save(metrics);
    }

    private Consultation requireConsultation(UUID consultationId, UUID doctorId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        if (!consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }
        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }
        return consultation;
    }

    private String buildAlertMessage(boolean lowEgfr, boolean rapidDecline, Double egfr,
                                     TrendAnalysisService.TrendData trendData, String formulaUsed) {
        StringBuilder msg = new StringBuilder();
        if (formulaUsed != null) {
            msg.append("Formula: ").append(formulaUsed.replace('_', ' ')).append(". ");
        }

        if (lowEgfr && egfr != null) {
            msg.append("⚠️ Low eGFR (").append(String.format("%.1f", egfr))
                    .append(" mL/min/1.73m²). Possible kidney insufficiency. ");
        }

        if (rapidDecline && trendData != null) {
            msg.append("⚠️ Rapid kidney function decline (")
                    .append(String.format("%.1f", Math.abs(trendData.getPercentChange())))
                    .append("% change since last consultation). Consider nephrology referral. ");
        }

        if (!lowEgfr && !rapidDecline) {
            msg.append("✓ Normal kidney function at this time.");
        }

        return msg.toString();
    }

    private String calculateQualityIndicator(
            boolean hasCreatinine,
            boolean hasAge,
            boolean hasSex,
            boolean hasHeight,
            boolean pediatricCase,
            boolean hasPrevious,
            CKDEPICalculationService.QualityFlag scrQuality) {

        int score = 0;
        if (hasCreatinine) score += 25;
        if (hasAge) score += 25;
        if (hasSex) score += pediatricCase ? 10 : 25;
        if (hasHeight) score += pediatricCase ? 15 : 0;
        if (hasPrevious) score += 15;
        if (scrQuality == CKDEPICalculationService.QualityFlag.NORMAL) score += 10;

        if (score >= 90) return "HIGH_QUALITY";
        if (score >= 70) return "MEDIUM_QUALITY";
        return "LOW_QUALITY";
    }

    /**
     * Convert CKDStageResolver.CKDStage to CkdStage entity enum
     */
    private CkdStage convertToCkdStageEnum(CKDStageResolver.CKDStage stage) {
        if (stage == null) return null;
        return switch (stage) {
            case STAGE_1, STAGE_2 -> CkdStage.G1;  // G1/G2 for stages 1-2
            case STAGE_3A -> CkdStage.G3A;
            case STAGE_3B -> CkdStage.G3B;
            case STAGE_4 -> CkdStage.G4;
            case STAGE_5 -> CkdStage.G5;
        };
    }

    private ConsultationMetricsResponse toResponse(ConsultationMetrics metrics) {
        return ConsultationMetricsResponse.builder()
                .id(metrics.getId())
                .consultationId(metrics.getConsultationId())
                .patientId(metrics.getPatientId())
                
                // Biometric data
                .heightCm(metrics.getHeightCm())
                .weightKg(metrics.getWeightKg())
                .ageYears(metrics.getAgeYears())
                .sex(metrics.getPatientSex())
                .systolicBpMmHg(metrics.getSystolicBpMmHg())
                .diastolicBpMmHg(metrics.getDiastolicBpMmHg())
                .heartRateBpm(metrics.getHeartRateBpm())
                .respiratoryRateBpm(metrics.getRespiratoryRateBpm())
                .temperatureC(metrics.getTemperatureC())
                .oxygenSaturationPct(metrics.getOxygenSaturationPct())
                
                // Serum creatinine (both units)
                .creatinineMgDl(metrics.getCreatinineMgDl())
                .creatinineUmol(metrics.getCreatinineUmol())
                .serumCreatinineUnit(metrics.getSerumCreatinineUnit())
                
                // eGFR calculations
                .egfr(metrics.getEgfr())
                .ckdEpiEgfr(metrics.getCkdEpiEgfr())
                .egfrFormulaUsed(metrics.getEgfrFormulaUsed())
                .egfrQualityIndicator(metrics.getEgfrQualityIndicator())
                
                // Trend analysis
                .previousEgfr(metrics.getPreviousEgfr())
                .egfrChange(metrics.getEgfrChange())
                .egfrChangePercent(metrics.getEgfrChangePercent())
                .egfrTrend(metrics.getEgfrTrend())
                .egfrLastUpdatedAt(metrics.getEgfrLastUpdatedAt())
                
                // CKD stage and alerts
                .ckdStage(metrics.getCkdStage() != null ? metrics.getCkdStage().name() : null)
                .alertLowEgfr(metrics.getAlertLowEgfr())
                .alertRapidDecline(metrics.getAlertRapidDecline())
                .alertMessage(metrics.getAlertMessage())

                // AI recommendation summary
                .aiRecommendation(metrics.getAiRecommendation())
                .aiConfidence(metrics.getAiConfidence())
                .aiSummary(metrics.getAiSummary())
                .aiRequiresReview(metrics.getAiRequiresReview())
                .aiSourceFileName(metrics.getAiSourceFileName())
                .aiUpdatedAt(metrics.getAiUpdatedAt())
                
                // Timestamps
                .createdAt(metrics.getCreatedAt())
                .updatedAt(metrics.getUpdatedAt())
                .build();
    }
}
