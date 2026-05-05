package tn.esprit.spring.procedureservice.whatsapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalCase;

@Service
public class ProcedureWhatsAppAlertService {
    private static final Logger log = LoggerFactory.getLogger(ProcedureWhatsAppAlertService.class);

    private final TwilioWhatsAppMessageService messageService;

    public ProcedureWhatsAppAlertService(TwilioWhatsAppMessageService messageService) {
        this.messageService = messageService;
    }

    public void sendSurgicalCaseCreatedAlert(SurgicalCase surgicalCase) {
        if (!messageService.isReady()) {
            return;
        }

        try {
            messageService.sendToDefaultRecipient(buildSurgicalCaseCreatedMessage(surgicalCase));
        } catch (RuntimeException ex) {
            log.warn("WhatsApp alert failed for surgical case {}", surgicalCase.getId(), ex);
        }
    }

    private String buildSurgicalCaseCreatedMessage(SurgicalCase surgicalCase) {
        return "Nouveau cas chirurgical cree. Cas #"
            + valueOrPending(surgicalCase.getId())
            + ". Patient: "
            + safePatientLabel(surgicalCase)
            + ". Consultez NephroPaidi pour les details.";
    }

    private String safePatientLabel(SurgicalCase surgicalCase) {
        String firstName = surgicalCase.getFirstName();
        String lastName = surgicalCase.getLastName();
        String firstInitial = firstName == null || firstName.isBlank() ? "" : firstName.substring(0, 1).toUpperCase();
        String lastInitial = lastName == null || lastName.isBlank() ? "" : lastName.substring(0, 1).toUpperCase();
        String label = (firstInitial + "." + lastInitial + ".").trim();
        return ".".equals(label) ? "N/A" : label;
    }

    private String valueOrPending(Long value) {
        return value == null ? "N/A" : value.toString();
    }
}
