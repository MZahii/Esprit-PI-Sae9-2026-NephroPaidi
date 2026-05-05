package tn.esprit.spring.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicalGatewayRoutesConfig {

    @Bean
    public RouteLocator clinicalRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("clinical-service-direct", route -> route
                .order(-100)
                .path("/clinical/**")
                .uri("lb://clinical-service")
            )
            .route("clinical-service-api-compat", route -> route
                .order(-99)
                .path("/api/clinical/**")
                .filters(filters -> filters.rewritePath("/api/clinical/?(?<segment>.*)", "/clinical/${segment}"))
                .uri("lb://clinical-service")
            )
            .build();
    }
}
