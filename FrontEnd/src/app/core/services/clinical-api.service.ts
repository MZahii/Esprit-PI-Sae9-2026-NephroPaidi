import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStorageService } from '../auth/auth-storage.service';

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

  getPatient(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/patients/${id}`,
      { headers: this.doctorHeaders() }
    );
  }

  listMyConsultations(filters?: { patientQuery?: string; status?: string }): Observable<any[]> {
    let params = new HttpParams();
    if (filters?.patientQuery) params = params.set('patientQuery', filters.patientQuery);
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    return this.http.get<any[]>(
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
    );
  }

  listDoctors(limit = 10): Observable<DoctorSearchResult[]> {
    return this.listStaffByRoles(['DOCTOR'], limit);
  }

  listSurgeons(limit = 10): Observable<DoctorSearchResult[]> {
    return this.listStaffByRoles(['SURGEON'], limit);
  }

  private listStaffByRoles(roles: string[], limit: number): Observable<DoctorSearchResult[]> {
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

  getConsultation(id: string): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/consultations/${id}`,
      { headers: this.authHeaders() }
    );
  }

  getConsultationOutcome(id: string): Observable<any> {
    return this.http.get<any>(
      `${this.base}/api/clinical/consultations/${id}/outcomes`,
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
