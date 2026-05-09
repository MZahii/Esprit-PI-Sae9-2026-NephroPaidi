package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;
import tn.esprit.spring.Administrationservice.entity.EquipmentStatus;
import tn.esprit.spring.Administrationservice.service.HospitalEquipmentService;

import java.util.List;

@RestController
@RequestMapping("/hospital-resources")
@RequiredArgsConstructor
public class HospitalEquipmentController {

    private final HospitalEquipmentService hospitalEquipmentService;

    @PostMapping("/equipment")
    public List<EquipmentResponse> create(@Valid @RequestBody CreateEquipmentRequest request) {
        return hospitalEquipmentService.create(request);
    }

    @PutMapping("/equipment/{equipmentId}")
    public EquipmentResponse update(@PathVariable Long equipmentId, @Valid @RequestBody UpdateEquipmentRequest request) {
        return hospitalEquipmentService.update(equipmentId, request);
    }

    @PatchMapping("/equipment/{equipmentId}/status")
    public EquipmentResponse updateStatus(@PathVariable Long equipmentId, @Valid @RequestBody UpdateEquipmentStatusRequest request) {
        return hospitalEquipmentService.updateStatus(equipmentId, request);
    }

    @DeleteMapping("/equipment/{equipmentId}")
    public EquipmentResponse archive(@PathVariable Long equipmentId, @RequestBody(required = false) ArchiveEquipmentRequest request) {
        return hospitalEquipmentService.archive(equipmentId, request);
    }

    @GetMapping("/equipment")
    public List<EquipmentResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EquipmentCategory category,
            @RequestParam(required = false) String subtype,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Boolean archived
    ) {
        return hospitalEquipmentService.search(query, category, subtype, status, archived);
    }

    @GetMapping("/equipment/summary")
    public EquipmentInventorySummaryResponse summary() {
        return hospitalEquipmentService.summary();
    }

    @GetMapping("/equipment/options")
    public List<EquipmentCategoryOptionsResponse> options() {
        return hospitalEquipmentService.categoryOptions();
    }

    @GetMapping("/placement/floors")
    public List<PlacementFloorResponse> placementFloors() {
        return hospitalEquipmentService.placementFloors();
    }

    @GetMapping("/placement/workspaces/{workspaceId}")
    public PlacementWorkspaceDetailsResponse placementWorkspaceDetails(@PathVariable Long workspaceId) {
        return hospitalEquipmentService.placementWorkspaceDetails(workspaceId);
    }

    @PostMapping("/placement/workspaces/{workspaceId}/place")
    public PlacementWorkspaceDetailsResponse place(@PathVariable Long workspaceId, @Valid @RequestBody PlaceEquipmentRequest request) {
        return hospitalEquipmentService.placeEquipment(workspaceId, request);
    }

    @PatchMapping("/placement/equipment/{equipmentId}/remove")
    public PlacementWorkspaceDetailsResponse remove(@PathVariable Long equipmentId) {
        return hospitalEquipmentService.removeEquipment(equipmentId);
    }

    @PatchMapping("/placement/equipment/{equipmentId}/move")
    public PlacementWorkspaceDetailsResponse move(@PathVariable Long equipmentId, @Valid @RequestBody MoveEquipmentRequest request) {
        return hospitalEquipmentService.moveEquipment(equipmentId, request);
    }

    @PatchMapping("/placement/equipment/{equipmentId}/status")
    public PlacementWorkspaceDetailsResponse statusFromPlacement(@PathVariable Long equipmentId, @Valid @RequestBody UpdateEquipmentStatusRequest request) {
        return hospitalEquipmentService.updateEquipmentStatusFromPlacement(equipmentId, request);
    }
}
