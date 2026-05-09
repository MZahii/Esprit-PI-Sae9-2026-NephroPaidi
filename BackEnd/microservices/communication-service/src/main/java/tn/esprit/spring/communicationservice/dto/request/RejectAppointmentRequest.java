package tn.esprit.spring.communicationservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectAppointmentRequest {

    @Size(max = 500)
    private String receptionistNotes;
}
