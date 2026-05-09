import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type PatientSex = 'MALE' | 'FEMALE';

interface GuardianUser {
  id: number;
  username: string;
  cin?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  sex?: string;
}

interface PatientProfile {
  id: number;
  guardianUserId: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  sex: PatientSex;
  bloodType?: string | null;
  allergies?: string | null;
  chronicConditions?: string | null;
  medicalNotes?: string | null;
}

@Component({
  selector: 'app-existing-guardian-patient',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './existing-guardian-patient.html',
  styleUrl: './existing-guardian-patient.scss'
})
export class ExistingGuardianPatient implements OnInit {
  loading = false;
  loadingGuardianPatients = false;
  successMessage = '';
  errorMessage = '';

  guardians: GuardianUser[] = [];
  selectedGuardianId: number | null = null;
  guardianSearch = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  linkedPatients: PatientProfile[] = [];

  form = {
    patientFirstName: '',
    patientLastName: '',
    patientDateOfBirth: '',
    patientSex: '' as PatientSex | '',
    patientBloodType: '',
    patientAllergies: '',
    patientChronicConditions: '',
    patientMedicalNotes: ''
  };
  maxPatientBirthDate = '';
  readonly bloodTypeOptions: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly allergyOptions: string[] = ['NONE', 'Penicillin', 'Peanuts', 'Milk', 'Egg', 'Seafood', 'Dust', 'Pollen', 'Latex'];
  readonly chronicConditionOptions: string[] = ['NONE', 'Asthma', 'Diabetes', 'Hypertension', 'Epilepsy', 'Heart Disease', 'Kidney Disease'];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  async ngOnInit(): Promise<void> {
    this.maxPatientBirthDate = new Date().toISOString().split('T')[0];
    await this.loadGuardians();
  }

  get filteredGuardians(): GuardianUser[] {
    const term = this.guardianSearch.trim().toLowerCase();
    if (!term) return this.guardians;
    return this.guardians.filter((guardian) =>
      this.guardianSearchTokens(guardian).some((value) => value.includes(term))
    );
  }

  get selectedGuardian(): GuardianUser | null {
    if (!this.selectedGuardianId) return null;
    return this.guardians.find(g => g.id === this.selectedGuardianId) ?? null;
  }

  get sortedLinkedPatients(): PatientProfile[] {
    const rows = [...this.linkedPatients];
    rows.sort((a, b) => {
      const left = `${a.firstName} ${a.lastName}`.toLowerCase();
      const right = `${b.firstName} ${b.lastName}`.toLowerCase();
      const result = left.localeCompare(right);
      return this.sortDirection === 'asc' ? result : -result;
    });
    return rows;
  }

  guardianName(guardian?: GuardianUser | null): string {
    if (!guardian) return '-';
    const full = `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim();
    return full || guardian.username;
  }

  patientName(patient: PatientProfile): string {
    return `${patient.firstName} ${patient.lastName}`.trim();
  }

  patientAge(dateOfBirth: string): number {
    const birth = new Date(dateOfBirth);
    if (Number.isNaN(birth.getTime())) return 0;
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDiff = now.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private showError(message: string): void {
    this.successMessage = '';
    this.errorMessage = message;
  }

  private showSuccess(message: string): void {
    this.errorMessage = '';
    this.successMessage = message;
  }

  private validateForm(): string | null {
    if (this.form.patientFirstName.trim().length < 3) return 'Patient first name must be at least 3 letters.';
    if (this.form.patientLastName.trim().length < 3) return 'Patient last name must be at least 3 letters.';
    if (!this.form.patientDateOfBirth) return 'Patient date of birth is required.';
    if (new Date(this.form.patientDateOfBirth).getTime() > Date.now()) return 'Patient date of birth cannot be in the future.';
    if (!this.form.patientSex) return 'Patient sex is required.';
    return null;
  }

  async loadGuardians(): Promise<void> {
    this.loading = true;
    this.showError('');

    try {
      const headers = await this.authHeaders();
      const guardians = await firstValueFrom(this.http.get<GuardianUser[]>(
        `${environment.apiBaseUrl}/api/users/guardians`,
        { headers }
      ));
      this.guardians = Array.isArray(guardians) ? guardians : [];
      if (!this.selectedGuardianId) {
        this.linkedPatients = [];
      }
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string; error?: string }; message?: string };
      this.showError(err?.error?.message || err?.error?.error || err?.message || 'Failed to load guardians.');
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  async onGuardianChange(): Promise<void> {
    this.successMessage = '';
    await this.loadLinkedPatients();
  }

  async loadLinkedPatients(): Promise<void> {
    if (!this.selectedGuardianId) {
      this.linkedPatients = [];
      return;
    }
    this.loadingGuardianPatients = true;
    this.showError('');
    try {
      const headers = await this.authHeaders();
      const response = await firstValueFrom(this.http.get<PatientProfile[]>(
        `${environment.apiBaseUrl}/api/patients/guardian/${this.selectedGuardianId}`,
        { headers }
      ));
      this.linkedPatients = Array.isArray(response) ? response : [];
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string; error?: string }; message?: string };
      this.showError(err?.error?.message || err?.error?.error || err?.message || 'Failed to load linked patients.');
      this.cdr.detectChanges();
    } finally {
      this.loadingGuardianPatients = false;
      this.cdr.detectChanges();
    }
  }

  async submit(): Promise<void> {
    this.showError('');
    if (!this.selectedGuardianId) {
      this.showError('Please select an existing guardian account.');
      return;
    }
    const validationError = this.validateForm();
    if (validationError) {
      this.showError(validationError);
      return;
    }
    if (this.loading) return;
    this.loading = true;

    try {
      const headers = await this.authHeaders();
      const payload = {
        guardianUserId: this.selectedGuardianId,
        firstName: this.form.patientFirstName.trim(),
        lastName: this.form.patientLastName.trim(),
        dateOfBirth: this.form.patientDateOfBirth,
        sex: this.form.patientSex,
        bloodType: this.form.patientBloodType.trim() || null,
        allergies: this.form.patientAllergies.trim() || null,
        chronicConditions: this.form.patientChronicConditions.trim() || null,
        medicalNotes: this.form.patientMedicalNotes.trim() || null
      };

      await firstValueFrom(this.http.post(
        `${environment.apiBaseUrl}/api/patients`,
        payload,
        { headers }
      ));

      this.showSuccess('New patient profile linked to existing guardian successfully.');
      this.form = {
        patientFirstName: '',
        patientLastName: '',
        patientDateOfBirth: '',
        patientSex: '',
        patientBloodType: '',
        patientAllergies: '',
        patientChronicConditions: '',
        patientMedicalNotes: ''
      };
      await this.loadLinkedPatients();
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { validationErrors?: Record<string, string>; message?: string; error?: string }; message?: string };
      if (err?.error?.validationErrors) {
        this.showError(Object.values(err.error.validationErrors).join(' | '));
      } else {
        this.showError(
          err?.error?.message ||
          err?.error?.error ||
          err?.message ||
          (error instanceof Error ? error.message : 'Failed to create linked patient profile.')
        );
      }
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private guardianSearchTokens(guardian: GuardianUser): string[] {
    return [
      guardian.username ?? '',
      guardian.firstName ?? '',
      guardian.lastName ?? '',
      this.guardianName(guardian),
      guardian.email ?? '',
      guardian.phone ?? '',
      guardian.cin ?? '',
      guardian.dateOfBirth ?? '',
      guardian.sex ?? ''
    ].map((value) => String(value).toLowerCase());
  }
}
