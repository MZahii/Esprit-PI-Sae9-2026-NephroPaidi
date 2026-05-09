package tn.esprit.spring.procedureservice.surgical.dto.response;

public record CareTaskResponse(Long id, Long surgicalCaseId, String title, boolean done) {
}
