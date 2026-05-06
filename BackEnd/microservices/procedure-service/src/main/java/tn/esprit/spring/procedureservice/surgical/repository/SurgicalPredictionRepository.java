package tn.esprit.spring.procedureservice.surgical.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalPrediction;

import java.util.List;

public interface SurgicalPredictionRepository extends JpaRepository<SurgicalPrediction, Long> {
    List<SurgicalPrediction> findBySurgicalCaseIdOrderByCreatedAtDesc(Long surgicalCaseId);
}
