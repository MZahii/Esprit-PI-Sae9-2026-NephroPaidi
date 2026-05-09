package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import tn.esprit.spring.pharmacyservice.entity.Stock;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByBatchId(Long batchId);

    @Query("SELECT s FROM Stock s WHERE s.quantityAvailable = 0")
    List<Stock> findOutOfStock();

    @Query("SELECT s FROM Stock s WHERE s.quantityAvailable <= :threshold")
    List<Stock> findLowStock(int threshold);

    @Query("SELECT COALESCE(SUM(s.quantityAvailable), 0) FROM Stock s WHERE s.batchId IN " +
           "(SELECT b.batchId FROM Batch b WHERE b.medication.medicationId = :medicationId)")
    Integer getTotalStockForMedication(Long medicationId);
}
