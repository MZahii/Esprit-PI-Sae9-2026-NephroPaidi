package tn.esprit.spring.clinicalservice.appointment.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartConsultationResponse {
    private UUID consultationId;
}
