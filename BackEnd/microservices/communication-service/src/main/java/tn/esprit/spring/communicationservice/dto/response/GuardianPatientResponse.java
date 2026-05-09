package tn.esprit.spring.communicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class GuardianPatientResponse {
    private Long patientId;
    private String fullName;
    private LocalDate dob;
}
