package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.pharmacyservice.dto.EquipmentItemDTO;
import tn.esprit.spring.pharmacyservice.entity.EquipmentItem;
import tn.esprit.spring.pharmacyservice.entity.EquipmentStock;
import tn.esprit.spring.pharmacyservice.repository.EquipmentItemRepository;
import tn.esprit.spring.pharmacyservice.repository.EquipmentStockRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EquipmentService {

    private final EquipmentItemRepository itemRepo;
    private final EquipmentStockRepository stockRepo;

    public EquipmentItemDTO create(EquipmentItemDTO dto) {
        EquipmentItem item = EquipmentItem.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .minimumStock(dto.getMinimumStock())
                .description(dto.getDescription())
                .build();
        item = itemRepo.save(item);
        EquipmentStock stock = EquipmentStock.builder()
                .item(item)
                .quantityAvailable(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0)
                .build();
        stockRepo.save(stock);
        return toDTO(item, stock);
    }

    @Transactional(readOnly = true)
    public List<EquipmentItemDTO> getAll() {
        return itemRepo.findAll().stream().map(item -> {
            EquipmentStock stock = stockRepo.findByItemItemId(item.getItemId()).orElse(null);
            return toDTO(item, stock);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EquipmentItemDTO getById(Long id) {
        EquipmentItem item = findItem(id);
        EquipmentStock stock = stockRepo.findByItemItemId(id).orElse(null);
        return toDTO(item, stock);
    }

    public EquipmentItemDTO update(Long id, EquipmentItemDTO dto) {
        EquipmentItem item = findItem(id);
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setUnit(dto.getUnit());
        item.setMinimumStock(dto.getMinimumStock());
        item.setDescription(dto.getDescription());
        item = itemRepo.save(item);
        EquipmentStock stock = stockRepo.findByItemItemId(id).orElse(null);
        return toDTO(item, stock);
    }

    public void delete(Long id) {
        stockRepo.findByItemItemId(id).ifPresent(stockRepo::delete);
        itemRepo.deleteById(id);
    }

    public EquipmentItemDTO adjustStock(Long itemId, int delta) {
        EquipmentItem item = findItem(itemId);
        EquipmentStock stock = stockRepo.findByItemItemId(itemId)
                .orElseGet(() -> EquipmentStock.builder().item(item).quantityAvailable(0).build());
        if (delta < 0) {
            stock.deduct(-delta);
        } else {
            stock.add(delta);
        }
        stockRepo.save(stock);
        return toDTO(item, stock);
    }

    @Transactional(readOnly = true)
    public List<EquipmentItemDTO> getLowStock() {
        return stockRepo.findLowStock().stream()
                .map(s -> toDTO(s.getItem(), s))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EquipmentItemDTO> getOutOfStock() {
        return stockRepo.findOutOfStock().stream()
                .map(s -> toDTO(s.getItem(), s))
                .collect(Collectors.toList());
    }

    private EquipmentItem findItem(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Equipment item not found: " + id));
    }

    private EquipmentItemDTO toDTO(EquipmentItem item, EquipmentStock stock) {
        int qty = stock != null ? stock.getQuantityAvailable() : 0;
        boolean low = item.getMinimumStock() != null && qty <= item.getMinimumStock();
        return EquipmentItemDTO.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .category(item.getCategory())
                .unit(item.getUnit())
                .minimumStock(item.getMinimumStock())
                .description(item.getDescription())
                .currentStock(qty)
                .lowStock(low)
                .build();
    }
}