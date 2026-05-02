package tn.esprit.spring.Administrationservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.spring.Administrationservice.dto.request.CreatePatientProfileRequest;
import tn.esprit.spring.Administrationservice.dto.response.PatientProfileResponse;
import tn.esprit.spring.Administrationservice.entity.PatientProfile;
import tn.esprit.spring.Administrationservice.entity.PatientSex;
import tn.esprit.spring.Administrationservice.repository.PatientProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientProfileServiceImpl - Profile Flows")
class PatientProfileServiceImplTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @InjectMocks
    private PatientProfileServiceImpl patientProfileService;

    private CreatePatientProfileRequest request;

    @BeforeEach
    void setUp() {
        request = new CreatePatientProfileRequest();
        request.setGuardianUserId(15L);
        request.setFirstName("Ines");
        request.setLastName("Mansouri");
        request.setDateOfBirth(LocalDate.of(2014, 3, 12));
        request.setSex(PatientSex.FEMALE);
        request.setBloodType("O+");
        request.setAllergies("Pollen");
        request.setChronicConditions("Asthma");
        request.setMedicalNotes("Needs inhaler at school");
    }

    @Test
    @DisplayName("create stores a new patient profile")
    void createStoresNewPatientProfile() {
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(invocation -> {
            PatientProfile profile = invocation.getArgument(0);
            profile.setId(101L);
            return profile;
        });

        PatientProfileResponse response = patientProfileService.create(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(15L, response.getGuardianUserId());
        assertEquals("Ines", response.getFirstName());
        assertEquals(PatientSex.FEMALE, response.getSex());
        verify(patientProfileRepository).save(any(PatientProfile.class));
    }

    @Test
    @DisplayName("update replaces the stored patient profile data")
    void updateReplacesPatientProfileData() {
        PatientProfile existing = PatientProfile.builder()
                .id(55L)
                .guardianUserId(8L)
                .firstName("Old")
                .lastName("Name")
                .dateOfBirth(LocalDate.of(2012, 1, 1))
                .sex(PatientSex.MALE)
                .bloodType("A-")
                .build();
        when(patientProfileRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(patientProfileRepository.save(any(PatientProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientProfileResponse response = patientProfileService.update(55L, request);

        assertEquals(15L, response.getGuardianUserId());
        assertEquals("Ines", response.getFirstName());
        assertEquals("O+", response.getBloodType());
        verify(patientProfileRepository).save(any(PatientProfile.class));
    }

    @Test
    @DisplayName("update fails when the patient profile does not exist")
    void updateFailsWhenProfileMissing() {
        when(patientProfileRepository.findById(55L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> patientProfileService.update(55L, request));

        assertEquals("Patient profile not found with id: 55", exception.getMessage());
    }

    @Test
    @DisplayName("getAll maps repository entities to response DTOs")
    void getAllMapsProfilesToResponses() {
        PatientProfile profile = PatientProfile.builder()
                .id(1L)
                .guardianUserId(15L)
                .firstName("Ines")
                .lastName("Mansouri")
                .dateOfBirth(LocalDate.of(2014, 3, 12))
                .sex(PatientSex.FEMALE)
                .bloodType("O+")
                .build();
        when(patientProfileRepository.findAll()).thenReturn(List.of(profile));

        List<PatientProfileResponse> responses = patientProfileService.getAll();

        assertEquals(1, responses.size());
        assertEquals(15L, responses.get(0).getGuardianUserId());
        assertEquals("Ines", responses.get(0).getFirstName());
    }

    @Test
    @DisplayName("getByGuardianUserId delegates to repository filtering")
    void getByGuardianUserIdDelegatesToRepository() {
        PatientProfile profile = PatientProfile.builder()
                .id(1L)
                .guardianUserId(15L)
                .firstName("Ines")
                .lastName("Mansouri")
                .dateOfBirth(LocalDate.of(2014, 3, 12))
                .sex(PatientSex.FEMALE)
                .bloodType("O+")
                .build();
        when(patientProfileRepository.findByGuardianUserId(15L)).thenReturn(List.of(profile));

        List<PatientProfileResponse> responses = patientProfileService.getByGuardianUserId(15L);

        assertEquals(1, responses.size());
        assertEquals(15L, responses.get(0).getGuardianUserId());
        verify(patientProfileRepository).findByGuardianUserId(15L);
    }
}