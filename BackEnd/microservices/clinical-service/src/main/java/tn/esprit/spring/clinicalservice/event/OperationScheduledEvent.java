package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when operation is scheduled.
 * Needed for Student C (procedure-service) Day 2 workflow.
 */
@Getter
public class OperationScheduledEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final LocalDateTime scheduledDateTime;
    private final String operatingRoomNumber;
    
    public OperationScheduledEvent(Object source, UUID patientId, UUID surgeryId, LocalDateTime scheduledDateTime, String operatingRoomNumber) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.scheduledDateTime = scheduledDateTime;
        this.operatingRoomNumber = operatingRoomNumber;
    }
}
