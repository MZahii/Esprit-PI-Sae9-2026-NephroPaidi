package tn.esprit.spring.procedureservice.surgical.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.surgical.domain.entity.PostOpObservation;

public interface PostOpObservationRepository extends JpaRepository<PostOpObservation, Long> {
    List<PostOpObservation> findBySurgicalCaseIdOrderByIdDesc(Long surgicalCaseId);
}
