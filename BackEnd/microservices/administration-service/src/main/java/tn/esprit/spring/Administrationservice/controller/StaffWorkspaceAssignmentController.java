package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.CreateStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.request.MoveStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceAssignmentResponse;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceOptionResponse;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;
import tn.esprit.spring.Administrationservice.service.StaffWorkspaceAssignmentService;

import java.util.List;

@RestController
@RequestMapping("/staff-assignments")
@RequiredArgsConstructor
public class StaffWorkspaceAssignmentController {

    private final StaffWorkspaceAssignmentService staffWorkspaceAssignmentService;

    @GetMapping
    public List<StaffWorkspaceAssignmentResponse> listAssignments(@RequestParam StaffPlacementRole role) {
        return staffWorkspaceAssignmentService.listAssignments(role);
    }

    @GetMapping("/workspaces")
    public List<StaffWorkspaceOptionResponse> listWorkspaceOptions(@RequestParam StaffPlacementRole role) {
        return staffWorkspaceAssignmentService.listWorkspaceOptions(role);
    }

    @PostMapping
    public StaffWorkspaceAssignmentResponse createAssignment(@Valid @RequestBody CreateStaffWorkspaceAssignmentRequest request) {
        return staffWorkspaceAssignmentService.createAssignment(request);
    }

    @PatchMapping("/{assignmentId}/move")
    public StaffWorkspaceAssignmentResponse moveAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody MoveStaffWorkspaceAssignmentRequest request
    ) {
        return staffWorkspaceAssignmentService.moveAssignment(assignmentId, request);
    }

    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        staffWorkspaceAssignmentService.deleteAssignment(assignmentId);
    }
}
