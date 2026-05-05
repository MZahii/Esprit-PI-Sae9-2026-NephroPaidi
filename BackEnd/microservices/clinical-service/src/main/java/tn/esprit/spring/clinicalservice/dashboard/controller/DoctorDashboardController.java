package tn.esprit.spring.clinicalservice.dashboard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.clinicalservice.dashboard.dto.DoctorDashboardDto;
import tn.esprit.spring.clinicalservice.dashboard.service.DoctorDashboardService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.util.UUID;

@RestController
@RequestMapping("/clinical/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DoctorDashboardController {

    private final DoctorDashboardService dashboardService;
    private final DoctorIdResolver doctorIdResolver;

    @GetMapping("/my")
    public ResponseEntity<DoctorDashboardDto> getDashboard(
            @RequestHeader(value = "X-Doctor-Id", required = false) String doctorIdHeader,
            Authentication authentication) {
        UUID doctorId = null;
        try {
            if (doctorIdHeader != null && !doctorIdHeader.isEmpty()) {
                doctorId = UUID.fromString(doctorIdHeader);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid UUID format
        }
        
        UUID resolvedId = doctorIdResolver.resolve(doctorId, authentication);
        if (resolvedId == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("Fetching dashboard for doctor: {}", resolvedId);

        DoctorDashboardDto dashboard = dashboardService.getDashboardData(resolvedId);
        return ResponseEntity.ok(dashboard);
    }
}
