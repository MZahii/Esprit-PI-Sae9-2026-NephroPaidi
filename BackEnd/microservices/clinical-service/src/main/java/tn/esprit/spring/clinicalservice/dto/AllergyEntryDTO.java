package tn.esprit.spring.clinicalservice.dto;

import lombok.*;

/**
 * DTO for AllergyEntry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllergyEntryDTO {
    private String allergyType;
    private String responsibleAgent;
    private String reactionType;
    private String severity;
    private String status;
}
