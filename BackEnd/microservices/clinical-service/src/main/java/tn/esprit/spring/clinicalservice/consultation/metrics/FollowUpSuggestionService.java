package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.FollowUpSuggestionResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpSuggestionService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationMetricsRepository metricsRepository;

    public FollowUpSuggestionResponse suggest(UUID consultationId, UUID doctorId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation not found"));

        if (!consultation.getDoctorId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your consultation");
        }
        if (consultation.getStatus() == ConsultationStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation archived");
        }

        CkdStage stage = metricsRepository.findByConsultationId(consultationId)
                .map(ConsultationMetrics::getCkdStage)
                .orElse(null);

        int intervalDays = switch (stage) {
            case G1, G2 -> 180;
            case G3A, G3B -> 90;
            case G4 -> 30;
            case G5 -> 14;
            default -> 90;
        };

        LocalDate baseDate = consultation.getDateTime() != null
                ? consultation.getDateTime().toLocalDate()
                : LocalDate.now();

        String reason = stage != null
                ? "Follow-up based on CKD stage " + stage
                : "Default follow-up interval";

        return FollowUpSuggestionResponse.builder()
                .suggestedDate(baseDate.plusDays(intervalDays))
                .intervalDays(intervalDays)
                .reason(reason)
                .build();
    }
}
