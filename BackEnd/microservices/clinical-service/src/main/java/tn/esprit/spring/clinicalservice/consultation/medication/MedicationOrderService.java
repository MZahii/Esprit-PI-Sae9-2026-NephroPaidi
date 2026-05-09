package tn.esprit.spring.clinicalservice.consultation.medication;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.MedicationOrderRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.MedicationOrderResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.metrics.ConsultationMetricsRepository;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicationOrderService {

    private final MedicationOrderRepository medicationOrderRepository;
    private final ConsultationRepository consultationRepository;
    private final ConsultationMetricsRepository metricsRepository;

    private static final Map<String, DoseRule> DOSE_RULES = Map.of(
            // TODO: validate and extend dosing ranges with clinical references
            "amoxicillin", new DoseRule(20.0, 90.0, null, null),
            "furosemide", new DoseRule(0.5, 6.0, null, null),
            "enalapril", new DoseRule(0.08, 0.6, null, null)
    );

    public MedicationOrderResponse create(UUID consultationId, UUID doctorId, MedicationOrderRequest request) {
        Consultation consultation = requireConsultation(consultationId, doctorId);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        String medicationName = request.getMedicationName();
        if (medicationName == null || medicationName.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "medicationName is required");
        }

        Double weightKg = request.getWeightKg();
        Integer ageYears = request.getAgeYears();

        if (weightKg == null || ageYears == null) {
            var metrics = metricsRepository.findByConsultationId(consultationId).orElse(null);
            if (metrics != null) {
                if (weightKg == null) {
                    weightKg = metrics.getWeightKg();
                }
                if (ageYears == null) {
                    ageYears = metrics.getAgeYears();
                }
            }
        }

        ValidationResult validation = validateDose(medicationName, request.getDoseMg(), request.getFrequencyPerDay(), weightKg, ageYears);

        MedicationOrder order = MedicationOrder.builder()
                .consultationId(consultationId)
                .medicationName(medicationName.trim())
                .doseMg(request.getDoseMg())
                .frequencyPerDay(request.getFrequencyPerDay())
                .durationDays(request.getDurationDays())
                .note(request.getNote())
                .weightKg(weightKg)
                .ageYears(ageYears)
                .validationStatus(validation.status)
                .validationMessage(validation.message)
                .build();

        MedicationOrder saved = medicationOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MedicationOrderResponse> list(UUID consultationId, UUID doctorId) {
        requireConsultation(consultationId, doctorId);
        return medicationOrderRepository.findByConsultationId(consultationId)
                .stream()
                .map(this::toResponse)
                .toList();
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

    private ValidationResult validateDose(String medicationName, Double doseMg, Integer frequencyPerDay, Double weightKg, Integer ageYears) {
        String key = medicationName == null ? "" : medicationName.trim().toLowerCase(Locale.ROOT);
        DoseRule rule = DOSE_RULES.get(key);

        if (rule == null) {
            return new ValidationResult(DoseValidationStatus.WARN, "No dosing rule configured for this medication.");
        }

        if (doseMg == null || weightKg == null || weightKg <= 0) {
            return new ValidationResult(DoseValidationStatus.WARN, "Dose and weight are required for validation.");
        }

        int freq = frequencyPerDay != null && frequencyPerDay > 0 ? frequencyPerDay : 1;
        double dailyDoseMg = doseMg * freq;
        double mgPerKgPerDay = dailyDoseMg / weightKg;

        DoseValidationStatus status = DoseValidationStatus.OK;
        String message;

        if (mgPerKgPerDay < rule.minMgPerKgPerDay || mgPerKgPerDay > rule.maxMgPerKgPerDay) {
            String msg = String.format(Locale.ROOT,
                    "Dose %.2f mg/kg/day is outside recommended range (%.2f - %.2f mg/kg/day).",
                    mgPerKgPerDay, rule.minMgPerKgPerDay, rule.maxMgPerKgPerDay);
            status = DoseValidationStatus.INVALID;
            message = msg;
        } else {
            message = "Dose within recommended range.";
        }

        String ageMsg = null;
        if (ageYears == null) {
            ageMsg = "Age not provided; validation is weight-based only.";
        } else if (rule.minAgeYears != null && ageYears < rule.minAgeYears) {
            ageMsg = "Age below recommended range for this medication.";
        } else if (rule.maxAgeYears != null && ageYears > rule.maxAgeYears) {
            ageMsg = "Age above recommended range for this medication.";
        }

        if (ageMsg != null) {
            if (status == DoseValidationStatus.OK) {
                status = DoseValidationStatus.WARN;
            }
            message = message + " " + ageMsg;
        }

        return new ValidationResult(status, message);
    }

    private MedicationOrderResponse toResponse(MedicationOrder order) {
        return MedicationOrderResponse.builder()
                .id(order.getId())
                .consultationId(order.getConsultationId())
                .medicationName(order.getMedicationName())
                .doseMg(order.getDoseMg())
                .frequencyPerDay(order.getFrequencyPerDay())
                .durationDays(order.getDurationDays())
                .note(order.getNote())
                .weightKg(order.getWeightKg())
                .ageYears(order.getAgeYears())
                .validationStatus(order.getValidationStatus() != null ? order.getValidationStatus().name() : null)
                .validationMessage(order.getValidationMessage())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private static class DoseRule {
        private final double minMgPerKgPerDay;
        private final double maxMgPerKgPerDay;
        private final Integer minAgeYears;
        private final Integer maxAgeYears;

        private DoseRule(double minMgPerKgPerDay, double maxMgPerKgPerDay, Integer minAgeYears, Integer maxAgeYears) {
            this.minMgPerKgPerDay = minMgPerKgPerDay;
            this.maxMgPerKgPerDay = maxMgPerKgPerDay;
            this.minAgeYears = minAgeYears;
            this.maxAgeYears = maxAgeYears;
        }
    }

    private static class ValidationResult {
        private final DoseValidationStatus status;
        private final String message;

        private ValidationResult(DoseValidationStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
