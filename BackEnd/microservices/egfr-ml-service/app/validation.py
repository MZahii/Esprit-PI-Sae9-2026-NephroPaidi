from __future__ import annotations

from typing import Any, Dict


class ValidationError(ValueError):
    pass


REQUIRED_FIELDS = (
    "Age_Years",
    "Height_cm",
    "Creatinine_Value",
    "Systolic_BP",
    "Diastolic_BP",
    "Measure_Year",
    "Measure_Month",
)

NUMERIC_RANGES: Dict[str, tuple[float, float, str]] = {
    "Age_Years": (0.0, 21.0, "years"),
    "Age_Months": (0.0, 252.0, "months"),
    "Height_cm": (30.0, 230.0, "cm"),
    "Weight_kg": (1.0, 250.0, "kg"),
    "Creatinine_Value": (0.1, 20.0, "mg/dL"),
    "Systolic_BP": (40.0, 260.0, "mmHg"),
    "Diastolic_BP": (20.0, 160.0, "mmHg"),
    "eGFR_Calculated": (1.0, 250.0, "mL/min/1.73m2"),
    "Measure_Year": (2000.0, 2100.0, "year"),
    "Measure_Month": (1.0, 12.0, "month"),
    "Measure_DayOfYear": (1.0, 366.0, "day_of_year"),
}


def _to_float(value: Any, field: str) -> float:
    try:
        return float(value)
    except (TypeError, ValueError) as exc:
        raise ValidationError(f"{field} must be numeric.") from exc


def validate_prediction_payload(payload: Dict[str, Any]) -> Dict[str, Any]:
    if not isinstance(payload, dict):
        raise ValidationError("Payload must be a JSON object.")

    missing = [field for field in REQUIRED_FIELDS if payload.get(field) in (None, "")]
    if missing:
        raise ValidationError(f"Missing required fields: {', '.join(missing)}")

    numeric_issues: list[str] = []
    for field, (min_value, max_value, unit) in NUMERIC_RANGES.items():
        raw = payload.get(field)
        if raw in (None, ""):
            continue
        value = _to_float(raw, field)
        if value < min_value or value > max_value:
            numeric_issues.append(f"{field} out of range [{min_value}, {max_value}] {unit}.")

    if numeric_issues:
        raise ValidationError(" ".join(numeric_issues))

    systolic = _to_float(payload["Systolic_BP"], "Systolic_BP")
    diastolic = _to_float(payload["Diastolic_BP"], "Diastolic_BP")
    if systolic <= diastolic:
        raise ValidationError("Systolic_BP must be higher than Diastolic_BP.")

    return payload

