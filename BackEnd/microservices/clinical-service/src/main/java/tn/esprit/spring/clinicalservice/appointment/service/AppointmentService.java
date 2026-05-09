package tn.esprit.spring.clinicalservice.appointment.service;

import tn.esprit.spring.clinicalservice.appointment.dto.*;
import tn.esprit.spring.clinicalservice.appointment.entity.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse create(AppointmentCreateRequest request);

    AppointmentResponse update(UUID id, AppointmentUpdateRequest request);

    AppointmentResponse getById(UUID id);

    List<AppointmentResponse> list(UUID doctorId, Long patientId, AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    AppointmentResponse cancel(UUID id, AppointmentCancelRequest request);

    StartConsultationResponse startConsultation(UUID id, UUID doctorId);

    AppointmentAvailabilityResponse checkAvailability(UUID doctorId, LocalDateTime from, LocalDateTime to);

    void delete(UUID id);
}
