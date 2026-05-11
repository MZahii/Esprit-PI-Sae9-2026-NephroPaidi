package tn.esprit.spring.clinicalservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.clinicalservice.client.AIClinicalServiceClient;
import tn.esprit.spring.clinicalservice.dto.AIPredictionRequestDTO;
import tn.esprit.spring.clinicalservice.dto.AIPredictionResponseDTO;
import tn.esprit.spring.clinicalservice.entity.*;
import tn.esprit.spring.clinicalservice.enums.AlertSeverity;
import tn.esprit.spring.clinicalservice.event.ConsultationCreatedEvent;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;

import java.time.LocalDateTime;

/**
 * Event listener for ConsultationCreatedEvent
 * Asynchronously processes consultation creation:
 * 1. Extracts relevant data for AI prediction
 * 2. Calls AI service for clinical alert recommendation
 * 3. Creates ClinicalAlert based on prediction
 * 4. Stores alert in database for clinical staff review
 */
@Slf4j
@Component
public class ConsultationCreatedEventListener {
    
    @Autowired
    private AIClinicalServiceClient aiClient;
    
    @Autowired
    private ConsultationRecordRepository consultationRepository;
    
    @Autowired
    private ClinicalAlertRepository alertRepository;
    
    /**
     * Listen for ConsultationCreatedEvent and request AI prediction
     * Runs asynchronously to avoid blocking the REST endpoint
     */
    @EventListener
    @Async
    @Transactional
    public void onConsultationCreated(ConsultationCreatedEvent event) {
        try {
            log.info("Processing consultation created event for patient: {}", event.getPatientId());
            
            // Fetch the full consultation record from database
            var consultation = consultationRepository.findById(event.getConsultationId());
            if (!consultation.isPresent()) {
                log.warn("Consultation not found: {}", event.getConsultationId());
                return;
            }
            
            ConsultationRecord consultationRecord = consultation.get();
            
            // Extract creatinine value and check if data is complete
            boolean hasCreatinineData = consultationRecord.getNephologyRecord() != null && 
                                       consultationRecord.getNephologyRecord().getSerumCreatinine_mgdL() != null;
            
            float creatinine_mg_dl = 0.0f;
            float creatinine_umol_l = 0.0f;
            if (hasCreatinineData) {
                creatinine_mg_dl = consultationRecord.getNephologyRecord().getSerumCreatinine_mgdL().floatValue();
            }
            if (consultationRecord.getSerumCreatinine_umolL() != null) {
                creatinine_umol_l = consultationRecord.getSerumCreatinine_umolL().floatValue();
            }
            
            // Build AI prediction request with extracted features
            AIPredictionRequestDTO predictionRequest = AIPredictionRequestDTO.builder()
                .ageYears(event.getAgeYears())
                .sex(event.getSex())
                .creatinine_mg_dl(creatinine_mg_dl)
                .creatinine_umol_l(creatinine_umol_l)
                .parserConfidence(event.getParserConfidence() != null ? event.getParserConfidence() : 0.0f)
                .has_creatinine(hasCreatinineData)
                .content_type(event.getContentType())
                .requires_manual_review(event.getRequiresManualReview() != null ? event.getRequiresManualReview() : false)
                .build();
            
            log.debug("Sending AI prediction request for consultation: {}", event.getConsultationId());
            
            // Call AI service for prediction
            AIPredictionResponseDTO prediction = aiClient.predict(predictionRequest);
            
            log.info("AI prediction received: {} (confidence: {})", 
                    prediction.getPrediction(), prediction.getConfidence());
            
            // Map AI prediction to alert severity
            AlertSeverity severity = mapPredictionToSeverity(prediction.getPrediction());
            
            // Create clinical alert based on AI prediction
            ClinicalAlert alert = new ClinicalAlert();
            alert.setPatientId(event.getPatientId());
            alert.setAlertType("AI_RECOMMENDATION");
            alert.setSeverity(severity);
            alert.setMessage("AI clinical recommendation: " + prediction.getPrediction());
            alert.setDetails(buildAlertDetails(prediction));
            alert.setCreatedAt(LocalDateTime.now());
            alert.setResolved(false);
            
            // Save alert
            alertRepository.save(alert);
            
            log.info("Clinical alert created for patient {} with severity {}", 
                    event.getPatientId(), severity);
            
        } catch (Exception e) {
            log.error("Error processing consultation created event", e);
            // Don't throw exception - this is async and shouldn't fail the consultation save
        }
    }
    
    /**
     * Map AI prediction class to alert severity
     */
    private AlertSeverity mapPredictionToSeverity(String prediction) {
        return switch (prediction.toLowerCase()) {
            case "urgent" -> AlertSeverity.URGENT;
            case "warning" -> AlertSeverity.WARNING;
            case "review" -> AlertSeverity.WARNING;
            case "other" -> AlertSeverity.ROUTINE;
            default -> AlertSeverity.ROUTINE;
        };
    }
    
    /**
     * Build detailed alert information from prediction response
     */
    private String buildAlertDetails(AIPredictionResponseDTO prediction) {
        StringBuilder details = new StringBuilder();
        details.append("Prediction: ").append(prediction.getPrediction()).append("\n");
        details.append("Confidence: ").append(String.format("%.2f%%", prediction.getConfidence() * 100)).append("\n");
        
        if (prediction.getClassProbabilities() != null) {
            details.append("Class Probabilities:\n");
            prediction.getClassProbabilities().forEach((className, prob) -> 
                details.append("  - ").append(className).append(": ").append(String.format("%.2f%%", prob * 100)).append("\n")
            );
        }
        
        return details.toString();
    }
}
