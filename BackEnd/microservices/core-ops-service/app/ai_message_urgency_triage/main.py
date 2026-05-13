from fastapi import FastAPI

from .model_loader import TriageModel
from .schemas import TriageRequest, TriageResponse


app = FastAPI(
    title="AI Message Urgency Triage",
    version="0.1.0",
    description="Predicts guardian message urgency using creation-time message features.",
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
