package tn.esprit.spring.clinicalservice.consultation.medication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, UUID> {
    List<MedicationOrder> findByConsultationId(UUID consultationId);
}
