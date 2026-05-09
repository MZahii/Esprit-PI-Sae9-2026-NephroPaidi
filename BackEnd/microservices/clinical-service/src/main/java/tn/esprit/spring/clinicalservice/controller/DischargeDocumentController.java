package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.DischargeDocumentDTO;
import tn.esprit.spring.clinicalservice.entity.DischargeDocument;
import tn.esprit.spring.clinicalservice.mapper.DischargeDocumentMapper;
import tn.esprit.spring.clinicalservice.repository.DischargeDocumentRepository;
import tn.esprit.spring.clinicalservice.service.HASDischargeValidator;
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
    
    /**
     * GET /api/v1/discharges/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<DischargeDocumentDTO> getDischargeDocument(@PathVariable UUID id) {
        return dischargeRepository.findById(id)
            .map(entity -> ResponseEntity.ok(mapper.toDTO(entity)))
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
            .map(mapper::toDTO)
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
        log.info("Discharge document created for patient {}", saved.getPatientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(saved));
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
            discharge.setFinalized(true);
            discharge.setFinalizedAt(java.time.LocalDateTime.now());
            DischargeDocument saved = dischargeRepository.save(discharge);
            log.info("Discharge document {} finalized (HAS compliant)", id);
            return ResponseEntity.ok(mapper.toDTO(saved));
        } catch (HASDischargeValidator.HASComplianceException e) {
            log.warn("HAS validation failed for discharge {}: {}", id, e.getMessage());
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
                DischargeDocument saved = dischargeRepository.save(updated);
                log.info("Discharge document {} updated", id);
                return ResponseEntity.ok(mapper.toDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/v1/discharges/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDischargeDocument(@PathVariable UUID id) {
        if (dischargeRepository.existsById(id)) {
            dischargeRepository.deleteById(id);
            log.info("Discharge document {} deleted", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
