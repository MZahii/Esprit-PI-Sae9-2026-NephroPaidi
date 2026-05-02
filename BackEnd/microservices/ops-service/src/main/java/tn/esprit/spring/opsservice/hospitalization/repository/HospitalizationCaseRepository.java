package tn.esprit.spring.opsservice.hospitalization.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationCase;
import tn.esprit.spring.opsservice.hospitalization.domain.HospitalizationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HospitalizationCaseRepository extends JpaRepository<HospitalizationCase, UUID> {

    @EntityGraph(attributePaths = {"tasks", "tasks.executions"})
    Optional<HospitalizationCase> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"tasks"})
    List<HospitalizationCase> findByStatusOrderByCreatedAtDesc(HospitalizationStatus status);
}
