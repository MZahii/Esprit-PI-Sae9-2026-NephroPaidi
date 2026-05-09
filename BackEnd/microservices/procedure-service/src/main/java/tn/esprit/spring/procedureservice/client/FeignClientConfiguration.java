package tn.esprit.spring.procedureservice.client;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

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
            requestTemplate.header("X-Service-Id", "procedure-service");
            requestTemplate.header("Accept", "application/json");
        };
    }
    
    @Bean
    public feign.Request.Options requestOptions() {
        return new feign.Request.Options(5000, 10000, true);
    }
    
    @Bean
    public SpringDecoder feignDecoder(ObjectProvider<HttpMessageConverters> httpMessageConverters) {
        return new SpringDecoder(httpMessageConverters);
    }
    
    @Bean
    public SpringEncoder feignEncoder(ObjectProvider<HttpMessageConverters> httpMessageConverters) {
        return new SpringEncoder(httpMessageConverters);
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
