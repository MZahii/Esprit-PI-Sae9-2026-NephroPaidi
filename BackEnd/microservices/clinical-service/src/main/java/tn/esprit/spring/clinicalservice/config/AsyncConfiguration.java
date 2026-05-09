package tn.esprit.spring.clinicalservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous event processing
 * Enables @Async annotation on event listeners
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
}
