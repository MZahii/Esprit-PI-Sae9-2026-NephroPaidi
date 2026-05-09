package tn.esprit.spring.clinicalservice.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDetails {
    private Long id;
    private Long guardianUserId;
    private String firstName;
    private String lastName;
}
