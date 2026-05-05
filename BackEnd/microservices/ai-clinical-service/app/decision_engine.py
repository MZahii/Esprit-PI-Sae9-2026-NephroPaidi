from typing import List, Optional


def pediatric_egfr(height_cm: Optional[float], creatinine_mg_dl: Optional[float], age_years: Optional[int]) -> Optional[float]:
    if age_years is None or age_years >= 18:
        return None
    if height_cm is None or height_cm <= 0 or creatinine_mg_dl is None or creatinine_mg_dl <= 0:
        return None
    return round((0.413 * height_cm) / creatinine_mg_dl, 1)


def recommend_case(age_years: Optional[int], height_cm: Optional[float], creatinine_mg_dl: Optional[float],
                   parser_confidence: float, warnings: List[str]) -> tuple[str, float, bool, str]:
    if creatinine_mg_dl is None:
        return (
            "review",
            0.32,
            True,
            "Creatinine could not be extracted automatically. Manual review is required."
        )

    egfr = pediatric_egfr(height_cm, creatinine_mg_dl, age_years)

    recommendation = "warning"
    confidence = 0.62
    requires_review = parser_confidence < 0.65 or bool(warnings)

    if egfr is not None:
        if egfr < 30:
            recommendation = "urgent"
            confidence = 0.84
        elif egfr < 60:
            recommendation = "warning"
            confidence = 0.76
        elif egfr < 90:
            recommendation = "review"
            confidence = 0.68
        else:
            recommendation = "other"
            confidence = 0.58

        summary = f"Pediatric eGFR estimated at {egfr} mL/min/1.73m² using bedside Schwartz."
    else:
        if creatinine_mg_dl >= 2.0:
            recommendation = "urgent"
            confidence = 0.74
        elif creatinine_mg_dl >= 1.2:
            recommendation = "warning"
            confidence = 0.68
        else:
            recommendation = "review"
            confidence = 0.55

        summary = "Creatinine extracted successfully, but full pediatric eGFR could not be computed from the provided metadata."

    confidence *= max(0.55, parser_confidence)
    confidence = round(min(confidence, 0.95), 4)
    if parser_confidence < 0.5:
        requires_review = True
        summary += " Parsing confidence is low."

    return recommendation, confidence, requires_review, summary
