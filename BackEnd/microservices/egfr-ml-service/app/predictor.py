from __future__ import annotations

import json
from typing import Any, Dict, List

import joblib

from app.config import (
    CLS_MODEL_PATH,
    FEATURES_PATH,
    FEATURE_SCHEMA_PATH,
    REG_MODEL_12M_PATH,
    REG_MODEL_3M_PATH,
    REG_MODEL_6M_PATH,
    REG_MODEL_PATH,
)
from app.preprocess import align_payload_to_features

reg_models: Dict[str, Any] = {}
cls_model = None
feature_columns: List[str] = []
feature_schema: Dict[str, List[str]] = {}


def load_models() -> None:
    global reg_models, cls_model, feature_columns, feature_schema
    reg_models = {
        "3m": joblib.load(REG_MODEL_3M_PATH),
        "6m": joblib.load(REG_MODEL_6M_PATH),
        "12m": joblib.load(REG_MODEL_12M_PATH if REG_MODEL_12M_PATH.exists() else REG_MODEL_PATH),
    }
    cls_model = joblib.load(CLS_MODEL_PATH)
    feature_columns = json.loads(FEATURES_PATH.read_text(encoding="utf-8"))
    feature_schema = json.loads(FEATURE_SCHEMA_PATH.read_text(encoding="utf-8"))


def predict_regression(payload: Dict[str, Any]) -> Dict[str, float]:
    x = align_payload_to_features(payload, feature_columns, feature_schema)
    return {
        "predicted_egfr_3_months": float(reg_models["3m"].predict(x)[0]),
        "predicted_egfr_6_months": float(reg_models["6m"].predict(x)[0]),
        "predicted_egfr_12_months": float(reg_models["12m"].predict(x)[0]),
    }


def predict_classification(payload: Dict[str, Any], threshold: float = 0.5) -> Dict[str, Any]:
    x = align_payload_to_features(payload, feature_columns, feature_schema)
    prob = float(cls_model.predict_proba(x)[0, 1])
    pred = int(prob >= threshold)
    confidence = max(0.0, min(1.0, abs(prob - threshold) * 2.0))
    return {
        "rapid_decline_flag": pred,
        "rapid_decline_probability": prob,
        "confidence_score": confidence,
        "risk_label": "HIGH" if pred == 1 else "LOW",
        "threshold": threshold,
    }
