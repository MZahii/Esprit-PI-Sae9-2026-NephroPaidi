package tn.esprit.spring.communicationservice.client;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
            requestTemplate.header("X-Service-Id", "communication-service");
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
