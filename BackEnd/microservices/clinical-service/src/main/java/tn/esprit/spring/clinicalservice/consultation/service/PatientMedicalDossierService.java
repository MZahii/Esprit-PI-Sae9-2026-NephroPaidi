package tn.esprit.spring.clinicalservice.consultation.service;

import tn.esprit.spring.clinicalservice.consultation.dto.PatientMedicalDossierResponse;

import java.util.UUID;

public interface PatientMedicalDossierService {
    PatientMedicalDossierResponse getByConsultationId(UUID consultationId, UUID doctorId);
    PatientMedicalDossierResponse getByPatientId(Long patientId);
}
