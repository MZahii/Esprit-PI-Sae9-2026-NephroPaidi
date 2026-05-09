package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.DispensationLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DispensationLogRepository extends JpaRepository<DispensationLog, Long> {
    List<DispensationLog> findByDispensedAtBetweenOrderByDispensedAtDesc(
            LocalDateTime start, LocalDateTime end);
}