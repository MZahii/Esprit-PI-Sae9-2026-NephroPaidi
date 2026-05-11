package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when discharge document is created.
 * Listened by MedicalDossierEventListener to create dossier entry.
 */
@Getter
public class DischargeCreatedEvent extends ApplicationEvent {
    private final UUID dischargeId;
    private final UUID patientId;
    private final String dischargeSummary;
    
    public DischargeCreatedEvent(Object source, UUID dischargeId, UUID patientId, String dischargeSummary) {
        super(source);
        this.dischargeId = dischargeId;
        this.patientId = patientId;
        this.dischargeSummary = dischargeSummary;
    }
}
