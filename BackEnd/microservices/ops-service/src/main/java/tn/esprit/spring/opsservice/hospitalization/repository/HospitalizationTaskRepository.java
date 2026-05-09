package tn.esprit.spring.opsservice.hospitalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTask;

import java.util.UUID;

public interface HospitalizationTaskRepository extends JpaRepository<HospitalizationTask, UUID> {
}
