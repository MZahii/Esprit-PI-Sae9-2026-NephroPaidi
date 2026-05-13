from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


app = FastAPI(
    title="AI Surgical Risk Prediction",
    version="0.1.0",
    description="Reserved surgical complication risk endpoint. No trained artifact is available in the repository yet.",
)


class SurgicalRiskPredictionRequest(BaseModel):
    patient_id: int | None = None
    surgical_case_id: int | None = None
    procedure_name: str | None = None
    surgery_type: str | None = None
    urgency_level: str | None = None
    age: int | None = None
    notes: str | None = None


class SurgicalRiskPredictionResponse(BaseModel):
    status: str
    detail: str
    model_version: str = Field(default="unavailable")


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "status": "UNAVAILABLE",
        "implemented": False,
        "reason": "No surgical-risk trained model artifact exists in the repository.",
    }


@app.post("/predict", response_model=SurgicalRiskPredictionResponse)
def predict(_: SurgicalRiskPredictionRequest) -> SurgicalRiskPredictionResponse:
    raise HTTPException(
        status_code=501,
        detail="AI Surgical Risk Prediction is defined architecturally, but no trained model artifact exists yet.",
    )
