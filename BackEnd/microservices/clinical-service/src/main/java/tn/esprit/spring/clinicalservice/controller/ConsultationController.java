package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.event.ConsultationClosedEvent;
import tn.esprit.spring.clinicalservice.event.ConsultationCreatedEvent;
import tn.esprit.spring.clinicalservice.mapper.ConsultationRecordMapper;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;
import tn.esprit.spring.clinicalservice.service.ClinicalAlertsService;
import tn.esprit.spring.clinicalservice.service.ClinicalValidationService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Consultation endpoints
 */
@Slf4j
@RestController("consultationRecordController")
@RequestMapping("/api/v1/consultations")
public class ConsultationController {
    
    @Autowired
    private ConsultationRecordRepository consultationRepository;
    
    @Autowired
    private ConsultationRecordMapper mapper;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ClinicalValidationService validationService;

    @Autowired
    private ClinicalAlertsService alertsService;
    
    /**
     * GET /api/v1/consultations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationRecordDTO> getConsultation(@PathVariable UUID id) {
        return consultationRepository.findById(id)
            .map(entity -> ResponseEntity.ok(mapper.toDTO(entity)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/v1/consultations?patientId={patientId}
     */
    @GetMapping
    public ResponseEntity<List<ConsultationRecordDTO>> getConsultationsByPatient(
            @RequestParam UUID patientId) {
        List<ConsultationRecordDTO> dtos = consultationRepository
            .findByPatientIdOrderByConsultationDateDesc(patientId)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * POST /api/v1/consultations
     * Creates a consultation and publishes event for async AI prediction
     */
    @PostMapping
    public ResponseEntity<ConsultationRecordDTO> createConsultation(
            @RequestBody ConsultationRecordDTO dto) {
        ConsultationRecord entity = mapper.toEntity(dto);
        validationService.validateSchwartzRequirements(entity);
        if (dto.getAgeYears() != null) {
            validationService.computeAndAssignCKDStage(entity, dto.getAgeYears(), dto.getIsPremature());
        }
        ConsultationRecord saved = consultationRepository.save(entity);
        runClinicalAutomation(saved, dto);
        log.info("Consultation created for patient {}", saved.getPatientId());
        
        // Publish event to trigger async AI prediction
        // Extract age from DTO (may come from query param or embedded user data)
        Integer ageYears = null;
        if (dto.getWeight_kg() != null) {
            // Age might be inferred from other data - for now we'll extract from DTOs when available
            // In production, call UserService to get patient demographics
        }
        
        ConsultationCreatedEvent event = new ConsultationCreatedEvent(
            saved.getId(),
            saved.getPatientId(),
            ageYears,
            null, // sex - would come from UserService
            saved.getParserConfidence(),
            saved.getContentType(),
            saved.getRequiresManualReview()
        );
        eventPublisher.publishEvent(event);
        log.debug("Published ConsultationCreatedEvent for async AI processing: {}", saved.getId());

        eventPublisher.publishEvent(new ConsultationClosedEvent(
            this,
            saved.getId(),
            saved.getPatientId(),
            buildConsultationSummary(saved)
        ));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(saved));
    }
    
    /**
     * PUT /api/v1/consultations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConsultationRecordDTO> updateConsultation(
            @PathVariable UUID id,
            @RequestBody ConsultationRecordDTO dto) {
        return consultationRepository.findById(id)
            .map(existing -> {
                ConsultationRecord updated = mapper.toEntity(dto);
                updated.setId(id);
                updated.setCreatedAt(existing.getCreatedAt());
                updated.setUpdatedAt(existing.getUpdatedAt());
                validationService.validateSchwartzRequirements(updated);
                if (dto.getAgeYears() != null) {
                    validationService.computeAndAssignCKDStage(updated, dto.getAgeYears(), dto.getIsPremature());
                }
                ConsultationRecord saved = consultationRepository.save(updated);
                runClinicalAutomation(saved, dto);
                log.info("Consultation {} updated", id);
                return ResponseEntity.ok(mapper.toDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/v1/consultations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConsultation(@PathVariable UUID id) {
        if (consultationRepository.existsById(id)) {
            consultationRepository.deleteById(id);
            log.info("Consultation {} deleted", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private void runClinicalAutomation(ConsultationRecord consultation, ConsultationRecordDTO dto) {
        alertsService.checkVigilanceRequired(consultation, consultation.getPatientId());
        alertsService.checkBPClassification(consultation.getPatientId(), consultation.getVitalSigns(), dto.getAgeYears());
        alertsService.checkProteinIntakeSafety(
            consultation.getPatientId(),
            consultation.getNephologyRecord(),
            dto.getProteinIntakeGPerKgPerDay()
        );
        alertsService.checkPhosphateLevel(consultation.getPatientId(), consultation.getNephologyRecord());
        alertsService.checkCalciumLevel(consultation.getPatientId(), consultation.getNephologyRecord());
        alertsService.checkPotassiumLevel(consultation.getPatientId(), consultation.getNephologyRecord());
        validationService.checkHUSAnnualFollowup(consultation.getPatientId(), consultation.getNephologyRecord());
    }

    private String buildConsultationSummary(ConsultationRecord consultation) {
        if (consultation.getChiefComplaint() != null && !consultation.getChiefComplaint().isBlank()) {
            return consultation.getChiefComplaint();
        }
        return "Consultation recorded on " + consultation.getConsultationDate();
    }
}
