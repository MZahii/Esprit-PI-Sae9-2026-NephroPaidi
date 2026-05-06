package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.DialysisItem;

import java.util.List;

@Repository
public interface DialysisItemRepository extends JpaRepository<DialysisItem, Long> {
    List<DialysisItem> findByCategoryIgnoreCase(String category);
}
