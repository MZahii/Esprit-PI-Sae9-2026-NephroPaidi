package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.EquipmentStock;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentStockRepository extends JpaRepository<EquipmentStock, Long> {
    Optional<EquipmentStock> findByItemItemId(Long itemId);

    @Query("SELECT s FROM EquipmentStock s WHERE s.quantityAvailable <= s.item.minimumStock")
    List<EquipmentStock> findLowStock();

    @Query("SELECT s FROM EquipmentStock s WHERE s.quantityAvailable = 0")
    List<EquipmentStock> findOutOfStock();
}