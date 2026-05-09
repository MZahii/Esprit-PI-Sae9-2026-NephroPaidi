package tn.esprit.spring.Administrationservice.service;

import tn.esprit.spring.Administrationservice.dto.request.CreateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.request.UpdateStaffContractRequest;
import tn.esprit.spring.Administrationservice.dto.response.StaffContractResponse;
import tn.esprit.spring.Administrationservice.entity.ContractStatus;

import java.util.List;

public interface StaffContractService {
    StaffContractResponse create(CreateStaffContractRequest request);
    StaffContractResponse update(Long contractId, UpdateStaffContractRequest request);
    StaffContractResponse getById(Long contractId);
    List<StaffContractResponse> getAll(Long staffUserId, ContractStatus status, boolean includeDeleted);
    StaffContractResponse endContract(Long contractId);
    StaffContractResponse suspendContract(Long contractId);
    StaffContractResponse resumeContract(Long contractId);
    void updateStaffAccess(Long staffUserId, boolean enabled);
    void updateStaffStatus(Long staffUserId, String status);
    void delete(Long contractId);
    StaffContractResponse restore(Long contractId);
    int expireContractsAndDisableAccess();
}
