package tn.esprit.spring.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.communicationservice.dto.response.GuardianPatientResponse;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.util.List;

@RestController
@RequestMapping("/api/communication/patients")
@RequiredArgsConstructor
public class CommunicationGuardianController {

    private final GuardianPatientResolverService guardianPatientResolverService;

    @GetMapping("/my")
    public List<GuardianPatientResponse> myPatients() {
        return guardianPatientResolverService.getMyPatients();
    }
}
