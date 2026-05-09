package tn.esprit.spring.clinicalservice.client;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * Global Feign client configuration
 * Applied to all @FeignClient interfaces
 */
@Configuration
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
            requestTemplate.header("X-Service-Id", "clinical-service");
            requestTemplate.header("Accept", "application/json");
            log.debug("Adding Feign request headers");
        };
    }
    
    @Bean
    public feign.Request.Options requestOptions() {
        return new feign.Request.Options(
            5000,     // connectTimeout: 5 seconds
            10000,    // readTimeout: 10 seconds
            true      // followRedirects
        );
    }
    
    @Bean
    public org.springframework.cloud.openfeign.support.SpringDecoder feignDecoder(
        ObjectProvider<HttpMessageConverters> httpMessageConverters) {
        return new SpringDecoder(httpMessageConverters);
    }
    
    @Bean
    public org.springframework.cloud.openfeign.support.SpringEncoder feignEncoder(
        ObjectProvider<HttpMessageConverters> httpMessageConverters) {
        return new SpringEncoder(httpMessageConverters);
    }
}

@Slf4j
class FeignErrorHandler implements ErrorDecoder {
    
    @Override
    public Exception decode(String methodKey, feign.Response response) {
        
        log.error("Feign error - Method: {}, Status: {}", methodKey, response.status());
        
        try {
            String body = response.body() != null ? new String(response.body().asInputStream().readAllBytes()) : "";
            log.error("Response body: {}", body);
        } catch (Exception e) {
            log.error("Could not read response body", e);
        }
        
        switch (response.status()) {
            case 400:
                return new IllegalArgumentException("Bad Request in " + methodKey);
            case 401:
                return new SecurityException("Unauthorized in " + methodKey);
            case 403:
                return new SecurityException("Forbidden in " + methodKey);
            case 404:
                return new ResourceNotFoundException("Resource not found in " + methodKey);
            case 409:
                return new ConflictException("Conflict in " + methodKey);
            case 500:
                return new InternalServerException("Internal server error in " + methodKey);
            case 503:
                return new ServiceUnavailableException("Service unavailable in " + methodKey);
            default:
                return new FeignException("Error calling " + methodKey + " - Status: " + response.status());
        }
    }
}

class FeignException extends RuntimeException {
    public FeignException(String message) { 
        super(message); 
    }
    public FeignException(String message, Throwable cause) { 
        super(message, cause); 
    }
}

class ResourceNotFoundException extends FeignException {
    public ResourceNotFoundException(String message) { 
        super(message); 
    }
}

class ServiceUnavailableException extends FeignException {
    public ServiceUnavailableException(String message) { 
        super(message); 
    }
}

class SecurityException extends FeignException {
    public SecurityException(String message) { 
        super(message); 
    }
}

class ConflictException extends FeignException {
    public ConflictException(String message) { 
        super(message); 
    }
}

class InternalServerException extends FeignException {
    public InternalServerException(String message) { 
        super(message); 
    }
}
