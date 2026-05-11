package tn.esprit.spring.clinicalservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

/**
 * Event published when lab results are uploaded.
 * Listened by MedicalDossierEventListener to create dossier entry.
 */
@Getter
public class LabResultUploadedEvent extends ApplicationEvent {
    private final UUID labResultId;
    private final UUID patientId;
    private final String labTestName;
    private final String result;
    
    public LabResultUploadedEvent(Object source, UUID labResultId, UUID patientId, String labTestName, String result) {
        super(source);
        this.labResultId = labResultId;
        this.patientId = patientId;
        this.labTestName = labTestName;
        this.result = result;
    }
}
