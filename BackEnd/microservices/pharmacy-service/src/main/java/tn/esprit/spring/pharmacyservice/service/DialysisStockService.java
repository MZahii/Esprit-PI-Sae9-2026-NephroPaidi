package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.pharmacyservice.dto.DialysisItemDTO;
import tn.esprit.spring.pharmacyservice.entity.DialysisItem;
import tn.esprit.spring.pharmacyservice.entity.DialysisStock;
import tn.esprit.spring.pharmacyservice.repository.DialysisItemRepository;
import tn.esprit.spring.pharmacyservice.repository.DialysisStockRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DialysisStockService {

    private final DialysisItemRepository itemRepo;
    private final DialysisStockRepository stockRepo;

    public DialysisItemDTO create(DialysisItemDTO dto) {
        DialysisItem item = DialysisItem.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .minimumStock(dto.getMinimumStock())
                .description(dto.getDescription())
                .build();
        item = itemRepo.save(item);
        DialysisStock stock = DialysisStock.builder()
                .item(item)
                .quantityAvailable(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0)
                .build();
        stockRepo.save(stock);
        return toDTO(item, stock);
    }

    @Transactional(readOnly = true)
    public List<DialysisItemDTO> getAll() {
        return itemRepo.findAll().stream().map(item -> {
            DialysisStock stock = stockRepo.findByItemItemId(item.getItemId()).orElse(null);
            return toDTO(item, stock);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DialysisItemDTO getById(Long id) {
        DialysisItem item = findItem(id);
        DialysisStock stock = stockRepo.findByItemItemId(id).orElse(null);
        return toDTO(item, stock);
    }

    public DialysisItemDTO update(Long id, DialysisItemDTO dto) {
        DialysisItem item = findItem(id);
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setUnit(dto.getUnit());
        item.setMinimumStock(dto.getMinimumStock());
        item.setDescription(dto.getDescription());
        item = itemRepo.save(item);
        DialysisStock stock = stockRepo.findByItemItemId(id).orElse(null);
        return toDTO(item, stock);
    }

    public void delete(Long id) {
        stockRepo.findByItemItemId(id).ifPresent(stockRepo::delete);
        itemRepo.deleteById(id);
    }

    public DialysisItemDTO adjustStock(Long itemId, int delta) {
        DialysisItem item = findItem(itemId);
        DialysisStock stock = stockRepo.findByItemItemId(itemId)
                .orElseGet(() -> DialysisStock.builder().item(item).quantityAvailable(0).build());
        if (delta < 0) {
            stock.deduct(-delta);
        } else {
            stock.add(delta);
        }
        stockRepo.save(stock);
        return toDTO(item, stock);
    }

    @Transactional(readOnly = true)
    public List<DialysisItemDTO> getLowStock() {
        return stockRepo.findLowStock().stream()
                .map(s -> toDTO(s.getItem(), s))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DialysisItemDTO> getOutOfStock() {
        return stockRepo.findOutOfStock().stream()
                .map(s -> toDTO(s.getItem(), s))
                .collect(Collectors.toList());
    }

    private DialysisItem findItem(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dialysis item not found: " + id));
    }

    private DialysisItemDTO toDTO(DialysisItem item, DialysisStock stock) {
        int qty = stock != null ? stock.getQuantityAvailable() : 0;
        boolean low = item.getMinimumStock() != null && qty <= item.getMinimumStock();
        return DialysisItemDTO.builder()
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