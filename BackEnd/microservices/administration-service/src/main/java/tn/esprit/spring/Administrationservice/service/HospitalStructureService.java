package tn.esprit.spring.Administrationservice.service;

import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;

import java.util.List;

public interface HospitalStructureService {

    HospitalStructureResponse getStructure();

    HospitalStructureResponse initializeFloors(InitializeFloorsRequest request);

    FloorResponse addFloor(AddFloorRequest request);

    FloorResponse updateFloor(Long floorId, UpdateFloorRequest request);

    HospitalStructureResponse deleteFloor(Long floorId);

    List<WorkspaceResponse> createWorkspaces(CreateWorkspaceRequest request);

    WorkspaceResponse updateWorkspace(Long workspaceId, UpdateWorkspaceRequest request);

    FloorResponse deleteWorkspace(Long workspaceId);

    FloorResponse updateWorkspaceGroupQuantity(UpdateWorkspaceGroupQuantityRequest request);

    FloorResponse deleteWorkspaceGroup(Long floorId, WorkspaceType workspaceType);

    List<WorkspaceTypeOptionResponse> getWorkspaceTypeOptions();
}
