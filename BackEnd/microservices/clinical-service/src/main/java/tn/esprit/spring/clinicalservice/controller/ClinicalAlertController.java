package tn.esprit.spring.clinicalservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.clinicalservice.dto.ClinicalAlertDTO;
import tn.esprit.spring.clinicalservice.entity.ClinicalAlert;
import tn.esprit.spring.clinicalservice.enums.AlertSeverity;
import tn.esprit.spring.clinicalservice.mapper.ClinicalAlertMapper;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Clinical Alert endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
public class ClinicalAlertController {
    
    @Autowired
    private ClinicalAlertRepository alertRepository;
    
    @Autowired
    private ClinicalAlertMapper mapper;
    
    /**
     * GET /api/v1/alerts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClinicalAlertDTO> getAlert(@PathVariable UUID id) {
        return alertRepository.findById(id)
            .map(entity -> ResponseEntity.ok(mapper.toDTO(entity)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/v1/alerts?patientId={patientId} - Active alerts only
     */
    @GetMapping
    public ResponseEntity<List<ClinicalAlertDTO>> getActiveAlerts(@RequestParam UUID patientId) {
        List<ClinicalAlertDTO> dtos = alertRepository
            .findByPatientIdAndResolvedFalseOrderByCreatedAtDesc(patientId)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * GET /api/v1/alerts/urgent?patientId={patientId} - Urgent/Warning alerts
     */
    @GetMapping("/urgent")
    public ResponseEntity<List<ClinicalAlertDTO>> getUrgentAlerts(@RequestParam UUID patientId) {
        List<ClinicalAlertDTO> warning = alertRepository
            .findByPatientIdAndSeverityAndResolvedFalseOrderByCreatedAtDesc(patientId, AlertSeverity.WARNING)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        
        List<ClinicalAlertDTO> urgent = alertRepository
            .findByPatientIdAndSeverityAndResolvedFalseOrderByCreatedAtDesc(patientId, AlertSeverity.URGENT)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
        
        List<ClinicalAlertDTO> combined = warning;
        combined.addAll(urgent);
        return ResponseEntity.ok(combined);
    }
    
    /**
     * POST /api/v1/alerts/{id}/acknowledge - Mark alert as acknowledged
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ClinicalAlertDTO> acknowledgeAlert(@PathVariable UUID id) {
        return alertRepository.findById(id)
            .map(alert -> {
                alert.setAcknowledgedAt(LocalDateTime.now());
                ClinicalAlert saved = alertRepository.save(alert);
                log.info("Alert {} acknowledged", id);
                return ResponseEntity.ok(mapper.toDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * POST /api/v1/alerts/{id}/resolve - Mark alert as resolved
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ClinicalAlertDTO> resolveAlert(@PathVariable UUID id) {
        return alertRepository.findById(id)
            .map(alert -> {
                alert.setResolved(true);
                alert.setResolvedAt(LocalDateTime.now());
                ClinicalAlert saved = alertRepository.save(alert);
                log.info("Alert {} resolved", id);
                return ResponseEntity.ok(mapper.toDTO(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * DELETE /api/v1/alerts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        if (alertRepository.existsById(id)) {
            alertRepository.deleteById(id);
            log.info("Alert {} deleted", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
