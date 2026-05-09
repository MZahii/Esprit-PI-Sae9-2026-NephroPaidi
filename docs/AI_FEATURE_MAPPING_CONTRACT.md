# AI-Clinical Service Feature Mapping Contract

## Overview
This document defines the contract between Java Clinical-Service DTOs and the AI model expectations (nephrospaidi_final_model_v1).

## AI Model Specification
- **Model Type:** Random Forest
- **Training Dataset:** Mohamed_DataSet_FEATURES_V4.csv
- **Input Features:** 21 (raw + engineered)
- **Output Classes:** other, review, urgent, warning
- **Key Metric:** Urgent Recall = 71.15% (designed for high sensitivity on urgent cases)

---

## Feature Mapping: Java DTOs → AI Model Input

### Raw Input Features (Source: ConsultationRecordDTO)

| AI Feature Name | Java Field | Type | Source | Notes |
|---|---|---|---|---|
| `patient_age` | `ageYears` | int | Query param or UserDTO | Required; age < 2 → group 0 |
| `y_value_creatinine` | `serumCreatinine_mgdL` | BigDecimal | nephologyRecord | Required for model; unit: mg/dL |
| `y_value_creatinine_umol_l` | `serumCreatinine_umolL` | BigDecimal | NEW field to add | Required; unit: μmol/L |
| `y_class_hasdata` | `hasCreatinineData` | boolean | Derived from serumCreatinine | 1 if value present, 0 if null |
| `parser_has_creatinine` | `hasCreatinineData` | boolean | Same as y_class_hasdata | Redundant but AI expects both |
| `is_reviewed` | `isReviewed` | boolean | NEW field to add | Clinical staff review flag |
| `metadata_quality_score` | `documentQualityScore` | float (0-100) | Calculated by feature_builder | Composite: age info (35) + sex (20) + creatinine (30) + parser_confidence (15) |
| `needs_manual_review` | `requiresManualReview` | boolean | NEW field to add | Flag if document parsing uncertain |

### Document/Source Metadata (Source: ConsultationRecordDTO - NEW)

| AI Feature Name | Java Field | Type | Source | Notes |
|---|---|---|---|---|
| `fe_document_age_days` | `documentAgeDays` | int | Calculated | Days since document creation |
| `fe_scan_quality_encoded` | `contentType` + `parserConfidence` | int (0-2) | NEW fields | 2=PDF+conf≥0.8, 1=PDF+conf<0.8 or image, 0=other |
| `fe_hospital_encoded` | `hospitalId` | int | NEW field | Hospital location encoding (0=unknown) |
| `fe_department_encoded` | `departmentId` | int | NEW field | Department/ward encoding (0=unknown) |

### Patient Demographics (Source: Derived from UserDTO or ConsultationRecordDTO)

| AI Feature Name | Java Field | Type | Source | Notes |
|---|---|---|---|---|
| `fe_sex_encoded` | `sex` | int (0 or 1) | UserDTO | 1="M", 0="F" or unknown |
| `fe_age_group` | `ageYears` | int (0-3) | Derived | Group encoding: <2→0, <6→1, <12→2, ≥12→3 |
| `fe_age_normalized` | `ageYears` | float (0-1) | Calculated | min(max(age/18, 0), 1) |

### Engineered Features (Calculated by feature_builder.py)

| AI Feature Name | Calculation | Dependencies | Notes |
|---|---|---|---|
| `fe_creatinine_log` | log1p(max(creatinine_mg_dl, 0)) | serumCreatinine_mgdL | Log transformation for model |
| `fe_creatinine_abnormal` | 1.0 if mg_dl ≥1.2 OR ≤0.2 else 0 | serumCreatinine_mgdL | Abnormality flag |
| `fe_data_quality_score` | (metadata_quality_score/100 + parser_confidence) / 2 | Both quality scores | Composite quality metric |
| `fe_high_risk_dept` | Derived from `fe_department_encoded` | departmentId | High-risk dept flag (0=no) |
| `fe_manual_review_flag` | Same as `needs_manual_review` | requiresManualReview | Clinical review needed |
| `fe_metadata_quality_score` | metadata_quality_score / 100 | Document metadata | Normalized quality (0-1) |

---

## UpdatedConsultationRecordDTO Fields (New Fields Required)

### Current Fields (Keep)
```java
UUID id
UUID patientId
ConsultationType consultationType
AdmissionMode admissionMode
LocalDateTime consultationDate
// ... existing vitals and nephology fields
```

### NEW Fields to Add (AI Metadata)
```java
// Document Parser Metadata (for AI feature engineering)
Float parserConfidence;           // 0.0-1.0 confidence score from PDF/image parser
String contentType;               // "application/pdf", "image/jpeg", "image/png", etc.
Boolean requiresManualReview;     // Flag if document parsing was uncertain
Integer documentAgeDays;          // Days since document was created (calculated on save)
Boolean isReviewed;               // Clinical staff has reviewed this consultation
Integer hospitalId;               // Hospital identifier (0 = unknown/default)
Integer departmentId;             // Department identifier (0 = unknown/default)
BigDecimal serumCreatinine_umolL; // IMPORTANT: Add umol/L unit variant
Float documentQualityScore;       // Calculated quality score (0-100)
```

---

## Data Flow: PDF Scanner → Feature Builder → AI Model

```
1. PDF/Image Document
   ↓
2. AI Scanner (Extract):
   - patient_age (from demographics)
   - sex (from demographics)
   - creatinine_mg_dl + umol_l (from lab values)
   - parser_confidence (0.0-1.0 confidence score)
   - content_type ("application/pdf", "image/*", etc.)
   ↓
3. Save to ConsultationRecordDTO with:
   - ageYears, sex, serumCreatinine_mgdL, serumCreatinine_umolL
   - parserConfidence, contentType, requiresManualReview
   ↓
4. Clinical staff manually fills:
   - Vitals (BP, heart rate, temperature, O2 sat)
   - Nephrology data (proteinuria, hematuria, eGFR, CKD stage)
   - Sets isReviewed=true
   ↓
5. When calling AI prediction endpoint:
   - Extract 21 features using feature_builder.py
   - Pass to model → Output: [other, review, urgent, warning]
   - Create ClinicalAlert with recommendation
```

---

## Neon PostgreSQL Schema Updates Required

### ConsultationRecord Table (NEW Columns)
```sql
ALTER TABLE consultation_records ADD COLUMN parser_confidence FLOAT;
ALTER TABLE consultation_records ADD COLUMN content_type VARCHAR(100);
ALTER TABLE consultation_records ADD COLUMN requires_manual_review BOOLEAN DEFAULT false;
ALTER TABLE consultation_records ADD COLUMN document_age_days INTEGER;
ALTER TABLE consultation_records ADD COLUMN is_reviewed BOOLEAN DEFAULT false;
ALTER TABLE consultation_records ADD COLUMN hospital_id INTEGER DEFAULT 0;
ALTER TABLE consultation_records ADD COLUMN department_id INTEGER DEFAULT 0;
ALTER TABLE consultation_records ADD COLUMN serum_creatinine_umol_l NUMERIC(8,2);
ALTER TABLE consultation_records ADD COLUMN document_quality_score FLOAT;
```

---

## Integration Points

### 1. AI Service → Clinical-Service (PDF Processing)
**Endpoint:** `POST /api/v1/consultations`
**Expected Payload:**
```json
{
  "patientId": "uuid",
  "ageYears": 5,
  "sex": "M",
  "serumCreatinine_mgdL": 0.8,
  "serumCreatinine_umolL": 70.8,
  "parserConfidence": 0.92,
  "contentType": "application/pdf",
  "requiresManualReview": false,
  "documentAgeDays": 2,
  ...other clinical fields
}
```

### 2. Clinical-Service → AI Service (Prediction Request)
**Endpoint:** `POST /ai-clinical-service/predict` (to be defined)
**Request Body:**
```json
{
  "consultationId": "uuid",
  "ageYears": 5,
  "sex": "M",
  "creatinine_mg_dl": 0.8,
  "creatinine_umol_l": 70.8,
  "parser_confidence": 0.92,
  "has_creatinine": true,
  "content_type": "application/pdf",
  "requires_manual_review": false
}
```
**Response:**
```json
{
  "prediction": "urgent",
  "confidence": 0.78,
  "class_probabilities": {
    "other": 0.05,
    "review": 0.12,
    "urgent": 0.78,
    "warning": 0.05
  }
}
```

### 3. Alert Creation (Clinical-Service)
**When prediction is "urgent" or "warning":**
- Create ClinicalAlert with severity mapped from prediction
- Set alertType = "AI_RECOMMENDATION"
- Store full prediction in alert details

---

## Implementation Checklist

- [ ] Add NEW fields to ConsultationRecord entity
- [ ] Add NEW fields to ConsultationRecordDTO
- [ ] Create Flyway migration V1.2_Add_AI_Metadata.sql
- [ ] Update ConsultationRecordMapper for new fields
- [ ] Create AIPredictionDTO for response from AI service
- [ ] Create ClinicalOperationsController endpoint: POST /api/v1/clinical/consultations/{id}/request-ai-prediction
- [ ] Add Feign client for AI-Clinical-Service calls
- [ ] Wire ApplicationEvent listener to call AI service on consultation save
- [ ] Create integration test: Save consultation → Call AI → Create alert

---

## Notes for Clinical Staff

- Parser confidence < 0.8 should set `requiresManualReview=true`
- If document is > 30 days old, additional clinical validation may be required
- AI predictions are **recommendations only**—clinical staff has final decision authority
- Always review the full nephrology workup, not just the AI alert
