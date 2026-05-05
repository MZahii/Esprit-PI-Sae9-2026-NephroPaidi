package tn.esprit.spring.opsservice.hospitalization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationTaskExecution;

import java.util.UUID;

public interface HospitalizationTaskExecutionRepository extends JpaRepository<HospitalizationTaskExecution, UUID> {
}
