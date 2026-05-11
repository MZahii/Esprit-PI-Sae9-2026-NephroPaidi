package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when pre-operative readiness assessment is requested.
 * Day 2 placeholder for Student C workflow.
 */
@Getter
public class PreOpReadinessRequestedEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String operationType;
    
    public PreOpReadinessRequestedEvent(Object source, UUID patientId, UUID surgeryId, String operationType) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.operationType = operationType;
    }
}
