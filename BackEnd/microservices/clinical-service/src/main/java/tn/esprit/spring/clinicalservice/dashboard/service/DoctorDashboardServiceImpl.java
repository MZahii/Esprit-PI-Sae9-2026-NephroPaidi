package tn.esprit.spring.clinicalservice.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.spring.clinicalservice.appointment.entity.Appointment;
import tn.esprit.spring.clinicalservice.appointment.repository.AppointmentRepository;
import tn.esprit.spring.clinicalservice.dashboard.dto.DoctorDashboardDto;
import tn.esprit.spring.clinicalservice.labRequest.repository.LabRequestRepository;
import tn.esprit.spring.clinicalservice.surgery.entity.SurgeryIndication;
import tn.esprit.spring.clinicalservice.surgery.repository.SurgeryIndicationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorDashboardServiceImpl implements DoctorDashboardService {

    private final AppointmentRepository appointmentRepository;
    private final LabRequestRepository labRequestRepository;
    private final SurgeryIndicationRepository surgeryIndicationRepository;

    @Override
    public DoctorDashboardDto getDashboardData(UUID doctorId) {
        log.info("Loading dashboard data for doctor: {}", doctorId);

        // Get today's appointments
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Appointment> todayAppointments = appointmentRepository
                .findByDoctorIdAndScheduledAtBetween(doctorId, startOfDay, endOfDay);

        List<DoctorDashboardDto.AppointmentSummary> appointmentSummaries = todayAppointments.stream()
                .map(apt -> DoctorDashboardDto.AppointmentSummary.builder()
                        .id(apt.getId().toString())
                        .patientId(apt.getPatientId())
                        .patientName("Patient " + apt.getPatientId()) // Fetch actual name if needed from patient service
                        .scheduledAt(apt.getScheduledAt().toString())
                        .status(apt.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        // Get lab request counts - using LabUrgency enum values instead of strings
        int pendingLabRequests = 0;
        int completedLabRequests = 0;
        try {
            // TODO: Implement proper lab request queries
            pendingLabRequests = 0;
            completedLabRequests = 0;
        } catch (Exception e) {
            log.warn("Could not fetch lab request counts: {}", e.getMessage());
        }

        // Get surgery indications sent by this doctor
        int surgeriesIndicationsSent = surgeryIndicationRepository
                .findByDoctorIdOrderByCreatedAtDesc(doctorId).size();

        // TODO: Fetch pending appointment requests from communication-service via HTTP
        // TODO: Fetch hospitalized patients from patient-service or administration-service
        int pendingAppointmentRequests = 0;
        int hospitalizedPatients = 0;

        return DoctorDashboardDto.builder()
                .todayAppointments(appointmentSummaries)
                .pendingAppointmentRequests(pendingAppointmentRequests)
                .pendingLabRequests(pendingLabRequests)
                .completedLabRequests(completedLabRequests)
                .surgeriesIndicationsSent(surgeriesIndicationsSent)
                .hospitalizedPatients(hospitalizedPatients)
                .build();
    }
}
