package tn.esprit.spring.communicationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tn.esprit.spring.communicationservice.dto.response.GuardianPatientResponse;
import tn.esprit.spring.communicationservice.exception.BadRequestException;
import tn.esprit.spring.communicationservice.integration.dto.AdministrationPatientProfile;
import tn.esprit.spring.communicationservice.integration.dto.UserSummary;
import tn.esprit.spring.communicationservice.integration.AdministrationPatientClient;
import tn.esprit.spring.communicationservice.integration.UserDirectoryClient;
import tn.esprit.spring.communicationservice.security.CurrentUserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardianPatientResolverService {

    private final CurrentUserService currentUserService;
    private final UserDirectoryClient userDirectoryClient;
    private final AdministrationPatientClient administrationPatientClient;

    public List<GuardianPatientResponse> getMyPatients() {
        List<AdministrationPatientProfile> linkedPatients = getLinkedPatients();
        if (linkedPatients.isEmpty()) {
            throw new BadRequestException("No linked patient found for this guardian");
        }

        return linkedPatients.stream()
                .map(patient -> GuardianPatientResponse.builder()
                .patientId(patient.getId())
                .fullName(patient.getFirstName() + " " + patient.getLastName())
                .dob(patient.getDateOfBirth())
                        .build())
                .toList();
    }

    public Long resolvePatientIdForMessage(Long requestedPatientId) {
        List<AdministrationPatientProfile> linkedPatients = getLinkedPatients();

        if (linkedPatients.isEmpty()) {
            throw new BadRequestException("No linked patient found for this guardian");
        }

        if (requestedPatientId != null) {
            boolean linked = linkedPatients.stream().anyMatch(patient -> requestedPatientId.equals(patient.getId()));
            if (!linked) {
                throw new AccessDeniedException("Selected patient is not linked to the current guardian");
            }
            return requestedPatientId;
        }

        if (linkedPatients.size() == 1) {
            return linkedPatients.get(0).getId();
        }

        throw new BadRequestException("Multiple linked patients found. Please select a patient.");
    }

    private List<AdministrationPatientProfile> getLinkedPatients() {
        currentUserService.requireRole("GUARDIAN");
        String keycloakId = currentUserService.getCurrentUserSub();
        String preferredUsername = currentUserService.getPreferredUsernameOrNull();

        UserSummary guardian = userDirectoryClient.resolveGuardian(keycloakId, preferredUsername);
        if (guardian == null || guardian.getId() == null) {
            throw new BadRequestException("Unable to resolve current guardian account");
        }

        return administrationPatientClient.getByGuardianUserId(guardian.getId());
    }
}
