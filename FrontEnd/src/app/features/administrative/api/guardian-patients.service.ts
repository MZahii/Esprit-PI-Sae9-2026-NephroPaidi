import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';

export interface GuardianPatientProfile {
  id: number;
  guardianUserId: number;
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  sex?: string | null;
  bloodType?: string | null;
  allergies?: string | null;
  chronicConditions?: string | null;
  medicalNotes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class GuardianPatientsService {
  private base = (window as any).__env?.API_BASE || environment.apiBaseUrl;

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService
  ) {}

  private authHeaders(): HttpHeaders {
    const token = this.authStorage.getAccessToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return new HttpHeaders(headers);
  }

  getGuardianPatientIds(): Observable<number[]> {
    return this.getGuardianPatients().pipe(
      map(list => (list ?? [])
        .map(item => Number(item?.id))
        .filter(id => Number.isFinite(id))),
      catchError(() => of([]))
    );
  }

  getGuardianPatients(): Observable<GuardianPatientProfile[]> {
    const user = this.authStorage.getUser();
    const guardianUserId = Number(user?.userId || 0);

    if (!guardianUserId) {
      return of([]);
    }

    return this.http.get<GuardianPatientProfile[]>(
      `${this.base}/api/patients/guardian/${guardianUserId}`,
      { headers: this.authHeaders() }
    ).pipe(
      map(list => list ?? []),
      catchError(() => of([]))
    );
  }
}
