package tn.esprit.spring.Administrationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.Administrationservice.dto.request.CreatePatientProfileRequest;
import tn.esprit.spring.Administrationservice.dto.response.PatientProfileResponse;
import tn.esprit.spring.Administrationservice.entity.PatientProfile;
import tn.esprit.spring.Administrationservice.repository.PatientProfileRepository;
import tn.esprit.spring.Administrationservice.service.PatientProfileService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;

    @Override
    public PatientProfileResponse create(CreatePatientProfileRequest request) {
        PatientProfile profile = patientProfileRepository.save(
                PatientProfile.builder()
                        .guardianUserId(request.getGuardianUserId())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .dateOfBirth(request.getDateOfBirth())
                        .sex(request.getSex())
                        .bloodType(request.getBloodType())
                        .allergies(request.getAllergies())
                        .chronicConditions(request.getChronicConditions())
                        .medicalNotes(request.getMedicalNotes())
                        .build()
        );

        return PatientProfileResponse.from(profile);
    }

    @Override
    public PatientProfileResponse update(Long patientId, CreatePatientProfileRequest request) {
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found with id: " + patientId));

        profile.setGuardianUserId(request.getGuardianUserId());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setSex(request.getSex());
        profile.setBloodType(request.getBloodType());
        profile.setAllergies(request.getAllergies());
        profile.setChronicConditions(request.getChronicConditions());
        profile.setMedicalNotes(request.getMedicalNotes());

        return PatientProfileResponse.from(patientProfileRepository.save(profile));
    }

    @Override
    public List<PatientProfileResponse> getAll() {
        return patientProfileRepository.findAll()
                .stream()
                .map(PatientProfileResponse::from)
                .toList();
    }

    @Override
    public PatientProfileResponse getById(Long patientId) {
        return patientProfileRepository.findById(patientId)
                .map(PatientProfileResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found with id: " + patientId));
    }

    @Override
    public List<PatientProfileResponse> getByIds(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return List.of();
        }
        return patientProfileRepository.findAllById(
                        patientIds.stream().filter(Objects::nonNull).distinct().toList()
                )
                .stream()
                .map(PatientProfileResponse::from)
                .toList();
    }

    @Override
    public List<PatientProfileResponse> search(String query, int limit) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.length() < 1) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return patientProfileRepository.searchByQuery(safeQuery)
                .stream()
                .limit(safeLimit)
                .map(PatientProfileResponse::from)
                .toList();
    }

    @Override
    public List<PatientProfileResponse> getByGuardianUserId(Long guardianUserId) {
        return patientProfileRepository.findByGuardianUserId(guardianUserId)
                .stream()
                .map(PatientProfileResponse::from)
                .toList();
    }
}
