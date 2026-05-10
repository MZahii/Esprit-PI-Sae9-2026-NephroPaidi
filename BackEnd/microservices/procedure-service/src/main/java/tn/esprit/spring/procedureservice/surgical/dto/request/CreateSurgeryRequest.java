package tn.esprit.spring.procedureservice.surgical.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSurgeryRequest(
    @NotBlank String patientId,
    @NotBlank String consultationId,
    @NotBlank String requestedByDoctorId,
    @NotBlank String patientFirstName,
    @NotBlank String patientLastName,
    @NotBlank String reason,
    @NotBlank String urgencyLevel,
    String clinicalNote
) {
}
