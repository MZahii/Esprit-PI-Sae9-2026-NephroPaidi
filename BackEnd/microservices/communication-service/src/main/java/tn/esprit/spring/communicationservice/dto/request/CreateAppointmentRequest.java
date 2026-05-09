package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateAppointmentRequest {

    private Long patientId;

    private LocalDateTime requestedDate;

    @NotNull
    @Size(min = 1, max = 500)
    private String reason;
}
