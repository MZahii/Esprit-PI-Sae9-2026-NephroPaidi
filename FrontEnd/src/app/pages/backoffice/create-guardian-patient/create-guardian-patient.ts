import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

interface GuardianCreateResponse {
  id: number;
  username: string;
  email: string;
  role: string;
}

@Component({
  selector: 'app-create-guardian-patient',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-guardian-patient.html',
  styleUrl: './create-guardian-patient.scss'
})
export class CreateGuardianPatient {
  loading = false;
  successMessage = '';
  errorMessage = '';
  maxGuardianBirthDate = '';
  maxPatientBirthDate = '';
  readonly bloodTypeOptions: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly allergyOptions: string[] = ['NONE', 'Penicillin', 'Peanuts', 'Milk', 'Egg', 'Seafood', 'Dust', 'Pollen', 'Latex'];
  readonly chronicConditionOptions: string[] = ['NONE', 'Asthma', 'Diabetes', 'Hypertension', 'Epilepsy', 'Heart Disease', 'Kidney Disease'];

  form = {
    guardianUsername: '',
    guardianEmail: '',
    guardianCin: '',
    guardianFirstName: '',
    guardianLastName: '',
    guardianPhone: '',
    guardianDateOfBirth: '',
    guardianSex: '' as 'MALE' | 'FEMALE' | '',
    patientFirstName: '',
    patientLastName: '',
    patientDateOfBirth: '',
    patientSex: '' as 'MALE' | 'FEMALE' | '',
    patientBloodType: '',
    patientAllergies: '',
    patientChronicConditions: '',
    patientMedicalNotes: ''
  };

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.maxGuardianBirthDate = this.computeMaxBirthDate(18);
    this.maxPatientBirthDate = this.computeMaxBirthDate(0);
  }

  private showError(message: string): void {
    this.successMessage = '';
    this.errorMessage = message;
  }

  private showSuccess(message: string): void {
    this.errorMessage = '';
    this.successMessage = message;
  }

  private computeMaxBirthDate(minYears: number): string {
    const date = new Date();
    date.setFullYear(date.getFullYear() - minYears);
    return date.toISOString().split('T')[0];
  }

  private normalizePhone(phone: string): string {
    return phone.replace(/\D/g, '');
  }

  private formatPhoneForApi(phoneLocal: string): string {
    return `+216${phoneLocal}`;
  }

  private isAdult(dateOfBirth: string): boolean {
    if (!dateOfBirth) return false;
    const birth = new Date(dateOfBirth);
    if (Number.isNaN(birth.getTime())) return false;
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDiff = now.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) age--;
    return age >= 18;
  }

  private validateForm(): string | null {
    const guardianPhone = this.normalizePhone(this.form.guardianPhone);
    if (this.form.guardianUsername.trim().length < 3) return 'Guardian username must be at least 3 characters.';
    if (!/^\d{8,9}$/.test(this.form.guardianCin.trim())) return 'Guardian CIN must be 8 to 9 digits.';
    if (this.form.guardianFirstName.trim().length < 3) return 'Guardian first name must be at least 3 letters.';
    if (this.form.guardianLastName.trim().length < 3) return 'Guardian last name must be at least 3 letters.';
    if (!/^[2459]\d{7}$/.test(guardianPhone)) return 'Guardian phone must start with 2,4,5,9 and have 8 digits.';
    if (!this.isAdult(this.form.guardianDateOfBirth)) return 'Guardian must be at least 18 years old.';
    if (this.form.patientFirstName.trim().length < 3) return 'Patient first name must be at least 3 letters.';
    if (this.form.patientLastName.trim().length < 3) return 'Patient last name must be at least 3 letters.';
    if (!this.form.patientDateOfBirth) return 'Patient date of birth is required.';
    if (new Date(this.form.patientDateOfBirth).getTime() > Date.now()) return 'Patient date of birth cannot be in the future.';
    return null;
  }

  async submit(): Promise<void> {
    this.showError('');

    if (this.loading) return;

    const validationError = this.validateForm();
    if (validationError) {
      this.showError(validationError);
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      });

      const guardianPayload = {
        username: this.form.guardianUsername.trim(),
        email: this.form.guardianEmail.trim(),
        cin: this.form.guardianCin.trim(),
        firstName: this.form.guardianFirstName.trim(),
        lastName: this.form.guardianLastName.trim(),
        phone: this.formatPhoneForApi(this.normalizePhone(this.form.guardianPhone)),
        dateOfBirth: this.form.guardianDateOfBirth,
        sex: this.form.guardianSex
      };

      const guardian = await firstValueFrom(this.http.post<GuardianCreateResponse>(
        `${environment.apiBaseUrl}/api/users/guardian`,
        guardianPayload,
        { headers }
      ));

      const patientPayload = {
        guardianUserId: guardian.id,
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
        patientPayload,
        { headers }
      ));

      this.showSuccess('Guardian and patient created successfully.');

      this.form = {
        guardianUsername: '',
        guardianEmail: '',
        guardianCin: '',
        guardianFirstName: '',
        guardianLastName: '',
        guardianPhone: '',
        guardianDateOfBirth: '',
        guardianSex: '',
        patientFirstName: '',
        patientLastName: '',
        patientDateOfBirth: '',
        patientSex: '',
        patientBloodType: '',
        patientAllergies: '',
        patientChronicConditions: '',
        patientMedicalNotes: ''
      };
      this.cdr.detectChanges();

      setTimeout(() => {
        this.router.navigate(['/backoffice/patients']);
      }, 1500);
    } catch (error: unknown) {
      const err = error as { error?: { validationErrors?: Record<string, string>; message?: string; error?: string }; message?: string };
      if (err?.error?.validationErrors) {
        this.showError(Object.values(err.error.validationErrors).join(' | '));
      } else {
        this.showError(
          err?.error?.message ||
          err?.error?.error ||
          err?.message ||
          (error instanceof Error ? error.message : 'Authentication problem. Please login again.')
        );
      }
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}
