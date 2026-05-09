package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.PatientProfile;
import tn.esprit.spring.Administrationservice.entity.PatientSex;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PatientProfileResponse {
    private Long id;
    private Long guardianUserId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private PatientSex sex;
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String medicalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PatientProfileResponse from(PatientProfile profile) {
        return PatientProfileResponse.builder()
                .id(profile.getId())
                .guardianUserId(profile.getGuardianUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .dateOfBirth(profile.getDateOfBirth())
                .sex(profile.getSex())
                .bloodType(profile.getBloodType())
                .allergies(profile.getAllergies())
                .chronicConditions(profile.getChronicConditions())
                .medicalNotes(profile.getMedicalNotes())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
