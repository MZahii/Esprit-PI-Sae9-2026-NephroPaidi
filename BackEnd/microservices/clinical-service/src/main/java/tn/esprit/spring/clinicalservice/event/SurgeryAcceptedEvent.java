package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when patient accepts surgery offer.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class SurgeryAcceptedEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    
    public SurgeryAcceptedEvent(Object source, UUID patientId, UUID surgeryId) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
    }
}
