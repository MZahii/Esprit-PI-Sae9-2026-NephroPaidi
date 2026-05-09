package tn.esprit.spring.Administrationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "tn.esprit.spring.Administrationservice.client")
@EnableScheduling
public class AdministrationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdministrationServiceApplication.class, args);
	}

}
