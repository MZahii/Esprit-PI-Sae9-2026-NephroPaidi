import json
from functools import lru_cache
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd


MODEL_DIR = Path(__file__).resolve().parent.parent / "model"


@lru_cache(maxsize=1)
def load_runtime_assets() -> dict[str, Any]:
    model = joblib.load(MODEL_DIR / "nephrospaidi_final_model_v1.joblib")
    label_encoder = joblib.load(MODEL_DIR / "nephrospaidi_label_encoder_v1.joblib")
    with (MODEL_DIR / "nephrospaidi_runtime_config_v1.json").open("r", encoding="utf-8") as handle:
        config = json.load(handle)
    return {
        "model": model,
        "label_encoder": label_encoder,
        "config": config,
    }


def build_feature_frame(feature_values: dict[str, Any]) -> pd.DataFrame:
    config = load_runtime_assets()["config"]
    feature_columns = config["feature_columns"]
    row = {name: feature_values.get(name, 0.0) for name in feature_columns}
    return pd.DataFrame([row], columns=feature_columns)


def runtime_model_used() -> str:
    config = load_runtime_assets()["config"]
    model_name = str(config.get("model_name", "unknown_model"))
    model_version = str(config.get("model_version", "v0"))
    return f"{model_name}_{model_version}"


def predict_from_features(feature_values: dict[str, Any]) -> tuple[str, float, dict[str, float]]:
    assets = load_runtime_assets()
    model = assets["model"]
    label_encoder = assets["label_encoder"]
    frame = build_feature_frame(feature_values)
    prediction = model.predict(frame)[0]

    class_probabilities: dict[str, float] = {}
    if hasattr(model, "predict_proba"):
        proba = model.predict_proba(frame)[0]
        confidence = float(np.max(proba))
        for class_index, score in enumerate(proba):
            label_name = str(label_encoder.inverse_transform([class_index])[0])
            class_probabilities[label_name] = round(float(score), 4)
    else:
        confidence = 0.55

    label = str(label_encoder.inverse_transform([int(prediction)])[0])
    return label, round(confidence, 4), class_probabilities
