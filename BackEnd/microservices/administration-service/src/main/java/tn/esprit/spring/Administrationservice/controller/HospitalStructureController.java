package tn.esprit.spring.Administrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.Administrationservice.dto.request.*;
import tn.esprit.spring.Administrationservice.dto.response.*;
import tn.esprit.spring.Administrationservice.entity.WorkspaceType;
import tn.esprit.spring.Administrationservice.service.HospitalStructureService;

import java.util.List;

@RestController
@RequestMapping("/hospital-structure")
@RequiredArgsConstructor
public class HospitalStructureController {

    private final HospitalStructureService hospitalStructureService;

    @GetMapping
    public HospitalStructureResponse getStructure() {
        return hospitalStructureService.getStructure();
    }

    @PostMapping("/floors/initialize")
    public HospitalStructureResponse initializeFloors(@Valid @RequestBody InitializeFloorsRequest request) {
        return hospitalStructureService.initializeFloors(request);
    }

    @PostMapping("/floors")
    public FloorResponse addFloor(@Valid @RequestBody AddFloorRequest request) {
        return hospitalStructureService.addFloor(request);
    }

    @PutMapping("/floors/{floorId}")
    public FloorResponse updateFloor(@PathVariable Long floorId, @RequestBody UpdateFloorRequest request) {
        return hospitalStructureService.updateFloor(floorId, request);
    }

    @DeleteMapping("/floors/{floorId}")
    public HospitalStructureResponse deleteFloor(@PathVariable Long floorId) {
        return hospitalStructureService.deleteFloor(floorId);
    }

    @PostMapping("/workspaces")
    public List<WorkspaceResponse> createWorkspaces(@Valid @RequestBody CreateWorkspaceRequest request) {
        return hospitalStructureService.createWorkspaces(request);
    }

    @PutMapping("/workspaces/{workspaceId}")
    public WorkspaceResponse updateWorkspace(@PathVariable Long workspaceId, @RequestBody UpdateWorkspaceRequest request) {
        return hospitalStructureService.updateWorkspace(workspaceId, request);
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    public FloorResponse deleteWorkspace(@PathVariable Long workspaceId) {
        return hospitalStructureService.deleteWorkspace(workspaceId);
    }

    @PutMapping("/workspaces/groups")
    public FloorResponse updateWorkspaceGroupQuantity(@Valid @RequestBody UpdateWorkspaceGroupQuantityRequest request) {
        return hospitalStructureService.updateWorkspaceGroupQuantity(request);
    }

    @DeleteMapping("/workspaces/groups")
    public FloorResponse deleteWorkspaceGroup(
            @RequestParam Long floorId,
            @RequestParam WorkspaceType workspaceType
    ) {
        return hospitalStructureService.deleteWorkspaceGroup(floorId, workspaceType);
    }

    @GetMapping("/workspace-types")
    public List<WorkspaceTypeOptionResponse> getWorkspaceTypeOptions() {
        return hospitalStructureService.getWorkspaceTypeOptions();
    }
}
