from fastapi import FastAPI

from .decision_engine import recommend_case
from .feature_builder import build_features
from .model_runtime import predict_from_features
from .parser import decode_file_content, extract_text, parse_creatinine
from .schemas import AiLabAnalysisRequest, AiLabAnalysisResponse

app = FastAPI(title="NephroPaidi AI Clinical Service", version="1.0.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/infer-lab-result", response_model=AiLabAnalysisResponse)
def infer_lab_result(request: AiLabAnalysisRequest) -> AiLabAnalysisResponse:
    raw_bytes = decode_file_content(request.fileContentBase64)
    extracted_text, text_warnings = extract_text(raw_bytes, request.fileName, request.contentType)
    parsed = parse_creatinine(extracted_text)

    warnings = text_warnings + parsed.warnings
    fallback_recommendation, fallback_confidence, requires_review, fallback_summary = recommend_case(
        request.patientAgeYears,
        request.heightCm,
        parsed.mg_dl,
        parsed.parser_confidence,
        warnings,
    )

    features = build_features(
        age_years=request.patientAgeYears,
        sex=request.patientSex,
        creatinine_mg_dl=parsed.mg_dl,
        creatinine_umol_l=parsed.umol_l,
        parser_confidence=parsed.parser_confidence,
        has_creatinine=parsed.mg_dl is not None,
        content_type=request.contentType,
        requires_manual_review=requires_review,
    )

    recommendation = fallback_recommendation
    confidence = fallback_confidence
    summary = fallback_summary
    model_used = "pediatric_rules_v1"

    try:
        recommendation, model_confidence = predict_from_features(features)
        confidence = round((model_confidence * 0.8) + (fallback_confidence * 0.2), 4)
        summary = (
            f"Model-backed recommendation generated using exported MLA artifact. "
            f"Fallback clinical interpretation: {fallback_summary}"
        )
        model_used = "mla_random_forest_v1"
    except Exception as exc:
        warnings.append(f"Model inference fallback activated: {exc}")

    if warnings:
        summary = f"{summary} Warnings: {'; '.join(warnings)}"

    return AiLabAnalysisResponse(
        status="COMPLETED",
        recommendation=recommendation,
        confidence=confidence,
        summary=summary,
        requiresDoctorReview=requires_review,
        extractedCreatinineMgDl=parsed.mg_dl,
        extractedCreatinineUmolL=parsed.umol_l,
        extractedUnit=parsed.unit,
        parserConfidence=parsed.parser_confidence,
        parserWarnings=warnings,
        modelUsed=model_used,
    )
