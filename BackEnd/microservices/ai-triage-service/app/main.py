from fastapi import FastAPI

from app.model_loader import TriageModel
from app.schemas import TriageRequest, TriageResponse


app = FastAPI(
    title="NephroPaidi AI Triage Service",
    version="0.1.0",
    description="Isolated follow-up message urgency prediction service.",
)

model = TriageModel()


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "status": "UP",
        "modelLoaded": model.is_model_loaded,
    }


@app.post("/predict", response_model=TriageResponse)
def predict(request: TriageRequest) -> TriageResponse:
    return model.predict(request)
