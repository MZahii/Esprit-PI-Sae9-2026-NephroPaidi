package tn.esprit.spring.clinicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.clinicalservice.entity.TechnicalAct;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TechnicalAct
 */
@Repository
public interface TechnicalActRepository extends JpaRepository<TechnicalAct, UUID> {
    
    /**
     * Find all technical acts for a discharge document
     */
    List<TechnicalAct> findByDischargeId(UUID dischargeId);

    void deleteByDischargeId(UUID dischargeId);
}
