import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type StaffRole = 'DOCTOR' | 'NURSE' | 'SURGEON' | 'PHARMACIST' | 'RECEPTIONIST' | 'LAB_AGENT';
type Sex = 'MALE' | 'FEMALE' | '';
interface UserRow {
  id: number;
  username: string;
  role: string;
}

@Component({
  selector: 'app-create-staff',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-staff.html',
  styleUrl: './create-staff.scss'
})
export class CreateStaff {
  @Input() modalMode = false;
  @Output() closeModal = new EventEmitter<void>();

  loading = false;
  loadingUsers = false;
  successMessage = '';
  errorMessage = '';
  maxBirthDate = '';
  private existingStaffUsernames = new Set<string>();

  staffForm: {
    username: string;
    cin: string;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    dateOfBirth: string;
    sex: Sex;
    role: StaffRole | '';
  } = {
    username: '',
    cin: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    sex: '',
    role: '',
  };

  readonly roleOptions: StaffRole[] = [
    'DOCTOR',
    'NURSE',
    'SURGEON',
    'PHARMACIST',
    'RECEPTIONIST',
    'LAB_AGENT'
  ];

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.maxBirthDate = this.computeMaxBirthDateForAdult();
    this.loadExistingStaffUsernames();
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
    const username = this.staffForm.username.trim();
    const cin = this.staffForm.cin.trim();
    const firstName = this.staffForm.firstName.trim();
    const lastName = this.staffForm.lastName.trim();
    const phoneLocal = this.normalizePhone(this.staffForm.phone);

    if (username.length < 3) return 'Username must contain at least 3 characters.';
    if (this.existingStaffUsernames.has(this.normalizeUsername(username))) return 'Username already exists in staff accounts.';
    if (!/^\d{8,9}$/.test(cin)) return 'CIN must be numeric with 8 to 9 digits.';
    if (firstName.length < 3) return 'First name must contain at least 3 letters.';
    if (lastName.length < 3) return 'Last name must contain at least 3 letters.';
    if (!/^[2459]\d{7}$/.test(phoneLocal)) return 'Phone must start with 2, 4, 5, or 9 and contain exactly 8 digits.';
    if (!this.isAdult(this.staffForm.dateOfBirth)) return 'Staff account age must be at least 18 years.';
    return null;
  }

  async loadExistingStaffUsernames(): Promise<void> {
    this.loadingUsers = true;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(
        this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers })
      );
      const users = Array.isArray(response) ? response : [];
      const staffRoles: StaffRole[] = ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];
      this.existingStaffUsernames = new Set(
        users
          .filter((u) => staffRoles.includes(u.role as StaffRole))
          .map((u) => this.normalizeUsername(u.username))
      );
    } catch {
      // Backend still validates uniqueness; this is only a UX pre-check.
    } finally {
      this.loadingUsers = false;
      this.cdr.detectChanges();
    }
  }

  async createStaff(): Promise<void> {
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

    const phoneLocal = this.normalizePhone(this.staffForm.phone);
    const payload = {
      username: this.staffForm.username.trim(),
      cin: this.staffForm.cin.trim(),
      firstName: this.staffForm.firstName.trim(),
      lastName: this.staffForm.lastName.trim(),
      email: this.staffForm.email.trim(),
      phone: this.formatPhoneForApi(phoneLocal),
      dateOfBirth: this.staffForm.dateOfBirth,
      sex: this.staffForm.sex,
      role: this.staffForm.role,
    };

    this.loading = true;

    try {
      const token = await getValidToken();

      await firstValueFrom(this.http.post(`${environment.apiBaseUrl}/api/users/staff`, payload, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        })
      }));

      this.showSuccess('Staff account created successfully.');
      this.staffForm = {
        username: '',
        cin: '',
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        dateOfBirth: '',
        sex: '',
        role: '',
      };
      this.cdr.detectChanges();

      setTimeout(() => {
        if (this.modalMode) {
          this.closeModal.emit();
          return;
        }

        this.router.navigate(['/backoffice/staff']);
      }, 1500);
    } catch (error: unknown) {
      const err = error as {
        status?: number;
        error?: { validationErrors?: Record<string, string>; message?: string; error?: string };
        message?: string;
      };

      if (err?.status === 401 || err?.status === 403) {
        this.showError('Your session is not valid anymore. Please login again.');
        this.cdr.detectChanges();
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1200);
        return;
      }

      if (err?.error?.validationErrors) {
        this.showError(Object.values(err.error.validationErrors).join(' | '));
        this.cdr.detectChanges();
        return;
      }

      this.showError(
        err?.error?.message ||
        err?.error?.error ||
        err?.message ||
        (error instanceof Error ? error.message : 'Authentication problem. Please login again.')
      );
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  cancel(): void {
    if (this.modalMode) {
      this.closeModal.emit();
      return;
    }

    this.router.navigate(['/backoffice/staff']);
  }
}
