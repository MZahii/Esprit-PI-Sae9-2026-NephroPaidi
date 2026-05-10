import importlib
import os
import sys
from pathlib import Path

from fastapi.testclient import TestClient


SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))


def _load_app(model_path: str | None = None):
    if model_path is None:
        os.environ["AI_TRIAGE_MODEL_PATH"] = str(SERVICE_ROOT / "models" / "followup_triage.joblib")
    else:
        os.environ["AI_TRIAGE_MODEL_PATH"] = model_path
    os.environ["AI_TRIAGE_MODEL_VERSION"] = "test-model"

    for module_name in ["app.main", "app.model_loader"]:
        sys.modules.pop(module_name, None)

    return importlib.import_module("app.main").app


def test_health_reports_loaded_model():
    client = TestClient(_load_app())

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP", "modelLoaded": True}


def test_predict_response_contract_for_realistic_messages():
    client = TestClient(_load_app())
    examples = [
        {
            "messageText": "Merci pour le document, je passerai demain le recuperer.",
            "subject": "Document administratif",
            "messageType": "ADMINISTRATIVE",
            "guardianPriority": "NORMAL",
        },
        {
            "messageText": "Bonjour, est-ce que je dois donner le medicament avant le repas?",
            "subject": "Question traitement",
            "messageType": "MEDICAL",
            "guardianPriority": "NORMAL",
        },
        {
            "messageText": "Fievre a 39 et vomissements depuis hier soir.",
            "subject": "Symptomes",
            "messageType": "MEDICAL",
            "guardianPriority": "HIGH",
        },
        {
            "messageText": "Difficulte a respirer et douleur severe, besoin d'un medecin urgent.",
            "subject": "Urgence",
            "messageType": "MEDICAL",
            "guardianPriority": "HIGH",
        },
    ]

    for payload in examples:
        response = client.post("/predict", json=payload)
        body = response.json()

        assert response.status_code == 200
        assert body["urgencyLevel"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
        assert 0 <= body["confidence"] <= 1
        assert body["status"] == "SUCCESS"
        assert body["explanation"]
        assert body["modelVersion"] == "test-model"


def test_predict_uses_fallback_when_model_is_missing():
    client = TestClient(_load_app(str(SERVICE_ROOT / "models" / "missing.joblib")))

    response = client.post(
        "/predict",
        json={
            "messageText": "Fievre et douleur depuis hier soir.",
            "subject": "Symptomes",
            "messageType": "MEDICAL",
            "guardianPriority": "HIGH",
        },
    )
    body = response.json()

    assert response.status_code == 200
    assert body["status"] == "FALLBACK"
    assert body["modelVersion"] == "fallback-rules-v0"
    assert body["urgencyLevel"] in {"HIGH", "CRITICAL"}
