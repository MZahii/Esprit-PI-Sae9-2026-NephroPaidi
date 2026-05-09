package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AppointmentRequestResponse {
    private UUID id;
    private Long patientId;
    private String guardianKeycloakId;
    private LocalDateTime requestedDate;
    private String reason;
    private AppointmentStatus status;
    private LocalDateTime scheduledDate;
    private String receptionistNotes;
    private Instant createdAt;
    private Instant updatedAt;
}
