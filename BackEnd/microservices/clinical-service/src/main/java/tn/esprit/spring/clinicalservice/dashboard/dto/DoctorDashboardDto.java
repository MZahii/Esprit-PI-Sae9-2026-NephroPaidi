package tn.esprit.spring.clinicalservice.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDashboardDto {
    private List<AppointmentSummary> todayAppointments;
    private int pendingAppointmentRequests;
    private int pendingLabRequests;
    private int completedLabRequests;
    private int surgeriesIndicationsSent;
    private int hospitalizedPatients;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentSummary {
        private String id;
        private Long patientId;
        private String patientName;
        private String scheduledAt;
        private String status;
    }
}
