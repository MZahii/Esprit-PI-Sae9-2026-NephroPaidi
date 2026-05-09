package tn.esprit.spring.Administrationservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.Administrationservice.entity.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateStaffContractRequest {

    @NotNull(message = "Staff user id is required")
    private Long staffUserId;

    private String contractReference;

    @NotNull(message = "Contract type is required")
    private ContractType contractType;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    private String department;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be positive")
    private BigDecimal salary;

    private String currency;

    @Min(value = 1, message = "Hours per week must be at least 1")
    @Max(value = 80, message = "Hours per week must be at most 80")
    private Integer hoursPerWeek;

    private String notes;
}
