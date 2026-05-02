# Ops Service Data Model and API Contract (v1)

This document defines:
- Exact Ops domain entities and constraints
- Exact request/response contracts for ops-service controllers
- Contract alignment needed in clinical-service for hospitalization flow

Status: Ready for implementation
Date: 2026-04-15

## 1. Data Model (Ops-Service)

### 1.1 `patient_dossier`

Purpose:
- Represents one hospitalization case created from a clinical consultation.
- Owned by ops-service, linked to clinical and communication identifiers.

SQL shape:

```sql
create table patient_dossier (
  id uuid primary key,
  patient_id bigint not null,
  source_consultation_id uuid not null,
  source_appointment_id uuid not null,
  hospitalization_request_id uuid not null,

  primary_doctor_id uuid not null,
  assigned_nurse_id uuid,

  admission_reason text not null,
  admission_priority varchar(20) not null,
  status varchar(24) not null,

  admitted_at timestamp not null,
  expected_discharge_at timestamp,
  discharged_at timestamp,

  discharge_summary text,
  archived_to_history boolean not null default false,
  archived_at timestamp,

  created_at timestamp not null,
  updated_at timestamp not null,

  version bigint not null
);
```

Constraints:
- `status` in (`ACTIVE`, `IN_PROGRESS`, `READY_FOR_DISCHARGE`, `DISCHARGED`, `ARCHIVED`)
- `admission_priority` in (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
- Unique one dossier per hospitalization request:
  - `unique (hospitalization_request_id)`
- Ensure logical date order:
  - `discharged_at >= admitted_at` when `discharged_at` is not null
  - `archived_at >= discharged_at` when `archived_at` is not null

Indexes:
- `idx_dossier_patient` on `(patient_id)`
- `idx_dossier_nurse_status` on `(assigned_nurse_id, status)`
- `idx_dossier_consultation` on `(source_consultation_id)`
- `idx_dossier_created` on `(created_at desc)`

Java enum targets:
- `DossierStatus { ACTIVE, IN_PROGRESS, READY_FOR_DISCHARGE, DISCHARGED, ARCHIVED }`
- `AdmissionPriority { LOW, MEDIUM, HIGH, CRITICAL }`

---

### 1.2 `dossier_entry`

Purpose:
- Immutable audit trail entries by nurse/doctor during care.
- Every clinical change and medication administration is signed.

SQL shape:

```sql
create table dossier_entry (
  id uuid primary key,
  dossier_id uuid not null references patient_dossier(id),

  entry_type varchar(24) not null,
  actor_role varchar(16) not null,
  actor_id uuid not null,
  actor_display_name varchar(120) not null,

  title varchar(180) not null,
  details text,

  medication_id uuid,
  medication_name varchar(180),
  dose_value numeric(10,2),
  dose_unit varchar(24),
  route varchar(32),

  patient_condition varchar(64),
  vitals_json text,

  occurred_at timestamp not null,

  requires_signature boolean not null default true,
  signed boolean not null default false,
  signed_at timestamp,
  signature_type varchar(20),
  signature_hash varchar(128),

  previous_entry_id uuid,

  created_at timestamp not null,
  updated_at timestamp not null,

  version bigint not null
);
```

Constraints:
- `entry_type` in (`MEDICATION_ADMIN`, `CONDITION_UPDATE`, `CARE_NOTE`, `TRANSFER`, `ALERT`, `DISCHARGE_NOTE`)
- `actor_role` in (`NURSE`, `DOCTOR`, `SYSTEM`)
- `signature_type` in (`DRAWN`, `TYPED`, `PKI`) when not null
- Signature coherence:
  - if `signed = true` then `signed_at`, `signature_type`, `signature_hash` are not null
- Optional chain coherence:
  - `previous_entry_id` should point to same dossier lineage (enforced at service layer)

Indexes:
- `idx_entry_dossier_occurred` on `(dossier_id, occurred_at desc)`
- `idx_entry_actor` on `(actor_id, occurred_at desc)`
- `idx_entry_unsigned` on `(dossier_id, signed)`

Java enum targets:
- `DossierEntryType { MEDICATION_ADMIN, CONDITION_UPDATE, CARE_NOTE, TRANSFER, ALERT, DISCHARGE_NOTE }`
- `ActorRole { NURSE, DOCTOR, SYSTEM }`
- `SignatureType { DRAWN, TYPED, PKI }`

---

### 1.3 `dossier_contributor`

Purpose:
- Supports your requirement: multiple doctors can contribute to one patient dossier.

SQL shape:

```sql
create table dossier_contributor (
  id uuid primary key,
  dossier_id uuid not null references patient_dossier(id),
  contributor_id uuid not null,
  contributor_role varchar(16) not null,
  active boolean not null default true,
  added_at timestamp not null,
  removed_at timestamp,
  added_by uuid not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  version bigint not null,
  unique (dossier_id, contributor_id, contributor_role, active)
);
```

Constraints:
- `contributor_role` in (`DOCTOR`, `NURSE`)
- `removed_at >= added_at` when not null

Indexes:
- `idx_contributor_dossier` on `(dossier_id, active)`
- `idx_contributor_user` on `(contributor_id, active)`

---

### 1.4 Service-level invariants

- A dossier cannot be archived unless status is `DISCHARGED`.
- On discharge:
  - set status `DISCHARGED`
  - create mandatory signed `DISCHARGE_NOTE` entry
  - then archive transition sets `ARCHIVED` and `archived_to_history = true`
- Entry creation requires actor to be:
  - assigned nurse, or
  - listed contributor doctor
- For entry types `MEDICATION_ADMIN` and `CONDITION_UPDATE`: signature is mandatory.

## 2. Controller Contract (Ops-Service)

Base path: `/api/ops`
Security: JWT resource server (already enabled)
Headers (recommended):
- `X-User-Id: <uuid>`
- `X-User-Role: NURSE|DOCTOR|COORDINATOR|SYSTEM`

### 2.1 Create dossier (called from clinical-service)

Endpoint:
- `POST /api/ops/dossiers`

Request body:

```json
{
  "patientId": 1024,
  "sourceConsultationId": "5ed3be5f-f35d-4fd0-a6e3-d4cbf8e7ea1f",
  "sourceAppointmentId": "ff9707b2-508a-4b5e-a157-5f8f3f8e3d9d",
  "hospitalizationRequestId": "1ee2ad7b-6825-4410-93e1-6203c578e095",
  "primaryDoctorId": "564b92a7-2df3-4eb1-a470-e49d6e4e16f4",
  "admissionReason": "Acute dehydration and observation",
  "admissionPriority": "HIGH",
  "expectedDischargeAt": "2026-04-18T12:00:00"
}
```

Response `201 Created`:

```json
{
  "id": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "patientId": 1024,
  "status": "ACTIVE",
  "assignedNurseId": "4a383ce9-c0f8-4ea9-97c4-4f021ec37949",
  "assignedNurseName": "Nurse Amina",
  "admittedAt": "2026-04-15T10:30:00",
  "createdAt": "2026-04-15T10:30:00"
}
```

Errors:
- `409 Conflict`: `hospitalizationRequestId` already used
- `422 Unprocessable Entity`: no nurse available

---

### 2.2 Get dossier by id

Endpoint:
- `GET /api/ops/dossiers/{dossierId}`

Response `200 OK`:

```json
{
  "id": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "patientId": 1024,
  "sourceConsultationId": "5ed3be5f-f35d-4fd0-a6e3-d4cbf8e7ea1f",
  "sourceAppointmentId": "ff9707b2-508a-4b5e-a157-5f8f3f8e3d9d",
  "hospitalizationRequestId": "1ee2ad7b-6825-4410-93e1-6203c578e095",
  "primaryDoctorId": "564b92a7-2df3-4eb1-a470-e49d6e4e16f4",
  "assignedNurseId": "4a383ce9-c0f8-4ea9-97c4-4f021ec37949",
  "admissionReason": "Acute dehydration and observation",
  "admissionPriority": "HIGH",
  "status": "IN_PROGRESS",
  "admittedAt": "2026-04-15T10:30:00",
  "expectedDischargeAt": "2026-04-18T12:00:00",
  "dischargedAt": null,
  "archivedToHistory": false,
  "createdAt": "2026-04-15T10:30:00",
  "updatedAt": "2026-04-15T10:47:15"
}
```

---

### 2.3 List dossiers by role views

Endpoints:
- `GET /api/ops/dossiers/my-assigned` (nurse)
- `GET /api/ops/dossiers?status=ACTIVE&patientId=1024&doctorId=<uuid>&nurseId=<uuid>&page=0&size=20`

List item response:

```json
{
  "content": [
    {
      "id": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
      "patientId": 1024,
      "status": "ACTIVE",
      "admissionPriority": "HIGH",
      "assignedNurseId": "4a383ce9-c0f8-4ea9-97c4-4f021ec37949",
      "admittedAt": "2026-04-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 2.4 Add dossier entry

Endpoint:
- `POST /api/ops/dossiers/{dossierId}/entries`

Request body:

```json
{
  "entryType": "MEDICATION_ADMIN",
  "title": "Paracetamol administered",
  "details": "Given after meal",
  "medicationId": "2f943719-c98f-43f7-a6d5-a64d60fa9653",
  "medicationName": "Paracetamol",
  "doseValue": 500,
  "doseUnit": "mg",
  "route": "ORAL",
  "patientCondition": "STABLE",
  "vitals": {
    "temperature": 37.6,
    "heartRate": 92
  },
  "occurredAt": "2026-04-15T14:00:00",
  "requiresSignature": true
}
```

Response `201 Created`:

```json
{
  "id": "4e76fe04-2e1e-4468-b100-34a1ab3ab8fc",
  "dossierId": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "entryType": "MEDICATION_ADMIN",
  "actorRole": "NURSE",
  "actorId": "4a383ce9-c0f8-4ea9-97c4-4f021ec37949",
  "actorDisplayName": "Nurse Amina",
  "title": "Paracetamol administered",
  "signed": false,
  "requiresSignature": true,
  "occurredAt": "2026-04-15T14:00:00",
  "createdAt": "2026-04-15T14:00:02"
}
```

Errors:
- `403 Forbidden`: actor not assigned/contributor
- `409 Conflict`: dossier already archived

---

### 2.5 Sign dossier entry

Endpoint:
- `POST /api/ops/dossiers/{dossierId}/entries/{entryId}/sign`

Request body:

```json
{
  "signatureType": "TYPED",
  "signaturePayload": "Nurse Amina",
  "signedAt": "2026-04-15T14:01:00"
}
```

Response `200 OK`:

```json
{
  "entryId": "4e76fe04-2e1e-4468-b100-34a1ab3ab8fc",
  "signed": true,
  "signedAt": "2026-04-15T14:01:00",
  "signatureType": "TYPED"
}
```

Error:
- `409 Conflict`: entry already signed

---

### 2.6 List dossier timeline entries

Endpoint:
- `GET /api/ops/dossiers/{dossierId}/entries?from=2026-04-01T00:00:00&to=2026-04-30T23:59:59&type=MEDICATION_ADMIN&page=0&size=30`

Response `200 OK`:

```json
{
  "content": [
    {
      "id": "4e76fe04-2e1e-4468-b100-34a1ab3ab8fc",
      "entryType": "MEDICATION_ADMIN",
      "title": "Paracetamol administered",
      "actorDisplayName": "Nurse Amina",
      "occurredAt": "2026-04-15T14:00:00",
      "signed": true
    }
  ],
  "page": 0,
  "size": 30,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 2.7 Add or remove dossier contributors (multi-doctor support)

Endpoints:
- `POST /api/ops/dossiers/{dossierId}/contributors`
- `DELETE /api/ops/dossiers/{dossierId}/contributors/{contributorId}?role=DOCTOR`

Create request:

```json
{
  "contributorId": "f52b9b75-e505-4c26-bf97-68818e9f15ab",
  "contributorRole": "DOCTOR"
}
```

Response `201 Created` contains active contributor record.

---

### 2.8 Discharge dossier

Endpoint:
- `PUT /api/ops/dossiers/{dossierId}/discharge`

Request body:

```json
{
  "dischargeSummary": "Patient stable and hydrated.",
  "dischargedAt": "2026-04-18T10:15:00",
  "requiresSignedDischargeNote": true,
  "signature": {
    "signatureType": "TYPED",
    "signaturePayload": "Nurse Amina"
  }
}
```

Response `200 OK`:

```json
{
  "id": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "status": "DISCHARGED",
  "dischargedAt": "2026-04-18T10:15:00",
  "archivedToHistory": false
}
```

---

### 2.9 Archive dossier to patient history

Endpoint:
- `PUT /api/ops/dossiers/{dossierId}/archive`

Response `200 OK`:

```json
{
  "id": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "status": "ARCHIVED",
  "archivedToHistory": true,
  "archivedAt": "2026-04-18T10:20:00"
}
```

Side-effect event (async):
- `DossierArchivedEvent` (for clinical patient history projection)

## 3. DTO Set (Java naming recommendation)

Requests:
- `CreatePatientDossierRequest`
- `CreateDossierEntryRequest`
- `SignDossierEntryRequest`
- `AddDossierContributorRequest`
- `DischargeDossierRequest`

Responses:
- `PatientDossierResponse`
- `PatientDossierSummaryResponse`
- `DossierEntryResponse`
- `DossierEntrySignatureResponse`
- `DossierContributorResponse`

## 4. Required Clinical-Service Contract Alignment

To integrate smoothly with ops-service, clinical-service should add this client-facing contract:

- Internal call to ops-service on hospitalization approval:
  - `POST /api/ops/dossiers`
- Clinical `HospitalizationRequest` must include:
  - `consultationId`
  - `communicationAppointmentId`
  - `patientId`
  - `primaryDoctorId`
  - `reason`
  - `priority`

And clinical-service should expose doctor UI endpoint:
- `POST /clinical/consultations/{id}/hospitalize`

Response shape from clinical endpoint should include both IDs:

```json
{
  "hospitalizationRequestId": "1ee2ad7b-6825-4410-93e1-6203c578e095",
  "dossierId": "8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d",
  "status": "DOSSIER_CREATED"
}
```

## 5. Validation Rules (must-have)

- One consultation maps to one communication appointment reference.
- One hospitalization request creates at most one dossier.
- Lab results remain hidden from nurse context until doctor-approved in clinical-service.
- Entry signing is idempotent; repeat sign call returns 409.
- Archived dossiers are read-only (except history retrieval endpoints).

## 6. Recommended HTTP Error Envelope

```json
{
  "timestamp": "2026-04-15T11:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Dossier already archived",
  "path": "/api/ops/dossiers/8c0b6689-5d2f-47ef-8ac5-430dcfdb8f4d/entries"
}
```

## 7. Implementation Notes

- Use optimistic locking with `@Version` on all mutable entities.
- Keep `dossier_entry` immutable after signature completion.
- Generate `signature_hash = SHA-256(signaturePayload + actorId + occurredAt + entryId)`.
- Add Flyway migration as first ops domain migration:
  - `V1__ops_domain_tables.sql`
