package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;
import tn.esprit.spring.Administrationservice.entity.EquipmentStatus;
import tn.esprit.spring.Administrationservice.entity.HospitalEquipment;

import java.time.LocalDateTime;

@Getter
@Builder
public class EquipmentResponse {
    private Long id;
    private String equipmentCode;
    private String name;
    private EquipmentCategory category;
    private String subtype;
    private EquipmentStatus status;
    private String description;
    private boolean archived;
    private LocalDateTime archivedAt;
    private String archivedReason;
    private Long currentWorkspaceId;
    private String currentWorkspaceCode;
    private String currentWorkspaceName;
    private String currentFloorLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EquipmentResponse from(HospitalEquipment equipment) {
        return EquipmentResponse.builder()
                .id(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .name(equipment.getName())
                .category(equipment.getCategory())
                .subtype(equipment.getSubtype())
                .status(equipment.getStatus())
                .description(equipment.getDescription())
                .archived(equipment.isArchived())
                .archivedAt(equipment.getArchivedAt())
                .archivedReason(equipment.getArchivedReason())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .build();
    }
}
