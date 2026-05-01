package tn.esprit.spring.clinicalservice.dashboard.service;

import tn.esprit.spring.clinicalservice.dashboard.dto.DoctorDashboardDto;

import java.util.UUID;

public interface DoctorDashboardService {
    DoctorDashboardDto getDashboardData(UUID doctorId);
}
