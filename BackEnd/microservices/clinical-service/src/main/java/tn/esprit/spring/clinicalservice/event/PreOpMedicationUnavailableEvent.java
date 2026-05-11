package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when pre-operative medication unavailable.
 * Needed for Student B (pharmacy-service) Day 2 workflow.
 */
@Getter
public class PreOpMedicationUnavailableEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String medicationName;
    private final String reason;
    
    public PreOpMedicationUnavailableEvent(Object source, UUID patientId, UUID surgeryId, String medicationName, String reason) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.medicationName = medicationName;
        this.reason = reason;
    }
}
