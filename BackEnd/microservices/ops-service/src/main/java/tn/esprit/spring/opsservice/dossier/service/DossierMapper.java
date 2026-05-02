package tn.esprit.spring.opsservice.dossier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierContributorResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.DossierEntryResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierResponse;
import tn.esprit.spring.opsservice.dossier.dto.response.PatientDossierSummaryResponse;
import tn.esprit.spring.opsservice.dossier.entity.DossierContributor;
import tn.esprit.spring.opsservice.dossier.entity.DossierEntry;
import tn.esprit.spring.opsservice.dossier.entity.PatientDossier;

import java.util.Map;

@Component
public class DossierMapper {

    private final ObjectMapper objectMapper;

    public DossierMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PatientDossierResponse toResponse(PatientDossier dossier) {
        PatientDossierResponse response = new PatientDossierResponse();
        response.setId(dossier.getId());
        response.setPatientId(dossier.getPatientId());
        response.setSourceConsultationId(dossier.getSourceConsultationId());
        response.setSourceAppointmentId(dossier.getSourceAppointmentId());
        response.setHospitalizationRequestId(dossier.getHospitalizationRequestId());
        response.setPrimaryDoctorId(dossier.getPrimaryDoctorId());
        response.setAssignedNurseId(dossier.getAssignedNurseId());
        if (dossier.getAssignedNurseId() != null) {
            response.setAssignedNurseName("Nurse-" + dossier.getAssignedNurseId().toString().substring(0, 8));
        }
        response.setAdmissionReason(dossier.getAdmissionReason());
        response.setAdmissionPriority(dossier.getAdmissionPriority());
        response.setStatus(dossier.getStatus());
        response.setAdmittedAt(dossier.getAdmittedAt());
        response.setExpectedDischargeAt(dossier.getExpectedDischargeAt());
        response.setDischargedAt(dossier.getDischargedAt());
        response.setArchivedToHistory(dossier.isArchivedToHistory());
        response.setArchivedAt(dossier.getArchivedAt());
        response.setCreatedAt(dossier.getCreatedAt());
        response.setUpdatedAt(dossier.getUpdatedAt());
        return response;
    }

    public PatientDossierSummaryResponse toSummary(PatientDossier dossier) {
        PatientDossierSummaryResponse response = new PatientDossierSummaryResponse();
        response.setId(dossier.getId());
        response.setPatientId(dossier.getPatientId());
        response.setStatus(dossier.getStatus());
        response.setAdmissionPriority(dossier.getAdmissionPriority());
        response.setAssignedNurseId(dossier.getAssignedNurseId());
        response.setAdmittedAt(dossier.getAdmittedAt());
        return response;
    }

    public DossierEntryResponse toEntryResponse(DossierEntry entry) {
        DossierEntryResponse response = new DossierEntryResponse();
        response.setId(entry.getId());
        response.setDossierId(entry.getDossierId());
        response.setEntryType(entry.getEntryType());
        response.setActorRole(entry.getActorRole());
        response.setActorId(entry.getActorId());
        response.setActorDisplayName(entry.getActorDisplayName());
        response.setTitle(entry.getTitle());
        response.setDetails(entry.getDetails());
        response.setSigned(entry.isSigned());
        response.setRequiresSignature(entry.isRequiresSignature());
        response.setOccurredAt(entry.getOccurredAt());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    public DossierContributorResponse toContributorResponse(DossierContributor contributor) {
        DossierContributorResponse response = new DossierContributorResponse();
        response.setId(contributor.getId());
        response.setDossierId(contributor.getDossierId());
        response.setContributorId(contributor.getContributorId());
        response.setContributorRole(contributor.getContributorRole());
        response.setActive(contributor.isActive());
        response.setAddedAt(contributor.getAddedAt());
        response.setRemovedAt(contributor.getRemovedAt());
        return response;
    }

    public String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid vitals payload", ex);
        }
    }
}
