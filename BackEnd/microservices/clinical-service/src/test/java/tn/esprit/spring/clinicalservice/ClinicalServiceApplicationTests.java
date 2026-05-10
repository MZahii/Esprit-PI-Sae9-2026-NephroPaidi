package tn.esprit.spring.clinicalservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tn.esprit.spring.clinicalservice.config.TestConfig;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(locations = "classpath:application-test.yml")
class ClinicalServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
