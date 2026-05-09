package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.pharmacyservice.dto.RecordMovementRequestDTO;
import tn.esprit.spring.pharmacyservice.dto.StockMovementDTO;
import tn.esprit.spring.pharmacyservice.entity.DialysisItem;
import tn.esprit.spring.pharmacyservice.entity.EquipmentItem;
import tn.esprit.spring.pharmacyservice.entity.StockMovement;
import tn.esprit.spring.pharmacyservice.repository.DialysisItemRepository;
import tn.esprit.spring.pharmacyservice.repository.DialysisStockRepository;
import tn.esprit.spring.pharmacyservice.repository.EquipmentItemRepository;
import tn.esprit.spring.pharmacyservice.repository.EquipmentStockRepository;
import tn.esprit.spring.pharmacyservice.repository.StockMovementRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockMovementService {

    private final StockMovementRepository movementRepo;
    private final EquipmentItemRepository equipItemRepo;
    private final EquipmentStockRepository equipStockRepo;
    private final DialysisItemRepository dialysisItemRepo;
    private final DialysisStockRepository dialysisStockRepo;
    private final PharmacyEventPublisher eventPublisher;

    public StockMovementDTO recordMovement(RecordMovementRequestDTO req) {
        StockMovement.StockType type = StockMovement.StockType.valueOf(req.getStockType());
        String itemName;

        if (type == StockMovement.StockType.EQUIPMENT) {
            EquipmentItem item = equipItemRepo.findById(req.getItemId())
                    .orElseThrow(() -> new NoSuchElementException("Equipment item not found: " + req.getItemId()));
            itemName = item.getName();
            equipStockRepo.findByItemItemId(req.getItemId()).ifPresent(stock -> {
                stock.deduct(req.getQuantityTaken());
                equipStockRepo.save(stock);
            });
        } else {
            DialysisItem item = dialysisItemRepo.findById(req.getItemId())
                    .orElseThrow(() -> new NoSuchElementException("Dialysis item not found: " + req.getItemId()));
            itemName = item.getName();
            dialysisStockRepo.findByItemItemId(req.getItemId()).ifPresent(stock -> {
                stock.deduct(req.getQuantityTaken());
                dialysisStockRepo.save(stock);
            });
        }

        StockMovement movement = StockMovement.builder()
                .stockType(type)
                .itemId(req.getItemId())
                .itemName(itemName)
                .quantityTaken(req.getQuantityTaken())
                .requestedBy(req.getRequestedBy())
                .requestedByRole(req.getRequestedByRole())
                .purpose(req.getPurpose())
                .takenAt(LocalDateTime.now())
                .build();

        StockMovementDTO saved = toDTO(movementRepo.save(movement));
        eventPublisher.movementEvent(saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getAll() {
        return movementRepo.findAllByOrderByTakenAtDesc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getByType(String stockType) {
        StockMovement.StockType type = StockMovement.StockType.valueOf(stockType);
        return movementRepo.findByStockTypeOrderByTakenAtDesc(type).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getByItemId(Long itemId, String stockType) {
        StockMovement.StockType type = StockMovement.StockType.valueOf(stockType);
        return movementRepo.findByItemIdAndStockTypeOrderByTakenAtDesc(itemId, type)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private StockMovementDTO toDTO(StockMovement m) {
        return StockMovementDTO.builder()
                .id(m.getId())
                .stockType(m.getStockType().name())
                .itemId(m.getItemId())
                .itemName(m.getItemName())
                .quantityTaken(m.getQuantityTaken())
                .requestedBy(m.getRequestedBy())
                .requestedByRole(m.getRequestedByRole())
                .purpose(m.getPurpose())
                .takenAt(m.getTakenAt())
                .build();
    }
}