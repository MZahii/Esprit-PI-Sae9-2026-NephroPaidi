package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

            // ============================================================
            // CKD-EPI European Formula Calculation
            // ============================================================

            // Step 1: Convert serum creatinine to SI units (µmol/L)
            Double creatinineMicromolPerL = null;
            if (request.getCreatinineMgDl() != null && request.getCreatinineMgDl() > 0) {
                // Auto-detect and convert to SI units
                creatinineMicromolPerL = unitConverter.convertToMicromolPerL(request.getCreatinineMgDl(), null);
                metrics.setCreatinineMgDl(request.getCreatinineMgDl());
                metrics.setSerumCreatinineUnit("MICROMOL_L");
                log.debug("Converted creatinine to SI units: {} µmol/L", creatinineMicromolPerL);
            }

            // Step 2: Calculate eGFR using CKD-EPI formula
            Double egfr = null;
            String qualityIndicator = "LOW_QUALITY";

            if (creatinineMicromolPerL != null && 
                metrics.getAgeYears() != null && metrics.getAgeYears() > 0 &&
                request.getSex() != null) {

                // Check serum creatinine quality
                CKDEPICalculationService.QualityFlag scrQuality = 
                        ckdEpiCalculator.checkSerumCreatinineQuality(creatinineMicromolPerL);
                
                // Calculate eGFR with CKD-EPI formula
                egfr = ckdEpiCalculator.calculateEgfr(
                        creatinineMicromolPerL,
                        metrics.getAgeYears(),
                        request.getSex()
                );

                if (egfr != null) {
                    metrics.setEgfr(egfr);
                    metrics.setEgfrFormulaUsed("CKD_EPI_2021");

                    // Determine CKD stage using CKDStageResolver
                    CKDStageResolver.CKDStage ckdStageResolver = stageResolver.resolveCKDStage(egfr);
                    // Convert CKDStageResolver.CKDStage to CkdStage entity enum
                    CkdStage ckdStageEntity = convertToCkdStageEnum(ckdStageResolver);
                    metrics.setCkdStage(ckdStageEntity);

                    log.info("CKD-EPI calculated: eGFR={} mL/min/1.73m², Stage={}", egfr, ckdStageEntity);
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
                        previous != null && previous.getEgfr() != null,
                        scrQuality
                );
                metrics.setEgfrQualityIndicator(qualityIndicator);

                // Step 5: Set alerts and recommendations
                boolean lowEgfr = egfr != null && egfr < 60.0;
                boolean rapidDecline = trendData != null && trendData.isAbnormal();

                metrics.setAlertLowEgfr(lowEgfr);
                metrics.setAlertRapidDecline(rapidDecline);
                metrics.setAlertMessage(buildAlertMessage(lowEgfr, rapidDecline, egfr, trendData));
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

    private Double calculateEgfr(Double heightCm, Double creatinineMgDl) {
        // Legacy Cockcroft-Gault approximation (kept for backward compatibility)
        if (heightCm == null || creatinineMgDl == null || creatinineMgDl <= 0) {
            return null;
        }
        double value = 0.413 * heightCm / creatinineMgDl;
        return Math.round(value * 10.0) / 10.0;
    }

    private String buildAlertMessage(boolean lowEgfr, boolean rapidDecline, Double egfr,
                                     TrendAnalysisService.TrendData trendData) {
        StringBuilder msg = new StringBuilder();

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
            boolean hasPrevious,
            CKDEPICalculationService.QualityFlag scrQuality) {

        int score = 0;
        if (hasCreatinine) score += 25;
        if (hasAge) score += 25;
        if (hasSex) score += 25;
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
                
                // Timestamps
                .createdAt(metrics.getCreatedAt())
                .updatedAt(metrics.getUpdatedAt())
                .build();
    }
}
