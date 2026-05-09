package tn.esprit.spring.opsservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "ops-service", "status", "ok");
    }
}