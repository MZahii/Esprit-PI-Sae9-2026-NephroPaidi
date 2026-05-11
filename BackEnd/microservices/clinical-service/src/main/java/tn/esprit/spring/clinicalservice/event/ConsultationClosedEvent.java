package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when a consultation is closed.
 * Listened by MedicalDossierEventListener to create dossier entry.
 */
@Getter
public class ConsultationClosedEvent extends ApplicationEvent {
    private final UUID consultationId;
    private final UUID patientId;
    private final String consultationSummary;
    
    public ConsultationClosedEvent(Object source, UUID consultationId, UUID patientId, String consultationSummary) {
        super(source);
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.consultationSummary = consultationSummary;
    }
}
