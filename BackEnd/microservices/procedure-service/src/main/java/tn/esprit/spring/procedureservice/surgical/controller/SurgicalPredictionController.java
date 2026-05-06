package tn.esprit.spring.procedureservice.surgical.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.procedureservice.surgical.domain.entity.SurgicalPrediction;
import tn.esprit.spring.procedureservice.surgical.dto.response.SurgicalPredictionResponse;
import tn.esprit.spring.procedureservice.surgical.service.SurgicalPredictionService;

import java.util.List;

@RestController
@RequestMapping("/api/procedures/surgical/predictions")
@RequiredArgsConstructor
public class SurgicalPredictionController {

    private final SurgicalPredictionService surgicalPredictionService;

    @GetMapping
    public List<SurgicalPredictionResponse> bySurgicalCase(@RequestParam Long surgicalCaseId) {
        return surgicalPredictionService.findBySurgicalCaseId(surgicalCaseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/pre-op/{surgicalCaseId}")
    public SurgicalPredictionResponse runPreOp(@PathVariable Long surgicalCaseId) {
        return toResponse(surgicalPredictionService.runPreOpPrediction(surgicalCaseId));
    }

    @PostMapping("/post-op/{surgicalCaseId}")
    public SurgicalPredictionResponse runPostOp(@PathVariable Long surgicalCaseId) {
        return toResponse(surgicalPredictionService.runPostOpPrediction(surgicalCaseId));
    }

    private SurgicalPredictionResponse toResponse(SurgicalPrediction prediction) {
        return new SurgicalPredictionResponse(
                prediction.getId(),
                prediction.getSurgicalCase().getId(),
                prediction.getPhase(),
                prediction.getModelName(),
                prediction.getModelVersion(),
                prediction.getPredictionLabel(),
                prediction.getRiskLevel(),
                prediction.getProbability(),
                prediction.getRecommendation(),
                prediction.getInputSnapshotJson(),
                prediction.getOutputJson(),
                prediction.getCreatedAt()
        );
    }
}
