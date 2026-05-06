package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.pharmacyservice.dto.PharmacyPrescriptionDTO;
import tn.esprit.spring.pharmacyservice.entity.PharmacyPrescription;
import tn.esprit.spring.pharmacyservice.repository.PharmacyPrescriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PharmacyPrescriptionService {

    private final PharmacyPrescriptionRepository repo;
    private final PharmacyEventPublisher eventPublisher;

    /** Called by doctor (via inter-service call or direct API) to send a prescription to pharmacy */
    public PharmacyPrescriptionDTO receive(PharmacyPrescriptionDTO dto) {
        PharmacyPrescription.Urgency urgency = PharmacyPrescription.Urgency.ROUTINE;
        if (dto.getUrgency() != null) {
            try { urgency = PharmacyPrescription.Urgency.valueOf(dto.getUrgency()); } catch (Exception ignored) {}
        }

        PharmacyPrescription p = PharmacyPrescription.builder()
                .consultationId(dto.getConsultationId())
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .patientAge(dto.getPatientAge())
                .patientWeight(dto.getPatientWeight())
                .doctorId(dto.getDoctorId())
                .doctorName(dto.getDoctorName())
                .urgency(urgency)
                .medicationsJson(dto.getMedicationsJson())
                .notes(dto.getNotes())
                .status(PharmacyPrescription.PrescriptionStatus.PENDING)
                .build();
        PharmacyPrescriptionDTO saved = toDTO(repo.save(p));
        eventPublisher.prescriptionEvent("PRESCRIPTION_CREATED", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PharmacyPrescriptionDTO> getAll() {
        return repo.findAllByOrderByReceivedAtDesc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacyPrescriptionDTO> getByStatus(String status) {
        PharmacyPrescription.PrescriptionStatus s = PharmacyPrescription.PrescriptionStatus.valueOf(status);
        return repo.findByStatusOrderByReceivedAtDesc(s).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PharmacyPrescriptionDTO> getByPatient(Long patientId) {
        return repo.findByPatientIdOrderByReceivedAtDesc(patientId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PharmacyPrescriptionDTO getById(Long id) {
        return toDTO(findById(id));
    }

    /** Pharmacist reviews and verifies the prescription (allergy check, dose check) */
    public PharmacyPrescriptionDTO verify(Long id, String verifiedBy, boolean allergyConfirmed) {
        PharmacyPrescription p = findById(id);
        p.setVerifiedBy(verifiedBy);
        p.setVerifiedAt(LocalDateTime.now());
        p.setAllergyConfirmed(allergyConfirmed);
        if (p.getStatus() == PharmacyPrescription.PrescriptionStatus.PENDING) {
            p.setStatus(PharmacyPrescription.PrescriptionStatus.PROCESSING);
        }
        PharmacyPrescriptionDTO updated = toDTO(repo.save(p));
        eventPublisher.prescriptionEvent("PRESCRIPTION_UPDATED", updated.getId());
        return updated;
    }

    public PharmacyPrescriptionDTO updateStatus(Long id, String status, String processedBy) {
        PharmacyPrescription p = findById(id);
        p.setStatus(PharmacyPrescription.PrescriptionStatus.valueOf(status));
        if (!status.equals("PENDING")) {
            p.setProcessedAt(LocalDateTime.now());
            p.setProcessedBy(processedBy);
        }
        PharmacyPrescriptionDTO updated = toDTO(repo.save(p));
        eventPublisher.prescriptionEvent("PRESCRIPTION_UPDATED", updated.getId());
        return updated;
    }

    private PharmacyPrescription findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found: " + id));
    }

    private PharmacyPrescriptionDTO toDTO(PharmacyPrescription p) {
        return PharmacyPrescriptionDTO.builder()
                .id(p.getId())
                .consultationId(p.getConsultationId())
                .patientId(p.getPatientId())
                .patientName(p.getPatientName())
                .patientAge(p.getPatientAge())
                .patientWeight(p.getPatientWeight())
                .doctorId(p.getDoctorId())
                .doctorName(p.getDoctorName())
                .urgency(p.getUrgency() != null ? p.getUrgency().name() : "ROUTINE")
                .medicationsJson(p.getMedicationsJson())
                .notes(p.getNotes())
                .status(p.getStatus().name())
                .receivedAt(p.getReceivedAt())
                .verifiedBy(p.getVerifiedBy())
                .verifiedAt(p.getVerifiedAt())
                .allergyConfirmed(p.getAllergyConfirmed())
                .processedAt(p.getProcessedAt())
                .processedBy(p.getProcessedBy())
                .build();
    }
}
