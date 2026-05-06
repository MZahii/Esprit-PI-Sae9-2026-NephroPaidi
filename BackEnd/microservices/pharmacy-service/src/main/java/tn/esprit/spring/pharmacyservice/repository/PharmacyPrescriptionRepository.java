package tn.esprit.spring.pharmacyservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.pharmacyservice.entity.PharmacyPrescription;

import java.util.List;

@Repository
public interface PharmacyPrescriptionRepository extends JpaRepository<PharmacyPrescription, Long> {
    List<PharmacyPrescription> findByStatusOrderByReceivedAtDesc(PharmacyPrescription.PrescriptionStatus status);
    List<PharmacyPrescription> findByPatientIdOrderByReceivedAtDesc(Long patientId);
    List<PharmacyPrescription> findAllByOrderByReceivedAtDesc();
}