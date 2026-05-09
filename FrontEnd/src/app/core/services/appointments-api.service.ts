import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type AppointmentStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface AppointmentRequestItem {
  id: string;
  patientId: number;
  guardianKeycloakId: string;
  requestedDate?: string | null;
  reason: string;
  status: AppointmentStatus;
  scheduledDate?: string | null;
  receptionistNotes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PatientDirectoryItem {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
}

@Injectable({
  providedIn: 'root'
})
export class AppointmentsApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/appointments`;

  constructor(private http: HttpClient) {}

  createRequest(payload: { patientId: number; requestedDate?: string | null; reason: string }): Observable<AppointmentRequestItem> {
    return this.http.post<AppointmentRequestItem>(`${this.baseUrl}/requests`, payload);
  }

  getMyRequests(): Observable<AppointmentRequestItem[]> {
    return this.http.get<AppointmentRequestItem[]>(`${this.baseUrl}/my`);
  }

  getRequests(status?: AppointmentStatus): Observable<AppointmentRequestItem[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<AppointmentRequestItem[]>(`${this.baseUrl}/requests`, {
      params
    });
  }

  approve(id: string, payload: { scheduledDate: string; receptionistNotes?: string | null }): Observable<AppointmentRequestItem> {
    return this.http.post<AppointmentRequestItem>(`${this.baseUrl}/requests/${id}/approve`, payload);
  }

  reject(id: string, payload: { receptionistNotes?: string | null }): Observable<AppointmentRequestItem> {
    return this.http.post<AppointmentRequestItem>(`${this.baseUrl}/requests/${id}/reject`, payload);
  }

  cancel(id: string): Observable<AppointmentRequestItem> {
    return this.http.post<AppointmentRequestItem>(`${this.baseUrl}/requests/${id}/cancel`, {});
  }

  getPatientsDirectory(): Observable<PatientDirectoryItem[]> {
    return this.http.get<PatientDirectoryItem[]>(`${environment.apiBaseUrl}/api/patients`);
  }
}
