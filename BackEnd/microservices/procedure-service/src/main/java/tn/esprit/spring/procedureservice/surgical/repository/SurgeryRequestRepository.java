package tn.esprit.spring.procedureservice.surgical.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgeryRequest;

public interface SurgeryRequestRepository extends JpaRepository<SurgeryRequest, Long> {
    List<SurgeryRequest> findAllByOrderByCreatedAtDesc();

    List<SurgeryRequest> findByConsultationIdOrderByCreatedAtDesc(String consultationId);

    List<SurgeryRequest> findByStatusOrderByCreatedAtDesc(String status);
}
