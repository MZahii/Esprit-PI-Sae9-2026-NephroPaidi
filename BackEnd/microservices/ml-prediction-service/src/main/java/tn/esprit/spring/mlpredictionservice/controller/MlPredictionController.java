package tn.esprit.spring.mlpredictionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.mlpredictionservice.dto.PredictionRequest;
import tn.esprit.spring.mlpredictionservice.dto.PredictionResponse;
import tn.esprit.spring.mlpredictionservice.service.MlPredictionEngine;

import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class MlPredictionController {

    private final MlPredictionEngine predictionEngine;

    @PostMapping("/pre-op")
    public ResponseEntity<PredictionResponse> predictPreOp(@Valid @RequestBody PredictionRequest request) {
        return ResponseEntity.ok(predictionEngine.predictPreOp(request));
    }

    @PostMapping("/post-op")
    public ResponseEntity<PredictionResponse> predictPostOp(@Valid @RequestBody PredictionRequest request) {
        return ResponseEntity.ok(predictionEngine.predictPostOp(request));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "mode", "stub",
                "message", "Replace the heuristic engine with the trained model when notebook and artifacts are ready."
        ));
    }
}
