package tn.esprit.spring.procedureservice.dialysis.dto.response;

import java.time.LocalDateTime;

public record DialysisSessionResponse(
    Long id,
    Long planId,
    String patientId,
    String consultationId,
    String appointmentId,
    LocalDateTime sessionDate,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
