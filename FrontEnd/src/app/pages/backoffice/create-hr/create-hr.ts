import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

interface UserRow {
  id: number;
  username: string;
  role: string;
}

@Component({
  selector: 'app-create-hr',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-hr.html',
  styleUrl: './create-hr.scss'
})
export class CreateHr {
  loading = false;
  loadingUsers = false;
  successMessage = '';
  errorMessage = '';
  maxBirthDate = '';
  private existingHrUsernames = new Set<string>();

  hrForm = {
    username: '',
    cin: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    sex: ''
  };

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.maxBirthDate = this.computeMaxBirthDateForAdult();
    this.loadExistingHrUsernames();
  }

  private showError(message: string): void {
    this.successMessage = '';
    this.errorMessage = message;
  }

  private showSuccess(message: string): void {
    this.errorMessage = '';
    this.successMessage = message;
  }

  private normalizeUsername(username: string): string {
    return username.trim().toLowerCase();
  }

  private normalizePhone(phone: string): string {
    return phone.replace(/\D/g, '');
  }

  private formatPhoneForApi(localPhone: string): string {
    return `+216${localPhone}`;
  }

  private isAdult(dateOfBirth: string): boolean {
    if (!dateOfBirth) return false;
    const birth = new Date(dateOfBirth);
    if (Number.isNaN(birth.getTime())) return false;

    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDiff = now.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
      age--;
    }
    return age >= 18;
  }

  private computeMaxBirthDateForAdult(): string {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 18);
    return date.toISOString().split('T')[0];
  }

  private validateForm(): string | null {
    const username = this.hrForm.username.trim();
    const cin = this.hrForm.cin.trim();
    const firstName = this.hrForm.firstName.trim();
    const lastName = this.hrForm.lastName.trim();
    const phoneLocal = this.normalizePhone(this.hrForm.phone);

    if (username.length < 3) {
      return 'Username must contain at least 3 characters.';
    }

    if (this.existingHrUsernames.has(this.normalizeUsername(username))) {
      return 'Username already exists in HR accounts.';
    }

    if (!/^\d{8,9}$/.test(cin)) {
      return 'CIN must be numeric with 8 to 9 digits.';
    }

    if (firstName.length < 3) {
      return 'First name must contain at least 3 letters.';
    }

    if (lastName.length < 3) {
      return 'Last name must contain at least 3 letters.';
    }

    if (!/^[2459]\d{7}$/.test(phoneLocal)) {
      return 'Phone must start with 2, 4, 5, or 9 and contain exactly 8 digits.';
    }

    if (!this.isAdult(this.hrForm.dateOfBirth)) {
      return 'HR account age must be at least 18 years.';
    }

    return null;
  }

  async loadExistingHrUsernames(): Promise<void> {
    this.loadingUsers = true;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(
        this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers })
      );
      const users = Array.isArray(response) ? response : [];
      this.existingHrUsernames = new Set(
        users
          .filter((u) => u.role === 'HR')
          .map((u) => this.normalizeUsername(u.username))
      );
    } catch {
      // Backend still validates uniqueness; this is only a UX pre-check.
    } finally {
      this.loadingUsers = false;
      this.cdr.detectChanges();
    }
  }

  async createHr(): Promise<void> {
    this.showError('');

    if (this.loading) {
      return;
    }

    const validationError = this.validateForm();
    if (validationError) {
      this.showError(validationError);
      this.cdr.detectChanges();
      return;
    }

    const normalizedPhone = this.normalizePhone(this.hrForm.phone);
    const payload = {
      username: this.hrForm.username.trim(),
      cin: this.hrForm.cin.trim(),
      firstName: this.hrForm.firstName.trim(),
      lastName: this.hrForm.lastName.trim(),
      email: this.hrForm.email.trim(),
      phone: this.formatPhoneForApi(normalizedPhone),
      dateOfBirth: this.hrForm.dateOfBirth,
      sex: this.hrForm.sex
    };

    this.loading = true;

    try {
      const token = await getValidToken();

      await firstValueFrom(this.http.post(`${environment.apiBaseUrl}/api/users/hr`, payload, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        })
      }));

      this.showSuccess('HR account created successfully.');

      this.hrForm = {
        username: '',
        cin: '',
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        dateOfBirth: '',
        sex: ''
      };
      this.cdr.detectChanges();

      setTimeout(() => {
        this.router.navigate(['/backoffice/hr-list']);
      }, 1500);
    } catch (error: unknown) {
      this.showError(this.resolveApiError(error));
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private resolveApiError(error: unknown): string {
    const fallback = 'Authentication problem. Please login again.';
    const err = error as HttpErrorResponse;

    if (!err) {
      return fallback;
    }

    const body = typeof err.error === 'string'
      ? this.tryParseJson(err.error)
      : err.error;

    if (body?.validationErrors && typeof body.validationErrors === 'object') {
      return Object.values(body.validationErrors).join(' | ');
    }

    return body?.message || body?.error || err.message || fallback;
  }

  private tryParseJson(value: string): any {
    try {
      return JSON.parse(value);
    } catch {
      return { message: value };
    }
  }
}
