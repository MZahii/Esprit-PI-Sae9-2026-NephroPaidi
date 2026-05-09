package tn.esprit.spring.Administrationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.Administrationservice.entity.ContractStatus;
import tn.esprit.spring.Administrationservice.entity.StaffContract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StaffContractRepository extends JpaRepository<StaffContract, Long> {
    Optional<StaffContract> findByContractReferenceAndDeletedFalse(String contractReference);
    List<StaffContract> findByStaffUserIdAndDeletedFalse(Long staffUserId);
    List<StaffContract> findByStatusAndDeletedFalse(ContractStatus status);
    boolean existsByStaffUserIdAndStatusInAndDeletedFalse(Long staffUserId, List<ContractStatus> statuses);
    List<StaffContract> findByStatusInAndEndDateBeforeAndDeletedFalse(List<ContractStatus> statuses, LocalDate date);
    List<StaffContract> findByStatusInAndEndDateBetweenAndDeletedFalse(List<ContractStatus> statuses, LocalDate from, LocalDate to);
    Optional<StaffContract> findByIdAndDeletedFalse(Long id);
}
