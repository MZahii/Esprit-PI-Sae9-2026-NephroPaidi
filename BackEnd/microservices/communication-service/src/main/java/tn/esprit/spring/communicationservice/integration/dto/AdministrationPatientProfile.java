package tn.esprit.spring.communicationservice.integration.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdministrationPatientProfile {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
}
