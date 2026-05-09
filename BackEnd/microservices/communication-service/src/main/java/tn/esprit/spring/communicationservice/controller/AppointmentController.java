package tn.esprit.spring.communicationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;
import tn.esprit.spring.communicationservice.dto.request.ApproveAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.RejectAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;
import tn.esprit.spring.communicationservice.dto.response.GuardianPatientResponse;
import tn.esprit.spring.communicationservice.service.AppointmentRequestService;
import tn.esprit.spring.communicationservice.service.GuardianPatientResolverService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRequestService appointmentRequestService;
    private final GuardianPatientResolverService guardianPatientResolverService;

    @PostMapping("/requests")
    public AppointmentRequestResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return appointmentRequestService.create(request);
    }

    @GetMapping("/my")
    public List<AppointmentRequestResponse> my() {
        return appointmentRequestService.myRequests();
    }

    @GetMapping("/my-patients")
    public List<GuardianPatientResponse> myPatients() {
        return guardianPatientResolverService.getMyPatients();
    }

    @GetMapping("/requests")
    public List<AppointmentRequestResponse> requests(@RequestParam(required = false) AppointmentStatus status) {
        return appointmentRequestService.listRequests(status);
    }

    @PostMapping("/requests/{id}/approve")
    public AppointmentRequestResponse approve(@PathVariable UUID id, @Valid @RequestBody ApproveAppointmentRequest request) {
        return appointmentRequestService.approve(id, request);
    }

    @PostMapping("/requests/{id}/reject")
    public AppointmentRequestResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectAppointmentRequest request) {
        return appointmentRequestService.reject(id, request);
    }

    @PostMapping("/requests/{id}/cancel")
    public AppointmentRequestResponse cancel(@PathVariable UUID id) {
        return appointmentRequestService.cancel(id);
    }
}
