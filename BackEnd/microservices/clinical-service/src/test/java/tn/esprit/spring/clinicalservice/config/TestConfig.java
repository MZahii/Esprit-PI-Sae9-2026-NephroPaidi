package tn.esprit.spring.clinicalservice.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import tn.esprit.spring.clinicalservice.client.AdministrationClient;

/**
 * Test configuration that provides mock Feign clients to prevent
 * actual service calls during unit tests.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Mock Administration Client for unit tests.
     * Prevents actual calls to administration-service during testing.
     */
    @Bean
    @Primary
    public AdministrationClient administrationClientMock() {
        return Mockito.mock(AdministrationClient.class);
    }
}
