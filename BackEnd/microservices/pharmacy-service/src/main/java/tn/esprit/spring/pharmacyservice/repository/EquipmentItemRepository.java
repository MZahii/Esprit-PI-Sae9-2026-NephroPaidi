package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.EquipmentItem;

import java.util.List;

@Repository
public interface EquipmentItemRepository extends JpaRepository<EquipmentItem, Long> {
    List<EquipmentItem> findByCategoryIgnoreCase(String category);
}