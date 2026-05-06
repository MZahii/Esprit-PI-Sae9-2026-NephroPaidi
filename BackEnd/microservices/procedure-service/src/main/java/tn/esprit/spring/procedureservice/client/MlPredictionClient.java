package tn.esprit.spring.procedureservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.spring.procedureservice.surgical.dto.ml.MlPredictionRequest;
import tn.esprit.spring.procedureservice.surgical.dto.ml.MlPredictionResponse;

@FeignClient(
        name = "ml-prediction-service",
        url = "${ML_PREDICTION_SERVICE_URL:}",
        path = "/api/predictions",
        configuration = FeignClientConfiguration.class
)
public interface MlPredictionClient {

    @PostMapping("/pre-op")
    MlPredictionResponse predictPreOp(@RequestBody MlPredictionRequest request);

    @PostMapping("/post-op")
    MlPredictionResponse predictPostOp(@RequestBody MlPredictionRequest request);
}
