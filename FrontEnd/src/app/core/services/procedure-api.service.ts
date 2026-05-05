import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DialysisPlan {
  id: number;
  patientId: string;
  firstName: string;
  lastName: string;
  doctorId: string;
  dialysisType: string;
  sessionsPerWeek: number;
  sessionDurationMinutes: number;
  startDate: string;
  endDate: string | null;
  daysOfWeek: string;
  bloodFlowRate: number | null;
  dialysateFlowRate: number | null;
  ultrafiltrationGoal: number | null;
  dialysisCenterId: string | null;
  roomNumber: string | null;
  machineId: string | null;
  status: string;
}

export interface SurgicalCase {
  id: number;
  patientId: string;
  firstName: string;
  lastName: string;
  age: number;
  gender: string;
  medicalRecordNumber: string;
  surgeryType: string;
  procedureName: string;
  surgeryCategory: string;
  urgencyLevel: string;
  surgeonId: string;
  assistantSurgeonId: string | null;
  anesthesiologistId: string | null;
  nurseTeam: string | null;
  scheduledDate: string;
  scheduledStartTime: string;
  estimatedDurationMinutes: number;
  operatingRoom: string | null;
  status: string;
  offerStatus: string;
}

export interface DialysisSession {
  id: number;
  planId: number;
  sessionDate: string;
  notes: string;
}

export interface DialysisOutcome {
  id: number;
  sessionId: number;
  validated: boolean;
  summary: string;
}

export interface DialysisPrescription {
  id: number;
  planId: number;
  details: string;
}

export interface PreOpAssessment {
  id: number;
  surgicalCaseId: number;
  notes: string;
}

export interface PostOpObservation {
  id: number;
  surgicalCaseId: number;
  notes: string;
}

export interface Complication {
  id: number;
  surgicalCaseId: number;
  description: string;
}

export interface CareTask {
  id: number;
  surgicalCaseId: number;
  title: string;
  done: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ProcedureApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getDialysisPlans(): Observable<DialysisPlan[]> {
    return this.http.get<DialysisPlan[]>(`${this.baseUrl}/api/procedures/dialysis/plans`);
  }

  createDialysisPlan(payload: {
    patientId: string;
    firstName: string;
    lastName: string;
    doctorId: string;
    dialysisType: string;
    sessionsPerWeek: number;
    sessionDurationMinutes: number;
    startDate: string;
    endDate?: string | null;
    daysOfWeek: string;
    bloodFlowRate?: number | null;
    dialysateFlowRate?: number | null;
    ultrafiltrationGoal?: number | null;
    dialysisCenterId?: string | null;
    roomNumber?: string | null;
    machineId?: string | null;
    status: string;
  }): Observable<DialysisPlan> {
    return this.http.post<DialysisPlan>(`${this.baseUrl}/api/procedures/dialysis/plans`, payload);
  }

  downloadDialysisPlanSummaryPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/procedures/dialysis/plans/${id}/summary-pdf`, {
      responseType: 'blob'
    });
  }

  updateDialysisPlan(id: number, payload: {
    firstName: string;
    lastName: string;
    doctorId: string;
    dialysisType: string;
    sessionsPerWeek: number;
    sessionDurationMinutes: number;
    startDate: string;
    endDate?: string | null;
    daysOfWeek: string;
    bloodFlowRate?: number | null;
    dialysateFlowRate?: number | null;
    ultrafiltrationGoal?: number | null;
    dialysisCenterId?: string | null;
    roomNumber?: string | null;
    machineId?: string | null;
    status: string;
  }): Observable<DialysisPlan> {
    return this.http.put<DialysisPlan>(`${this.baseUrl}/api/procedures/dialysis/plans/${id}`, payload);
  }

  getDialysisSessions(): Observable<DialysisSession[]> {
    return this.http.get<DialysisSession[]>(`${this.baseUrl}/api/procedures/dialysis/sessions`);
  }

  createDialysisSession(payload: {
    planId: number;
    sessionDate: string;
    notes: string;
  }): Observable<DialysisSession> {
    return this.http.post<DialysisSession>(`${this.baseUrl}/api/procedures/dialysis/sessions`, payload);
  }

  generateDialysisSessionsFromPlan(planId: number): Observable<DialysisSession[]> {
    return this.http.post<DialysisSession[]>(`${this.baseUrl}/api/procedures/dialysis/sessions/generate/plan/${planId}`, {});
  }

  getDialysisOutcomes(): Observable<DialysisOutcome[]> {
    return this.http.get<DialysisOutcome[]>(`${this.baseUrl}/api/procedures/dialysis/outcomes`);
  }

  createDialysisOutcomeForSession(sessionId: number): Observable<DialysisOutcome> {
    return this.http.post<DialysisOutcome>(
      `${this.baseUrl}/api/procedures/dialysis/outcomes/session/${sessionId}`,
      {}
    );
  }

  validateDialysisOutcome(
    id: number,
    payload: { validated: boolean; summary: string }
  ): Observable<DialysisOutcome> {
    return this.http.put<DialysisOutcome>(
      `${this.baseUrl}/api/procedures/dialysis/outcomes/${id}/validate`,
      payload
    );
  }

  getDialysisPrescriptions(): Observable<DialysisPrescription[]> {
    return this.http.get<DialysisPrescription[]>(`${this.baseUrl}/api/procedures/dialysis/prescriptions`);
  }

  createDialysisPrescription(payload: {
    planId: number;
    details: string;
  }): Observable<DialysisPrescription> {
    return this.http.post<DialysisPrescription>(
      `${this.baseUrl}/api/procedures/dialysis/prescriptions`,
      payload
    );
  }

  updateDialysisPrescription(
    id: number,
    payload: { details: string }
  ): Observable<DialysisPrescription> {
    return this.http.put<DialysisPrescription>(
      `${this.baseUrl}/api/procedures/dialysis/prescriptions/${id}`,
      payload
    );
  }

  getSurgicalCases(): Observable<SurgicalCase[]> {
    return this.http.get<SurgicalCase[]>(`${this.baseUrl}/api/procedures/surgical/cases`);
  }

  createSurgicalCase(payload: {
    patientId: string;
    firstName: string;
    lastName: string;
    age: number;
    gender: string;
    medicalRecordNumber: string;
    surgeryType: string;
    procedureName: string;
    surgeryCategory: string;
    urgencyLevel: string;
    surgeonId: string;
    assistantSurgeonId?: string | null;
    anesthesiologistId?: string | null;
    nurseTeam?: string | null;
    scheduledDate: string;
    scheduledStartTime: string;
    estimatedDurationMinutes: number;
    operatingRoom?: string | null;
    status: string;
  }): Observable<SurgicalCase> {
    return this.http.post<SurgicalCase>(`${this.baseUrl}/api/procedures/surgical/cases`, payload);
  }

  downloadSurgicalCaseSummaryPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/procedures/surgical/cases/${id}/summary-pdf`, {
      responseType: 'blob'
    });
  }

  updateSurgicalCase(id: number, payload: { status: string }): Observable<SurgicalCase> {
    return this.http.put<SurgicalCase>(`${this.baseUrl}/api/procedures/surgical/cases/${id}`, payload);
  }

  updateTransplantOffer(id: number, payload: { offerStatus: string }): Observable<SurgicalCase> {
    return this.http.put<SurgicalCase>(`${this.baseUrl}/api/procedures/surgical/cases/${id}/offer`, payload);
  }

  getPreOpAssessments(): Observable<PreOpAssessment[]> {
    return this.http.get<PreOpAssessment[]>(`${this.baseUrl}/api/procedures/surgical/preop-assessments`);
  }

  createPreOpAssessment(payload: { surgicalCaseId: number; notes: string }): Observable<PreOpAssessment> {
    return this.http.post<PreOpAssessment>(`${this.baseUrl}/api/procedures/surgical/preop-assessments`, payload);
  }

  updatePreOpAssessment(id: number, payload: { notes: string }): Observable<PreOpAssessment> {
    return this.http.put<PreOpAssessment>(`${this.baseUrl}/api/procedures/surgical/preop-assessments/${id}`, payload);
  }

  getPostOpObservations(): Observable<PostOpObservation[]> {
    return this.http.get<PostOpObservation[]>(`${this.baseUrl}/api/procedures/surgical/postop-observations`);
  }

  createPostOpObservation(payload: { surgicalCaseId: number; notes: string }): Observable<PostOpObservation> {
    return this.http.post<PostOpObservation>(`${this.baseUrl}/api/procedures/surgical/postop-observations`, payload);
  }

  updatePostOpObservation(id: number, payload: { notes: string }): Observable<PostOpObservation> {
    return this.http.put<PostOpObservation>(`${this.baseUrl}/api/procedures/surgical/postop-observations/${id}`, payload);
  }

  getComplications(): Observable<Complication[]> {
    return this.http.get<Complication[]>(`${this.baseUrl}/api/procedures/surgical/complications`);
  }

  createComplication(payload: { surgicalCaseId: number; description: string }): Observable<Complication> {
    return this.http.post<Complication>(`${this.baseUrl}/api/procedures/surgical/complications`, payload);
  }

  updateComplication(id: number, payload: { description: string }): Observable<Complication> {
    return this.http.put<Complication>(`${this.baseUrl}/api/procedures/surgical/complications/${id}`, payload);
  }

  getCareTasks(): Observable<CareTask[]> {
    return this.http.get<CareTask[]>(`${this.baseUrl}/api/procedures/surgical/care-tasks`);
  }

  createCareTask(payload: { surgicalCaseId: number; title: string }): Observable<CareTask> {
    return this.http.post<CareTask>(`${this.baseUrl}/api/procedures/surgical/care-tasks`, payload);
  }

  updateCareTask(id: number, payload: { title: string; done: boolean }): Observable<CareTask> {
    return this.http.put<CareTask>(`${this.baseUrl}/api/procedures/surgical/care-tasks/${id}`, payload);
  }
}
