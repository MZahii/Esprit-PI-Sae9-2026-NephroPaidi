package tn.esprit.spring.opsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "tn.esprit.spring.opsservice.client")
public class OpsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpsServiceApplication.class, args);
	}

}
