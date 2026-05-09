package tn.esprit.spring.procedureservice.surgical.dto.response;

public record PostOpObservationResponse(Long id, Long surgicalCaseId, String notes) {
}
