package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.DialysisStock;

import java.util.List;
import java.util.Optional;

@Repository
public interface DialysisStockRepository extends JpaRepository<DialysisStock, Long> {
    Optional<DialysisStock> findByItemItemId(Long itemId);

    @Query("SELECT s FROM DialysisStock s WHERE s.quantityAvailable <= s.item.minimumStock")
    List<DialysisStock> findLowStock();

    @Query("SELECT s FROM DialysisStock s WHERE s.quantityAvailable = 0")
    List<DialysisStock> findOutOfStock();
}