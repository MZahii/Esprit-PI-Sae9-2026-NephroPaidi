package tn.esprit.spring.clinicalservice.appointment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCancelRequest {
    private String reason;
}
