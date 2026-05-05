package tn.esprit.spring.clinicalservice.labRequest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabResultRepository extends JpaRepository<LabResult, UUID> {
    List<LabResult> findByLabRequestIdOrderByUploadedAtDesc(UUID labRequestId);
    Optional<LabResult> findTopByLabRequestIdOrderByUploadedAtDesc(UUID labRequestId);
}
