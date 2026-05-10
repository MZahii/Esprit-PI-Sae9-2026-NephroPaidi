from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class MessageType(str, Enum):
    ADMINISTRATIVE = "ADMINISTRATIVE"
    MEDICAL = "MEDICAL"
    LAB_RESULT = "LAB_RESULT"
    APPOINTMENT = "APPOINTMENT"
    QUESTION = "QUESTION"
    COMPLAINT = "COMPLAINT"
    OTHER = "OTHER"


class GuardianPriority(str, Enum):
    NORMAL = "NORMAL"
    HIGH = "HIGH"


class UrgencyLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class PredictionStatus(str, Enum):
    SUCCESS = "SUCCESS"
    FALLBACK = "FALLBACK"
    FAILED = "FAILED"


class TriageRequest(BaseModel):
    messageText: str = Field(..., min_length=1, max_length=2000)
    subject: Optional[str] = Field(default=None, max_length=120)
    messageType: MessageType
    guardianPriority: Optional[GuardianPriority] = None


class TriageResponse(BaseModel):
    urgencyLevel: UrgencyLevel
    confidence: float = Field(..., ge=0.0, le=1.0)
    status: PredictionStatus
    explanation: str
    modelVersion: str
