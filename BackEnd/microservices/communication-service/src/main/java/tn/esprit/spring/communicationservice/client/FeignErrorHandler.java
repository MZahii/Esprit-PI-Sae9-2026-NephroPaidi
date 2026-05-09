package tn.esprit.spring.communicationservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom error decoder for Feign clients
 * Handles inter-service communication errors
 */
@Slf4j
public class FeignErrorHandler implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = String.format(
            "Service call failed: %s - Status: %d",
            methodKey, response.status()
        );
        
        log.error(message);
        
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(response.status());
        } catch (IllegalArgumentException e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        return new ResponseStatusException(status, message);
    }
}
