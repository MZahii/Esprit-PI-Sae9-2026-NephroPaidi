package tn.esprit.spring.pharmacyservice.client;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

// NOTE: deliberately NOT annotated with @Configuration — if it were, Spring's
// component scan would pick it up and apply it globally to ALL Feign clients.
// Feign uses this class per-client via the `configuration` attribute on @FeignClient.
@Slf4j
public class FeignClientConfiguration {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorHandler();
    }

    @Bean
    public feign.RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Service-Id", "pharmacy-service");
            requestTemplate.header("Accept", "application/json");
        };
    }

    @Bean
    public feign.Request.Options requestOptions() {
        return new feign.Request.Options(5000, 10000, true);
    }
}

@Slf4j
class FeignErrorHandler implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, feign.Response response) {
        log.error("Feign error - Method: {}, Status: {}", methodKey, response.status());
        switch (response.status()) {
            case 404: return new ResourceNotFoundException("Resource not found in " + methodKey);
            case 503: return new ServiceUnavailableException("Service unavailable in " + methodKey);
            default: return new FeignException("Error calling " + methodKey);
        }
    }
}

class FeignException extends RuntimeException {
    public FeignException(String message) { super(message); }
}

class ResourceNotFoundException extends FeignException {
    public ResourceNotFoundException(String message) { super(message); }
}

class ServiceUnavailableException extends FeignException {
    public ServiceUnavailableException(String message) { super(message); }
}
