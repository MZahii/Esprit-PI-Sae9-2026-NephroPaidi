package tn.esprit.spring.clinicalservice;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tn.esprit.spring.clinicalservice.config.TestConfig;

/**
 * Base integration test class for Clinical Service
 * Provides common setup for all integration tests
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = ClinicalServiceApplication.class,
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=false"
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(locations = "classpath:application-test.yml")
public abstract class BaseIntegrationTest {
    
    // Common test setup and utilities can be added here
}
