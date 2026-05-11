import os
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.schemas import PredictionStatus, TriageRequest, TriageResponse, UrgencyLevel


DEFAULT_MODEL_PATH = "/app/models/followup_triage.joblib"
FALLBACK_MODEL_VERSION = "fallback-rules-v0"


@dataclass(frozen=True)
class ModelBundle:
    artifact: Any
    version: str


class TriageModel:
    def __init__(self) -> None:
        self._bundle = self._load_bundle()

    @property
    def is_model_loaded(self) -> bool:
        return self._bundle is not None

    def predict(self, request: TriageRequest) -> TriageResponse:
        if self._bundle is None:
            return self._fallback_predict(request)

        # The production artifact should expose a sklearn-compatible predict_proba API.
        # This branch is intentionally conservative until the export pipeline is approved.
        try:
            features = self._build_creation_time_features(request)
            proba = self._bundle.artifact.predict_proba([features])[0]
            class_index = int(max(range(len(proba)), key=lambda index: proba[index]))
            classes = getattr(self._bundle.artifact, "classes_", None)
            class_label = classes[class_index] if classes is not None else self._bundle.artifact.predict([features])[0]
            urgency = UrgencyLevel(str(class_label))
            confidence = float(proba[class_index])
            return TriageResponse(
                urgencyLevel=urgency,
                confidence=round(confidence, 4),
                status=PredictionStatus.SUCCESS,
                explanation="Model prediction generated from creation-time message features.",
                modelVersion=self._bundle.version,
            )
        except Exception as exc:
            fallback = self._fallback_predict(request)
            return fallback.model_copy(
                update={
                    "status": PredictionStatus.FAILED,
                    "explanation": f"Model inference failed; fallback used. Reason: {type(exc).__name__}",
                }
            )

    def _load_bundle(self) -> ModelBundle | None:
        model_path = Path(os.getenv("AI_TRIAGE_MODEL_PATH", DEFAULT_MODEL_PATH))
        if not model_path.exists():
            return None

        try:
            import joblib

            artifact = joblib.load(model_path)
            version = os.getenv("AI_TRIAGE_MODEL_VERSION", model_path.stem)
            return ModelBundle(artifact=artifact, version=version)
        except Exception:
            return None

    def _build_creation_time_features(self, request: TriageRequest) -> dict[str, Any]:
        text = request.messageText.strip()
        subject = (request.subject or "").strip()
        combined = f"{subject} {text}".strip()
        normalized = _normalize(combined)
        return {
            "message_text": text,
            "subject": subject,
            "message_type": request.messageType.value,
            "guardian_priority": request.guardianPriority.value if request.guardianPriority else "UNKNOWN",
            "character_count": len(combined),
            "word_count": len(re.findall(r"\b\w+\b", normalized)),
            "has_medical_keyword": _has_any(normalized, MEDICAL_KEYWORDS),
            "has_urgent_keyword": _has_any(normalized, URGENT_KEYWORDS),
        }

    def _fallback_predict(self, request: TriageRequest) -> TriageResponse:
        features = self._build_creation_time_features(request)
        normalized = _normalize(f"{request.subject or ''} {request.messageText}")

        if _has_any(normalized, CRITICAL_KEYWORDS):
            urgency = UrgencyLevel.CRITICAL
            confidence = 0.62
            reason = "Fallback detected critical symptom or emergency wording."
        elif features["has_urgent_keyword"] or (
            request.guardianPriority is not None and request.guardianPriority.value == "HIGH"
        ):
            urgency = UrgencyLevel.HIGH
            confidence = 0.55
            reason = "Fallback detected urgent wording or guardian high priority."
        elif features["has_medical_keyword"] or request.messageType.value in {"MEDICAL", "LAB_RESULT"}:
            urgency = UrgencyLevel.MEDIUM
            confidence = 0.5
            reason = "Fallback detected medical context without critical wording."
        else:
            urgency = UrgencyLevel.LOW
            confidence = 0.45
            reason = "Fallback detected no urgent or critical signal."

        return TriageResponse(
            urgencyLevel=urgency,
            confidence=confidence,
            status=PredictionStatus.FALLBACK,
            explanation=reason,
            modelVersion=FALLBACK_MODEL_VERSION,
        )


def _normalize(value: str) -> str:
    without_accents = unicodedata.normalize("NFKD", value)
    ascii_text = without_accents.encode("ascii", "ignore").decode("ascii")
    return ascii_text.lower()


def _has_any(value: str, keywords: set[str]) -> bool:
    return any(keyword in value for keyword in keywords)


CRITICAL_KEYWORDS = {
    "convulsion",
    "seizure",
    "unconscious",
    "not breathing",
    "breathing difficulty",
    "difficulty breathing",
    "chest pain",
    "severe pain",
    "blood",
    "bleeding",
    "emergency",
    "urgent doctor",
}

URGENT_KEYWORDS = {
    "urgent",
    "high fever",
    "fever",
    "fievre",
    "vomiting",
    "vomit",
    "swollen",
    "edema",
    "pain",
    "douleur",
    "infection",
}

MEDICAL_KEYWORDS = {
    "dialysis",
    "creatinine",
    "urea",
    "kidney",
    "renal",
    "nephro",
    "medication",
    "dose",
    "ordonnance",
    "traitement",
    "urine",
    "blood pressure",
    "tension",
}
