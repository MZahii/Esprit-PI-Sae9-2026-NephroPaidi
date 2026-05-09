package tn.esprit.spring.procedureservice.surgical.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PreOpAssessment;

public interface PreOpAssessmentRepository extends JpaRepository<PreOpAssessment, Long> {
    @Query("SELECT pa FROM PreOpAssessment pa WHERE pa.surgicalCase.id = :surgicalCaseId ORDER BY pa.id DESC LIMIT 1")
    Optional<PreOpAssessment> findTopBySurgicalCaseIdOrderByIdDesc(@Param("surgicalCaseId") Long surgicalCaseId);
}
