package tn.esprit.spring.procedureservice.surgical.dto.response;

import java.time.LocalDateTime;

public record SurgeryRequestResponse(
    Long id,
    String patientId,
    String consultationId,
    String requestedByDoctorId,
    String patientFirstName,
    String patientLastName,
    String reason,
    String urgencyLevel,
    String clinicalNote,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
