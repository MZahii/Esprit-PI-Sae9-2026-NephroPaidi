package tn.esprit.spring.clinicalservice.consultation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class FollowUpSuggestionResponse {
    private LocalDate suggestedDate;
    private Integer intervalDays;
    private String reason;
}
