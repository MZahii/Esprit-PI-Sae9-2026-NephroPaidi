package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when medication is dispensed.
 * Needed for Student B (pharmacy-service) Day 2 workflow.
 */
@Getter
public class MedicationDispensedEvent extends ApplicationEvent {
    private final UUID requestId;
    private final UUID patientId;
    private final String medicationName;
    private final String batchNumber;
    
    public MedicationDispensedEvent(Object source, UUID requestId, UUID patientId, String medicationName, String batchNumber) {
        super(source);
        this.requestId = requestId;
        this.patientId = patientId;
        this.medicationName = medicationName;
        this.batchNumber = batchNumber;
    }
}
