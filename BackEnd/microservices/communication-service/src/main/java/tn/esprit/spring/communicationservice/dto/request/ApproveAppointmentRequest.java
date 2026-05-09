package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ApproveAppointmentRequest {

    @NotNull
    private LocalDateTime scheduledDate;

    @Size(max = 500)
    private String receptionistNotes;
}
