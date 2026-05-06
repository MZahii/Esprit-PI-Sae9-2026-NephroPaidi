package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationMetricsRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationMetricsResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationMetricsService {

    private final ConsultationMetricsRepository metricsRepository;
    private final ConsultationRepository consultationRepository;

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

        metrics.setPatientId(consultation.getPatientId());
        metrics.setHeightCm(request.getHeightCm());
        metrics.setCreatinineMgDl(request.getCreatinineMgDl());
        metrics.setWeightKg(request.getWeightKg());
        metrics.setAgeYears(request.getAgeYears());

        Double egfr = calculateEgfr(metrics.getHeightCm(), metrics.getCreatinineMgDl());
        metrics.setEgfr(egfr);
        metrics.setCkdStage(egfr != null ? CkdStage.fromEgfr(egfr) : null);

        boolean lowEgfr = egfr != null && egfr < 60.0;
        boolean rapidDecline = false;

        ConsultationMetrics previous = metricsRepository
                .findTopByPatientIdAndConsultationIdNotOrderByCreatedAtDesc(consultation.getPatientId(), consultationId)
                .orElse(null);

        if (previous != null && previous.getEgfr() != null && egfr != null && previous.getEgfr() > 0) {
            double declinePercent = ((previous.getEgfr() - egfr) / previous.getEgfr()) * 100.0;
            rapidDecline = declinePercent >= 20.0;
        }

        metrics.setAlertLowEgfr(lowEgfr);
        metrics.setAlertRapidDecline(rapidDecline);
        metrics.setAlertMessage(buildAlertMessage(lowEgfr, rapidDecline, egfr));

        ConsultationMetrics saved = metricsRepository.save(metrics);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationMetricsResponse get(UUID consultationId, UUID doctorId) {
        Consultation consultation = requireConsultation(consultationId, doctorId);
        ConsultationMetrics metrics = metricsRepository.findByConsultationId(consultation.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Metrics not found"));
        return toResponse(metrics);
    }

    @Transactional(readOnly = true)
    public ConsultationMetricsResponse getForPharmacist(UUID consultationId) {
        ConsultationMetrics metrics = metricsRepository.findByConsultationId(consultationId)
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
        if (heightCm == null || creatinineMgDl == null || creatinineMgDl <= 0) {
            return null;
        }
        double value = 0.413 * heightCm / creatinineMgDl;
        return Math.round(value * 10.0) / 10.0;
    }

    private String buildAlertMessage(boolean lowEgfr, boolean rapidDecline, Double egfr) {
        StringBuilder sb = new StringBuilder();
        if (lowEgfr) {
            sb.append("eGFR below 60 (possible renal insufficiency). ");
        }
        if (rapidDecline) {
            sb.append("eGFR declined more than 20% since last consultation. ");
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString().trim();
    }

    private ConsultationMetricsResponse toResponse(ConsultationMetrics metrics) {
        return ConsultationMetricsResponse.builder()
                .id(metrics.getId())
                .consultationId(metrics.getConsultationId())
                .patientId(metrics.getPatientId())
                .heightCm(metrics.getHeightCm())
                .creatinineMgDl(metrics.getCreatinineMgDl())
                .weightKg(metrics.getWeightKg())
                .ageYears(metrics.getAgeYears())
                .egfr(metrics.getEgfr())
                .ckdStage(metrics.getCkdStage() != null ? metrics.getCkdStage().name() : null)
                .alertLowEgfr(metrics.getAlertLowEgfr())
                .alertRapidDecline(metrics.getAlertRapidDecline())
                .alertMessage(metrics.getAlertMessage())
                .createdAt(metrics.getCreatedAt())
                .updatedAt(metrics.getUpdatedAt())
                .build();
    }
}
