import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

interface PatientProfileRow {
  id: number;
  guardianUserId: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  sex: 'MALE' | 'FEMALE' | string;
  bloodType?: string | null;
  allergies?: string | null;
  chronicConditions?: string | null;
  medicalNotes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

@Component({
  selector: 'app-frontoffice-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './frontoffice-home.html',
  styleUrl: './frontoffice-home.scss'
})
export class FrontofficeHomeComponent implements OnInit {
  loadingPatients = false;
  patientsError = '';
  patientProfiles: PatientProfileRow[] = [];
  patientSearchTerm = '';
  patientSortBy: 'name' | 'dob' | 'newest' = 'newest';
  patientSortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private authStorage: AuthStorageService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadGuardianPatients();
  }

  get user(): any | null {
    return this.authStorage.getUser();
  }

  get displayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }

    return this.user?.username ?? 'Guardian';
  }

  get displayEmail(): string {
    return this.user?.email ?? '';
  }

  get guardianUserId(): number | null {
    const id = Number(this.user?.userId);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  fullPatientName(patient: PatientProfileRow): string {
    return `${patient.firstName} ${patient.lastName}`.trim();
  }

  get filteredPatients(): PatientProfileRow[] {
    const term = this.patientSearchTerm.trim().toLowerCase();
    const filtered = this.patientProfiles.filter((patient) => {
      if (!term) return true;
      const text = [
        this.fullPatientName(patient),
        patient.sex ?? '',
        patient.bloodType ?? '',
        patient.dateOfBirth ?? ''
      ].join(' ').toLowerCase();
      return text.includes(term);
    });

    filtered.sort((a, b) => this.comparePatients(a, b));
    return filtered;
  }

  trackByPatientId(_: number, patient: PatientProfileRow): number {
    return patient.id;
  }

  onPatientFiltersChanged(): void {
    // Triggers template update for immediate first-click behavior.
    this.patientSearchTerm = this.patientSearchTerm;
  }

  patientAge(dateOfBirth?: string): number | null {
    if (!dateOfBirth) return null;
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;

    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age >= 0 ? age : null;
  }

  private async loadGuardianPatients(): Promise<void> {
    const guardianId = this.guardianUserId;
    if (!guardianId) {
      this.patientsError = 'Guardian user id is missing. Please login again.';
      this.cdr.detectChanges();
      return;
    }

    const cacheKey = `np_guardian_patients_${guardianId}`;
    const cached = sessionStorage.getItem(cacheKey);
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed)) {
          this.patientProfiles = parsed;
        }
      } catch {
        // ignore invalid cache
      }
    }

    this.loadingPatients = true;
    this.patientsError = '';

    try {
      const token = await this.resolveAccessToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(this.http.get<PatientProfileRow[] | unknown>(
        `${environment.apiBaseUrl}/api/patients/guardian/${guardianId}`,
        { headers }
      ));
      this.patientProfiles = Array.isArray(response) ? response : [];
      sessionStorage.setItem(cacheKey, JSON.stringify(this.patientProfiles));
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.patientsError = err?.error?.message || err?.message || 'Failed to load patient profiles.';
    } finally {
      this.loadingPatients = false;
      this.cdr.detectChanges();
    }
  }

  private async resolveAccessToken(): Promise<string> {
    const stored = this.authStorage.getAccessToken();
    if (stored) return stored;

    return Promise.race([
      getValidToken(),
      new Promise<string>((_, reject) =>
        setTimeout(() => reject(new Error('Session timeout while preparing patient request.')), 4000)
      )
    ]);
  }

  private comparePatients(a: PatientProfileRow, b: PatientProfileRow): number {
    let result = 0;

    if (this.patientSortBy === 'name') {
      const aName = this.fullPatientName(a).toLowerCase();
      const bName = this.fullPatientName(b).toLowerCase();
      result = aName.localeCompare(bName);
    } else if (this.patientSortBy === 'dob') {
      const aDob = this.safeTimestamp(a.dateOfBirth);
      const bDob = this.safeTimestamp(b.dateOfBirth);
      result = aDob - bDob;
    } else {
      const aNewest = this.safeTimestamp(a.createdAt ?? a.updatedAt ?? a.dateOfBirth) || a.id;
      const bNewest = this.safeTimestamp(b.createdAt ?? b.updatedAt ?? b.dateOfBirth) || b.id;
      result = aNewest - bNewest;
    }

    return this.patientSortDirection === 'asc' ? result : (result * -1);
  }

  private safeTimestamp(value?: string | null): number {
    if (!value) return 0;
    const time = new Date(value).getTime();
    return Number.isNaN(time) ? 0 : time;
  }
}
