package tn.esprit.spring.clinicalservice.appointment.service;

import tn.esprit.spring.clinicalservice.appointment.dto.AppointmentResponse;

import java.util.UUID;

public interface AppointmentStartService {
    AppointmentResponse startConsultation(UUID appointmentId);
    AppointmentResponse receptionistOverrideWindow(UUID appointmentId, int extensionMinutes);
    void autoCancelStaleAppointments();
}
