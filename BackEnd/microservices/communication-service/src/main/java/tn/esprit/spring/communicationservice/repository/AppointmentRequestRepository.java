package tn.esprit.spring.communicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.communicationservice.domain.entity.AppointmentRequest;

import java.util.List;
import java.util.UUID;

public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest, UUID> {
    List<AppointmentRequest> findByGuardianKeycloakIdOrderByCreatedAtDesc(String guardianKeycloakId);

    List<AppointmentRequest> findAllByOrderByCreatedAtDesc();
}
