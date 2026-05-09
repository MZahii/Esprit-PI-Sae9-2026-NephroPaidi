package tn.esprit.spring.communicationservice.service;

import tn.esprit.spring.communicationservice.domain.enums.AppointmentStatus;
import tn.esprit.spring.communicationservice.dto.request.ApproveAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.CreateAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.request.RejectAppointmentRequest;
import tn.esprit.spring.communicationservice.dto.response.AppointmentRequestResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentRequestService {
    AppointmentRequestResponse create(CreateAppointmentRequest request);

    List<AppointmentRequestResponse> myRequests();

    List<AppointmentRequestResponse> listRequests(AppointmentStatus status);

    AppointmentRequestResponse approve(UUID id, ApproveAppointmentRequest request);

    AppointmentRequestResponse reject(UUID id, RejectAppointmentRequest request);

    AppointmentRequestResponse cancel(UUID id);
}
