package tn.esprit.spring.coreopsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "tn.esprit.spring.coreopsservice.client")
public class CoreOpsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreOpsServiceApplication.class, args);
	}

}
