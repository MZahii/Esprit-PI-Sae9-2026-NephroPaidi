package tn.esprit.spring.procedureservice.shared.mapper;

import tn.esprit.spring.procedureservice.surgical.domain.entity.CareTask;
import tn.esprit.spring.procedureservice.surgical.domain.entity.Complication;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PostOpObservation;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalCase;
import tn.esprit.spring.procedureservice.surgical.dto.response.CareTaskResponse;
import tn.esprit.spring.procedureservice.surgical.dto.response.ComplicationResponse;
import tn.esprit.spring.procedureservice.surgical.dto.response.PostOpObservationResponse;
import tn.esprit.spring.procedureservice.surgical.dto.response.PreOpAssessmentResponse;
import tn.esprit.spring.procedureservice.surgical.dto.response.SurgicalCaseResponse;

public final class SurgicalCaseMapper {
    private SurgicalCaseMapper() {
    }

    public static SurgicalCaseResponse toResponse(SurgicalCase surgicalCase) {
        return new SurgicalCaseResponse(
            surgicalCase.getId(),
            surgicalCase.getPatientId(),
            surgicalCase.getConsultationId(),
            surgicalCase.getAppointmentId(),
            surgicalCase.getSurgeryRequestId(),
            surgicalCase.getFirstName(),
            surgicalCase.getLastName(),
            surgicalCase.getAge(),
            surgicalCase.getGender(),
            surgicalCase.getMedicalRecordNumber(),
            surgicalCase.getSurgeryType(),
            surgicalCase.getProcedureName(),
            surgicalCase.getSurgeryCategory(),
            surgicalCase.getUrgencyLevel(),
            surgicalCase.getSurgeonId(),
            surgicalCase.getAssistantSurgeonId(),
            surgicalCase.getAnesthesiologistId(),
            surgicalCase.getNurseTeam(),
            surgicalCase.getScheduledDate(),
            surgicalCase.getScheduledStartTime(),
            surgicalCase.getEstimatedDurationMinutes(),
            surgicalCase.getOperatingRoom(),
            surgicalCase.getStatus(),
            surgicalCase.getOfferStatus(),
            surgicalCase.getCreatedAt(),
            surgicalCase.getUpdatedAt()
        );
    }

    public static PreOpAssessmentResponse toResponse(PreOpAssessment assessment) {
        Long caseId = assessment.getSurgicalCase() != null ? assessment.getSurgicalCase().getId() : null;
        return new PreOpAssessmentResponse(assessment.getId(), caseId, assessment.getNotes());
    }

    public static PostOpObservationResponse toResponse(PostOpObservation observation) {
        Long caseId = observation.getSurgicalCase() != null ? observation.getSurgicalCase().getId() : null;
        return new PostOpObservationResponse(observation.getId(), caseId, observation.getNotes());
    }

    public static CareTaskResponse toResponse(CareTask task) {
        Long caseId = task.getSurgicalCase() != null ? task.getSurgicalCase().getId() : null;
        return new CareTaskResponse(task.getId(), caseId, task.getTitle(), task.isDone());
    }

    public static ComplicationResponse toResponse(Complication complication) {
        Long caseId = complication.getSurgicalCase() != null ? complication.getSurgicalCase().getId() : null;
        return new ComplicationResponse(complication.getId(), caseId, complication.getDescription());
    }
}
