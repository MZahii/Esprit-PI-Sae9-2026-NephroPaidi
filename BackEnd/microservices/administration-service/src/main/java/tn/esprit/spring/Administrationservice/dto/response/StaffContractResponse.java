package tn.esprit.spring.Administrationservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import tn.esprit.spring.Administrationservice.entity.ContractStatus;
import tn.esprit.spring.Administrationservice.entity.ContractType;
import tn.esprit.spring.Administrationservice.entity.StaffContract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StaffContractResponse {
    private Long id;
    private Long staffUserId;
    private String contractReference;
    private ContractType contractType;
    private ContractStatus status;
    private String jobTitle;
    private String department;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal salary;
    private String currency;
    private Integer hoursPerWeek;
    private String notes;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StaffContractResponse from(StaffContract contract) {
        return StaffContractResponse.builder()
                .id(contract.getId())
                .staffUserId(contract.getStaffUserId())
                .contractReference(contract.getContractReference())
                .contractType(contract.getContractType())
                .status(contract.getStatus())
                .jobTitle(contract.getJobTitle())
                .department(contract.getDepartment())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .salary(contract.getSalary())
                .currency(contract.getCurrency())
                .hoursPerWeek(contract.getHoursPerWeek())
                .notes(contract.getNotes())
                .deleted(contract.isDeleted())
                .deletedAt(contract.getDeletedAt())
                .deletedBy(contract.getDeletedBy())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
}
