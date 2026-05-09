package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when surgery offer is made to patient.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class SurgeryOfferMadeEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String operationType;
    private final LocalDateTime suggestedDate;
    
    public SurgeryOfferMadeEvent(Object source, UUID patientId, UUID surgeryId, String operationType, LocalDateTime suggestedDate) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.operationType = operationType;
        this.suggestedDate = suggestedDate;
    }
}
