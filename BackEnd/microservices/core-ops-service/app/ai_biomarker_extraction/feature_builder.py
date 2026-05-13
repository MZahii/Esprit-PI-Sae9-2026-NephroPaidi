import math
from typing import Optional


def _safe_age_group(age_years: Optional[int]) -> int:
    if age_years is None or age_years < 0:
        return 0
    if age_years < 2:
        return 0
    if age_years < 6:
        return 1
    if age_years < 12:
        return 2
    return 3


def _sex_encoded(sex: Optional[str]) -> int:
    if not sex:
        return 0
    return 1 if sex.upper() == "M" else 0


def _scan_quality_encoded(content_type: Optional[str], parser_confidence: float) -> int:
    lowered = (content_type or "").lower()
    if "pdf" in lowered:
        return 2 if parser_confidence >= 0.8 else 1
    if lowered.startswith("image/"):
        return 1
    return 0


def build_features(*, age_years: Optional[int], sex: Optional[str], creatinine_mg_dl: Optional[float],
                   creatinine_umol_l: Optional[float], parser_confidence: float, has_creatinine: bool,
                   content_type: Optional[str], requires_manual_review: bool) -> dict[str, float]:
    age = float(age_years or 0)
    mg_dl = float(creatinine_mg_dl or 0.0)
    umol = float(creatinine_umol_l or 0.0)

    metadata_quality_score = 0.0
    if age_years is not None:
        metadata_quality_score += 35.0
    if sex:
        metadata_quality_score += 20.0
    if has_creatinine:
        metadata_quality_score += 30.0
    metadata_quality_score += round(parser_confidence * 15.0, 2)

    normalized_metadata = round(metadata_quality_score / 100.0, 4)
    data_quality = round((normalized_metadata + parser_confidence) / 2.0, 4)
    creatinine_abnormal = 1.0 if mg_dl >= 1.2 or mg_dl <= 0.2 else 0.0

    return {
        "patient_age": age,
        "y_value_creatinine": mg_dl,
        "y_value_creatinine_umol_l": umol,
        "y_class_hasdata": 1.0 if has_creatinine else 0.0,
        "parser_has_creatinine": 1.0 if has_creatinine else 0.0,
        "is_reviewed": 0.0,
        "metadata_quality_score": metadata_quality_score,
        "needs_manual_review": 1.0 if requires_manual_review else 0.0,
        "fe_document_age_days": 0.0,
        "fe_creatinine_log": round(math.log1p(max(mg_dl, 0.0)), 6),
        "fe_age_normalized": round(min(max(age / 18.0, 0.0), 1.0), 6),
        "fe_age_group": float(_safe_age_group(age_years)),
        "fe_scan_quality_encoded": float(_scan_quality_encoded(content_type, parser_confidence)),
        "fe_hospital_encoded": 0.0,
        "fe_department_encoded": 0.0,
        "fe_sex_encoded": float(_sex_encoded(sex)),
        "fe_creatinine_abnormal": creatinine_abnormal,
        "fe_data_quality_score": data_quality,
        "fe_high_risk_dept": 0.0,
        "fe_manual_review_flag": 1.0 if requires_manual_review else 0.0,
        "fe_metadata_quality_score": normalized_metadata,
    }
