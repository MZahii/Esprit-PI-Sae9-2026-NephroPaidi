package tn.esprit.spring.clinicalservice.consultation.section;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationSectionRepository extends JpaRepository<ConsultationSection, UUID> {
    List<ConsultationSection> findByConsultationId(UUID consultationId);
    Optional<ConsultationSection> findByConsultationIdAndSectionType(UUID consultationId, ConsultationSectionType sectionType);
}
