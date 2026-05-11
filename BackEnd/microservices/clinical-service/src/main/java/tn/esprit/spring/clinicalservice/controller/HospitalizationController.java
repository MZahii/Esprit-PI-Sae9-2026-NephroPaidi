package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.HospitalizationRecordDTO;
import tn.esprit.spring.clinicalservice.entity.HospitalizationRecord;
import tn.esprit.spring.clinicalservice.mapper.HospitalizationRecordMapper;
import tn.esprit.spring.clinicalservice.repository.HospitalizationRecordRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Hospitalization endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/hospitalizations")
public class HospitalizationController {
    
    @Autowired
    private HospitalizationRecordRepository hospitalizationRepository;
    
    @Autowired
    private HospitalizationRecordMapper mapper;
    
    /**
     * GET /api/v1/hospitalizations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<HospitalizationRecordDTO> getHospitalization(@PathVariable UUID id) {
        return hospitalizationRepository.findById(id)
            .map(entity -> ResponseEntity.ok(mapper.toDTO(entity)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/v1/hospitalizations?patientId={patientId}
     */
    @GetMapping
    public ResponseEntity<List<HospitalizationRecordDTO>> getHospitalizationsByPatient(
            @RequestParam UUID patientId) {
        List<HospitalizationRecordDTO> dtos = hospitalizationRepository
            .findByPatientIdOrderByAdmissionDateDesc(patientId)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * POST /api/v1/hospitalizations
     */
    @PostMapping
    public ResponseEntity<HospitalizationRecordDTO> createHospitalization(
            @RequestBody HospitalizationRecordDTO dto) {
        HospitalizationRecord entity = mapper.toEntity(dto);
        HospitalizationRecord saved = hospitalizationRepository.save(entity);
        log.info("Hospitalization created for patient {}", saved.getPatientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(saved));
    }
    
    /**
     * PUT /api/v1/hospitalizations/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<HospitalizationRecordDTO> updateHospitalization(
            @PathVariable UUID id,
            @RequestBody HospitalizationRecordDTO dto) {
        return hospitalizationRepository.findById(id)
            .map(existing -> {
                HospitalizationRecord updated = mapper.toEntity(dto);
                updated.setId(id);
                HospitalizationRecord saved = hospitalizationRepository.save(updated);
                log.info("Hospitalization {} updated", id);
                return ResponseEntity.ok(mapper.toDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/v1/hospitalizations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospitalization(@PathVariable UUID id) {
        if (hospitalizationRepository.existsById(id)) {
            hospitalizationRepository.deleteById(id);
            log.info("Hospitalization {} deleted", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
