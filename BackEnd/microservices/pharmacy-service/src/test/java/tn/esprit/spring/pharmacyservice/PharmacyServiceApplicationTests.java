package tn.esprit.spring.pharmacyservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application Context — Smoke Test")
class PharmacyServiceApplicationTests {

    @Test
    @DisplayName("Spring context loads without errors")
    void contextLoads() {
        // Verifies all beans wire correctly (security, JPA, services, controllers)
    }
}
