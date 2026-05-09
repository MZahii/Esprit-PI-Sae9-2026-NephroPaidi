package tn.esprit.spring.Administrationservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.PatientSex;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePatientProfileRequest {

    @NotNull(message = "Guardian user id is required")
    private Long guardianUserId;

    @NotBlank(message = "Patient first name is required")
    private String firstName;

    @NotBlank(message = "Patient last name is required")
    private String lastName;

    @NotNull(message = "Patient date of birth is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @NotNull(message = "Patient sex is required")
    private PatientSex sex;

    private String bloodType;

    private String allergies;

    private String chronicConditions;

    private String medicalNotes;
}
