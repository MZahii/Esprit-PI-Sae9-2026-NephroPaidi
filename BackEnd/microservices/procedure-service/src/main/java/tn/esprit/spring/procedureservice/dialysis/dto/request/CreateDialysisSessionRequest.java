package tn.esprit.spring.procedureservice.dialysis.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateDialysisSessionRequest(
    @NotNull Long planId,
    String patientId,
    String consultationId,
    String appointmentId,
    @NotNull LocalDateTime sessionDate,
    String notes,
    String idempotencyKey
) {
}
