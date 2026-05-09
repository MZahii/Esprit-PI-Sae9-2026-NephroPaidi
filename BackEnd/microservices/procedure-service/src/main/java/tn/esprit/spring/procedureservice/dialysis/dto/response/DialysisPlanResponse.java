package tn.esprit.spring.procedureservice.dialysis.dto.response;

import java.time.LocalDate;

public record DialysisPlanResponse(
    Long id,
    String patientId,
    String firstName,
    String lastName,
    String doctorId,
    String dialysisType,
    Integer sessionsPerWeek,
    Integer sessionDurationMinutes,
    LocalDate startDate,
    LocalDate endDate,
    String daysOfWeek,
    Integer bloodFlowRate,
    Integer dialysateFlowRate,
    Integer ultrafiltrationGoal,
    String dialysisCenterId,
    String roomNumber,
    String machineId,
    String status
) {
}
