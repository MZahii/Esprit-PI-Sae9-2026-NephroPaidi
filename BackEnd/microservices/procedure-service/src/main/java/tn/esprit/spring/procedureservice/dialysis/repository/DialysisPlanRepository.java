package tn.esprit.spring.procedureservice.dialysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.dialysis.domain.entity.DialysisPlan;

public interface DialysisPlanRepository extends JpaRepository<DialysisPlan, Long> {
}
