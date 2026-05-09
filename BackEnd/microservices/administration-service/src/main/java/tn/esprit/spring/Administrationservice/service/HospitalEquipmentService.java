package tn.esprit.spring.Administrationservice.service;

import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;
import tn.esprit.spring.Administrationservice.entity.EquipmentStatus;

import java.util.List;

public interface HospitalEquipmentService {
    List<EquipmentResponse> create(CreateEquipmentRequest request);

    EquipmentResponse update(Long equipmentId, UpdateEquipmentRequest request);

    EquipmentResponse updateStatus(Long equipmentId, UpdateEquipmentStatusRequest request);

    EquipmentResponse archive(Long equipmentId, ArchiveEquipmentRequest request);

    List<EquipmentResponse> search(String query, EquipmentCategory category, String subtype, EquipmentStatus status, Boolean archived);

    EquipmentInventorySummaryResponse summary();

    List<EquipmentCategoryOptionsResponse> categoryOptions();

    List<PlacementFloorResponse> placementFloors();

    PlacementWorkspaceDetailsResponse placementWorkspaceDetails(Long workspaceId);

    PlacementWorkspaceDetailsResponse placeEquipment(Long workspaceId, PlaceEquipmentRequest request);

    PlacementWorkspaceDetailsResponse removeEquipment(Long equipmentId);

    PlacementWorkspaceDetailsResponse moveEquipment(Long equipmentId, MoveEquipmentRequest request);

    PlacementWorkspaceDetailsResponse updateEquipmentStatusFromPlacement(Long equipmentId, UpdateEquipmentStatusRequest request);
}
