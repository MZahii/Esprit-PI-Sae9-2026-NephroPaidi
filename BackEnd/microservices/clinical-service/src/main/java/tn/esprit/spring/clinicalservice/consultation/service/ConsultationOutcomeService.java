package tn.esprit.spring.clinicalservice.consultation.service;

import tn.esprit.spring.clinicalservice.consultation.dto.ConsultationOutcomeResponse;

import java.util.UUID;

public interface ConsultationOutcomeService {
    ConsultationOutcomeResponse updateNotes(UUID consultationId, UUID doctorId, String content);
    ConsultationOutcomeResponse updateDiagnosis(UUID consultationId, UUID doctorId, String content);
    ConsultationOutcomeResponse updatePrescriptions(UUID consultationId, UUID doctorId, String content);
    ConsultationOutcomeResponse updateLabRequests(UUID consultationId, UUID doctorId, String content);
    ConsultationOutcomeResponse updateTreatmentPlan(UUID consultationId, UUID doctorId, String content);
    ConsultationOutcomeResponse getOutcome(UUID consultationId, UUID doctorId);
}
