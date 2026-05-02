package tn.esprit.spring.pharmacyservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tn.esprit.spring.pharmacyservice.dto.*;
import tn.esprit.spring.pharmacyservice.entity.*;
import tn.esprit.spring.pharmacyservice.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService — Unit Tests")
class StockServiceTest {

    @Mock StockRepository          stockRepo;
    @Mock BatchRepository          batchRepo;
    @Mock DispensationLogRepository logRepo;
    @Mock MedicationRepository     medRepo;

    @InjectMocks StockService svc;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Medication medication;
    private Batch      validBatch;
    private Batch      expiredBatch;
    private Stock      stock;

    @BeforeEach
    void setUp() {
        medication = new Medication();
        medication.setMedicationId(1L);
        medication.setName("Amoxicillin");

        validBatch = new Batch();
        validBatch.setBatchId(10L);
        validBatch.setBatchNumber("B-001");
        validBatch.setExpirationDate(LocalDate.now().plusMonths(6));
        validBatch.setMedication(medication);
        validBatch.setQuantity(100);

        expiredBatch = new Batch();
        expiredBatch.setBatchId(11L);
        expiredBatch.setBatchNumber("B-OLD");
        expiredBatch.setExpirationDate(LocalDate.now().minusDays(1));
        expiredBatch.setMedication(medication);

        stock = new Stock();
        stock.setStockId(1L);
        stock.setBatchId(10L);
        stock.setQuantityAvailable(50);
    }

    // ── initializeStock ───────────────────────────────────────────────────────

    @Nested @DisplayName("initializeStock")
    class InitializeStock {

        @Test @DisplayName("creates stock entry when none exists")
        void success() {
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.empty());
            given(stockRepo.save(any(Stock.class))).willAnswer(inv -> inv.getArgument(0));

            StockDTO result = svc.initializeStock(10L, 30);

            assertThat(result.getBatchId()).isEqualTo(10L);
            assertThat(result.getQuantityAvailable()).isEqualTo(30);
            then(stockRepo).should().save(any(Stock.class));
        }

        @Test @DisplayName("throws when stock already exists for batch")
        void alreadyExists() {
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));

            assertThatThrownBy(() -> svc.initializeStock(10L, 30))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Stock already exists");
        }
    }

    // ── dispense ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("dispense")
    class Dispense {

        @Test @DisplayName("reduces stock quantity on valid dispense")
        void success() {
            given(batchRepo.findById(10L)).willReturn(Optional.of(validBatch));
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));
            given(stockRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(logRepo.save(any())).willReturn(null);

            DispenseRequestDTO req = new DispenseRequestDTO();
            req.setBatchId(10L);
            req.setQuantity(10);

            StockDTO result = svc.dispense(req);

            assertThat(result.getQuantityAvailable()).isEqualTo(40);
        }

        @Test @DisplayName("throws when batch is expired")
        void expiredBatch() {
            given(batchRepo.findById(11L)).willReturn(Optional.of(expiredBatch));

            DispenseRequestDTO req = new DispenseRequestDTO();
            req.setBatchId(11L);
            req.setQuantity(5);

            assertThatThrownBy(() -> svc.dispense(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired batch");
        }

        @Test @DisplayName("throws when insufficient stock")
        void insufficientStock() {
            given(batchRepo.findById(10L)).willReturn(Optional.of(validBatch));
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));

            DispenseRequestDTO req = new DispenseRequestDTO();
            req.setBatchId(10L);
            req.setQuantity(100); // only 50 available

            assertThatThrownBy(() -> svc.dispense(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test @DisplayName("throws when batch not found")
        void batchNotFound() {
            given(batchRepo.findById(99L)).willReturn(Optional.empty());

            DispenseRequestDTO req = new DispenseRequestDTO();
            req.setBatchId(99L);
            req.setQuantity(5);

            assertThatThrownBy(() -> svc.dispense(req))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // ── adjustStock ───────────────────────────────────────────────────────────

    @Nested @DisplayName("adjustStock")
    class AdjustStock {

        @Test @DisplayName("applies positive delta")
        void positiveAdjustment() {
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));
            given(stockRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            StockDTO result = svc.adjustStock(10L, 20, "Recount");

            assertThat(result.getQuantityAvailable()).isEqualTo(70);
        }

        @Test @DisplayName("applies negative delta")
        void negativeAdjustment() {
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));
            given(stockRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            StockDTO result = svc.adjustStock(10L, -10, "Damaged");

            assertThat(result.getQuantityAvailable()).isEqualTo(40);
        }
    }

    // ── transferStock ─────────────────────────────────────────────────────────

    @Nested @DisplayName("transferStock")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class TransferStock {

        private Stock targetStock;

        @BeforeEach
        void setUp() {
            targetStock = new Stock();
            targetStock.setStockId(2L);
            targetStock.setBatchId(12L);
            targetStock.setQuantityAvailable(20);

            Batch targetBatch = new Batch();
            targetBatch.setBatchId(12L);
            targetBatch.setExpirationDate(LocalDate.now().plusMonths(3));

            given(batchRepo.findById(12L)).willReturn(Optional.of(targetBatch));
            given(stockRepo.findByBatchId(10L)).willReturn(Optional.of(stock));
            given(stockRepo.findByBatchId(12L)).willReturn(Optional.of(targetStock));
            given(stockRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test @DisplayName("transfers quantity from source to target")
        void success() {
            TransferStockRequestDTO req = new TransferStockRequestDTO();
            req.setSourceBatchId(10L);
            req.setTargetBatchId(12L);
            req.setQuantity(15);
            req.setReason("Ward transfer");

            TransferStockResponseDTO result = svc.transferStock(req);

            assertThat(result.getQuantityTransferred()).isEqualTo(15);
            assertThat(result.getSourceQuantityAvailable()).isEqualTo(35);
            assertThat(result.getTargetQuantityAvailable()).isEqualTo(35);
        }

        @Test @DisplayName("throws when source equals target")
        void sameSourceTarget() {
            TransferStockRequestDTO req = new TransferStockRequestDTO();
            req.setSourceBatchId(10L);
            req.setTargetBatchId(10L);
            req.setQuantity(5);

            assertThatThrownBy(() -> svc.transferStock(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different");
        }

        @Test @DisplayName("throws when source has insufficient stock")
        void insufficientSource() {
            TransferStockRequestDTO req = new TransferStockRequestDTO();
            req.setSourceBatchId(10L);
            req.setTargetBatchId(12L);
            req.setQuantity(100); // only 50 available

            assertThatThrownBy(() -> svc.transferStock(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }

    // ── smartDispense (FEFO) ──────────────────────────────────────────────────

    @Nested @DisplayName("smartDispense")
    class SmartDispense {

        @Test @DisplayName("dispenses from earliest-expiring batch first")
        void fefoOrder() {
            Batch batch1 = new Batch();
            batch1.setBatchId(20L); batch1.setBatchNumber("B-2025");
            batch1.setExpirationDate(LocalDate.now().plusMonths(2));
            batch1.setMedication(medication);

            Batch batch2 = new Batch();
            batch2.setBatchId(21L); batch2.setBatchNumber("B-2026");
            batch2.setExpirationDate(LocalDate.now().plusMonths(8));
            batch2.setMedication(medication);

            Stock s1 = new Stock(); s1.setBatchId(20L); s1.setQuantityAvailable(10);
            Stock s2 = new Stock(); s2.setBatchId(21L); s2.setQuantityAvailable(30);

            given(medRepo.findById(1L)).willReturn(Optional.of(medication));
            given(batchRepo.findNonExpiredByMedicationOrderedByExpiry(eq(1L), any()))
                    .willReturn(List.of(batch1, batch2));
            given(stockRepo.findByBatchId(20L)).willReturn(Optional.of(s1));
            given(stockRepo.findByBatchId(21L)).willReturn(Optional.of(s2));
            given(stockRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(logRepo.save(any())).willReturn(null);

            SmartDispenseRequestDTO req = new SmartDispenseRequestDTO();
            req.setMedicationId(1L);
            req.setQuantity(15);

            SmartDispenseResponseDTO result = svc.smartDispense(req);

            assertThat(result.getTotalDispensed()).isEqualTo(15);
            assertThat(result.getLines()).hasSize(2);
            assertThat(result.getLines().get(0).getBatchId()).isEqualTo(20L); // earliest first
            assertThat(result.getLines().get(0).getQuantityDispensed()).isEqualTo(10);
            assertThat(result.getLines().get(1).getQuantityDispensed()).isEqualTo(5);
        }

        @Test @DisplayName("throws when total stock is insufficient")
        void insufficientTotal() {
            Stock s1 = new Stock(); s1.setBatchId(20L); s1.setQuantityAvailable(3);
            Batch b1 = new Batch();
            b1.setBatchId(20L); b1.setExpirationDate(LocalDate.now().plusMonths(2));
            b1.setMedication(medication);

            given(medRepo.findById(1L)).willReturn(Optional.of(medication));
            given(batchRepo.findNonExpiredByMedicationOrderedByExpiry(eq(1L), any()))
                    .willReturn(List.of(b1));
            given(stockRepo.findByBatchId(20L)).willReturn(Optional.of(s1));

            SmartDispenseRequestDTO req = new SmartDispenseRequestDTO();
            req.setMedicationId(1L);
            req.setQuantity(50);

            assertThatThrownBy(() -> svc.smartDispense(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient total stock");
        }

        @Test @DisplayName("throws when no non-expired batches found")
        void noBatches() {
            given(medRepo.findById(1L)).willReturn(Optional.of(medication));
            given(batchRepo.findNonExpiredByMedicationOrderedByExpiry(eq(1L), any()))
                    .willReturn(List.of());

            SmartDispenseRequestDTO req = new SmartDispenseRequestDTO();
            req.setMedicationId(1L);
            req.setQuantity(5);

            assertThatThrownBy(() -> svc.smartDispense(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No valid");
        }
    }
}
