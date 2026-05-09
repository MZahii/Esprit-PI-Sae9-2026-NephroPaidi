package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.ConsultationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.mapper.ConsultationRecordMapper;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Consultation endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {
    
    @Autowired
    private ConsultationRecordRepository consultationRepository;
    
    @Autowired
    private ConsultationRecordMapper mapper;
    
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
     */
    @PostMapping
    public ResponseEntity<ConsultationRecordDTO> createConsultation(
            @RequestBody ConsultationRecordDTO dto) {
        ConsultationRecord entity = mapper.toEntity(dto);
        ConsultationRecord saved = consultationRepository.save(entity);
        log.info("Consultation created for patient {}", saved.getPatientId());
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
                ConsultationRecord saved = consultationRepository.save(updated);
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
}
