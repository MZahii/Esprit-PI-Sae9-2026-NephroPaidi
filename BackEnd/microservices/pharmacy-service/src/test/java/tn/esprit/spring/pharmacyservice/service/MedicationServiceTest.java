package tn.esprit.spring.pharmacyservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.spring.pharmacyservice.dto.MedicationDTO;
import tn.esprit.spring.pharmacyservice.dto.ReorderAlertDTO;
import tn.esprit.spring.pharmacyservice.entity.Medication;
import tn.esprit.spring.pharmacyservice.repository.BatchRepository;
import tn.esprit.spring.pharmacyservice.repository.MedicationRepository;
import tn.esprit.spring.pharmacyservice.repository.StockRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationService — Unit Tests")
class MedicationServiceTest {

    @Mock MedicationRepository medRepo;
    @Mock BatchRepository      batchRepo;
    @Mock StockRepository      stockRepo;

    @InjectMocks MedicationService svc;

    private Medication med;

    @BeforeEach
    void setUp() {
        med = new Medication();
        med.setMedicationId(1L);
        med.setName("Ibuprofen");
        med.setForm("tablet");
        med.setPediatricDosage("10mg/kg");
        med.setMinimumStock(50);
    }

    // ── createMedication ──────────────────────────────────────────────────────

    @Nested @DisplayName("createMedication")
    class CreateMedication {

        @Test @DisplayName("saves medication and returns DTO")
        void success() {
            given(medRepo.save(any(Medication.class))).willReturn(med);

            MedicationDTO dto = new MedicationDTO();
            dto.setName("Ibuprofen"); dto.setForm("tablet");
            dto.setPediatricDosage("10mg/kg"); dto.setMinimumStock(50);

            MedicationDTO result = svc.createMedication(dto);

            assertThat(result.getName()).isEqualTo("Ibuprofen");
            assertThat(result.getMinimumStock()).isEqualTo(50);
            then(medRepo).should(times(1)).save(any(Medication.class));
        }
    }

    // ── getMedication ─────────────────────────────────────────────────────────

    @Nested @DisplayName("getMedication")
    class GetMedication {

        @Test @DisplayName("returns DTO when found")
        void found() {
            given(medRepo.findById(1L)).willReturn(Optional.of(med));

            MedicationDTO result = svc.getMedication(1L);

            assertThat(result.getMedicationId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Ibuprofen");
        }

        @Test @DisplayName("throws NoSuchElementException when not found")
        void notFound() {
            given(medRepo.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> svc.getMedication(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Medication not found");
        }
    }

    // ── updateMedication ──────────────────────────────────────────────────────

    @Nested @DisplayName("updateMedication")
    class UpdateMedication {

        @Test @DisplayName("updates fields and saves")
        void success() {
            given(medRepo.findById(1L)).willReturn(Optional.of(med));
            given(medRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            MedicationDTO dto = new MedicationDTO();
            dto.setName("Paracetamol"); dto.setForm("syrup");
            dto.setPediatricDosage("15mg/kg"); dto.setMinimumStock(30);

            MedicationDTO result = svc.updateMedication(1L, dto);

            assertThat(result.getName()).isEqualTo("Paracetamol");
            assertThat(result.getMinimumStock()).isEqualTo(30);
        }
    }

    // ── getMedicationsNeedingReorder ──────────────────────────────────────────

    @Nested @DisplayName("getMedicationsNeedingReorder")
    class ReorderAlerts {

        @Test @DisplayName("returns medications whose stock is below minimum")
        void belowMinimum() {
            given(medRepo.findAll()).willReturn(List.of(med));
            given(stockRepo.getTotalStockForMedication(1L)).willReturn(20); // below 50

            List<ReorderAlertDTO> alerts = svc.getMedicationsNeedingReorder();

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getName()).isEqualTo("Ibuprofen");
            assertThat(alerts.get(0).getCurrentTotalStock()).isEqualTo(20);
            assertThat(alerts.get(0).getDeficit()).isEqualTo(30);
        }

        @Test @DisplayName("excludes medications with sufficient stock")
        void sufficientStock() {
            given(medRepo.findAll()).willReturn(List.of(med));
            given(stockRepo.getTotalStockForMedication(1L)).willReturn(100); // above 50

            List<ReorderAlertDTO> alerts = svc.getMedicationsNeedingReorder();

            assertThat(alerts).isEmpty();
        }

        @Test @DisplayName("excludes medications with null minimumStock")
        void nullMinimumStock() {
            med.setMinimumStock(null);
            given(medRepo.findAll()).willReturn(List.of(med));

            List<ReorderAlertDTO> alerts = svc.getMedicationsNeedingReorder();

            assertThat(alerts).isEmpty();
            then(stockRepo).shouldHaveNoInteractions();
        }
    }
}
