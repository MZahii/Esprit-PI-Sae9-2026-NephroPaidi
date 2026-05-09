package tn.esprit.spring.pharmacyservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.spring.pharmacyservice.dto.SupplierDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplierStatsDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplyOrderDTO;
import tn.esprit.spring.pharmacyservice.entity.Medication;
import tn.esprit.spring.pharmacyservice.entity.Supplier;
import tn.esprit.spring.pharmacyservice.entity.SupplyOrder;
import tn.esprit.spring.pharmacyservice.repository.MedicationRepository;
import tn.esprit.spring.pharmacyservice.repository.SupplierRepository;
import tn.esprit.spring.pharmacyservice.repository.SupplyOrderRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierService — Unit Tests")
class SupplierServiceTest {

    @Mock SupplierRepository     supplierRepo;
    @Mock SupplyOrderRepository  orderRepo;
    @Mock MedicationRepository   medRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock EmailService           emailService;

    @InjectMocks SupplierService svc;

    private Supplier activeSupplier;
    private Supplier inactiveSupplier;
    private Medication medication;

    @BeforeEach
    void setUp() {
        activeSupplier = Supplier.builder()
                .supplierId(1L).name("PharmaCo").contactInfo("pharma@co.com")
                .isActive(true).build();

        inactiveSupplier = Supplier.builder()
                .supplierId(2L).name("OldSupplier").contactInfo("old@co.com")
                .isActive(false).build();

        medication = new Medication();
        medication.setMedicationId(10L);
        medication.setName("Amoxicillin");
    }

    // ── createSupplier ────────────────────────────────────────────────────────

    @Nested @DisplayName("createSupplier")
    class CreateSupplier {

        @Test @DisplayName("saves and returns DTO")
        void success() {
            given(supplierRepo.save(any(Supplier.class))).willReturn(activeSupplier);

            SupplierDTO dto = new SupplierDTO();
            dto.setName("PharmaCo"); dto.setContactInfo("pharma@co.com");

            SupplierDTO result = svc.createSupplier(dto);

            assertThat(result.getName()).isEqualTo("PharmaCo");
            assertThat(result.getIsActive()).isTrue();
        }
    }

    // ── toggleStatus ──────────────────────────────────────────────────────────

    @Nested @DisplayName("toggleStatus")
    class ToggleStatus {

        @Test @DisplayName("deactivates an active supplier")
        void deactivate() {
            given(supplierRepo.findById(1L)).willReturn(Optional.of(activeSupplier));
            given(supplierRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            SupplierDTO result = svc.toggleStatus(1L);

            assertThat(result.getIsActive()).isFalse();
        }

        @Test @DisplayName("activates an inactive supplier")
        void activate() {
            given(supplierRepo.findById(2L)).willReturn(Optional.of(inactiveSupplier));
            given(supplierRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            SupplierDTO result = svc.toggleStatus(2L);

            assertThat(result.getIsActive()).isTrue();
        }

        @Test @DisplayName("throws when supplier not found")
        void notFound() {
            given(supplierRepo.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> svc.toggleStatus(99L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Supplier not found");
        }
    }

    // ── placeOrder ────────────────────────────────────────────────────────────

    @Nested @DisplayName("placeOrder")
    class PlaceOrder {

        @Test @DisplayName("creates a PENDING order for active supplier")
        void success() {
            SupplyOrder saved = SupplyOrder.builder()
                    .orderId(100L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(50)
                    .orderDate(LocalDate.now())
                    .status(SupplyOrder.OrderStatus.PENDING).build();

            given(supplierRepo.findById(1L)).willReturn(Optional.of(activeSupplier));
            given(orderRepo.save(any())).willReturn(saved);
            given(medRepo.findById(10L)).willReturn(Optional.of(medication));
            willDoNothing().given(emailService).sendOrderPlacedEmail(any(), any(), any());

            SupplyOrderDTO dto = new SupplyOrderDTO();
            dto.setMedicationId(10L); dto.setOrderedQuantity(50);

            SupplyOrderDTO result = svc.placeOrder(1L, dto);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getOrderedQuantity()).isEqualTo(50);
        }

        @Test @DisplayName("throws when supplier is inactive")
        void inactiveSupplier() {
            given(supplierRepo.findById(2L)).willReturn(Optional.of(inactiveSupplier));

            SupplyOrderDTO dto = new SupplyOrderDTO();
            dto.setMedicationId(10L); dto.setOrderedQuantity(10);

            assertThatThrownBy(() -> svc.placeOrder(2L, dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inactive supplier");
        }
    }

    // ── cancelOrder ───────────────────────────────────────────────────────────

    @Nested @DisplayName("cancelOrder")
    class CancelOrder {

        @Test @DisplayName("cancels a PENDING order")
        void cancelPending() {
            SupplyOrder order = SupplyOrder.builder()
                    .orderId(1L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(20)
                    .status(SupplyOrder.OrderStatus.PENDING)
                    .orderDate(LocalDate.now()).build();

            given(orderRepo.findById(1L)).willReturn(Optional.of(order));
            given(orderRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            SupplyOrderDTO result = svc.cancelOrder(1L);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test @DisplayName("throws when trying to cancel a DELIVERED order")
        void cannotCancelDelivered() {
            SupplyOrder order = SupplyOrder.builder()
                    .orderId(2L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(20)
                    .status(SupplyOrder.OrderStatus.DELIVERED)
                    .orderDate(LocalDate.now()).build();

            given(orderRepo.findById(2L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> svc.cancelOrder(2L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Nested @DisplayName("getStats")
    class GetStats {

        @Test @DisplayName("calculates delivery rate correctly")
        void statsCalculation() {
            SupplyOrder delivered = SupplyOrder.builder().orderId(1L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(10)
                    .status(SupplyOrder.OrderStatus.DELIVERED).orderDate(LocalDate.now()).build();
            SupplyOrder pending = SupplyOrder.builder().orderId(2L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(10)
                    .status(SupplyOrder.OrderStatus.PENDING).orderDate(LocalDate.now()).build();
            SupplyOrder cancelled = SupplyOrder.builder().orderId(3L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(10)
                    .status(SupplyOrder.OrderStatus.CANCELLED).orderDate(LocalDate.now()).build();

            given(supplierRepo.findById(1L)).willReturn(Optional.of(activeSupplier));
            given(orderRepo.findBySupplierSupplierId(1L))
                    .willReturn(List.of(delivered, pending, cancelled));

            SupplierStatsDTO stats = svc.getStats(1L);

            assertThat(stats.getTotalOrders()).isEqualTo(3);
            assertThat(stats.getDeliveredOrders()).isEqualTo(1);
            assertThat(stats.getPendingOrders()).isEqualTo(1);
            assertThat(stats.getCancelledOrders()).isEqualTo(1);
            // rate = 1 delivered / 2 non-cancelled = 50%
            assertThat(stats.getDeliveryRate()).isEqualTo(50.0);
        }

        @Test @DisplayName("detects overdue pending orders")
        void overdueDetection() {
            SupplyOrder overdue = SupplyOrder.builder().orderId(1L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(10)
                    .status(SupplyOrder.OrderStatus.PENDING)
                    .expectedDeliveryDate(LocalDate.now().minusDays(3))
                    .orderDate(LocalDate.now().minusDays(10)).build();

            given(supplierRepo.findById(1L)).willReturn(Optional.of(activeSupplier));
            given(orderRepo.findBySupplierSupplierId(1L)).willReturn(List.of(overdue));

            SupplierStatsDTO stats = svc.getStats(1L);

            assertThat(stats.getOverdueOrders()).isEqualTo(1);
        }

        @Test @DisplayName("returns zero delivery rate when all orders cancelled")
        void allCancelled() {
            SupplyOrder cancelled = SupplyOrder.builder().orderId(1L).supplier(activeSupplier)
                    .medicationId(10L).orderedQuantity(10)
                    .status(SupplyOrder.OrderStatus.CANCELLED).orderDate(LocalDate.now()).build();

            given(supplierRepo.findById(1L)).willReturn(Optional.of(activeSupplier));
            given(orderRepo.findBySupplierSupplierId(1L)).willReturn(List.of(cancelled));

            SupplierStatsDTO stats = svc.getStats(1L);

            assertThat(stats.getDeliveryRate()).isEqualTo(0.0);
        }
    }
}
