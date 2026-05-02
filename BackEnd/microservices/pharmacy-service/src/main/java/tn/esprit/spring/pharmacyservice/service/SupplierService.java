package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import tn.esprit.spring.pharmacyservice.dto.SupplierDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplierStatsDTO;
import tn.esprit.spring.pharmacyservice.dto.SupplyOrderDTO;
import tn.esprit.spring.pharmacyservice.entity.Medication;
import tn.esprit.spring.pharmacyservice.entity.Supplier;
import tn.esprit.spring.pharmacyservice.entity.SupplyOrder;
import tn.esprit.spring.pharmacyservice.event.publisher.OrderDeliveredEvent;
import tn.esprit.spring.pharmacyservice.repository.MedicationRepository;
import tn.esprit.spring.pharmacyservice.repository.SupplierRepository;
import tn.esprit.spring.pharmacyservice.repository.SupplyOrderRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplyOrderRepository supplyOrderRepository;
    private final MedicationRepository medicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    // ─── Supplier ─────────────────────────────────────────────────────────────

    public SupplierDTO createSupplier(SupplierDTO dto) {
        Supplier s = Supplier.builder()
                .name(dto.getName())
                .contactInfo(dto.getContactInfo())
                .email(dto.getEmail())
                .isActive(true)
                .build();
        return toSupplierDTO(supplierRepository.save(s));
    }

    public List<SupplierDTO> getAllSuppliers() {
        return getAllSuppliers(null, null);
    }

    /**
     * Filtered list of suppliers.
     *
     * @param name   case-insensitive contains search on supplier name or contactInfo
     * @param active true → only active, false → only inactive, null → all
     */
    public List<SupplierDTO> getAllSuppliers(String name, Boolean active) {
        Stream<Supplier> stream = supplierRepository.findAll().stream();

        if (name != null && !name.isBlank()) {
            String q = name.trim().toLowerCase();
            stream = stream.filter(s ->
                    s.getName().toLowerCase().contains(q) ||
                    (s.getContactInfo() != null && s.getContactInfo().toLowerCase().contains(q)));
        }

        if (active != null)
            stream = stream.filter(s -> Boolean.TRUE.equals(s.getIsActive()) == active);

        return stream.map(this::toSupplierDTO).collect(Collectors.toList());
    }

    public SupplierDTO getSupplier(Long id) {
        return toSupplierDTO(findSupplierById(id));
    }

    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier s = findSupplierById(id);
        s.setName(dto.getName());
        s.setContactInfo(dto.getContactInfo());
        s.setEmail(dto.getEmail());
        return toSupplierDTO(supplierRepository.save(s));
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }

    // ─── SupplyOrder ──────────────────────────────────────────────────────────

    public SupplyOrderDTO placeOrder(Long supplierId, SupplyOrderDTO dto) {
        Supplier supplier = findSupplierById(supplierId);
        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            throw new IllegalStateException("Cannot place orders for an inactive supplier: " + supplier.getName());
        }
        SupplyOrder order = SupplyOrder.builder()
                .supplier(supplier)
                .medicationId(dto.getMedicationId())
                .orderedQuantity(dto.getOrderedQuantity())
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .notes(dto.getNotes())
                .status(SupplyOrder.OrderStatus.PENDING)
                .build();
        SupplyOrder saved = supplyOrderRepository.save(order);
        String medName = medicationRepository.findById(dto.getMedicationId())
                .map(Medication::getName).orElse("Unknown Medication");
        emailService.sendOrderPlacedEmail(supplier, saved, medName);
        return toOrderDTO(saved);
    }

    public SupplyOrderDTO markDelivered(Long orderId) {
        SupplyOrder order = findOrderById(orderId);
        order.setStatus(SupplyOrder.OrderStatus.DELIVERED);
        SupplyOrder saved = supplyOrderRepository.save(order);
        // Fire event so StockService can update stock
        eventPublisher.publishEvent(new OrderDeliveredEvent(
                saved.getOrderId(), saved.getMedicationId(), saved.getOrderedQuantity()));
        String medName = medicationRepository.findById(saved.getMedicationId())
                .map(Medication::getName).orElse("Unknown Medication");
        Supplier supplier = findSupplierById(saved.getSupplier().getSupplierId());
        emailService.sendOrderDeliveredEmail(supplier, saved, medName);
        return toOrderDTO(saved);
    }

    public SupplyOrderDTO cancelOrder(Long orderId) {
        SupplyOrder order = findOrderById(orderId);
        order.cancelOrder();
        return toOrderDTO(supplyOrderRepository.save(order));
    }

    public List<SupplyOrderDTO> getOrdersForSupplier(Long supplierId) {
        return supplyOrderRepository.findBySupplierSupplierId(supplierId).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    public List<SupplyOrderDTO> getOrdersByStatus(String status) {
        return supplyOrderRepository.findByStatus(SupplyOrder.OrderStatus.valueOf(status.toUpperCase()))
                .stream().map(this::toOrderDTO).collect(Collectors.toList());
    }

    /** Toggle a supplier between active and inactive. */
    public SupplierDTO toggleStatus(Long supplierId) {
        Supplier s = findSupplierById(supplierId);
        s.setIsActive(!Boolean.TRUE.equals(s.getIsActive()));
        return toSupplierDTO(supplierRepository.save(s));
    }

    /** Performance stats for a single supplier. */
    public SupplierStatsDTO getStats(Long supplierId) {
        Supplier s = findSupplierById(supplierId);
        List<SupplyOrder> orders = supplyOrderRepository.findBySupplierSupplierId(supplierId);

        long total     = orders.size();
        long delivered = orders.stream().filter(o -> o.getStatus() == SupplyOrder.OrderStatus.DELIVERED).count();
        long pending   = orders.stream().filter(o -> o.getStatus() == SupplyOrder.OrderStatus.PENDING).count();
        long cancelled = orders.stream().filter(o -> o.getStatus() == SupplyOrder.OrderStatus.CANCELLED).count();
        long overdue   = orders.stream()
                .filter(o -> o.getStatus() == SupplyOrder.OrderStatus.PENDING
                        && o.getExpectedDeliveryDate() != null
                        && o.getExpectedDeliveryDate().isBefore(LocalDate.now()))
                .count();

        long nonCancelled = total - cancelled;
        double rate = nonCancelled > 0 ? Math.round((delivered * 100.0 / nonCancelled) * 10) / 10.0 : 0.0;

        return SupplierStatsDTO.builder()
                .supplierId(s.getSupplierId())
                .supplierName(s.getName())
                .totalOrders(total)
                .deliveredOrders(delivered)
                .pendingOrders(pending)
                .cancelledOrders(cancelled)
                .overdueOrders(overdue)
                .deliveryRate(rate)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Supplier not found: " + id));
    }

    private SupplyOrder findOrderById(Long id) {
        return supplyOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SupplyOrder not found: " + id));
    }

    private SupplierDTO toSupplierDTO(Supplier s) {
        return SupplierDTO.builder()
                .supplierId(s.getSupplierId())
                .name(s.getName())
                .contactInfo(s.getContactInfo())
                .email(s.getEmail())
                .isActive(s.getIsActive())
                .build();
    }

    private SupplyOrderDTO toOrderDTO(SupplyOrder o) {
        return SupplyOrderDTO.builder()
                .orderId(o.getOrderId())
                .supplierId(o.getSupplier().getSupplierId())
                .medicationId(o.getMedicationId())
                .orderDate(o.getOrderDate())
                .status(o.getStatus().name())
                .orderedQuantity(o.getOrderedQuantity())
                .expectedDeliveryDate(o.getExpectedDeliveryDate())
                .notes(o.getNotes())
                .build();
    }
}
