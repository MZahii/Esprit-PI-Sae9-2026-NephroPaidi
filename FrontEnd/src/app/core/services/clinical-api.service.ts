import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStorageService } from '../auth/auth-storage.service';
import {
  Consultation,
  ConsultationMedicalDossier,
  ConsultationOutcomeResponse,
  PatientProfile
} from '../../features/clinical/models/clinical.models';

export interface DoctorSearchResult {
  id?: number;
  keycloakId?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
  phone?: string;
  avatarUrl?: string;
  enabled?: boolean;
}

export interface ConsultationMetricsRequest {
  heightCm?: number;
  creatinineMgDl?: number;
  weightKg?: number;
  ageYears?: number;
  systolicBpMmHg?: number;
  diastolicBpMmHg?: number;
  heartRateBpm?: number;
  respiratoryRateBpm?: number;
  temperatureC?: number;
  oxygenSaturationPct?: number;
  sex?: string;  // 'M' or 'F' - REQUIRED for CKD-EPI formula
  // Response fields (calculated by backend)
  creatinineUmol?: number;  // SI units storage
  serumCreatinineUnit?: string;  // "MICROMOL_L" or "MG_DL"
  egfrFormulaUsed?: string;  // "CKD_EPI_2021" or "COCKCROFT_GAULT"
  ckdEpiEgfr?: number;  // CKD-EPI result
  previousEgfr?: number;  // Trend comparison
  egfrChange?: number;  // Absolute change
  egfrChangePercent?: number;  // Percentage change
  egfrTrend?: string;  // "STABLE", "DECLINING", etc.
  egfrQualityIndicator?: string;  // "HIGH_QUALITY", "MEDIUM_QUALITY", "LOW_QUALITY"
  egfrLastUpdatedAt?: string;  // ISO timestamp
  egfr?: number;  // eGFR value
  ckdStage?: string;  // "NORMAL", "STAGE_1", etc.
  alertLowEgfr?: boolean;  // Low eGFR alert
  alertRapidDecline?: boolean;  // Rapid decline alert
  alertMessage?: string;  // Clinical alert message
  aiRecommendation?: string;
  aiConfidence?: number;
  aiSummary?: string;
  aiRequiresReview?: boolean;
  aiSourceFileName?: string;
  aiUpdatedAt?: string;
}

export interface ClinicalLabRequestPayload {
  patientId: number;
  consultationId?: string;
  testType: string;
  urgency: string;
  notes?: string;
  testItems?: ClinicalLabRequestTestItemPayload[];
}

export interface ClinicalLabRequestTestItemPayload {
  key: string;
  label: string;
  note?: string;
}

export interface ClinicalLabRequestItemResponse {
  key?: string;
  label: string;
  note?: string;
}

export interface ClinicalLabRequestResponse {
  id: string;
  doctorId: string;
  patientId: number;
  consultationId?: string;
  testType: string;
  testItems?: ClinicalLabRequestItemResponse[];
  urgency: string;
  status: string;
  notes?: string;
  latestAiRecommendation?: string;
  latestAiConfidence?: number;
  latestAiRequiresDoctorReview?: boolean;
  latestAiSummary?: string;
  latestResultAvailable?: boolean;
  latestResultFileName?: string;
  latestResultUploadedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EgfrMlRegressionResponse {
  predicted_egfr_3_months: number;
  predicted_egfr_6_months: number;
  predicted_egfr_12_months: number;
}

export interface EgfrMlClassificationResponse {
  rapid_decline_flag: number;
  rapid_decline_probability: number;
  confidence_score: number;
  risk_label: 'LOW' | 'HIGH' | string;
  threshold: number;
}

@Injectable({ providedIn: 'root' })
export class ClinicalApiService {
  private base = (window as any).__env?.API_BASE || 'http://localhost:8083';

  constructor(private http: HttpClient, private auth: AuthStorageService) {}

  private authHeaders(): HttpHeaders {
    const token = this.auth.getAccessToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return new HttpHeaders(headers);
  }

  private doctorHeaders(): HttpHeaders {
    const user = this.auth.getUser();
    const doctorId = user?.keycloakId;
    const token = this.auth.getAccessToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (doctorId) headers['X-Doctor-Id'] = doctorId;
    return new HttpHeaders(headers);
  }

  private guardianHeaders(): HttpHeaders {
    const user = this.auth.getUser();
    const guardianId = user?.userId;
    const token = this.auth.getAccessToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (guardianId) headers['X-Guardian-Id'] = String(guardianId);
    return new HttpHeaders(headers);
  }

  getDoctor(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/doctors/${id}`,
      { headers: this.doctorHeaders() }
    );
  }

  getPatient(id: number): Observable<PatientProfile> {
    return this.http.get<PatientProfile>(
      `${this.base}/api/clinical/patients/${id}`,
      { headers: this.doctorHeaders() }
    );
  }

  listMyConsultations(filters?: { patientQuery?: string; status?: string }): Observable<Consultation[]> {
    let params = new HttpParams();
    if (filters?.patientQuery) params = params.set('patientQuery', filters.patientQuery);
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    return this.http.get<Consultation[]>(
      `${this.base}/api/clinical/consultations/mine`,
      { headers: this.doctorHeaders(), params }
    );
  }

  createConsultation(payload: { patientId: number; dateTime: string }): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations`,
      payload,
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultation(
    id: string,
    payload: { dateTime?: string; status: 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' }
  ): Observable<any> {
    return this.http.put<any>(
      `${this.base}/api/clinical/consultations/${id}`,
      payload,
      { headers: this.doctorHeaders() }
    );
  }

  cancelConsultation(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/api/clinical/consultations/${id}`,
      { headers: this.doctorHeaders() }
    );
  }

  listPatients(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.base}/api/patients`,
      { headers: this.authHeaders() }
    );
  }

  searchPatients(query: string): Observable<any[]> {
    const trimmed = (query ?? '').trim();
    if (trimmed.length < 2) {
      return of([]);
    }
    const term = trimmed.toLowerCase();
    return this.http.get<any[]>(
      `${this.base}/api/patients`,
      { headers: this.authHeaders() }
    ).pipe(
      map((list) => (list ?? [])
        .filter((item) => {
          const id = String(item?.id ?? '').toLowerCase();
          const firstName = String(item?.firstName ?? '').toLowerCase();
          const lastName = String(item?.lastName ?? '').toLowerCase();
          const fullName = `${firstName} ${lastName}`.trim();
          const bloodType = String(item?.bloodType ?? '').toLowerCase();
          const allergies = String(item?.allergies ?? '').toLowerCase();
          return id.includes(term)
            || firstName.includes(term)
            || lastName.includes(term)
            || fullName.includes(term)
            || bloodType.includes(term)
            || allergies.includes(term);
        })
        .slice(0, 3))
    );
  }

  listUsers(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.base}/api/users`,
      { headers: this.authHeaders() }
    ).pipe(
      catchError(() => of([]))
    );
  }

  listDoctors(limit = 10): Observable<DoctorSearchResult[]> {
    return this.listStaffByRoles(['DOCTOR'], limit);
  }

  listSurgeons(limit = 10): Observable<DoctorSearchResult[]> {
    return this.listStaffByRoles(['SURGEON'], limit);
  }

  listStaffByRoles(roles: string[], limit = 100): Observable<DoctorSearchResult[]> {
    const payload = {
      query: '',
      roles,
      page: 0,
      size: limit,
      sortBy: 'firstName',
      sortDir: 'asc'
    };

    return this.http.post<{ items?: DoctorSearchResult[] }>(
      `${this.base}/api/users/staff/search`,
      payload,
      { headers: this.authHeaders() }
    ).pipe(
      map((res) => res?.items ?? [])
    );
  }

  searchDoctors(query: string, limit = 3): Observable<DoctorSearchResult[]> {
    const trimmed = (query ?? '').trim();
    if (trimmed.length < 2) {
      return of([]);
    }

    const payload = {
      query: trimmed,
      roles: ['DOCTOR'],
      page: 0,
      size: limit,
      sortBy: 'firstName',
      sortDir: 'asc'
    };

    return this.http.post<{ items?: DoctorSearchResult[] }>(
      `${this.base}/api/users/staff/search`,
      payload,
      { headers: this.authHeaders() }
    ).pipe(
      map((res) => res?.items ?? [])
    );
  }

  getPublicDoctors(): Observable<DoctorSearchResult[]> {
    return this.http.get<DoctorSearchResult[]>(`${this.base}/api/users/public/doctors`).pipe(
      catchError(() => of([]))
    );
  }

  listAppointments(filters?: {
    doctorId?: string;
    patientId?: number | string;
    status?: string;
    from?: string;
    to?: string;
  }): Observable<any[]> {
    let params = new HttpParams();
    if (filters?.doctorId) params = params.set('doctorId', filters.doctorId);
    if (filters?.patientId !== undefined && filters?.patientId !== null && filters?.patientId !== '') {
      params = params.set('patientId', String(filters.patientId));
    }
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);

    return this.http.get<any[]>(
      `${this.base}/api/clinical/appointments`,
      { headers: this.authHeaders(), params }
    );
  }

  listConsultations(filters?: {
    patientId?: number | string;
    status?: string;
    from?: string;
    to?: string;
  }): Observable<any[]> {
    let params = new HttpParams();
    if (filters?.patientId !== undefined && filters?.patientId !== null && filters?.patientId !== '') {
      params = params.set('patientId', String(filters.patientId));
    }
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);

    return this.http.get<any[]>(
      `${this.base}/api/clinical/consultations`,
      { headers: this.authHeaders(), params }
    );
  }

  listGuardianConsultations(filters?: {
    patientId?: number | string;
    status?: string;
  }): Observable<any[]> {
    let params = new HttpParams();
    if (filters?.patientId !== undefined && filters?.patientId !== null && filters?.patientId !== '') {
      params = params.set('patientId', String(filters.patientId));
    }
    if (filters?.status && filters.status !== 'ALL') {
      params = params.set('status', filters.status);
    }

    return this.http.get<any[]>(
      `${this.base}/api/clinical/guardian/consultations`,
      { headers: this.guardianHeaders(), params }
    );
  }

  getGuardianConsultationOutcome(consultationId: string): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/guardian/consultations/${consultationId}/outcomes`,
      { headers: this.guardianHeaders() }
    );
  }

  getGuardianMedicalDossier(patientId: number | string): Observable<ConsultationMedicalDossier> {
    return this.http.get<ConsultationMedicalDossier>(
      `${this.base}/api/clinical/guardian/patients/${patientId}/medical-dossier`,
      { headers: this.guardianHeaders() }
    );
  }

  getConsultation(id: string): Observable<Consultation> {
    return this.http.get<Consultation>(
      `${this.base}/api/clinical/consultations/${id}`,
      { headers: this.doctorHeaders() }
    );
  }

  getConsultationOutcome(id: string): Observable<ConsultationOutcomeResponse> {
    return this.http.get<ConsultationOutcomeResponse>(
      `${this.base}/api/clinical/consultations/${id}/outcomes`,
      { headers: this.doctorHeaders() }
    );
  }

  getConsultationMedicalDossier(id: string): Observable<ConsultationMedicalDossier> {
    return this.http.get<ConsultationMedicalDossier>(
      `${this.base}/api/clinical/consultations/${id}/medical-dossier`,
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultationLabRequests(id: string, content: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/lab-requests`,
      { content },
      { headers: this.doctorHeaders() }
    );
  }

  createLabRequest(payload: ClinicalLabRequestPayload): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/lab-requests`,
      payload,
      { headers: this.doctorHeaders() }
    );
  }

  getConsultationLabRequests(consultationId: string): Observable<ClinicalLabRequestResponse[]> {
    return this.http.get<ClinicalLabRequestResponse[]>(
      `${this.base}/api/clinical/lab-requests/consultation/${consultationId}`,
      { headers: this.doctorHeaders() }
    );
  }

  getPatientLabRequests(patientId: number | string): Observable<ClinicalLabRequestResponse[]> {
    return this.http.get<ClinicalLabRequestResponse[]>(
      `${this.base}/api/clinical/lab-requests/patient/${patientId}`,
      { headers: this.doctorHeaders() }
    );
  }

  uploadLabSupportingFile(
    labRequestId: string,
    file: File,
    testItemKey?: string,
    testItemLabel?: string
  ): Observable<any> {
    const form = new FormData();
    form.append('file', file);
    if (testItemKey) {
      form.append('testItemKey', testItemKey);
    }
    if (testItemLabel) {
      form.append('testItemLabel', testItemLabel);
    }
    return this.http.post<any>(
      `${this.base}/api/clinical/lab-requests/${labRequestId}/results`,
      form,
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultationNotes(id: string, content: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/notes`,
      { content },
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultationDiagnosis(id: string, content: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/diagnosis`,
      { content },
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultationPrescriptions(id: string, content: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/prescriptions`,
      { content },
      { headers: this.doctorHeaders() }
    );
  }

  updateConsultationTreatmentPlan(id: string, content: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/treatment-plan`,
      { content },
      { headers: this.doctorHeaders() }
    );
  }

  upsertConsultationMetrics(id: string, payload: ConsultationMetricsRequest): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${id}/metrics`,
      payload,
      { headers: this.doctorHeaders() }
    );
  }

  getConsultationMetrics(id: string): Observable<ConsultationMetricsRequest> {
    return this.http.get<ConsultationMetricsRequest>(
      `${this.base}/api/clinical/consultations/${id}/metrics`,
      { headers: this.doctorHeaders() }
    );
  }

  predictEgfrRegression(payload: Record<string, unknown>): Observable<EgfrMlRegressionResponse> {
    return this.http.post<EgfrMlRegressionResponse>(
      `${this.base}/api/egfr-ml/predict/regression`,
      payload,
      { headers: this.doctorHeaders() }
    );
  }

  predictEgfrClassification(
    payload: Record<string, unknown>,
    threshold = 0.5
  ): Observable<EgfrMlClassificationResponse> {
    const params = new HttpParams().set('threshold', String(threshold));
    return this.http.post<EgfrMlClassificationResponse>(
      `${this.base}/api/egfr-ml/predict/classification`,
      payload,
      { headers: this.doctorHeaders(), params }
    );
  }

  downloadLatestConsultationLabResult(consultationId: string): Observable<Blob> {
    return this.http.get(
      `${this.base}/api/clinical/lab-requests/consultation/${consultationId}/results/latest/download`,
      { headers: this.doctorHeaders(), responseType: 'blob' }
    );
  }

  /** Pharmacy catalog via gateway → pharmacy-service */
  searchMedications(namePrefix: string, limit = 20): Observable<any[]> {
    const term = (namePrefix ?? '').trim();
    let params = new HttpParams().set('sort', 'az');
    if (term.length > 0) {
      params = params.set('name', term);
    }
    return this.http.get<any[]>(`${this.base}/api/medications`, {
      headers: this.authHeaders(),
      params
    }).pipe(
      map((list) => (Array.isArray(list) ? list.slice(0, limit) : []))
    );
  }

  listPendingDoctorFollowUpRequests(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.base}/api/clinical/follow-up-requests`,
      { headers: this.authHeaders() }
    );
  }

  createDoctorFollowUpRequest(
    consultationId: string,
    body: { offsetAmount: number; offsetUnit: 'DAYS' | 'WEEKS' | 'MONTHS'; notes?: string }
  ): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/consultations/${consultationId}/follow-up-requests`,
      body,
      { headers: this.doctorHeaders() }
    );
  }

  confirmDoctorFollowUpRequest(
    id: string,
    body: {
      scheduledAt: string;
      doctorId?: string;
      durationMinutes?: number;
      reason?: string;
    }
  ): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/follow-up-requests/${id}/confirm`,
      body,
      { headers: this.authHeaders() }
    );
  }

  createAppointment(payload: {
    patientId: number;
    doctorId: string;
    scheduledAt: string;
    durationMinutes?: number;
    reason?: string;
  }): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/appointments`,
      payload,
      { headers: this.authHeaders() }
    );
  }

  updateAppointment(id: string, payload: {
    patientId?: number;
    doctorId?: string;
    scheduledAt?: string;
    durationMinutes?: number;
    reason?: string;
    status?: 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'NO_SHOW';
  }): Observable<any> {
    return this.http.put<any>(
      `${this.base}/api/clinical/appointments/${id}`,
      payload,
      { headers: this.authHeaders() }
    );
  }

  cancelAppointment(id: string, reason?: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/appointments/${id}/cancel`,
      { reason },
      { headers: this.authHeaders() }
    );
  }

  startConsultation(appointmentId: string): Observable<any> {
    return this.http.post<any>(
      `${this.base}/api/clinical/appointments/${appointmentId}/start`,
      {},
      { headers: this.doctorHeaders() }
    );
  }

  startAppointmentConsultation(id: string): Observable<{ consultationId: string }> {
    return this.http.post<{ consultationId: string }>(
      `${this.base}/api/clinical/appointments/${id}/start-consultation`,
      {},
      { headers: this.doctorHeaders() }
    );
  }

  checkDoctorAvailability(
    doctorId: string,
    from: string,
    to: string
  ): Observable<{ available: boolean; conflictReason?: string }> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('from', from)
      .set('to', to);
    return this.http.get<{ available: boolean; conflictReason?: string }>(
      `${this.base}/api/clinical/appointments/availability`,
      { headers: this.authHeaders(), params }
    );
  }

  listAuditEvents(limit = 200): Observable<any[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<any[]>(
      `${this.base}/api/clinical/audit`,
      { headers: this.authHeaders(), params }
    );
  }

  // Doctor endpoints
  getDoctorById(doctorId: string): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/doctors/${doctorId}`,
      { headers: this.authHeaders() }
    );
  }

  // Backoffice: List all consultations with filters (admin/staff only)
  listAllConsultations(filters?: {
    patientId?: number | string;
    status?: string;
    from?: string;
    to?: string;
  }): Observable<any[]> {
    let params = new HttpParams();
    if (filters?.patientId !== undefined && filters?.patientId !== null && filters?.patientId !== '') {
      params = params.set('patientId', String(filters.patientId));
    }
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters?.from) params = params.set('from', filters.from);
    if (filters?.to) params = params.set('to', filters.to);

    return this.http.get<any[]>(
      `${this.base}/api/clinical/consultations/backoffice/list`,
      { headers: this.authHeaders(), params }
    );
  }
}
