package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.DischargeDocumentDTO;
import tn.esprit.spring.clinicalservice.dto.MedicationAtDischargeDTO;
import tn.esprit.spring.clinicalservice.dto.TechnicalActDTO;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;
import tn.esprit.spring.clinicalservice.entity.MedicationAtDischarge;
import tn.esprit.spring.clinicalservice.entity.TechnicalAct;
import tn.esprit.spring.clinicalservice.event.DischargeCreatedEvent;
import tn.esprit.spring.clinicalservice.mapper.DischargeDocumentMapper;
import tn.esprit.spring.clinicalservice.mapper.MedicationAtDischargeMapper;
import tn.esprit.spring.clinicalservice.mapper.TechnicalActMapper;
import tn.esprit.spring.clinicalservice.repository.DischargeDocumentRepository;
import tn.esprit.spring.clinicalservice.repository.MedicationAtDischargeRepository;
import tn.esprit.spring.clinicalservice.repository.TechnicalActRepository;
import tn.esprit.spring.clinicalservice.service.ClinicalAlertsService;
import tn.esprit.spring.clinicalservice.service.ClinicalValidationService;
import tn.esprit.spring.clinicalservice.service.HASDischargeValidator;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Discharge Document endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/discharges")
public class DischargeDocumentController {
    
    @Autowired
    private DischargeDocumentRepository dischargeRepository;
    
    @Autowired
    private DischargeDocumentMapper mapper;
    
    @Autowired
    private HASDischargeValidator hasValidator;

    @Autowired
    private TechnicalActRepository technicalActRepository;

    @Autowired
    private MedicationAtDischargeRepository medicationRepository;

    @Autowired
    private TechnicalActMapper technicalActMapper;

    @Autowired
    private MedicationAtDischargeMapper medicationMapper;

    @Autowired
    private ClinicalValidationService validationService;

    @Autowired
    private ClinicalAlertsService alertsService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * GET /api/v1/discharges/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DischargeDocumentDTO> getDischargeDocument(@PathVariable UUID id) {
        return dischargeRepository.findById(id)
            .map(entity -> ResponseEntity.ok(buildResponse(entity)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/v1/discharges?patientId={patientId}
     */
    @GetMapping
    public ResponseEntity<List<DischargeDocumentDTO>> getDischargeDocumentsByPatient(
            @RequestParam UUID patientId) {
        List<DischargeDocumentDTO> dtos = dischargeRepository
            .findByPatientIdOrderByDischargeDateDesc(patientId)
            .stream()
            .map(this::buildResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * POST /api/v1/discharges
     */
    @PostMapping
    public ResponseEntity<DischargeDocumentDTO> createDischargeDocument(
            @RequestBody DischargeDocumentDTO dto) {
        DischargeDocument entity = mapper.toEntity(dto);
        DischargeDocument saved = dischargeRepository.save(entity);
        saveAssociatedRecords(saved, dto);
        validationService.checkCRH8DayCompliance(saved);
        eventPublisher.publishEvent(new DischargeCreatedEvent(
            this,
            saved.getId(),
            saved.getPatientId(),
            saved.getMedicalSummary()
        ));
        log.info("Discharge document created for patient {}", saved.getPatientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(saved));
    }
    
    /**
     * POST /api/v1/discharges/{id}/finalize - Finalize discharge with HAS validation
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<?> finalizeDischargeDocument(@PathVariable UUID id) {
        var optional = dischargeRepository.findById(id);
        if (!optional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        DischargeDocument discharge = optional.get();
        try {
            hasValidator.validate(discharge);
            validationService.checkCRH8DayCompliance(discharge);
            validateAssociatedMedicationChanges(discharge.getId());
            alertsService.scanForNephrotoxicDrugs(
                discharge.getPatientId(),
                medicationRepository.findByDischargeId(discharge.getId())
            );
            discharge.setFinalized(true);
            discharge.setFinalizedAt(java.time.LocalDateTime.now());
            DischargeDocument saved = dischargeRepository.save(discharge);
            log.info("Discharge document {} finalized (HAS compliant)", id);
            return ResponseEntity.ok(buildResponse(saved));
        } catch (HASDischargeValidator.HASComplianceException e) {
            log.warn("HAS validation failed for discharge {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for discharge {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * PUT /api/v1/discharges/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DischargeDocumentDTO> updateDischargeDocument(
            @PathVariable UUID id,
            @RequestBody DischargeDocumentDTO dto) {
        return dischargeRepository.findById(id)
            .map(existing -> {
                DischargeDocument updated = mapper.toEntity(dto);
                updated.setId(id);
                updated.setCreatedAt(existing.getCreatedAt());
                updated.setFinalized(existing.getFinalized());
                updated.setFinalizedAt(existing.getFinalizedAt());
                DischargeDocument saved = dischargeRepository.save(updated);
                saveAssociatedRecords(saved, dto);
                validationService.checkCRH8DayCompliance(saved);
                log.info("Discharge document {} updated", id);
                return ResponseEntity.ok(buildResponse(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/v1/discharges/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDischargeDocument(@PathVariable UUID id) {
        if (dischargeRepository.existsById(id)) {
            technicalActRepository.deleteByDischargeId(id);
            medicationRepository.deleteByDischargeId(id);
            dischargeRepository.deleteById(id);
            log.info("Discharge document {} deleted", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private DischargeDocumentDTO buildResponse(DischargeDocument discharge) {
        DischargeDocumentDTO dto = mapper.toDTO(discharge);
        dto.setTechnicalActs(
            technicalActRepository.findByDischargeId(discharge.getId())
                .stream()
                .map(technicalActMapper::toDTO)
                .collect(Collectors.toList())
        );
        dto.setMedications(
            medicationRepository.findByDischargeId(discharge.getId())
                .stream()
                .map(medicationMapper::toDTO)
                .collect(Collectors.toList())
        );
        return dto;
    }

    private void saveAssociatedRecords(DischargeDocument discharge, DischargeDocumentDTO dto) {
        technicalActRepository.deleteByDischargeId(discharge.getId());
        medicationRepository.deleteByDischargeId(discharge.getId());

        List<TechnicalAct> technicalActs = dto.getTechnicalActs() == null
            ? Collections.emptyList()
            : dto.getTechnicalActs().stream()
                .map(technicalActMapper::toEntity)
                .peek(act -> act.setDischargeId(discharge.getId()))
                .collect(Collectors.toList());

        List<MedicationAtDischarge> medications = dto.getMedications() == null
            ? Collections.emptyList()
            : dto.getMedications().stream()
                .map(medicationMapper::toEntity)
                .peek(med -> {
                    med.setDischargeId(discharge.getId());
                    validationService.validateMedicationModification(med);
                })
                .collect(Collectors.toList());

        technicalActRepository.saveAll(technicalActs);
        medicationRepository.saveAll(medications);
        alertsService.scanForNephrotoxicDrugs(discharge.getPatientId(), medications);
    }

    private void validateAssociatedMedicationChanges(UUID dischargeId) {
        for (MedicationAtDischarge medication : medicationRepository.findByDischargeId(dischargeId)) {
            validationService.validateMedicationModification(medication);
        }
    }
}
