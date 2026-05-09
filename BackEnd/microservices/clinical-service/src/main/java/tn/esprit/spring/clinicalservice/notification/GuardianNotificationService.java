package tn.esprit.spring.clinicalservice.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.patient.PatientDirectoryClient;
import tn.esprit.spring.clinicalservice.consultation.repository.ConsultationRepository;
import tn.esprit.spring.clinicalservice.consultation.entity.Consultation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GuardianNotificationService {

    private final GuardianNotificationRepository notificationRepository;
    private final PatientDirectoryClient patientDirectoryClient;
    private final ConsultationRepository consultationRepository;

    public void notifyGuardians(UUID consultationId, GuardianNotificationType type, String message) {
        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);
        if (consultation == null) {
            return;
        }

        Long guardianUserId = patientDirectoryClient.getGuardianUserIdForPatient(consultation.getPatientId());
        if (guardianUserId == null) {
            return;
        }

        GuardianNotification notification = GuardianNotification.builder()
                .guardianUserId(guardianUserId)
                .consultationId(consultationId)
                .type(type)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<GuardianNotification> listForGuardian(Long guardianUserId) {
        return notificationRepository.findByGuardianUserIdOrderByCreatedAtDesc(guardianUserId);
    }

    public GuardianNotification markRead(UUID id, Long guardianUserId) {
        GuardianNotification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null || !notification.getGuardianUserId().equals(guardianUserId)) {
            return null;
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notification;
    }
}
