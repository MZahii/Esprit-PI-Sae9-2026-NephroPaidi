import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
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

interface MyAccountSettingsResponse {
  id?: number | null;
  keycloakId?: string | null;
  username?: string | null;
  email?: string | null;
  firstName?: string | null;
  lastName?: string | null;
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

  private guardianCacheKey(guardianUserId: number): string {
    return `np_guardian_patients_${guardianUserId}`;
  }

  private resolveGuardianUserId(): Observable<number | null> {
    const user = this.authStorage.getUser();
    const guardianUserId = Number(user?.userId || 0);
    if (Number.isFinite(guardianUserId) && guardianUserId > 0) {
      return of(guardianUserId);
    }

    if (!this.authStorage.getAccessToken()) {
      return of(null);
    }

    return this.http.get<MyAccountSettingsResponse>(
      `${this.base}/api/users/me/settings`,
      { headers: this.authHeaders() }
    ).pipe(
      map((account) => {
        const id = Number(account?.id || 0);
        return Number.isFinite(id) && id > 0 ? id : null;
      }),
      catchError(() => of(null))
    );
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
    return this.resolveGuardianUserId().pipe(
      switchMap((guardianUserId) => {
        if (!guardianUserId) {
          return of([]);
        }

        const cacheKey = this.guardianCacheKey(guardianUserId);
        const cached = sessionStorage.getItem(cacheKey);

        return this.http.get<GuardianPatientProfile[]>(
          `${this.base}/api/patients/guardian/${guardianUserId}`,
          { headers: this.authHeaders() }
        ).pipe(
          map((list) => list ?? []),
          tap((list) => sessionStorage.setItem(cacheKey, JSON.stringify(list))),
          catchError(() => {
            if (!cached) {
              return of([]);
            }
            try {
              const parsed = JSON.parse(cached);
              return of(Array.isArray(parsed) ? parsed as GuardianPatientProfile[] : []);
            } catch {
              return of([]);
            }
          })
        );
      })
    );
  }
}
