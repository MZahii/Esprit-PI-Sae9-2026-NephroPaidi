package tn.esprit.spring.clinicalservice.followup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorFollowUpConfirmRequest {

    @NotNull
    private LocalDateTime scheduledAt;

    /** Optional override; defaults to requesting doctor */
    private UUID doctorId;

    private Integer durationMinutes;

    private String reason;
}
