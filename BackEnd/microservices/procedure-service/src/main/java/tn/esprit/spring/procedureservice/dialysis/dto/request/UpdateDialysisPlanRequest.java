package tn.esprit.spring.procedureservice.dialysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateDialysisPlanRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String doctorId,
    @NotBlank String dialysisType,
    @NotNull Integer sessionsPerWeek,
    @NotNull Integer sessionDurationMinutes,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotBlank String daysOfWeek,
    Integer bloodFlowRate,
    Integer dialysateFlowRate,
    Integer ultrafiltrationGoal,
    String dialysisCenterId,
    String roomNumber,
    String machineId,
    @NotBlank String status
) {
}
