package tn.esprit.spring.procedureservice.dialysis.dto.response;

public record DialysisOutcomeResponse(Long id, Long sessionId, boolean validated, String summary) {
}
