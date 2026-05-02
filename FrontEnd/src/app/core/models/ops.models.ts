export type AdmissionPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type DossierStatus =
  | 'ACTIVE'
  | 'IN_PROGRESS'
  | 'READY_FOR_DISCHARGE'
  | 'DISCHARGED'
  | 'ARCHIVED';

export type DossierEntryType =
  | 'MEDICATION_ADMIN'
  | 'CONDITION_UPDATE'
  | 'CARE_NOTE'
  | 'TRANSFER'
  | 'ALERT'
  | 'DISCHARGE_NOTE';

export type ActorRole = 'NURSE' | 'DOCTOR' | 'SYSTEM';
export type ContributorRole = 'DOCTOR' | 'NURSE';
export type SignatureType = 'DRAWN' | 'TYPED' | 'PKI';

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreatePatientDossierRequest {
  patientId: number;
  sourceConsultationId: string;
  sourceAppointmentId: string;
  hospitalizationRequestId: string;
  primaryDoctorId: string;
  admissionReason: string;
  admissionPriority: AdmissionPriority;
  expectedDischargeAt?: string | null;
}

export interface PatientDossierResponse {
  id: string;
  patientId: number;
  sourceConsultationId: string;
  sourceAppointmentId: string;
  hospitalizationRequestId: string;
  primaryDoctorId: string;
  assignedNurseId?: string | null;
  assignedNurseName?: string | null;
  admissionReason: string;
  admissionPriority: AdmissionPriority;
  status: DossierStatus;
  admittedAt: string;
  expectedDischargeAt?: string | null;
  dischargedAt?: string | null;
  archivedToHistory: boolean;
  archivedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PatientDossierSummaryResponse {
  id: string;
  patientId: number;
  status: DossierStatus;
  admissionPriority: AdmissionPriority;
  assignedNurseId?: string | null;
  admittedAt: string;
}

export interface CreateDossierEntryRequest {
  entryType: DossierEntryType;
  title: string;
  details?: string;
  medicationId?: string;
  medicationName?: string;
  doseValue?: number;
  doseUnit?: string;
  route?: string;
  patientCondition?: string;
  vitals?: Record<string, unknown>;
  occurredAt?: string;
  requiresSignature?: boolean;
}

export interface DossierEntryResponse {
  id: string;
  dossierId: string;
  entryType: DossierEntryType;
  actorRole: ActorRole;
  actorId: string;
  actorDisplayName: string;
  title: string;
  details?: string;
  signed: boolean;
  requiresSignature: boolean;
  occurredAt: string;
  createdAt: string;
}

export interface SignDossierEntryRequest {
  signatureType: SignatureType;
  signaturePayload: string;
  signedAt?: string;
}

export interface DossierEntrySignatureResponse {
  entryId: string;
  signed: boolean;
  signedAt: string;
  signatureType: SignatureType;
}

export interface AddDossierContributorRequest {
  contributorId: string;
  contributorRole: ContributorRole;
}

export interface DossierContributorResponse {
  id: string;
  dossierId: string;
  contributorId: string;
  contributorRole: ContributorRole;
  active: boolean;
  addedAt: string;
  removedAt?: string | null;
}

export interface DischargeDossierRequest {
  dischargeSummary: string;
  dischargedAt: string;
  requiresSignedDischargeNote?: boolean;
  signature?: {
    signatureType: SignatureType;
    signaturePayload: string;
  };
}

export type HospitalizationTaskType = 'WEIGHT_CHECK' | 'MEDICATION' | 'TEMPERATURE' | 'BLOOD_MONITORING' | 'PATIENT_MONITORING' | 'CUSTOM';
export type HospitalizationMeasurementKind = 'NONE' | 'NUMERIC' | 'TEXT';
export type HospitalizationTaskStatus = 'PENDING' | 'DONE' | 'NOT_DONE';
export type HospitalizationStatus = 'REQUESTED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface HospitalizationTaskExecutionResponse {
  id: string;
  status: HospitalizationTaskStatus;
  nurseKeycloakId: string;
  nurseUsername: string;
  note?: string | null;
  numericValue?: number | null;
  textValue?: string | null;
  unit?: string | null;
  recordedAt: string;
}

export interface HospitalizationTaskResponse {
  id: string;
  type: HospitalizationTaskType;
  title: string;
  instructions?: string | null;
  measurementKind: HospitalizationMeasurementKind;
  expectedUnit?: string | null;
  displayOrder: number;
  status: HospitalizationTaskStatus;
  latestNote?: string | null;
  latestNumericValue?: number | null;
  latestTextValue?: string | null;
  latestUnit?: string | null;
  lastUpdatedByNurseId?: string | null;
  lastUpdatedByNurseUsername?: string | null;
  lastUpdatedAt?: string | null;
  executions: HospitalizationTaskExecutionResponse[];
}

export interface HospitalizationTaskRequest {
  type: HospitalizationTaskType;
  title: string;
  instructions?: string | null;
  measurementKind: HospitalizationMeasurementKind;
  expectedUnit?: string | null;
  displayOrder?: number | null;
}

export interface HospitalizationCaseResponse {
  id: string;
  patientId: number;
  consultationId: string;
  doctorKeycloakId: string;
  doctorUsername: string;
  reason: string;
  status: HospitalizationStatus;
  createdAt: string;
  updatedAt: string;
  tasks: HospitalizationTaskResponse[];
}

export interface HospitalizationSummaryResponse {
  id: string;
  patientId: number;
  consultationId: string;
  doctorUsername: string;
  reason: string;
  status: HospitalizationStatus;
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  createdAt: string;
  updatedAt: string;
}

export interface HospitalizationTaskUpdateRequest {
  status: HospitalizationTaskStatus;
  note?: string | null;
  numericValue?: number | null;
  textValue?: string | null;
  unit?: string | null;
}
