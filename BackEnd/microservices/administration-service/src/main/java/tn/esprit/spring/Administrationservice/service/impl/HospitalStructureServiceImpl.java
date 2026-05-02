package tn.esprit.spring.Administrationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.*;
import tn.esprit.spring.Administrationservice.repository.EquipmentPlacementRepository;
import tn.esprit.spring.Administrationservice.repository.FloorWorkspaceRepository;
import tn.esprit.spring.Administrationservice.repository.HospitalFloorRepository;
import tn.esprit.spring.Administrationservice.service.HospitalStructureService;
import tn.esprit.spring.Administrationservice.service.NotificationPublisherService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalStructureServiceImpl implements HospitalStructureService {
    private static final int FLOOR_ORDER_TEMP_OFFSET = 1000;

    private static final Comparator<FloorWorkspace> WORKSPACE_NAME_SORT = Comparator
            .comparing(FloorWorkspace::getWorkspaceName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(FloorWorkspace::getId);

    private final HospitalFloorRepository floorRepository;
    private final FloorWorkspaceRepository workspaceRepository;
    private final EquipmentPlacementRepository equipmentPlacementRepository;
    private final NotificationPublisherService notificationPublisherService;

    @Override
    @Transactional(readOnly = true)
    public HospitalStructureResponse getStructure() {
        List<HospitalFloor> floors = floorRepository.findAllWithWorkspacesOrdered();
        return toStructureResponse(floors);
    }

    @Override
    @Transactional
    public HospitalStructureResponse initializeFloors(InitializeFloorsRequest request) {
        long existingFloors = floorRepository.count();
        if (existingFloors > 0) {
            throw new IllegalArgumentException("Floors are already initialized. Use Manage Floors to add or remove floors.");
        }

        int totalFloors = request.getTotalFloors();
        List<HospitalFloor> floorsToCreate = new ArrayList<>();
        for (int i = 0; i < totalFloors; i++) {
            floorsToCreate.add(HospitalFloor.builder()
                    .floorOrder(i)
                    .description(null)
                    .build());
        }

        floorRepository.saveAll(floorsToCreate);
        return getStructure();
    }

    @Override
    @Transactional
    public FloorResponse addFloor(AddFloorRequest request) {
        List<HospitalFloor> floors = floorRepository.findAllByOrderByFloorOrderAsc();
        if (floors.isEmpty()) {
            throw new IllegalArgumentException("Initialize floors first before adding new floors.");
        }

        int insertIndex;
        FloorInsertPosition position = request.getPosition();
        switch (position) {
            case TOP -> insertIndex = floors.size();
            case BOTTOM -> insertIndex = 0;
            case BETWEEN -> {
                if (request.getAfterFloorId() == null) {
                    throw new IllegalArgumentException("afterFloorId is required when position is BETWEEN.");
                }

                int afterIndex = indexOfFloor(floors, request.getAfterFloorId());
                if (afterIndex < 0) {
                    throw new IllegalArgumentException("The selected floor for BETWEEN insertion does not exist.");
                }
                if (afterIndex == floors.size() - 1) {
                    throw new IllegalArgumentException("Cannot insert BETWEEN after the last floor. Use TOP instead.");
                }
                insertIndex = afterIndex + 1;
            }
            default -> throw new IllegalArgumentException("Unsupported floor insertion position.");
        }

        HospitalFloor newFloor = HospitalFloor.builder()
                .floorOrder(0)
                .description(trimOrNull(request.getDescription()))
                .build();

        floors.add(insertIndex, newFloor);
        renumberAndPersistFloors(floors);
        refreshWorkspaceCodesForAllFloors();

        notificationPublisherService.publish(
                "HospitalStructureFloorAdded",
                "Hospital Structure Updated",
                "A new floor was added (" + floorLabel(newFloor.getFloorOrder()) + ") and floor numbering was recalculated."
        );

        return toFloorResponse(newFloor);
    }

    @Override
    @Transactional
    public FloorResponse updateFloor(Long floorId, UpdateFloorRequest request) {
        HospitalFloor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + floorId));

        floor.setDescription(trimOrNull(request.getDescription()));
        HospitalFloor saved = floorRepository.save(floor);
        return toFloorResponse(saved);
    }

    @Override
    @Transactional
    public HospitalStructureResponse deleteFloor(Long floorId) {
        HospitalFloor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + floorId));

        for (FloorWorkspace workspace : floor.getWorkspaces()) {
            cleanupPlacementsForWorkspace(workspace.getId());
        }
        floorRepository.delete(floor);

        List<HospitalFloor> remainingFloors = floorRepository.findAllByOrderByFloorOrderAsc();
        if (!remainingFloors.isEmpty()) {
            renumberAndPersistFloors(remainingFloors);
            refreshWorkspaceCodesForAllFloors();
        }

        notificationPublisherService.publish(
                "HospitalStructureFloorDeleted",
                "Floor Deleted",
                "Floor " + floorLabel(floor.getFloorOrder()) + " and related structure data were deleted."
        );

        return getStructure();
    }

    @Override
    @Transactional
    public List<WorkspaceResponse> createWorkspaces(CreateWorkspaceRequest request) {
        HospitalFloor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + request.getFloorId()));

        int quantity = request.getQuantity();
        WorkspaceType workspaceType = request.getWorkspaceType();
        int currentMaxSequence = workspaceRepository.findMaxSequenceByFloorIdAndType(floor.getId(), workspaceType);

        List<FloorWorkspace> toCreate = new ArrayList<>();
        for (int i = 1; i <= quantity; i++) {
            int sequence = currentMaxSequence + i;
            toCreate.add(FloorWorkspace.builder()
                    .floor(floor)
                    .workspaceType(workspaceType)
                    .sequenceNumber(sequence)
                    .workspaceName(generateWorkspaceName(workspaceType, sequence))
                    .workspaceCode(generateWorkspaceCode(floor.getFloorOrder(), workspaceType, sequence))
                    .description(trimOrNull(request.getDescription()))
                    .metadata(trimOrNull(request.getMetadata()))
                    .build());
        }

        List<FloorWorkspace> created = workspaceRepository.saveAll(toCreate);
        String floorLabel = floorLabel(floor.getFloorOrder());

        notificationPublisherService.publish(
                "HospitalStructureWorkspaceGroupCreated",
                "Workspace Group Added",
                quantity + " " + workspaceType.getDisplayName() + " workspace(s) were added on floor " + floorLabel + "."
        );

        return created.stream()
                .sorted(WORKSPACE_NAME_SORT)
                .map(workspace -> WorkspaceResponse.from(workspace, floorLabel, floor.getFloorOrder()))
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request) {
        FloorWorkspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        workspace.setDescription(trimOrNull(request.getDescription()));
        workspace.setMetadata(trimOrNull(request.getMetadata()));

        FloorWorkspace saved = workspaceRepository.save(workspace);
        HospitalFloor floor = saved.getFloor();

        return WorkspaceResponse.from(saved, floorLabel(floor.getFloorOrder()), floor.getFloorOrder());
    }

    @Override
    @Transactional
    public FloorResponse deleteWorkspace(Long workspaceId) {
        FloorWorkspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        cleanupPlacementsForWorkspace(workspaceId);
        HospitalFloor floor = workspace.getFloor();
        String deletedName = workspace.getWorkspaceName();
        String floorLabel = floorLabel(floor.getFloorOrder());
        workspaceRepository.delete(workspace);
        HospitalFloor refreshedFloor = floorRepository.findById(floor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Floor not found after workspace deletion: " + floor.getId()));

        notificationPublisherService.publish(
                "HospitalStructureWorkspaceDeleted",
                "Workspace Deleted",
                "Workspace " + deletedName + " was deleted from floor " + floorLabel + "."
        );

        return toFloorResponse(refreshedFloor);
    }

    @Override
    @Transactional
    public FloorResponse updateWorkspaceGroupQuantity(UpdateWorkspaceGroupQuantityRequest request) {
        HospitalFloor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + request.getFloorId()));

        WorkspaceType type = request.getWorkspaceType();
        int desiredQuantity = request.getQuantity();

        List<FloorWorkspace> current = workspaceRepository
                .findByFloorIdAndWorkspaceTypeOrderBySequenceNumberAsc(floor.getId(), type);

        int currentSize = current.size();
        if (desiredQuantity == currentSize) {
            return toFloorResponse(floor);
        }

        if (desiredQuantity > currentSize) {
            int toCreate = desiredQuantity - currentSize;
            List<FloorWorkspace> additions = new ArrayList<>();
            int maxSequence = current.isEmpty() ? 0 : current.get(current.size() - 1).getSequenceNumber();
            for (int i = 1; i <= toCreate; i++) {
                int sequence = maxSequence + i;
                additions.add(FloorWorkspace.builder()
                        .floor(floor)
                        .workspaceType(type)
                        .sequenceNumber(sequence)
                        .workspaceName(generateWorkspaceName(type, sequence))
                        .workspaceCode(generateWorkspaceCode(floor.getFloorOrder(), type, sequence))
                        .build());
            }
            workspaceRepository.saveAll(additions);
        } else {
            int toDelete = currentSize - desiredQuantity;
            List<FloorWorkspace> deletions = current.subList(currentSize - toDelete, currentSize);
            for (FloorWorkspace deletion : deletions) {
                cleanupPlacementsForWorkspace(deletion.getId());
            }
            workspaceRepository.deleteAll(deletions);
        }

        notificationPublisherService.publish(
                "HospitalStructureWorkspaceGroupQuantityUpdated",
                "Workspace Quantity Updated",
                type.getDisplayName() + " quantity was set to " + desiredQuantity + " on floor " + floorLabel(floor.getFloorOrder()) + "."
        );

        HospitalFloor refreshed = floorRepository.findById(floor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Floor not found after workspace group update: " + floor.getId()));
        return toFloorResponse(refreshed);
    }

    @Override
    @Transactional
    public FloorResponse deleteWorkspaceGroup(Long floorId, WorkspaceType workspaceType) {
        HospitalFloor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + floorId));

        List<FloorWorkspace> groupItems = workspaceRepository
                .findByFloorIdAndWorkspaceTypeOrderBySequenceNumberAsc(floorId, workspaceType);
        if (groupItems.isEmpty()) {
            return toFloorResponse(floor);
        }

        for (FloorWorkspace workspace : groupItems) {
            cleanupPlacementsForWorkspace(workspace.getId());
        }
        workspaceRepository.deleteAll(groupItems);
        notificationPublisherService.publish(
                "HospitalStructureWorkspaceGroupDeleted",
                "Workspace Group Deleted",
                "All " + workspaceType.getDisplayName() + " workspaces were removed from floor " + floorLabel(floor.getFloorOrder()) + "."
        );
        HospitalFloor refreshed = floorRepository.findById(floor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Floor not found after workspace group deletion: " + floor.getId()));
        return toFloorResponse(refreshed);
    }

    @Override
    public List<WorkspaceTypeOptionResponse> getWorkspaceTypeOptions() {
        return List.of(WorkspaceType.values()).stream()
                .map(type -> WorkspaceTypeOptionResponse.builder()
                        .value(type)
                        .label(type.getDisplayName())
                        .codePrefix(type.getCodePrefix())
                        .build())
                .toList();
    }

    private HospitalStructureResponse toStructureResponse(List<HospitalFloor> floors) {
        List<FloorResponse> floorResponses = floors.stream()
                .map(this::toFloorResponse)
                .toList();

        int totalWorkspaces = floorResponses.stream()
                .mapToInt(FloorResponse::getTotalWorkspaces)
                .sum();

        return HospitalStructureResponse.builder()
                .initialized(!floors.isEmpty())
                .totalFloors(floors.size())
                .totalWorkspaces(totalWorkspaces)
                .floors(floorResponses)
                .build();
    }

    private FloorResponse toFloorResponse(HospitalFloor floor) {
        String label = floorLabel(floor.getFloorOrder());

        List<FloorWorkspace> workspaceEntities = new ArrayList<>(floor.getWorkspaces());
        workspaceEntities.sort(WORKSPACE_NAME_SORT);

        List<WorkspaceResponse> workspaces = workspaceEntities.stream()
                .map(workspace -> WorkspaceResponse.from(workspace, label, floor.getFloorOrder()))
                .toList();

        return FloorResponse.builder()
                .id(floor.getId())
                .floorOrder(floor.getFloorOrder())
                .floorLabel(label)
                .description(floor.getDescription())
                .totalWorkspaces(workspaces.size())
                .createdAt(floor.getCreatedAt())
                .updatedAt(floor.getUpdatedAt())
                .workspaces(workspaces)
                .build();
    }

    private int indexOfFloor(List<HospitalFloor> floors, Long floorId) {
        for (int i = 0; i < floors.size(); i++) {
            HospitalFloor floor = floors.get(i);
            if (floorId.equals(floor.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void renumberAndPersistFloors(List<HospitalFloor> floors) {
        // Two-pass renumber to avoid transient unique collisions on floor_order.
        for (int i = 0; i < floors.size(); i++) {
            floors.get(i).setFloorOrder(i + FLOOR_ORDER_TEMP_OFFSET);
        }
        floorRepository.saveAll(floors);
        floorRepository.flush();

        for (int i = 0; i < floors.size(); i++) {
            floors.get(i).setFloorOrder(i);
        }
        floorRepository.saveAll(floors);
        floorRepository.flush();
    }

    private void refreshWorkspaceCodesForAllFloors() {
        List<HospitalFloor> floors = floorRepository.findAllWithWorkspacesOrdered();
        List<FloorWorkspace> updatedWorkspaces = new ArrayList<>();

        for (HospitalFloor floor : floors) {
            for (FloorWorkspace workspace : floor.getWorkspaces()) {
                workspace.setWorkspaceCode(generateWorkspaceCode(
                        floor.getFloorOrder(),
                        workspace.getWorkspaceType(),
                        workspace.getSequenceNumber()
                ));
                updatedWorkspaces.add(workspace);
            }
        }
        workspaceRepository.saveAll(updatedWorkspaces);
    }

    private String floorLabel(Integer floorOrder) {
        return floorOrder == 0 ? "GF" : String.valueOf(floorOrder);
    }

    private String floorCode(Integer floorOrder) {
        return floorOrder == 0 ? "GF" : "E" + floorOrder;
    }

    private String generateWorkspaceCode(Integer floorOrder, WorkspaceType type, Integer sequence) {
        return String.format("%s-%s-%02d", floorCode(floorOrder), type.getCodePrefix(), sequence);
    }

    private String generateWorkspaceName(WorkspaceType type, Integer sequence) {
        return String.format("%s %02d", type.getDisplayName(), sequence);
    }

    private String trimOrNull(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void cleanupPlacementsForWorkspace(Long workspaceId) {
        List<EquipmentPlacement> placements = equipmentPlacementRepository.findByWorkspaceId(workspaceId);
        if (placements.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (EquipmentPlacement placement : placements) {
            if (placement.isActive()) {
                placement.setActive(false);
            }
            if (placement.getRemovedAt() == null) {
                placement.setRemovedAt(now);
            }
        }
        equipmentPlacementRepository.saveAll(placements);
        equipmentPlacementRepository.deleteAll(placements);
    }
}
