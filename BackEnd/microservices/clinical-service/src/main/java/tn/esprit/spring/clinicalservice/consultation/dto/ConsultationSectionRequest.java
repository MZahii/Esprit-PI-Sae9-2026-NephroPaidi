package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultationSectionRequest {
    private Boolean checked;
    private String content;
}
