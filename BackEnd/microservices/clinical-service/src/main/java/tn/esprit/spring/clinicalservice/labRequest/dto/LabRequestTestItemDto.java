package tn.esprit.spring.clinicalservice.labRequest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabRequestTestItemDto {
    private String key;
    private String label;
    private String note;
}
