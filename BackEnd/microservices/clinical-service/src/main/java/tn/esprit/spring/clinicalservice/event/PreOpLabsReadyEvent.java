package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when pre-operative labs are ready.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class PreOpLabsReadyEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    
    public PreOpLabsReadyEvent(Object source, UUID patientId, UUID surgeryId) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
    }
}
