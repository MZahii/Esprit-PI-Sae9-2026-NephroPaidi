from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import joblib
import pandas as pd
import numpy as np
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

app = FastAPI(title="AI Pediatric Dose Safety")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_PATH = os.getenv("MODEL_PATH", str(BASE_DIR / "model.joblib"))
SCALER_PATH = os.getenv("SCALER_PATH", str(BASE_DIR / "scaler.joblib"))
ENCODER_PATH = os.getenv("ENCODER_PATH", str(BASE_DIR / "label_encoder.joblib"))

try:
    model  = joblib.load(MODEL_PATH)
    scaler = joblib.load(SCALER_PATH)
    le     = joblib.load(ENCODER_PATH)
    print("Model, scaler and label encoder loaded.")
except Exception as e:
    model = scaler = le = None
    print(f"Warning: could not load artifacts: {e}")

# Same order the scaler was fit on in the notebook
FEATURES_TO_NORMALIZE = [
    'age_years', 'weight_kg', 'prescribed_dose_mg',
    'recommended_dose_mg', 'frequency_per_day',
    'dose_ratio', 'dose_mg_per_kg'
]
SELECTED_FEATURES = ['prescribed_dose_mg', 'dose_ratio', 'dose_mg_per_kg']

LABEL_MAP = {"normal": "SAFE", "overdose": "OVERDOSE", "underdose": "UNDERDOSE"}


class DoseVerificationRequest(BaseModel):
    age_years:           float
    weight_kg:           float
    prescribed_dose_mg:  float
    recommended_dose_mg: float
    frequency_per_day:   int


class DoseVerificationResponse(BaseModel):
    prediction: str   # SAFE | OVERDOSE | UNDERDOSE
    confidence: float


@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": model is not None}


@app.post("/predict/pediatric-dose", response_model=DoseVerificationResponse)
def verify_dose(req: DoseVerificationRequest):
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="Model not loaded — place model.joblib, scaler.joblib and label_encoder.joblib in the service directory."
        )

    if req.recommended_dose_mg == 0 or req.weight_kg == 0:
        raise HTTPException(status_code=422, detail="recommended_dose_mg and weight_kg must be non-zero.")

    dose_ratio     = req.prescribed_dose_mg / req.recommended_dose_mg
    dose_mg_per_kg = req.prescribed_dose_mg / req.weight_kg

    # Build full feature row in the same order the scaler was fit
    row = pd.DataFrame([{
        'age_years':           req.age_years,
        'weight_kg':           req.weight_kg,
        'prescribed_dose_mg':  req.prescribed_dose_mg,
        'recommended_dose_mg': req.recommended_dose_mg,
        'frequency_per_day':   req.frequency_per_day,
        'dose_ratio':          dose_ratio,
        'dose_mg_per_kg':      dose_mg_per_kg,
    }])

    scaled     = scaler.transform(row[FEATURES_TO_NORMALIZE])
    scaled_df  = pd.DataFrame(scaled, columns=FEATURES_TO_NORMALIZE)
    X          = scaled_df[SELECTED_FEATURES].values

    pred_enc   = model.predict(X)[0]
    label      = le.inverse_transform([pred_enc])[0]
    prediction = LABEL_MAP.get(str(label).lower(), str(label).upper())

    try:
        proba      = model.predict_proba(X)[0]
        confidence = float(np.max(proba))
    except AttributeError:
        confidence = 1.0

    return DoseVerificationResponse(prediction=prediction, confidence=round(confidence, 4))
