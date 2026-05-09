import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
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
  selector: 'app-frontoffice-patient-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './frontoffice-patient-details.html',
  styleUrl: './frontoffice-patient-details.scss'
})
export class FrontofficePatientDetailsComponent implements OnInit {
  loading = false;
  errorMessage = '';
  patient?: PatientProfileRow;

  constructor(
    private authStorage: AuthStorageService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPatientDetails();
  }

  get guardianUserId(): number | null {
    const id = Number(this.authStorage.getUser()?.userId);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get patientId(): number | null {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get fullName(): string {
    if (!this.patient) return '';
    return `${this.patient.firstName} ${this.patient.lastName}`.trim();
  }

  get age(): number | null {
    if (!this.patient?.dateOfBirth) return null;
    const dob = new Date(this.patient.dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;
    const today = new Date();
    let years = today.getFullYear() - dob.getFullYear();
    const monthDiff = today.getMonth() - dob.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
      years--;
    }
    return years >= 0 ? years : null;
  }

  private async loadPatientDetails(): Promise<void> {
    const guardianId = this.guardianUserId;
    const patientId = this.patientId;
    if (!guardianId || !patientId) {
      this.errorMessage = 'Invalid patient details link.';
      this.cdr.detectChanges();
      return;
    }

    const cacheKey = `np_guardian_patients_${guardianId}`;
    const cached = sessionStorage.getItem(cacheKey);
    if (cached) {
      try {
        const list = JSON.parse(cached);
        if (Array.isArray(list)) {
          const cachedPatient = list.find((p: PatientProfileRow) => p.id === patientId);
          if (cachedPatient) {
            this.patient = cachedPatient;
          }
        }
      } catch {
        // ignore cache parse issues
      }
    }

    this.loading = true;
    this.errorMessage = '';
    if (!this.patient) {
      this.patient = undefined;
    }

    try {
      const token = await this.resolveAccessToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const all = await firstValueFrom(this.http.get<PatientProfileRow[] | unknown>(
        `${environment.apiBaseUrl}/api/patients/guardian/${guardianId}`,
        { headers }
      ));
      const list = Array.isArray(all) ? all : [];
      sessionStorage.setItem(cacheKey, JSON.stringify(list));
      this.patient = list.find((p) => p.id === patientId);
      if (!this.patient) {
        this.errorMessage = 'Patient profile not found or not linked to your guardian account.';
      }
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to load patient details.';
    } finally {
      this.loading = false;
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
}
