package tn.esprit.spring.clinicalservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.clinicalservice.entity.ConsultationRecord;
import tn.esprit.spring.clinicalservice.entity.MedicalDossierEntry;
import tn.esprit.spring.clinicalservice.enums.EntryType;
import tn.esprit.spring.clinicalservice.mapper.ClinicalAlertMapper;
import tn.esprit.spring.clinicalservice.repository.ClinicalAlertRepository;
import tn.esprit.spring.clinicalservice.repository.ConsultationRecordRepository;
import tn.esprit.spring.clinicalservice.repository.MedicalDossierRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientClinicalController {

    @Autowired
    private ConsultationRecordRepository consultationRepository;

    @Autowired
    private ClinicalAlertRepository clinicalAlertRepository;

    @Autowired
    private ClinicalAlertMapper clinicalAlertMapper;

    @Autowired
    private MedicalDossierRepository medicalDossierRepository;

    @GetMapping("/{patientId}/gfr")
    public ResponseEntity<?> getLatestGfr(@PathVariable UUID patientId) {
        return consultationRepository.findByPatientIdOrderByConsultationDateDesc(patientId)
            .stream()
            .filter(this::hasComputedGfr)
            .findFirst()
            .<ResponseEntity<?>>map(consultation -> ResponseEntity.ok(buildGfrResponse(consultation)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{patientId}/alerts")
    public ResponseEntity<?> getPatientAlerts(@PathVariable UUID patientId) {
        return ResponseEntity.ok(
            clinicalAlertRepository.findByPatientIdAndResolvedFalseOrderByCreatedAtDesc(patientId)
                .stream()
                .map(clinicalAlertMapper::toDTO)
                .collect(Collectors.toList())
        );
    }

    @GetMapping("/{patientId}/medical-dossier")
    public ResponseEntity<List<MedicalDossierEntry>> getMedicalDossier(
            @PathVariable Long patientId,
            @RequestParam(required = false) String filters) {
        if (filters == null || filters.isBlank()) {
            return ResponseEntity.ok(medicalDossierRepository.findByPatientIdOrderByCreatedAtDesc(patientId));
        }

        List<EntryType> entryTypes = parseEntryTypes(filters);
        if (entryTypes.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<MedicalDossierEntry> results = new ArrayList<>();
        for (EntryType entryType : entryTypes) {
            results.addAll(medicalDossierRepository.findByPatientIdAndEntryTypeOrderByCreatedAtDesc(patientId, entryType));
        }

        results.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        return ResponseEntity.ok(results);
    }

    private boolean hasComputedGfr(ConsultationRecord consultation) {
        return consultation.getNephologyRecord() != null
            && consultation.getNephologyRecord().getEGFR() != null;
    }

    private Map<String, Object> buildGfrResponse(ConsultationRecord consultation) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("consultationId", consultation.getId());
        response.put("patientId", consultation.getPatientId());
        response.put("consultationDate", consultation.getConsultationDate());
        response.put("eGFR", consultation.getNephologyRecord().getEGFR());
        response.put("ckdStage", consultation.getNephologyRecord().getCkdStage());
        response.put("schwartzK", consultation.getNephologyRecord().getSchwartz_k());
        return response;
    }

    private List<EntryType> parseEntryTypes(String filters) {
        return Arrays.stream(filters.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> value.toUpperCase(Locale.ROOT))
            .map(this::safeEntryType)
            .filter(entryType -> entryType != null)
            .collect(Collectors.toList());
    }

    private EntryType safeEntryType(String value) {
        try {
            return EntryType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
