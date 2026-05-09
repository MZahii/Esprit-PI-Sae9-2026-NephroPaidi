package tn.esprit.spring.procedureservice.shared.mapper;

import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisOutcome;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPrescription;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisSession;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisOutcomeResponse;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisPlanResponse;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisPrescriptionResponse;
import tn.esprit.spring.procedureservice.dialysis.dto.response.DialysisSessionResponse;

public final class DialysisMapper {
    private DialysisMapper() {
    }

    public static DialysisPlanResponse toResponse(DialysisPlan plan) {
        return new DialysisPlanResponse(
            plan.getId(),
            plan.getPatientId(),
            plan.getFirstName(),
            plan.getLastName(),
            plan.getDoctorId(),
            plan.getDialysisType(),
            plan.getSessionsPerWeek(),
            plan.getSessionDurationMinutes(),
            plan.getStartDate(),
            plan.getEndDate(),
            plan.getDaysOfWeek(),
            plan.getBloodFlowRate(),
            plan.getDialysateFlowRate(),
            plan.getUltrafiltrationGoal(),
            plan.getDialysisCenterId(),
            plan.getRoomNumber(),
            plan.getMachineId(),
            plan.getStatus()
        );
    }

    public static DialysisSessionResponse toResponse(DialysisSession session) {
        Long planId = session.getPlan() != null ? session.getPlan().getId() : null;
        return new DialysisSessionResponse(
            session.getId(),
            planId,
            session.getPatientId(),
            session.getConsultationId(),
            session.getAppointmentId(),
            session.getSessionDate(),
            session.getNotes(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }

    public static DialysisOutcomeResponse toResponse(DialysisOutcome outcome) {
        Long sessionId = outcome.getSession() != null ? outcome.getSession().getId() : null;
        return new DialysisOutcomeResponse(outcome.getId(), sessionId, outcome.isValidated(), outcome.getSummary());
    }

    public static DialysisPrescriptionResponse toResponse(DialysisPrescription prescription) {
        Long planId = prescription.getPlan() != null ? prescription.getPlan().getId() : null;
        return new DialysisPrescriptionResponse(prescription.getId(), planId, prescription.getDetails());
    }
}
