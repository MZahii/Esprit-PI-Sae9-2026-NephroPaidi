package tn.esprit.spring.pharmacyservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierDTO {
    private Long supplierId;
    private String name;
    private String contactInfo;
    private String email;
    private Boolean isActive;
}
