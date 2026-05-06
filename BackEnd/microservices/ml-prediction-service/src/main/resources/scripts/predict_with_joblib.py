import json
import sys
from pathlib import Path

import joblib
import pandas as pd


def load_request(path_str):
    with open(path_str, "r", encoding="utf-8") as handle:
        return json.load(handle)


def to_bool(value, default=False):
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    text = str(value).strip().lower()
    if text in {"true", "1", "yes", "y", "ok"}:
        return True
    if text in {"false", "0", "no", "n"}:
        return False
    return default


def to_int(value, default):
    try:
        return int(float(value))
    except (TypeError, ValueError):
        return default


def to_float(value, default):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def pick(*values, default=""):
    for value in values:
        if value is None:
            continue
        text = str(value).strip()
        if text:
            return text
    return default


def normalize_gender(value):
    text = pick(value, default="MALE").upper()
    if text.startswith("F"):
        return "FEMALE"
    return "MALE"


def normalize_urgency(value):
    text = pick(value, default="SCHEDULED").upper()
    return "URGENT" if text == "URGENT" else "SCHEDULED"


def normalize_asa(value, default="II"):
    text = pick(value, default=default).upper().replace("ASA", "").strip()
    allowed = {"I", "II", "III", "IV"}
    return text if text in allowed else default


def normalize_preop_procedure(value):
    text = pick(value, default="TRANSPLANT").upper()
    if "TRANS" in text:
        return "TRANSPLANT"
    if "CATH" in text:
        return "CATHETER"
    if "BIOP" in text:
        return "BIOPSY"
    if "FIST" in text:
        return "FISTULA"
    if "NEPH" in text:
        return "NEPHRECTOMY"
    if "PYELO" in text:
        return "PYELOPLASTY"
    return "TRANSPLANT"


def normalize_postop_procedure(value):
    text = pick(value, default="TRANSPLANT").upper()
    if "CATH" in text:
        return "CATHETER"
    if "BIOP" in text:
        return "BIOPSY"
    return "TRANSPLANT"


def normalize_allergy_group(value):
    text = pick(value, default="NONE").upper()
    allowed = {"IODINE", "LATEX", "MULTIPLE", "NONE", "PENICILLIN"}
    return text if text in allowed else "NONE"


def normalize_chronic_group(value):
    text = pick(value, default="CKD").upper().replace(" ", "_")
    allowed = {
        "CKD",
        "CKD|ANEMIA",
        "CKD|HYPERTENSION",
        "CONGENITAL_UROPATHY",
        "HYPERTENSION",
        "MULTIPLE",
        "NONE",
    }
    return text if text in allowed else "CKD"


def normalize_wound_status(value):
    text = pick(value, default="CLEAN").upper()
    allowed = {"CLEAN", "DISCHARGE", "RED", "SWELLING"}
    return text if text in allowed else "CLEAN"


def extract_features(request):
    return request.get("features") or {}


def build_preop_row(request):
    features = extract_features(request)
    hemodynamics_ok = to_bool(features.get("hemodynamicsOk"), True)
    infection_ok = to_bool(features.get("infectionScreenOk"), True)
    anesthesia_ok = to_bool(features.get("anesthesiaClearanceOk"), True)
    consent_signed = to_bool(features.get("consentSigned"), True)
    previous_surgery = to_bool(features.get("previousSurgeryHistory"), False)

    urgency = normalize_urgency(request.get("urgencyLevel"))
    default_temp = 38.1 if not infection_ok else 36.9
    default_hr = 122 if not hemodynamics_ok else 88
    default_sys = 92 if not hemodynamics_ok else 114
    default_dia = 58 if not hemodynamics_ok else 72
    default_o2 = 91 if not hemodynamics_ok else 98
    default_hb = 9.4 if not anesthesia_ok else 11.7
    default_creatinine = 2.4 if urgency == "URGENT" else 1.4
    default_wbc = 14900 if not infection_ok else 8300
    default_platelets = 132000 if not anesthesia_ok else 245000

    return {
        "procedure_type": normalize_preop_procedure(features.get("procedureType") or pick(request.get("surgeryType"), request.get("procedureName"))),
        "asa_class": normalize_asa(features.get("asaClass"), default="III" if urgency == "URGENT" else "II"),
        "urgency_level": urgency,
        "age": to_int(request.get("age"), 12),
        "sex": normalize_gender(request.get("gender")),
        "preop_temperature": to_float(features.get("temperature"), default_temp),
        "preop_heart_rate": to_float(features.get("heartRate"), default_hr),
        "preop_systolic_bp": to_float(features.get("systolicBp"), default_sys),
        "preop_diastolic_bp": to_float(features.get("diastolicBp"), default_dia),
        "oxygen_saturation": to_float(features.get("oxygenSaturation"), default_o2),
        "hemoglobin": to_float(features.get("hemoglobin"), default_hb),
        "creatinine": to_float(features.get("creatinine"), default_creatinine),
        "wbc": to_float(features.get("wbc"), default_wbc),
        "platelets": to_float(features.get("platelets"), default_platelets),
        "hemodynamics_ok": int(hemodynamics_ok),
        "infection_screen_ok": int(infection_ok),
        "anesthesia_clearance_ok": int(anesthesia_ok),
        "consent_signed": int(consent_signed),
        "previous_surgery_history": int(previous_surgery),
        "allergy_group": normalize_allergy_group(features.get("allergyGroup") or request.get("allergies")),
        "chronic_condition_group": normalize_chronic_group(features.get("chronicConditionGroup")),
    }


def build_postop_row(request):
    features = extract_features(request)
    stable = to_bool(features.get("hemodynamicsStable"), True)
    bleeding = to_bool(features.get("bleedingControlled"), True)
    pain = to_bool(features.get("painControlled"), True)
    consciousness = to_bool(features.get("consciousnessNormal"), True)

    default_temp = 38.3 if not stable else 37.0
    default_hr = 121 if not stable else 86
    default_sys = 94 if not stable else 118
    default_dia = 57 if not stable else 74
    default_pain = 8 if not pain else 3
    default_hours = 12 if not consciousness else 6
    wound = features.get("woundStatus") or ("DISCHARGE" if not bleeding else "CLEAN")

    return {
        "procedure_type": normalize_postop_procedure(features.get("procedureType") or pick(request.get("surgeryType"), request.get("procedureName"))),
        "asa_class": normalize_asa(features.get("asaClass"), default="III" if request.get("complicationCount", 0) else "II"),
        "temperature": to_float(features.get("temperature"), default_temp),
        "heart_rate": to_float(features.get("heartRate"), default_hr),
        "systolic_bp": to_float(features.get("systolicBp"), default_sys),
        "diastolic_bp": to_float(features.get("diastolicBp"), default_dia),
        "wound_status": normalize_wound_status(wound),
        "pain_level": to_float(features.get("painLevel"), default_pain),
        "hours_after_surgery": to_float(features.get("hoursAfterSurgery"), default_hours),
    }


def probability_from_model(model, frame):
    if hasattr(model, "predict_proba"):
        proba = model.predict_proba(frame)[0]
        return float(proba[1] if len(proba) > 1 else proba[0])
    prediction = model.predict(frame)[0]
    try:
        return float(prediction)
    except (TypeError, ValueError):
        return 1.0 if prediction else 0.0


def build_explanations(phase, row):
    explanations = {}
    if phase == "PRE_OP":
        if row["urgency_level"] == "URGENT":
            explanations["urgency"] = 0.18
        if row["hemodynamics_ok"] == 0:
            explanations["hemodynamics"] = 0.22
        if row["infection_screen_ok"] == 0:
            explanations["infection_screen"] = 0.18
        if row["anesthesia_clearance_ok"] == 0:
            explanations["anesthesia_clearance"] = 0.16
        if row["creatinine"] >= 2.0:
            explanations["creatinine"] = 0.12
    else:
        if row["temperature"] >= 38.0:
            explanations["temperature"] = 0.16
        if row["heart_rate"] >= 115:
            explanations["heart_rate"] = 0.14
        if row["pain_level"] >= 7:
            explanations["pain_level"] = 0.12
        if row["wound_status"] != "CLEAN":
            explanations["wound_status"] = 0.18
    return explanations


def classify(phase, probability):
    probability = max(0.01, min(0.99, probability))
    if probability >= 0.75:
        return {
            "predictionLabel": "HIGH_RISK" if phase == "PRE_OP" else "COMPLICATION_LIKELY",
            "riskLevel": "HIGH",
            "recommendation": "Immediate specialist review and reinforced monitoring recommended.",
        }
    if probability >= 0.45:
        return {
            "predictionLabel": "MEDIUM_RISK" if phase == "PRE_OP" else "COMPLICATION_RISK",
            "riskLevel": "MEDIUM",
            "recommendation": "Clinical review recommended before progressing to the next workflow step.",
        }
    return {
        "predictionLabel": "LOW_RISK" if phase == "PRE_OP" else "STABLE_SIGNAL",
        "riskLevel": "LOW",
        "recommendation": "No major risk signal detected by the trained model.",
    }


def main():
    if len(sys.argv) != 5:
        raise SystemExit("Usage: predict_with_joblib.py <phase> <request_json> <preop_model> <postop_model>")

    phase = sys.argv[1].strip().upper()
    request = load_request(sys.argv[2])
    preop_model_path = Path(sys.argv[3])
    postop_model_path = Path(sys.argv[4])

    if phase == "PRE_OP":
        model = joblib.load(preop_model_path)
        row = build_preop_row(request)
        model_version = preop_model_path.name
    elif phase == "POST_OP":
        model = joblib.load(postop_model_path)
        row = build_postop_row(request)
        model_version = postop_model_path.name
    else:
        raise SystemExit(f"Unsupported phase: {phase}")

    frame = pd.DataFrame([row])
    probability = probability_from_model(model, frame)
    explanations = build_explanations(phase, row)
    label_info = classify(phase, probability)

    response = {
        "phase": phase,
        "modelName": "joblib-surgical-risk-pipeline",
        "modelVersion": model_version,
        "predictionLabel": label_info["predictionLabel"],
        "riskLevel": label_info["riskLevel"],
        "probability": float(max(0.01, min(0.99, probability))),
        "recommendation": label_info["recommendation"],
        "explanations": explanations,
    }
    print(json.dumps(response))


if __name__ == "__main__":
    main()
