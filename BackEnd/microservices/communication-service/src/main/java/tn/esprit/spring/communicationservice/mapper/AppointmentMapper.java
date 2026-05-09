package tn.esprit.spring.communicationservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.spring.communicationservice.domain.entity.AppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;

@Component
public class AppointmentMapper {

    public AppointmentRequestResponse toResponse(AppointmentRequest entity) {
        return AppointmentRequestResponse.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .guardianKeycloakId(entity.getGuardianKeycloakId())
                .requestedDate(entity.getRequestedDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .scheduledDate(entity.getScheduledDate())
                .receptionistNotes(entity.getReceptionistNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
