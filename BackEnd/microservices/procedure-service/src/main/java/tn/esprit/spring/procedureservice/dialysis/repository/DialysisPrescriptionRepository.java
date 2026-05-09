package tn.esprit.spring.procedureservice.dialysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPrescription;

public interface DialysisPrescriptionRepository extends JpaRepository<DialysisPrescription, Long> {
}
