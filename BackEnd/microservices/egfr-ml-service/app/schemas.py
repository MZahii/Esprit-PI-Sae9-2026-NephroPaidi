from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class PredictRequest(BaseModel):
    model_config = ConfigDict(extra="allow")


class RegressionResponse(BaseModel):
    predicted_egfr_3_months: float
    predicted_egfr_6_months: float
    predicted_egfr_12_months: float


class ClassificationResponse(BaseModel):
    rapid_decline_flag: int
    rapid_decline_probability: float
    confidence_score: float = Field(ge=0.0, le=1.0)
    risk_label: str
    threshold: float = Field(ge=0.0, le=1.0)


class MetadataResponse(BaseModel):
    feature_count: int


class MonitoringResponse(BaseModel):
    requests_total: int
    validation_errors: int
    inference_errors: int
    low_confidence_predictions: int
    recent_events: list[dict]
