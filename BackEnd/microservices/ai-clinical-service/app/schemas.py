from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel


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
