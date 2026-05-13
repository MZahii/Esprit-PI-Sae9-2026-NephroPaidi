import base64
import io
import re
from dataclasses import dataclass
from typing import List, Optional

import fitz
import pytesseract
from PIL import Image, ImageOps
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


def ocr_image(image: Image.Image) -> str:
    normalized = ImageOps.exif_transpose(image)
    if normalized.mode not in ("RGB", "L"):
        normalized = normalized.convert("RGB")
    grayscale = ImageOps.grayscale(normalized)
    prepared = ImageOps.autocontrast(grayscale)

    text = pytesseract.image_to_string(prepared)
    if text.strip():
        return text
    return pytesseract.image_to_string(normalized)


def extract_text_from_image_ocr(content: bytes) -> tuple[str, Optional[str]]:
    try:
        with Image.open(io.BytesIO(content)) as image:
            return ocr_image(image), None
    except pytesseract.TesseractNotFoundError:
        return "", "Tesseract OCR binary is unavailable in this environment."
    except Exception:
        return "", "Image OCR failed."


def extract_text_from_pdf_ocr(content: bytes, max_pages: int = 3) -> tuple[str, Optional[str]]:
    try:
        document = fitz.open(stream=content, filetype="pdf")
    except Exception:
        return "", "Scanned PDF OCR preparation failed."

    pages: List[str] = []
    try:
        for page_index, page in enumerate(document):
            if page_index >= max_pages:
                break
            pixmap = page.get_pixmap(matrix=fitz.Matrix(2, 2), alpha=False)
            with Image.open(io.BytesIO(pixmap.tobytes("png"))) as image:
                page_text = ocr_image(image)
            if page_text.strip():
                pages.append(page_text)
    except pytesseract.TesseractNotFoundError:
        return "", "Tesseract OCR binary is unavailable in this environment."
    except Exception:
        return "", "Scanned PDF OCR failed."
    finally:
        document.close()

    return "\n".join(pages), None


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

        ocr_text, ocr_warning = extract_text_from_pdf_ocr(content)
        if ocr_text.strip():
            warnings.append("Scanned PDF text was recovered with OCR.")
            return ocr_text, warnings
        if ocr_warning:
            warnings.append(ocr_warning)

    if lowered_type.startswith("image/") or lowered_name.endswith((".png", ".jpg", ".jpeg", ".bmp")):
        ocr_text, ocr_warning = extract_text_from_image_ocr(content)
        if ocr_text.strip():
            warnings.append("Image text was recovered with OCR.")
            return ocr_text, warnings
        if ocr_warning:
            warnings.append(ocr_warning)
        warnings.append("Image OCR returned no readable text. Manual review recommended.")

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


def normalize_ocr_text(text: str) -> str:
    normalized = text.replace("\r", "\n")
    normalized = re.sub(r"\bmg\s*[/|iIl1]?\s*d[l1i]\b", "mg/dl", normalized, flags=re.IGNORECASE)
    normalized = re.sub(r"\bmg\s*[/|iIl1]?\s*l\b", "mg/l", normalized, flags=re.IGNORECASE)
    normalized = re.sub(r"\bu\s*m[o0]l\s*[/|iIl1]?\s*l\b", "umol/l", normalized, flags=re.IGNORECASE)
    normalized = re.sub(r"\bmm[o0]l\s*[/|iIl1]?\s*l\b", "mmol/l", normalized, flags=re.IGNORECASE)
    return normalized


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

    normalized_text = normalize_ocr_text(text)

    for pattern in CREATININE_PATTERNS:
        match = pattern.search(normalized_text)
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

        excerpt = normalized_text[max(match.start() - 40, 0): min(match.end() + 40, len(normalized_text))]
        confidence = 0.9 if "creatin" in excerpt.lower() else 0.75
        if unit in {"mg/L", "mmol/L"}:
            warnings.append(f"Creatinine unit '{unit}' was converted before inference.")
            confidence -= 0.1

        return ParsedCreatinine(mg_dl, umol_l, unit, max(confidence, 0.4), warnings, excerpt)

    return ParsedCreatinine(None, None, None, 0.2, ["Creatinine value could not be extracted automatically."], normalized_text[:120])
