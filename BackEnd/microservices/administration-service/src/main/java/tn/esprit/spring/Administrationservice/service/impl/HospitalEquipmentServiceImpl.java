package tn.esprit.spring.Administrationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.*;
import tn.esprit.spring.Administrationservice.repository.*;
import tn.esprit.spring.Administrationservice.service.HospitalEquipmentService;
import tn.esprit.spring.Administrationservice.service.NotificationPublisherService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalEquipmentServiceImpl implements HospitalEquipmentService {

    private static final Set<EquipmentStatus> NON_OPERATIONAL_STATUSES = Set.of(
            EquipmentStatus.UNDER_MAINTENANCE,
            EquipmentStatus.OUT_OF_SERVICE,
            EquipmentStatus.ARCHIVED
    );

    private static final Set<WorkspaceType> PLACEMENT_ELIGIBLE_WORKSPACES = Set.of(
            WorkspaceType.DOCTOR_OFFICE,
            WorkspaceType.HOSPITALIZATION_ROOM,
            WorkspaceType.DIALYSIS_ROOM,
            WorkspaceType.SURGERY_ROOM,
            WorkspaceType.LABORATORY
    );

    private static final Map<WorkspaceType, Set<EquipmentCategory>> WORKSPACE_COMPATIBILITY = Map.of(
            WorkspaceType.DOCTOR_OFFICE, Set.of(EquipmentCategory.CONSULTATION_BED),
            WorkspaceType.HOSPITALIZATION_ROOM, Set.of(EquipmentCategory.HOSPITALIZATION_BED),
            WorkspaceType.DIALYSIS_ROOM, Set.of(EquipmentCategory.DIALYSIS_BED, EquipmentCategory.DIALYSIS_MACHINE),
            WorkspaceType.SURGERY_ROOM, Set.of(EquipmentCategory.SURGERY_TABLE_OR_BED),
            WorkspaceType.LABORATORY, Set.of(EquipmentCategory.LABORATORY_EQUIPMENT, EquipmentCategory.ANALYSIS_EQUIPMENT, EquipmentCategory.IMAGING_EQUIPMENT)
    );

    private final HospitalEquipmentRepository equipmentRepository;
    private final EquipmentArchiveLogRepository archiveLogRepository;
    private final EquipmentPlacementRepository placementRepository;
    private final FloorWorkspaceRepository workspaceRepository;
    private final HospitalFloorRepository floorRepository;
    private final NotificationPublisherService notificationPublisherService;

    @Override
    @Transactional
    public List<EquipmentResponse> create(CreateEquipmentRequest request) {
        EquipmentCategory category = request.getCategory();
        String resolvedSubtype = resolveSubtype(category, request.getSubtype(), request.getCustomSubtype());
        int quantity = request.getQuantity();
        String prefix = codePrefix(category, resolvedSubtype);
        int nextSequence = nextSequenceStart(prefix);

        List<HospitalEquipment> batch = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String code = buildCode(prefix, nextSequence++);
            HospitalEquipment equipment = HospitalEquipment.builder()
                    .equipmentCode(code)
                    .name(buildAutoName(category, resolvedSubtype, code))
                    .category(category)
                    .subtype(resolvedSubtype)
                    .status(EquipmentStatus.AVAILABLE)
                    .description(trimOrNull(request.getDescription()))
                    .archived(false)
                    .build();
            batch.add(equipment);
        }

        List<HospitalEquipment> saved = equipmentRepository.saveAll(batch);
        notificationPublisherService.publish(
                "EquipmentCreated",
                "Equipment Inventory Updated",
                saved.size() + " equipment item(s) created in category " + category.name() + "."
        );
        return saved.stream().map(EquipmentResponse::from).map(this::enrichWithPlacement).toList();
    }

    @Override
    @Transactional
    public EquipmentResponse update(Long equipmentId, UpdateEquipmentRequest request) {
        HospitalEquipment equipment = getEquipmentOrThrow(equipmentId);

        EquipmentCategory category = request.getCategory();
        String resolvedSubtype = resolveSubtype(category, request.getSubtype(), request.getCustomSubtype());
        String code = request.getAutoGenerateCode() == null || request.getAutoGenerateCode()
                ? generateCode(category, resolvedSubtype)
                : normalizeManualCode(request.getEquipmentCode());

        validateUniqueCode(code, equipmentId);

        equipment.setEquipmentCode(code);
        equipment.setName(buildAutoName(category, resolvedSubtype, code));
        equipment.setCategory(category);
        equipment.setSubtype(resolvedSubtype);
        equipment.setStatus(request.getStatus());
        equipment.setDescription(trimOrNull(request.getDescription()));

        if (request.getStatus() == EquipmentStatus.ARCHIVED) {
            equipment.setArchived(true);
            if (equipment.getArchivedAt() == null) {
                equipment.setArchivedAt(LocalDateTime.now());
            }
        } else {
            equipment.setArchived(false);
            equipment.setArchivedAt(null);
        }

        HospitalEquipment saved = equipmentRepository.save(equipment);
        return enrichWithPlacement(EquipmentResponse.from(saved));
    }

    @Override
    @Transactional
    public EquipmentResponse updateStatus(Long equipmentId, UpdateEquipmentStatusRequest request) {
        HospitalEquipment equipment = getEquipmentOrThrow(equipmentId);
        EquipmentStatus previousStatus = equipment.getStatus();
        equipment.setStatus(request.getStatus());

        if (request.getStatus() == EquipmentStatus.ARCHIVED) {
            equipment.setArchived(true);
            equipment.setArchivedAt(LocalDateTime.now());
            archivePlacementIfExists(equipment.getId());
            recordArchiveLog(equipment, "STATUS_ARCHIVED", "Archived via status update");
        } else if (equipment.isArchived()) {
            equipment.setArchived(false);
            equipment.setArchivedAt(null);
        }

        HospitalEquipment saved = equipmentRepository.save(equipment);
        notificationPublisherService.publish(
                "EquipmentStatusChanged",
                "Equipment Status Updated",
                saved.getEquipmentCode() + " status changed from " + previousStatus + " to " + saved.getStatus() + "."
        );
        return enrichWithPlacement(EquipmentResponse.from(saved));
    }

    @Override
    @Transactional
    public EquipmentResponse archive(Long equipmentId, ArchiveEquipmentRequest request) {
        HospitalEquipment equipment = getEquipmentOrThrow(equipmentId);
        if (equipment.isArchived()) {
            return enrichWithPlacement(EquipmentResponse.from(equipment));
        }

        String reason = trimOrNull(request != null ? request.getReason() : null);
        equipment.setArchived(true);
        equipment.setArchivedAt(LocalDateTime.now());
        equipment.setArchivedReason(reason);
        equipment.setStatus(EquipmentStatus.ARCHIVED);

        HospitalEquipment saved = equipmentRepository.save(equipment);
        archivePlacementIfExists(saved.getId());
        recordArchiveLog(saved, "ARCHIVED", reason);
        notificationPublisherService.publish(
                "EquipmentArchived",
                "Equipment Archived",
                saved.getEquipmentCode() + " was archived" + (reason == null ? "." : " (" + reason + ").")
        );

        return enrichWithPlacement(EquipmentResponse.from(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentResponse> search(String query, EquipmentCategory category, String subtype, EquipmentStatus status, Boolean archived) {
        boolean archivedFilter = archived != null && archived;

        Specification<HospitalEquipment> spec = Specification.<HospitalEquipment>where((root, cq, cb) -> cb.equal(root.get("archived"), archivedFilter));

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), q),
                    cb.like(cb.lower(root.get("equipmentCode")), q)
            ));
        }

        if (category != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("category"), category));
        }

        if (subtype != null && !subtype.trim().isEmpty()) {
            String normalizedSubtype = subtype.trim().toUpperCase(Locale.ROOT);
            spec = spec.and((root, cq, cb) -> cb.equal(cb.upper(root.get("subtype")), normalizedSubtype));
        }

        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }

        List<HospitalEquipment> rows = equipmentRepository.findAll(spec);
        rows.sort(Comparator.comparing(HospitalEquipment::getName, String.CASE_INSENSITIVE_ORDER));
        return enrichWithPlacement(rows.stream().map(EquipmentResponse::from).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentInventorySummaryResponse summary() {
        List<HospitalEquipment> all = equipmentRepository.findAll();

        Map<EquipmentCategory, Long> byCategory = all.stream()
                .filter(eq -> !eq.isArchived())
                .collect(Collectors.groupingBy(HospitalEquipment::getCategory, Collectors.counting()));

        List<EquipmentCategorySummaryResponse> categoryResponses = Arrays.stream(EquipmentCategory.values())
                .map(category -> EquipmentCategorySummaryResponse.builder()
                        .category(category.name())
                        .count(byCategory.getOrDefault(category, 0L))
                        .build())
                .toList();

        long available = all.stream().filter(eq -> eq.getStatus() == EquipmentStatus.AVAILABLE && !eq.isArchived()).count();
        long maintenance = all.stream().filter(eq -> eq.getStatus() == EquipmentStatus.UNDER_MAINTENANCE && !eq.isArchived()).count();
        long outOfService = all.stream().filter(eq -> eq.getStatus() == EquipmentStatus.OUT_OF_SERVICE && !eq.isArchived()).count();
        long archived = all.stream().filter(HospitalEquipment::isArchived).count();

        return EquipmentInventorySummaryResponse.builder()
                .total(all.size())
                .available(available)
                .underMaintenance(maintenance)
                .outOfService(outOfService)
                .archived(archived)
                .byCategory(categoryResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentCategoryOptionsResponse> categoryOptions() {
        List<EquipmentCategoryOptionsResponse> responses = new ArrayList<>();

        for (EquipmentCategory category : EquipmentCategory.values()) {
            List<String> values = EquipmentSubtypeCatalog.CATALOG.getOrDefault(category, List.of(category.name()));
            List<EquipmentSubtypeOptionResponse> options = values.stream()
                    .map(value -> EquipmentSubtypeOptionResponse.builder().value(value).label(value).build())
                    .toList();

            responses.add(EquipmentCategoryOptionsResponse.builder()
                    .category(category.name())
                    .subtypeOptions(options)
                    .build());
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlacementFloorResponse> placementFloors() {
        List<HospitalFloor> floors = floorRepository.findAllWithWorkspacesOrdered();
        List<EquipmentPlacement> activePlacements = placementRepository.findByActiveTrue();

        Map<Long, Integer> workspacePlacedCounts = new HashMap<>();
        for (EquipmentPlacement placement : activePlacements) {
            workspacePlacedCounts.merge(placement.getWorkspace().getId(), 1, Integer::sum);
        }

        List<PlacementFloorResponse> result = new ArrayList<>();

        for (HospitalFloor floor : floors) {
            List<FloorWorkspace> eligible = floor.getWorkspaces().stream()
                    .filter(ws -> PLACEMENT_ELIGIBLE_WORKSPACES.contains(ws.getWorkspaceType()))
                    .sorted(Comparator.comparing(FloorWorkspace::getWorkspaceName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            List<PlacementWorkspaceSummaryResponse> workspaceRows = eligible.stream()
                    .map(ws -> PlacementWorkspaceSummaryResponse.builder()
                            .workspaceId(ws.getId())
                            .workspaceCode(ws.getWorkspaceCode())
                            .workspaceName(ws.getWorkspaceName())
                            .workspaceType(ws.getWorkspaceType().name())
                            .placedCount(workspacePlacedCounts.getOrDefault(ws.getId(), 0))
                            .build())
                    .toList();

            int totalPlaced = workspaceRows.stream().mapToInt(PlacementWorkspaceSummaryResponse::getPlacedCount).sum();

            result.add(PlacementFloorResponse.builder()
                    .floorId(floor.getId())
                    .floorLabel(floorLabel(floor.getFloorOrder()))
                    .eligibleWorkspaceCount(workspaceRows.size())
                    .totalPlacedEquipment(totalPlaced)
                    .workspaces(workspaceRows)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementWorkspaceDetailsResponse placementWorkspaceDetails(Long workspaceId) {
        FloorWorkspace workspace = getWorkspaceOrThrow(workspaceId);
        ensureWorkspaceEligible(workspace);
        return buildWorkspacePlacementDetails(workspace);
    }

    @Override
    @Transactional
    public PlacementWorkspaceDetailsResponse placeEquipment(Long workspaceId, PlaceEquipmentRequest request) {
        FloorWorkspace workspace = getWorkspaceOrThrow(workspaceId);
        ensureWorkspaceEligible(workspace);

        HospitalEquipment equipment = getEquipmentOrThrow(request.getEquipmentId());
        if (equipment.isArchived() || equipment.getStatus() == EquipmentStatus.ARCHIVED) {
            throw new IllegalArgumentException("Archived equipment cannot be placed.");
        }

        if (NON_OPERATIONAL_STATUSES.contains(equipment.getStatus())) {
            throw new IllegalArgumentException("This equipment is not operational and cannot be placed.");
        }

        if (placementRepository.findByEquipmentIdAndActiveTrue(equipment.getId()).isPresent()) {
            throw new IllegalArgumentException("Equipment is already placed in another workspace. Use move instead.");
        }

        ensureCompatibility(workspace.getWorkspaceType(), equipment.getCategory());

        placementRepository.save(EquipmentPlacement.builder()
                .equipment(equipment)
                .workspace(workspace)
                .active(true)
                .placedAt(LocalDateTime.now())
                .build());
        notificationPublisherService.publish(
                "EquipmentPlaced",
                "Equipment Placement Updated",
                equipment.getEquipmentCode() + " placed in " + workspace.getWorkspaceName()
                        + " (" + floorLabel(workspace.getFloor().getFloorOrder()) + ")."
        );

        return buildWorkspacePlacementDetails(workspace);
    }

    @Override
    @Transactional
    public PlacementWorkspaceDetailsResponse removeEquipment(Long equipmentId) {
        EquipmentPlacement placement = placementRepository.findByEquipmentIdAndActiveTrue(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Active placement not found for equipment: " + equipmentId));

        FloorWorkspace workspace = placement.getWorkspace();
        placement.setActive(false);
        placement.setRemovedAt(LocalDateTime.now());
        placementRepository.save(placement);

        HospitalEquipment equipment = placement.getEquipment();
        if (!equipment.isArchived() && !NON_OPERATIONAL_STATUSES.contains(equipment.getStatus())) {
            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }
        notificationPublisherService.publish(
                "EquipmentRemovedFromWorkspace",
                "Equipment Removed From Workspace",
                equipment.getEquipmentCode() + " was removed from " + workspace.getWorkspaceName() + "."
        );

        return buildWorkspacePlacementDetails(workspace);
    }

    @Override
    @Transactional
    public PlacementWorkspaceDetailsResponse moveEquipment(Long equipmentId, MoveEquipmentRequest request) {
        EquipmentPlacement currentPlacement = placementRepository.findByEquipmentIdAndActiveTrue(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Active placement not found for equipment: " + equipmentId));

        FloorWorkspace targetWorkspace = getWorkspaceOrThrow(request.getTargetWorkspaceId());
        ensureWorkspaceEligible(targetWorkspace);

        HospitalEquipment equipment = currentPlacement.getEquipment();
        FloorWorkspace sourceWorkspace = currentPlacement.getWorkspace();
        ensureCompatibility(targetWorkspace.getWorkspaceType(), equipment.getCategory());

        currentPlacement.setActive(false);
        currentPlacement.setRemovedAt(LocalDateTime.now());
        placementRepository.save(currentPlacement);

        placementRepository.save(EquipmentPlacement.builder()
                .equipment(equipment)
                .workspace(targetWorkspace)
                .active(true)
                .placedAt(LocalDateTime.now())
                .build());
        notificationPublisherService.publish(
                "EquipmentMoved",
                "Equipment Moved",
                equipment.getEquipmentCode() + " moved from " + sourceWorkspace.getWorkspaceName()
                        + " to " + targetWorkspace.getWorkspaceName() + "."
        );

        return buildWorkspacePlacementDetails(targetWorkspace);
    }

    @Override
    @Transactional
    public PlacementWorkspaceDetailsResponse updateEquipmentStatusFromPlacement(Long equipmentId, UpdateEquipmentStatusRequest request) {
        HospitalEquipment equipment = getEquipmentOrThrow(equipmentId);
        EquipmentStatus previousStatus = equipment.getStatus();
        EquipmentPlacement previousPlacement = placementRepository.findByEquipmentIdAndActiveTrue(equipmentId).orElse(null);

        equipment.setStatus(request.getStatus());
        if (request.getStatus() == EquipmentStatus.ARCHIVED) {
            equipment.setArchived(true);
            equipment.setArchivedAt(LocalDateTime.now());
            archivePlacementIfExists(equipmentId);
            recordArchiveLog(equipment, "ARCHIVED_FROM_PLACEMENT", "Archived from placement view");
        }
        equipmentRepository.save(equipment);
        notificationPublisherService.publish(
                "EquipmentStatusChanged",
                "Equipment Status Updated",
                equipment.getEquipmentCode() + " status changed from " + previousStatus + " to " + equipment.getStatus() + "."
        );

        FloorWorkspace workspaceForResponse = previousPlacement != null
                ? previousPlacement.getWorkspace()
                : placementRepository.findByEquipmentIdAndActiveTrue(equipmentId).map(EquipmentPlacement::getWorkspace).orElse(null);

        if (workspaceForResponse == null) {
            throw new IllegalArgumentException("Equipment is no longer placed in a workspace.");
        }
        return buildWorkspacePlacementDetails(workspaceForResponse);
    }

    private String resolveSubtype(EquipmentCategory category, String subtype, String customSubtype) {
        if (requiresSubtypeSelection(category)) {
            String normalizedSubtype = subtype == null ? "" : subtype.trim().toUpperCase(Locale.ROOT);
            List<String> controlled = EquipmentSubtypeCatalog.CATALOG.get(category);
            if (controlled == null || controlled.isEmpty()) {
                throw new IllegalArgumentException("Subtype catalog missing for category " + category.name());
            }
            if (normalizedSubtype.isEmpty()) {
                throw new IllegalArgumentException("Subtype is required for category " + category.name());
            }
            if (!controlled.contains(normalizedSubtype)) {
                throw new IllegalArgumentException("Subtype " + normalizedSubtype + " is not allowed for category " + category.name());
            }
            if (EquipmentSubtypeCatalog.OTHER.equals(normalizedSubtype)) {
                String custom = trimOrNull(customSubtype);
                if (custom == null) {
                    throw new IllegalArgumentException("customSubtype is required when subtype is OTHER.");
                }
                return custom.toUpperCase(Locale.ROOT).replace(' ', '_');
            }
            return normalizedSubtype;
        }

        return category.name();
    }

    private boolean requiresSubtypeSelection(EquipmentCategory category) {
        return category == EquipmentCategory.LABORATORY_EQUIPMENT
                || category == EquipmentCategory.ANALYSIS_EQUIPMENT
                || category == EquipmentCategory.IMAGING_EQUIPMENT;
    }

    private String buildAutoName(EquipmentCategory category, String subtype, String code) {
        String categoryLabel = category.name().replace('_', ' ');
        String subtypeLabel = subtype.replace('_', ' ');
        return requiresSubtypeSelection(category)
                ? categoryLabel + " / " + subtypeLabel + " - " + code
                : categoryLabel + " - " + code;
    }

    private void validateUniqueCode(String code, Long equipmentId) {
        Optional<HospitalEquipment> existing = equipmentRepository.findByEquipmentCodeIgnoreCase(code);
        if (existing.isPresent() && !existing.get().getId().equals(equipmentId)) {
            throw new IllegalArgumentException("Equipment code already exists: " + code);
        }
    }

    private String generateCode(EquipmentCategory category, String subtype) {
        String prefix = codePrefix(category, subtype);
        int next = nextSequenceStart(prefix);
        String generated = buildCode(prefix, next);
        while (equipmentRepository.existsByEquipmentCodeIgnoreCase(generated)) {
            next++;
            generated = buildCode(prefix, next);
        }
        return generated;
    }

    private int nextSequenceStart(String prefix) {
        String persistedPrefix = prefix + "-";
        Optional<HospitalEquipment> latest = equipmentRepository.findTopByEquipmentCodeStartingWithOrderByEquipmentCodeDesc(persistedPrefix);
        int next = 1;
        if (latest.isPresent()) {
            String code = latest.get().getEquipmentCode();
            int dash = code.lastIndexOf('-');
            if (dash > -1 && dash < code.length() - 1) {
                try {
                    next = Integer.parseInt(code.substring(dash + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return next;
    }

    private String buildCode(String prefix, int sequence) {
        return String.format(Locale.ROOT, "%s-%03d", prefix, sequence);
    }

    private String codePrefix(EquipmentCategory category, String subtype) {
        return switch (category) {
            case CONSULTATION_BED -> "CONS-BED";
            case HOSPITALIZATION_BED -> "HOSP-BED";
            case DIALYSIS_BED -> "DIAL-BED";
            case DIALYSIS_MACHINE -> "DIAL-MAC";
            case SURGERY_TABLE_OR_BED -> "SURG-BED";
            case LABORATORY_EQUIPMENT -> "LAB-" + compactSubtype(subtype);
            case IMAGING_EQUIPMENT -> "IMG-" + compactSubtype(subtype);
            case ANALYSIS_EQUIPMENT -> "ANL-" + compactSubtype(subtype);
        };
    }

    private String compactSubtype(String subtype) {
        String normalized = subtype.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        String[] parts = normalized.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                builder.append(part, 0, Math.min(3, part.length()));
            }
            if (builder.length() >= 6) {
                break;
            }
        }
        String compact = builder.toString();
        return compact.isBlank() ? "GEN" : compact;
    }

    private String normalizeManualCode(String code) {
        String normalized = trimOrNull(code);
        if (normalized == null) {
            throw new IllegalArgumentException("equipmentCode is required when autoGenerateCode is false.");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private HospitalEquipment getEquipmentOrThrow(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Equipment not found: " + equipmentId));
    }

    private FloorWorkspace getWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
    }

    private void ensureWorkspaceEligible(FloorWorkspace workspace) {
        if (!PLACEMENT_ELIGIBLE_WORKSPACES.contains(workspace.getWorkspaceType())) {
            throw new IllegalArgumentException("Equipment placement is not allowed in workspace type: " + workspace.getWorkspaceType());
        }
    }

    private void ensureCompatibility(WorkspaceType workspaceType, EquipmentCategory category) {
        Set<EquipmentCategory> allowed = WORKSPACE_COMPATIBILITY.getOrDefault(workspaceType, Set.of());
        if (!allowed.contains(category)) {
            throw new IllegalArgumentException("Category " + category + " is not allowed in workspace type " + workspaceType);
        }
    }

    private PlacementWorkspaceDetailsResponse buildWorkspacePlacementDetails(FloorWorkspace workspace) {
        Set<EquipmentCategory> allowedCategories = WORKSPACE_COMPATIBILITY.getOrDefault(workspace.getWorkspaceType(), Set.of());

        List<EquipmentPlacement> placements = placementRepository.findByWorkspaceIdAndActiveTrueOrderByPlacedAtAsc(workspace.getId());
        List<EquipmentResponse> placed = placements.stream()
                .map(EquipmentPlacement::getEquipment)
                .map(EquipmentResponse::from)
                .map(this::enrichWithPlacement)
                .sorted(Comparator.comparing(EquipmentResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Specification<HospitalEquipment> availableSpec = Specification.<HospitalEquipment>where((root, cq, cb) -> cb.isFalse(root.get("archived")))
                .and((root, cq, cb) -> root.get("category").in(allowedCategories))
                .and((root, cq, cb) -> cb.not(root.get("status").in(NON_OPERATIONAL_STATUSES)));

        List<HospitalEquipment> candidates = equipmentRepository.findAll(availableSpec);

        Set<Long> placedEquipmentIds = placementRepository.findByActiveTrue().stream()
                .map(p -> p.getEquipment().getId())
                .collect(Collectors.toSet());

        List<EquipmentResponse> available = candidates.stream()
                .filter(eq -> !placedEquipmentIds.contains(eq.getId()))
                .map(EquipmentResponse::from)
                .sorted(Comparator.comparing(EquipmentResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return PlacementWorkspaceDetailsResponse.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .workspaceCode(workspace.getWorkspaceCode())
                .workspaceType(workspace.getWorkspaceType().name())
                .floorLabel(floorLabel(workspace.getFloor().getFloorOrder()))
                .allowedCategories(allowedCategories.stream().map(Enum::name).sorted().toList())
                .placedEquipment(placed)
                .availableCompatibleEquipment(available)
                .build();
    }

    private void recordArchiveLog(HospitalEquipment equipment, String action, String reason) {
        archiveLogRepository.save(EquipmentArchiveLog.builder()
                .equipment(equipment)
                .action(action)
                .reason(trimOrNull(reason))
                .snapshot(snapshot(equipment))
                .build());
    }

    private String snapshot(HospitalEquipment equipment) {
        return "{" +
                "\"code\":\"" + equipment.getEquipmentCode() + "\"," +
                "\"name\":\"" + equipment.getName() + "\"," +
                "\"category\":\"" + equipment.getCategory() + "\"," +
                "\"subtype\":\"" + equipment.getSubtype() + "\"," +
                "\"status\":\"" + equipment.getStatus() + "\"" +
                "}";
    }

    private void archivePlacementIfExists(Long equipmentId) {
        placementRepository.findByEquipmentIdAndActiveTrue(equipmentId).ifPresent(placement -> {
            placement.setActive(false);
            placement.setRemovedAt(LocalDateTime.now());
            placementRepository.save(placement);
        });
    }

    private EquipmentResponse enrichWithPlacement(EquipmentResponse response) {
        EquipmentPlacement placement = placementRepository.findByEquipmentIdAndActiveTrue(response.getId()).orElse(null);
        if (placement == null) {
            return response;
        }

        return EquipmentResponse.builder()
                .id(response.getId())
                .equipmentCode(response.getEquipmentCode())
                .name(response.getName())
                .category(response.getCategory())
                .subtype(response.getSubtype())
                .status(response.getStatus())
                .description(response.getDescription())
                .archived(response.isArchived())
                .archivedAt(response.getArchivedAt())
                .archivedReason(response.getArchivedReason())
                .currentWorkspaceId(placement.getWorkspace().getId())
                .currentWorkspaceCode(placement.getWorkspace().getWorkspaceCode())
                .currentWorkspaceName(placement.getWorkspace().getWorkspaceName())
                .currentFloorLabel(floorLabel(placement.getWorkspace().getFloor().getFloorOrder()))
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    private List<EquipmentResponse> enrichWithPlacement(List<EquipmentResponse> base) {
        if (base.isEmpty()) {
            return base;
        }

        List<Long> ids = base.stream().map(EquipmentResponse::getId).toList();
        Map<Long, EquipmentPlacement> placementByEquipmentId = placementRepository.findByEquipmentIdInAndActiveTrue(ids).stream()
                .collect(Collectors.toMap(p -> p.getEquipment().getId(), p -> p));

        List<EquipmentResponse> enriched = new ArrayList<>();
        for (EquipmentResponse response : base) {
            EquipmentPlacement placement = placementByEquipmentId.get(response.getId());
            if (placement == null) {
                enriched.add(response);
                continue;
            }

            enriched.add(EquipmentResponse.builder()
                    .id(response.getId())
                    .equipmentCode(response.getEquipmentCode())
                    .name(response.getName())
                    .category(response.getCategory())
                    .subtype(response.getSubtype())
                    .status(response.getStatus())
                    .description(response.getDescription())
                    .archived(response.isArchived())
                    .archivedAt(response.getArchivedAt())
                    .archivedReason(response.getArchivedReason())
                    .currentWorkspaceId(placement.getWorkspace().getId())
                    .currentWorkspaceCode(placement.getWorkspace().getWorkspaceCode())
                    .currentWorkspaceName(placement.getWorkspace().getWorkspaceName())
                    .currentFloorLabel(floorLabel(placement.getWorkspace().getFloor().getFloorOrder()))
                    .createdAt(response.getCreatedAt())
                    .updatedAt(response.getUpdatedAt())
                    .build());
        }

        return enriched;
    }

    private String floorLabel(Integer order) {
        return order == 0 ? "GF" : String.valueOf(order);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
