package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when patient rejects surgery offer.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class SurgeryRejectedEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String reason;
    
    public SurgeryRejectedEvent(Object source, UUID patientId, UUID surgeryId, String reason) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.reason = reason;
    }
}
