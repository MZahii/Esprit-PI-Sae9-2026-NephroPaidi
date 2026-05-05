package tn.esprit.spring.Administrationservice.service;

import tn.esprit.spring.Administrationservice.dto.request.CreateStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.request.MoveStaffWorkspaceAssignmentRequest;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceAssignmentResponse;
import tn.esprit.spring.Administrationservice.dto.response.StaffWorkspaceOptionResponse;
import tn.esprit.spring.Administrationservice.entity.StaffPlacementRole;

import java.util.List;

public interface StaffWorkspaceAssignmentService {
    List<StaffWorkspaceAssignmentResponse> listAssignments(StaffPlacementRole role);

    List<StaffWorkspaceOptionResponse> listWorkspaceOptions(StaffPlacementRole role);

    StaffWorkspaceAssignmentResponse createAssignment(CreateStaffWorkspaceAssignmentRequest request);

    StaffWorkspaceAssignmentResponse moveAssignment(Long assignmentId, MoveStaffWorkspaceAssignmentRequest request);

    void deleteAssignment(Long assignmentId);
}
