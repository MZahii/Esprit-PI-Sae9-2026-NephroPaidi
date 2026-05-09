package tn.esprit.spring.procedureservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "tn.esprit.spring.procedureservice.client")
public class ProcedureServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcedureServiceApplication.class, args);
	}

}
