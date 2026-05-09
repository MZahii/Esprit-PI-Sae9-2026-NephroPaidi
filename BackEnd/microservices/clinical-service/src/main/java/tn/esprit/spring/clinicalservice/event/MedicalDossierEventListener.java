package tn.esprit.spring.clinicalservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.esprit.spring.clinicalservice.entity.MedicalDossierEntry;
import tn.esprit.spring.clinicalservice.enums.EntryType;
import tn.esprit.spring.clinicalservice.repository.MedicalDossierRepository;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event listener for medical dossier timeline creation.
 * Responds to consultation, lab, discharge, and procedure events to maintain longitudinal record.
 */
@Slf4j
@Component
public class MedicalDossierEventListener {
    
    @Autowired
    private MedicalDossierRepository dossierRepository;
    
    /**
     * Listen for consultation closed event and create dossier entry
     */
    @EventListener
    public void onConsultationClosed(ConsultationClosedEvent event) {
        log.info("Received ConsultationClosedEvent for patient: {}", event.getPatientId());
        
        MedicalDossierEntry entry = new MedicalDossierEntry();
        entry.setPatientId(event.getPatientId());
        entry.setEntryType(EntryType.CONSULTATION);
        entry.setSummary(event.getConsultationSummary());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setSourceServiceId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // clinical-service ID
        
        dossierRepository.save(entry);
        log.info("Created dossier entry for consultation: {}", entry.getId());
    }
    
    /**
     * Listen for lab result uploaded event and create dossier entry
     */
    @EventListener
    public void onLabResultUploaded(LabResultUploadedEvent event) {
        log.info("Received LabResultUploadedEvent for patient: {}", event.getPatientId());
        
        MedicalDossierEntry entry = new MedicalDossierEntry();
        entry.setPatientId(event.getPatientId());
        entry.setEntryType(EntryType.LAB);
        entry.setSummary(String.format("Lab: %s - %s", event.getLabTestName(), event.getResult()));
        entry.setCreatedAt(LocalDateTime.now());
        entry.setSourceServiceId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // clinical-service ID
        
        dossierRepository.save(entry);
        log.info("Created dossier entry for lab result: {}", entry.getId());
    }
    
    /**
     * Listen for discharge created event and create dossier entry
     */
    @EventListener
    public void onDischargeCreated(DischargeCreatedEvent event) {
        log.info("Received DischargeCreatedEvent for patient: {}", event.getPatientId());
        
        MedicalDossierEntry entry = new MedicalDossierEntry();
        entry.setPatientId(event.getPatientId());
        entry.setEntryType(EntryType.DISCHARGE);
        entry.setSummary(event.getDischargeSummary());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setSourceServiceId(UUID.fromString("00000000-0000-0000-0000-000000000001")); // clinical-service ID
        
        dossierRepository.save(entry);
        log.info("Created dossier entry for discharge: {}", entry.getId());
    }
    
    /**
     * Placeholder for pre-operative readiness (Day 2 - Student C)
     */
    @EventListener
    public void onPreOpReadinessRequested(PreOpReadinessRequestedEvent event) {
        log.info("Received PreOpReadinessRequestedEvent for patient: {}", event.getPatientId());
        // TODO: Day 2 - Create dossier entry and trigger readiness checks
    }
    
    /**
     * Placeholder for post-operative report (Day 2 - Student C)
     */
    @EventListener
    public void onPostOpReportCreated(PostOpReportCreatedEvent event) {
        log.info("Received PostOpReportCreatedEvent for patient: {}", event.getPatientId());
        // TODO: Day 2 - Create dossier entry with post-op summary
    }
}
