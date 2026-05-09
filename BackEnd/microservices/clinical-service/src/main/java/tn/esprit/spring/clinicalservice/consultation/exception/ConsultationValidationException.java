package tn.esprit.spring.clinicalservice.consultation.exception;

/**
 * Exception thrown when consultation validation fails.
 * This includes doctor existence validation and appointment state checks.
 */
public class ConsultationValidationException extends RuntimeException {
    public ConsultationValidationException(String message) {
        super(message);
    }

    public ConsultationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
