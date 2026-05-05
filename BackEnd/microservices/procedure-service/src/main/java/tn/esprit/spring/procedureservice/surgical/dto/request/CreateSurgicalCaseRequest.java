package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateSurgicalCaseRequest(
    @NotBlank String patientId,
    String consultationId,
    String appointmentId,
    Long surgeryRequestId,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotNull Integer age,
    @NotBlank String gender,
    @NotBlank String medicalRecordNumber,
    @NotBlank String surgeryType,
    @NotBlank String procedureName,
    @NotBlank String surgeryCategory,
    @NotBlank String urgencyLevel,
    @NotBlank String surgeonId,
    String assistantSurgeonId,
    String anesthesiologistId,
    String nurseTeam,
    @NotNull LocalDate scheduledDate,
    @NotNull LocalTime scheduledStartTime,
    @NotNull Integer estimatedDurationMinutes,
    String operatingRoom,
    @NotBlank String status
) {
}
