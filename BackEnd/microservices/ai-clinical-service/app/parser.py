import base64
import io
import re
from dataclasses import dataclass
from typing import List, Optional

from pypdf import PdfReader


CREATININE_PATTERNS = [
    re.compile(r"creatinin(?:e)?[^0-9]{0,20}(\d+(?:[.,]\d+)?)\s*(mg/dl|mg/l|umol/l|µmol/l|mmol/l)", re.IGNORECASE),
    re.compile(r"\b(scr|serum creatinine)\b[^0-9]{0,20}(\d+(?:[.,]\d+)?)\s*(mg/dl|mg/l|umol/l|µmol/l|mmol/l)", re.IGNORECASE),
]


@dataclass
class ParsedCreatinine:
    mg_dl: Optional[float]
    umol_l: Optional[float]
    unit: Optional[str]
    parser_confidence: float
    warnings: List[str]
    text_excerpt: str


def decode_file_content(file_content_b64: str) -> bytes:
    return base64.b64decode(file_content_b64)


def extract_text(content: bytes, file_name: str, content_type: Optional[str]) -> tuple[str, List[str]]:
    warnings: List[str] = []
    lowered_name = (file_name or "").lower()
    lowered_type = (content_type or "").lower()

    if lowered_type == "application/pdf" or lowered_name.endswith(".pdf"):
        try:
            reader = PdfReader(io.BytesIO(content))
            text = "\n".join((page.extract_text() or "") for page in reader.pages)
            if text.strip():
                return text, warnings
            warnings.append("PDF text extraction returned no readable text.")
        except Exception:
            warnings.append("PDF text extraction failed.")

    if lowered_type.startswith("image/") or lowered_name.endswith((".png", ".jpg", ".jpeg", ".bmp")):
        warnings.append("Image OCR is not enabled in this build. Manual review recommended.")

    for encoding in ("utf-8", "latin-1"):
        try:
            text = content.decode(encoding)
            if text.strip():
                return text, warnings
        except UnicodeDecodeError:
            continue

    warnings.append("Unable to decode document text reliably.")
    return "", warnings


def normalize_unit(raw_unit: str) -> str:
    unit = raw_unit.strip().lower().replace("µ", "u")
    mapping = {
        "mg/dl": "mg/dL",
        "mg/l": "mg/L",
        "umol/l": "umol/L",
        "mmol/l": "mmol/L",
    }
    return mapping.get(unit, raw_unit)


def to_mg_dl(value: float, unit: str) -> float:
    normalized = normalize_unit(unit)
    if normalized == "mg/dL":
        return value
    if normalized == "mg/L":
        return value / 10.0
    if normalized == "umol/L":
        return value / 88.4
    if normalized == "mmol/L":
        return (value * 1000.0) / 88.4
    raise ValueError(f"Unsupported creatinine unit: {unit}")


def to_umol_l(value: float, unit: str) -> float:
    normalized = normalize_unit(unit)
    if normalized == "umol/L":
        return value
    if normalized == "mg/dL":
        return value * 88.4
    if normalized == "mg/L":
        return (value / 10.0) * 88.4
    if normalized == "mmol/L":
        return value * 1000.0
    raise ValueError(f"Unsupported creatinine unit: {unit}")


def parse_creatinine(text: str) -> ParsedCreatinine:
    warnings: List[str] = []
    if not text.strip():
        return ParsedCreatinine(None, None, None, 0.1, ["No readable text available for parsing."], "")

    for pattern in CREATININE_PATTERNS:
        match = pattern.search(text)
        if not match:
            continue

        if len(match.groups()) == 3:
            _, raw_value, raw_unit = match.groups()
        else:
            raw_value, raw_unit = match.groups()

        value = float(raw_value.replace(",", "."))
        unit = normalize_unit(raw_unit)
        try:
            mg_dl = round(to_mg_dl(value, unit), 4)
            umol_l = round(to_umol_l(value, unit), 4)
        except ValueError:
            warnings.append(f"Unsupported extracted unit '{raw_unit}'.")
            continue

        excerpt = text[max(match.start() - 40, 0): min(match.end() + 40, len(text))]
        confidence = 0.9 if "creatin" in excerpt.lower() else 0.75
        if unit in {"mg/L", "mmol/L"}:
            warnings.append(f"Creatinine unit '{unit}' was converted before inference.")
            confidence -= 0.1

        return ParsedCreatinine(mg_dl, umol_l, unit, max(confidence, 0.4), warnings, excerpt)

    return ParsedCreatinine(None, None, None, 0.2, ["Creatinine value could not be extracted automatically."], text[:120])
