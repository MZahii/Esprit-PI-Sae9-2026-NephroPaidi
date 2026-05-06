package tn.esprit.spring.mlpredictionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import tn.esprit.spring.mlpredictionservice.config.MlInferenceProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(MlInferenceProperties.class)
public class MlPredictionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlPredictionServiceApplication.class, args);
    }
}
