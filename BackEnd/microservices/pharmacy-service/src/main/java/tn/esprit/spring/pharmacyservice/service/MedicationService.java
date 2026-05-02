package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.BatchDTO;
import tn.esprit.spring.pharmacyservice.dto.MedicationDTO;
import tn.esprit.spring.pharmacyservice.dto.ReorderAlertDTO;
import tn.esprit.spring.pharmacyservice.entity.Batch;
import tn.esprit.spring.pharmacyservice.entity.Medication;
import tn.esprit.spring.pharmacyservice.repository.BatchRepository;
import tn.esprit.spring.pharmacyservice.repository.MedicationRepository;
import tn.esprit.spring.pharmacyservice.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final BatchRepository batchRepository;
    private final StockRepository stockRepository;

    // ─── Medication CRUD ───────────────────────────────────────────────────────

    public MedicationDTO createMedication(MedicationDTO dto) {
        Medication med = Medication.builder()
                .name(dto.getName())
                .form(dto.getForm())
                .pediatricDosage(dto.getPediatricDosage())
                .minimumStock(dto.getMinimumStock())
                .build();
        return toMedicationDTO(medicationRepository.save(med));
    }

    public MedicationDTO getMedication(Long id) {
        return toMedicationDTO(findMedById(id));
    }

    public List<MedicationDTO> getAllMedications() {
        return getAllMedications(null, null, null);
    }

    /**
     * Filtered + sorted list of medications.
     *
     * @param name case-insensitive contains search on medication name
     * @param form exact form filter (tablet, syrup, …) — case-insensitive
     * @param sort "az" → A→Z, "za" → Z→A, null → insertion order
     */
    public List<MedicationDTO> getAllMedications(String name, String form, String sort) {
        Stream<Medication> stream = medicationRepository.findAll().stream();

        if (name != null && !name.isBlank())
            stream = stream.filter(m -> m.getName().toLowerCase()
                    .contains(name.trim().toLowerCase()));

        if (form != null && !form.isBlank())
            stream = stream.filter(m -> form.equalsIgnoreCase(m.getForm()));

        List<MedicationDTO> list = stream.map(this::toMedicationDTO).collect(Collectors.toList());

        if ("az".equalsIgnoreCase(sort))
            list.sort(Comparator.comparing(MedicationDTO::getName, String.CASE_INSENSITIVE_ORDER));
        else if ("za".equalsIgnoreCase(sort))
            list.sort(Comparator.comparing(MedicationDTO::getName, String.CASE_INSENSITIVE_ORDER).reversed());

        return list;
    }

    public MedicationDTO updateMedication(Long id, MedicationDTO dto) {
        Medication med = findMedById(id);
        med.setName(dto.getName());
        med.setForm(dto.getForm());
        med.setPediatricDosage(dto.getPediatricDosage());
        med.setMinimumStock(dto.getMinimumStock());
        return toMedicationDTO(medicationRepository.save(med));
    }

    public void deleteMedication(Long id) {
        medicationRepository.deleteById(id);
    }

    // ─── Batch CRUD ────────────────────────────────────────────────────────────

    public BatchDTO addBatch(Long medicationId, BatchDTO dto) {
        Medication med = findMedById(medicationId);
        Batch batch = Batch.builder()
                .batchNumber(dto.getBatchNumber())
                .manufactureDate(dto.getManufactureDate())
                .expirationDate(dto.getExpirationDate())
                .quantity(dto.getQuantity())
                .medication(med)
                .build();
        return toBatchDTO(batchRepository.save(batch));
    }

    public List<BatchDTO> getBatchesForMedication(Long medicationId) {
        return batchRepository.findByMedicationMedicationId(medicationId).stream()
                .map(this::toBatchDTO)
                .collect(Collectors.toList());
    }

    public List<BatchDTO> getExpiredBatches() {
        return batchRepository.findExpiredBatches(LocalDate.now()).stream()
                .map(this::toBatchDTO)
                .collect(Collectors.toList());
    }

    public List<BatchDTO> getBatchesExpiringSoon(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return batchRepository.findBatchesExpiringBefore(LocalDate.now(), threshold).stream()
                .map(this::toBatchDTO)
                .collect(Collectors.toList());
    }

    // ─── Reorder Alerts ────────────────────────────────────────────────────────

    /**
     * Returns all medications whose total available stock is below their configured minimumStock threshold.
     */
    public List<ReorderAlertDTO> getMedicationsNeedingReorder() {
        return medicationRepository.findAll().stream()
                .filter(med -> med.getMinimumStock() != null && med.getMinimumStock() > 0)
                .map(med -> {
                    Integer total = stockRepository.getTotalStockForMedication(med.getMedicationId());
                    int current = total != null ? total : 0;
                    return ReorderAlertDTO.builder()
                            .medicationId(med.getMedicationId())
                            .name(med.getName())
                            .form(med.getForm())
                            .minimumStock(med.getMinimumStock())
                            .currentTotalStock(current)
                            .deficit(med.getMinimumStock() - current)
                            .build();
                })
                .filter(alert -> alert.getCurrentTotalStock() < alert.getMinimumStock())
                .collect(Collectors.toList());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Medication findMedById(Long id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Medication not found: " + id));
    }

    private MedicationDTO toMedicationDTO(Medication med) {
        return MedicationDTO.builder()
                .medicationId(med.getMedicationId())
                .name(med.getName())
                .form(med.getForm())
                .pediatricDosage(med.getPediatricDosage())
                .minimumStock(med.getMinimumStock())
                .build();
    }

    private BatchDTO toBatchDTO(Batch b) {
        return BatchDTO.builder()
                .batchId(b.getBatchId())
                .batchNumber(b.getBatchNumber())
                .manufactureDate(b.getManufactureDate())
                .expirationDate(b.getExpirationDate())
                .quantity(b.getQuantity())
                .medicationId(b.getMedication().getMedicationId())
                .expired(b.isExpired())
                .build();
    }
}
