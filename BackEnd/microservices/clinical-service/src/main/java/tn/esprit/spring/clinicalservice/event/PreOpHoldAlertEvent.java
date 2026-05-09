package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when pre-operative hold alert is triggered.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class PreOpHoldAlertEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String reason;
    
    public PreOpHoldAlertEvent(Object source, UUID patientId, UUID surgeryId, String reason) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.reason = reason;
    }
}
