package tn.esprit.spring.clinicalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "tn.esprit.spring.clinicalservice.client")
@SpringBootApplication
public class ClinicalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClinicalServiceApplication.class, args);
	}

}
