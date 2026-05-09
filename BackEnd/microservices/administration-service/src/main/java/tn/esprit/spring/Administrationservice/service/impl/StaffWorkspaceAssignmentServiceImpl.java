package tn.esprit.spring.Administrationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.Administrationservice.dto.request.CreateStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.request.MoveStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceAssignmentResponse;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceOptionResponse;
import tn.esprit.spring.Administrationservice.entity.FloorWorkspace;
import tn.esprit.spring.Administrationservice.entity.HospitalFloor;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;
import tn.esprit.spring.Administrationservice.entity.StaffWorkspaceAssignment;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;
import tn.esprit.spring.Administrationservice.repository.FloorWorkspaceRepository;
import tn.esprit.spring.Administrationservice.repository.HospitalFloorRepository;
import tn.esprit.spring.Administrationservice.repository.StaffWorkspaceAssignmentRepository;
import tn.esprit.spring.Administrationservice.service.NotificationPublisherService;
import tn.esprit.spring.Administrationservice.service.StaffWorkspaceAssignmentService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StaffWorkspaceAssignmentServiceImpl implements StaffWorkspaceAssignmentService {

    private static final Map<StaffPlacementRole, WorkspaceType> ROLE_WORKSPACE_MAP = new EnumMap<>(StaffPlacementRole.class);

    static {
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.ADMIN, WorkspaceType.ADMIN_OFFICE);
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.HR, WorkspaceType.HR_OFFICE);
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.DOCTOR, WorkspaceType.DOCTOR_OFFICE);
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.LAB_AGENT, WorkspaceType.LABORATORY);
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.PHARMACIST, WorkspaceType.PHARMACY);
        ROLE_WORKSPACE_MAP.put(StaffPlacementRole.RECEPTIONIST, WorkspaceType.RECEPTION);
    }

    private static final Set<StaffPlacementRole> SINGLE_ASSIGNMENT_ROLES = Set.of(
            StaffPlacementRole.ADMIN,
            StaffPlacementRole.HR,
            StaffPlacementRole.LAB_AGENT,
            StaffPlacementRole.PHARMACIST,
            StaffPlacementRole.RECEPTIONIST
    );

    private final StaffWorkspaceAssignmentRepository assignmentRepository;
    private final FloorWorkspaceRepository workspaceRepository;
    private final HospitalFloorRepository floorRepository;
    private final NotificationPublisherService notificationPublisherService;

    @Override
    @Transactional(readOnly = true)
    public List<StaffWorkspaceAssignmentResponse> listAssignments(StaffPlacementRole role) {
        assertSupportedRole(role);

        List<StaffWorkspaceAssignment> assignments = assignmentRepository.findByRoleOrderByCreatedAtDesc(role);
        return assignments.stream()
                .sorted(Comparator
                        .comparing((StaffWorkspaceAssignment assignment) -> assignment.getWorkspace().getFloor().getFloorOrder())
                        .thenComparing(assignment -> assignment.getWorkspace().getWorkspaceName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StaffWorkspaceAssignment::getUserId))
                .map(assignment -> StaffWorkspaceAssignmentResponse.from(
                        assignment,
                        floorLabel(assignment.getWorkspace().getFloor().getFloorOrder())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffWorkspaceOptionResponse> listWorkspaceOptions(StaffPlacementRole role) {
        assertSupportedRole(role);
        WorkspaceType expectedType = expectedWorkspaceType(role);

        List<HospitalFloor> floors = floorRepository.findAllWithWorkspacesOrdered();
        List<StaffWorkspaceOptionResponse> options = new ArrayList<>();

        for (HospitalFloor floor : floors) {
            String floorLabel = floorLabel(floor.getFloorOrder());
            floor.getWorkspaces().stream()
                    .filter(workspace -> workspace.getWorkspaceType() == expectedType)
                    .sorted(Comparator.comparing(FloorWorkspace::getWorkspaceName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(workspace -> options.add(StaffWorkspaceOptionResponse.builder()
                            .workspaceId(workspace.getId())
                            .workspaceCode(workspace.getWorkspaceCode())
                            .workspaceName(workspace.getWorkspaceName())
                            .workspaceType(workspace.getWorkspaceType().name())
                            .floorLabel(floorLabel)
                            .assignedCount(assignmentRepository.countByWorkspaceIdAndRole(workspace.getId(), role))
                            .build()));
        }

        return options;
    }

    @Override
    @Transactional
    public StaffWorkspaceAssignmentResponse createAssignment(CreateStaffWorkspaceAssignmentRequest request) {
        StaffPlacementRole role = request.getRole();
        assertSupportedRole(role);

        FloorWorkspace workspace = getWorkspaceOrThrow(request.getWorkspaceId());
        ensureWorkspaceCompatible(role, workspace);

        if (SINGLE_ASSIGNMENT_ROLES.contains(role) && assignmentRepository.existsByUserIdAndRole(request.getUserId(), role)) {
            throw new IllegalArgumentException("This user already has a " + role.name() + " workspace assignment.");
        }

        if (role == StaffPlacementRole.ADMIN && assignmentRepository.existsByWorkspaceIdAndRole(workspace.getId(), role)) {
            throw new IllegalArgumentException("This Admin Office already has an assigned Admin.");
        }

        Optional<StaffWorkspaceAssignment> duplicate = assignmentRepository
                .findByUserIdAndWorkspaceIdAndRole(request.getUserId(), workspace.getId(), role);
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("This assignment already exists.");
        }

        StaffWorkspaceAssignment created = assignmentRepository.save(StaffWorkspaceAssignment.builder()
                .userId(request.getUserId())
                .role(role)
                .workspace(workspace)
                .build());

        notificationPublisherService.publish(
                "StaffAssignmentCreated",
                "Staff Assignment Created",
                role.name() + " user #" + request.getUserId() + " assigned to " + workspace.getWorkspaceName()
                        + " (" + floorLabel(workspace.getFloor().getFloorOrder()) + ")."
        );

        return StaffWorkspaceAssignmentResponse.from(created, floorLabel(workspace.getFloor().getFloorOrder()));
    }

    @Override
    @Transactional
    public StaffWorkspaceAssignmentResponse moveAssignment(Long assignmentId, MoveStaffWorkspaceAssignmentRequest request) {
        StaffWorkspaceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        FloorWorkspace targetWorkspace = getWorkspaceOrThrow(request.getWorkspaceId());
        StaffPlacementRole role = assignment.getRole();
        ensureWorkspaceCompatible(role, targetWorkspace);
        if (assignment.getWorkspace().getId().equals(targetWorkspace.getId())) {
            throw new IllegalArgumentException("Assignment is already linked to this workspace.");
        }

        FloorWorkspace previousWorkspace = assignment.getWorkspace();

        if (role == StaffPlacementRole.ADMIN) {
            assignmentRepository.findByWorkspaceIdAndRole(targetWorkspace.getId(), role).stream()
                    .filter(existing -> !existing.getId().equals(assignment.getId()))
                    .findFirst()
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("This Admin Office already has an assigned Admin.");
                    });
        }

        Optional<StaffWorkspaceAssignment> duplicate = assignmentRepository
                .findByUserIdAndWorkspaceIdAndRole(assignment.getUserId(), targetWorkspace.getId(), role);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(assignmentId)) {
            throw new IllegalArgumentException("This assignment already exists.");
        }

        assignment.setWorkspace(targetWorkspace);
        StaffWorkspaceAssignment saved = assignmentRepository.save(assignment);
        notificationPublisherService.publish(
                "StaffAssignmentMoved",
                "Staff Assignment Moved",
                role.name() + " user #" + assignment.getUserId() + " moved from "
                        + previousWorkspace.getWorkspaceName() + " to " + targetWorkspace.getWorkspaceName() + "."
        );

        return StaffWorkspaceAssignmentResponse.from(saved, floorLabel(targetWorkspace.getFloor().getFloorOrder()));
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        StaffWorkspaceAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        String workspaceName = assignment.getWorkspace().getWorkspaceName();
        String role = assignment.getRole().name();
        Long userId = assignment.getUserId();
        assignmentRepository.delete(assignment);
        notificationPublisherService.publish(
                "StaffAssignmentDeleted",
                "Staff Assignment Deleted",
                role + " user #" + userId + " was unassigned from " + workspaceName + "."
        );
    }

    private void ensureWorkspaceCompatible(StaffPlacementRole role, FloorWorkspace workspace) {
        WorkspaceType expectedType = expectedWorkspaceType(role);
        if (workspace.getWorkspaceType() != expectedType) {
            throw new IllegalArgumentException("Role " + role.name() + " can only be assigned to workspace type " + expectedType.name());
        }
    }

    private FloorWorkspace getWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
    }

    private void assertSupportedRole(StaffPlacementRole role) {
        if (role == null || !ROLE_WORKSPACE_MAP.containsKey(role)) {
            throw new IllegalArgumentException("Unsupported role for workspace assignment.");
        }
    }

    private WorkspaceType expectedWorkspaceType(StaffPlacementRole role) {
        WorkspaceType type = ROLE_WORKSPACE_MAP.get(role);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported role for workspace assignment: " + role);
        }
        return type;
    }

    private String floorLabel(Integer floorOrder) {
        return floorOrder == 0 ? "GF" : String.valueOf(floorOrder);
    }
}
