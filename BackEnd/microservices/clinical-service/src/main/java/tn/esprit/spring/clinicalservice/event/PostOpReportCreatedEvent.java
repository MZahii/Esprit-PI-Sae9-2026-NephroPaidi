package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when post-operative report is created.
 * Day 2 placeholder for Student C workflow.
 */
@Getter
public class PostOpReportCreatedEvent extends ApplicationEvent {
    private final UUID patientId;
    private final UUID surgeryId;
    private final String reportContent;
    
    public PostOpReportCreatedEvent(Object source, UUID patientId, UUID surgeryId, String reportContent) {
        super(source);
        this.patientId = patientId;
        this.surgeryId = surgeryId;
        this.reportContent = reportContent;
    }
}
