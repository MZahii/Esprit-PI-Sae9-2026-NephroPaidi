package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when medication request is rejected.
 * Needed for Student B (pharmacy-service) Day 2 workflow.
 */
@Getter
public class MedicationRequestRejectedEvent extends ApplicationEvent {
    private final UUID requestId;
    private final UUID patientId;
    private final String medicationName;
    private final String reason;
    
    public MedicationRequestRejectedEvent(Object source, UUID requestId, UUID patientId, String medicationName, String reason) {
        super(source);
        this.requestId = requestId;
        this.patientId = patientId;
        this.medicationName = medicationName;
        this.reason = reason;
    }
}
