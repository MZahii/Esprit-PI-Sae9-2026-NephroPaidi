from typing import Dict, List, Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class AiLabAnalysisRequest(BaseModel):
    consultationId: Optional[UUID] = None
    patientId: Optional[int] = None
    patientAgeYears: Optional[int] = None
    patientSex: Optional[str] = None
    heightCm: Optional[float] = None
    weightKg: Optional[float] = None
    documentType: Optional[str] = None
    fileName: str
    contentType: Optional[str] = None
    fileContentBase64: str


class AiLabAnalysisResponse(BaseModel):
    status: str
    recommendation: str
    confidence: float
    summary: str
    requiresDoctorReview: bool
    extractedCreatinineMgDl: Optional[float] = None
    extractedCreatinineUmolL: Optional[float] = None
    extractedUnit: Optional[str] = None
    parserConfidence: float
    parserWarnings: List[str]
    modelUsed: str


class AIPredictionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    ageYears: Optional[int] = None
    sex: Optional[str] = None
    creatinine_mg_dl: Optional[float] = None
    creatinine_umol_l: Optional[float] = None
    parserConfidence: float = 0.0
    has_creatinine: bool = False
    content_type: Optional[str] = None
    requires_manual_review: bool = False


class AIPredictionResponse(BaseModel):
    prediction: str
    confidence: float
    class_probabilities: Dict[str, float]
