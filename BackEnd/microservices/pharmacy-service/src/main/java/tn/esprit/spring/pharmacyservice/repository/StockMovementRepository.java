package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByStockTypeOrderByTakenAtDesc(StockMovement.StockType stockType);
    List<StockMovement> findByItemIdAndStockTypeOrderByTakenAtDesc(Long itemId, StockMovement.StockType stockType);
    List<StockMovement> findByTakenAtAfterOrderByTakenAtDesc(LocalDateTime since);
    List<StockMovement> findAllByOrderByTakenAtDesc();
}