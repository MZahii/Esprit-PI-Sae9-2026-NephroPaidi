package tn.esprit.spring.Administrationservice.exception;

/**
 * Exception thrown when external service calls fail.
 * Typically used for inter-service communication failures.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
