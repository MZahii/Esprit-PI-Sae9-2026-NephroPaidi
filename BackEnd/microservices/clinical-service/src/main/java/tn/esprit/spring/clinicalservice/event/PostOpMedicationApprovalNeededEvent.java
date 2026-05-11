package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when post-operative medication approval is needed.
 * Needed for Student B (pharmacy-service) Day 2 workflow.
 */
@Getter
public class PostOpMedicationApprovalNeededEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String medicationName;
    private final String indication;
    
    public PostOpMedicationApprovalNeededEvent(Object source, UUID patientId, UUID surgeryId, String medicationName, String indication) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.medicationName = medicationName;
        this.indication = indication;
    }
}
