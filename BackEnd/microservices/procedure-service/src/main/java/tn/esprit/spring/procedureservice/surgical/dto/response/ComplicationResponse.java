package tn.esprit.spring.procedureservice.surgical.dto.response;

public record ComplicationResponse(Long id, Long surgicalCaseId, String description) {
}
