package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import tn.esprit.spring.pharmacyservice.entity.Batch;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByMedicationMedicationId(Long medicationId);

    @Query("SELECT b FROM Batch b WHERE b.expirationDate < :today")
    List<Batch> findExpiredBatches(LocalDate today);

    @Query("SELECT b FROM Batch b WHERE b.expirationDate BETWEEN :today AND :threshold")
    List<Batch> findBatchesExpiringBefore(LocalDate today, LocalDate threshold);

    /** FEFO: non-expired batches for a medication, ordered by expiration date ascending. */
    @Query("SELECT b FROM Batch b WHERE b.medication.medicationId = :medicationId " +
           "AND b.expirationDate >= :today ORDER BY b.expirationDate ASC")
    List<Batch> findNonExpiredByMedicationOrderedByExpiry(Long medicationId, LocalDate today);
}
