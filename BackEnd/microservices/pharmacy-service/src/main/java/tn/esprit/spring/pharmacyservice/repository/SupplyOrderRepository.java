package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.esprit.spring.pharmacyservice.entity.SupplyOrder;

import java.util.List;

@Repository
public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, Long> {
    List<SupplyOrder> findBySupplierSupplierId(Long supplierId);
    List<SupplyOrder> findByStatus(SupplyOrder.OrderStatus status);
    List<SupplyOrder> findByMedicationId(Long medicationId);
}
