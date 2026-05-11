package tn.esprit.spring.clinicalservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Event published when a consultation record is created/finalized
 * Triggers AI prediction request to identify clinical alerts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationCreatedEvent {
    private UUID consultationId;
    private UUID patientId;
    private Integer ageYears;
    private String sex;
    private Float parserConfidence;
    private String contentType;
    private Boolean requiresManualReview;
}
