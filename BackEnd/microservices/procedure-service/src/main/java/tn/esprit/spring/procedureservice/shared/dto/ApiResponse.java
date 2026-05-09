package tn.esprit.spring.procedureservice.shared.dto;

public record ApiResponse<T>(String message, T data) {
}
