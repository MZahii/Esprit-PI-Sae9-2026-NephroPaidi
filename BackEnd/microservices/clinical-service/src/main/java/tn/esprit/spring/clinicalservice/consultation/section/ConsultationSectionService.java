package tn.esprit.spring.clinicalservice.consultation.section;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationSectionRequest;
import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationSectionResponse;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;
import tn.esprit.spring.clinicalservice.consultation.entity.ConsultationStatus;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationService;
import tn.esprit.spring.clinicalservice.notification.GuardianNotificationType;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationSectionService {

    private final ConsultationSectionRepository sectionRepository;
    private final ConsultationRepository consultationRepository;
    private final GuardianNotificationService guardianNotificationService;

    @Transactional(readOnly = true)
    public List<ConsultationSectionResponse> list(UUID consultationId, UUID doctorId) {
        requireConsultation(consultationId, doctorId);
        return sectionRepository.findByConsultationId(consultationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ConsultationSectionResponse upsert(UUID consultationId, UUID doctorId, ConsultationSectionType type, ConsultationSectionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        Consultation consultation = requireConsultation(consultationId, doctorId);

        ConsultationSection section = sectionRepository.findByConsultationIdAndSectionType(consultationId, type)
                .orElseGet(() -> ConsultationSection.builder()
                        .consultationId(consultationId)
                        .sectionType(type)
                        .checked(false)
                        .build());

        String beforeContent = section.getContent();
        Boolean checked = request.getChecked();
        if (checked != null) {
            section.setChecked(checked);
        }
        section.setContent(request.getContent());

        ConsultationSection saved = sectionRepository.save(section);

        if (type == ConsultationSectionType.PRESCRIPTIONS) {
            notifyIfChanged(consultationId, beforeContent, saved.getContent(), GuardianNotificationType.PRESCRIPTIONS_UPDATED,
                    "Doctor updated prescriptions.");
        } else if (type == ConsultationSectionType.TREATMENT_PLAN) {
            notifyIfChanged(consultationId, beforeContent, saved.getContent(), GuardianNotificationType.TREATMENT_PLAN_UPDATED,
                    "Doctor updated treatment plan.");
        }

        return toResponse(saved);
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

    private ConsultationSectionResponse toResponse(ConsultationSection section) {
        return ConsultationSectionResponse.builder()
                .id(section.getId())
                .consultationId(section.getConsultationId())
                .sectionType(section.getSectionType() != null ? section.getSectionType().name() : null)
                .checked(section.isChecked())
                .content(section.getContent())
                .updatedAt(section.getUpdatedAt())
                .build();
    }

    private void notifyIfChanged(UUID consultationId, String before, String after, GuardianNotificationType type, String message) {
        if (after == null || after.isBlank()) {
            return;
        }
        if (Objects.equals(before, after)) {
            return;
        }
        guardianNotificationService.notifyGuardians(consultationId, type, message);
    }
}
