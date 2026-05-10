import re
import unicodedata
from typing import Any

import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin


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


class CreationTimeFeatureBuilder(BaseEstimator, TransformerMixin):
    """Converts request-like dictionaries into stable creation-time model features."""

    def fit(self, X: Any, y: Any = None) -> "CreationTimeFeatureBuilder":
        return self

    def transform(self, X: Any) -> pd.DataFrame:
        rows = []
        for item in _as_records(X):
            message_text = str(item.get("message_text") or item.get("messageText") or "").strip()
            subject = str(item.get("subject") or "").strip()
            message_type = str(item.get("message_type") or item.get("messageType") or "OTHER").strip().upper()
            guardian_priority = str(
                item.get("guardian_priority") or item.get("guardianPriority") or "UNKNOWN"
            ).strip().upper()

            combined_text = f"{subject} {message_text}".strip()
            normalized = normalize_text(combined_text)
            rows.append(
                {
                    "combined_text": combined_text,
                    "message_type": message_type or "OTHER",
                    "guardian_priority": guardian_priority or "UNKNOWN",
                    "character_count": len(combined_text),
                    "word_count": len(re.findall(r"\b\w+\b", normalized)),
                    "has_medical_keyword": int(has_any(normalized, MEDICAL_KEYWORDS)),
                    "has_urgent_keyword": int(has_any(normalized, URGENT_KEYWORDS)),
                    "has_critical_keyword": int(has_any(normalized, CRITICAL_KEYWORDS)),
                    "question_mark_count": combined_text.count("?"),
                    "exclamation_count": combined_text.count("!"),
                }
            )
        return pd.DataFrame(rows)


def normalize_text(value: str) -> str:
    without_accents = unicodedata.normalize("NFKD", value)
    ascii_text = without_accents.encode("ascii", "ignore").decode("ascii")
    return ascii_text.lower()


def has_any(value: str, keywords: set[str]) -> bool:
    return any(keyword in value for keyword in keywords)


def _as_records(X: Any) -> list[dict[str, Any]]:
    if isinstance(X, pd.DataFrame):
        return X.to_dict(orient="records")
    if isinstance(X, dict):
        return [X]
    return list(X)
