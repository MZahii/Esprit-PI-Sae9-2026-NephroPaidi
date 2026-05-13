from __future__ import annotations

import logging
from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException, Query

from .monitoring import monitoring_state
from .predictor import feature_columns, load_models, predict_classification, predict_regression
from .schemas import (
    ClassificationResponse,
    MetadataResponse,
    MonitoringResponse,
    PredictRequest,
    RegressionResponse,
)
from .validation import ValidationError, validate_prediction_payload

app = FastAPI(title="AI Renal Function Forecasting", version="1.1.0")
MODELS_LOADED = False
logger = logging.getLogger("egfr_ml_service")
logging.basicConfig(level=logging.INFO)


@app.on_event("startup")
def startup() -> None:
    global MODELS_LOADED
    try:
        load_models()
        MODELS_LOADED = True
    except Exception:
        MODELS_LOADED = False


@app.get("/")
def root() -> dict:
    return {
        "message": "eGFR ML service is running",
        "docs": "/docs",
        "health": "/health",
        "monitoring": "/monitoring",
        "predict_regression": "/predict/regression",
        "predict_classification": "/predict/classification?threshold=0.5",
    }


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "models_loaded": MODELS_LOADED}


@app.get("/metadata", response_model=MetadataResponse)
def metadata() -> MetadataResponse:
    return MetadataResponse(feature_count=len(feature_columns))


@app.get("/monitoring", response_model=MonitoringResponse)
def monitoring() -> MonitoringResponse:
    return MonitoringResponse(**monitoring_state.snapshot())


@app.post("/predict/regression", response_model=RegressionResponse)
def predict_regression_endpoint(payload: PredictRequest) -> RegressionResponse:
    raw_payload = payload.model_dump()
    monitoring_state.requests_total += 1
    try:
        validated_payload = validate_prediction_payload(raw_payload)
        pred = predict_regression(validated_payload)
        logger.info(
            "regression_ok ts=%s keys=%s",
            datetime.now(timezone.utc).isoformat(),
            sorted(validated_payload.keys()),
        )
        monitoring_state.record_event(
            {
                "type": "regression_ok",
                "ts": datetime.now(timezone.utc).isoformat(),
                "predicted_egfr_12_months": round(pred["predicted_egfr_12_months"], 3),
            }
        )
    except ValidationError as exc:
        monitoring_state.validation_errors += 1
        monitoring_state.record_event(
            {"type": "validation_error", "ts": datetime.now(timezone.utc).isoformat(), "detail": str(exc)}
        )
        logger.warning("regression_validation_error detail=%s payload=%s", str(exc), raw_payload)
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:
        monitoring_state.inference_errors += 1
        monitoring_state.record_event(
            {"type": "regression_error", "ts": datetime.now(timezone.utc).isoformat(), "detail": str(exc)}
        )
        logger.exception("regression_failed payload=%s", raw_payload)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return RegressionResponse(**pred)


@app.post("/predict/classification", response_model=ClassificationResponse)
def predict_classification_endpoint(
    payload: PredictRequest, threshold: float = Query(0.5, ge=0.0, le=1.0)
) -> ClassificationResponse:
    raw_payload = payload.model_dump()
    monitoring_state.requests_total += 1
    try:
        validated_payload = validate_prediction_payload(raw_payload)
        out = predict_classification(validated_payload, threshold)
        if out["confidence_score"] < 0.2:
            monitoring_state.low_confidence_predictions += 1
            monitoring_state.record_event(
                {
                    "type": "low_confidence",
                    "ts": datetime.now(timezone.utc).isoformat(),
                    "probability": round(float(out["rapid_decline_probability"]), 4),
                    "threshold": threshold,
                }
            )
        monitoring_state.record_event(
            {
                "type": "classification_ok",
                "ts": datetime.now(timezone.utc).isoformat(),
                "risk_label": out["risk_label"],
                "confidence_score": round(float(out["confidence_score"]), 4),
            }
        )
        logger.info(
            "classification_ok ts=%s risk=%s prob=%.4f confidence=%.4f",
            datetime.now(timezone.utc).isoformat(),
            out["risk_label"],
            float(out["rapid_decline_probability"]),
            float(out["confidence_score"]),
        )
    except ValidationError as exc:
        monitoring_state.validation_errors += 1
        monitoring_state.record_event(
            {"type": "validation_error", "ts": datetime.now(timezone.utc).isoformat(), "detail": str(exc)}
        )
        logger.warning("classification_validation_error detail=%s payload=%s", str(exc), raw_payload)
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:
        monitoring_state.inference_errors += 1
        monitoring_state.record_event(
            {"type": "classification_error", "ts": datetime.now(timezone.utc).isoformat(), "detail": str(exc)}
        )
        logger.exception("classification_failed payload=%s", raw_payload)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return ClassificationResponse(**out)

