import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { CountUpDirective } from '../../../shared/directives/count-up.directive';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

type PatientSex = 'MALE' | 'FEMALE';

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
  createdAt?: string | null;
}

interface GuardianUser {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

type AgeGroup = 'ALL' | 'BABY' | 'CHILD' | 'TEEN';

@Component({
  selector: 'app-patients-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CountUpDirective],
  templateUrl: './patients-list.html',
  styleUrl: './patients-list.scss'
})
export class PatientsList implements OnInit {
  loading = false;
  errorMessage = '';
  role = '';

  searchTerm = '';
  sexFilter: PatientSex | 'ALL' = 'ALL';
  bloodTypeFilter: string | 'ALL' = 'ALL';
  ageGroupFilter: AgeGroup = 'ALL';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageSize = 5;
  currentPage = 1;
  readonly pageSizeOptions: number[] = [5, 10, 20];
  detailsPatient: PatientProfile | null = null;
  editingPatientId: number | null = null;
  editMode: 'PATIENT' | 'GUARDIAN' = 'PATIENT';
  savingEdit = false;

  allPatients: PatientProfile[] = [];
  guardianMap: Record<number, GuardianUser> = {};
  patientEditForm = {
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    sex: '' as PatientSex | '',
    bloodType: '',
    allergies: '',
    chronicConditions: '',
    medicalNotes: ''
  };
  readonly bloodTypeEditOptions: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', '-'];
  readonly allergyEditOptions: string[] = ['NONE', 'Penicillin', 'Peanuts', 'Milk', 'Egg', 'Seafood', 'Dust', 'Pollen', 'Latex'];
  readonly chronicConditionEditOptions: string[] = ['NONE', 'Asthma', 'Diabetes', 'Hypertension', 'Epilepsy', 'Heart Disease', 'Kidney Disease'];
  guardianEditForm = {
    firstName: '',
    lastName: '',
    email: '',
    phone: ''
  };

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.loadPatients();
  }

  get isReceptionist(): boolean {
    return this.role === 'RECEPTIONIST';
  }

  get filteredPatients(): PatientProfile[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.allPatients.filter((patient) => {
      const guardian = this.guardianMap[patient.guardianUserId];
      const group = this.patientAgeGroup(patient.dateOfBirth);

      const matchesSex = this.sexFilter === 'ALL' || patient.sex === this.sexFilter;
      const matchesBloodType = this.bloodTypeFilter === 'ALL' || (patient.bloodType ?? '-') === this.bloodTypeFilter;
      const matchesAgeGroup = this.ageGroupFilter === 'ALL' || group === this.ageGroupFilter;
      const matchesSearch = !term || this.patientSearchTokens(patient, guardian, group).some((v) => v.includes(term));

      return matchesSex && matchesBloodType && matchesAgeGroup && matchesSearch;
    });
  }

  get sortedPatients(): PatientProfile[] {
    const rows = [...this.filteredPatients];
    rows.sort((a, b) => {
      const result = this.patientFullName(a).toLowerCase().localeCompare(this.patientFullName(b).toLowerCase());
      return this.sortDirection === 'asc' ? result : -result;
    });
    return rows;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.sortedPatients.length / this.pageSize));
  }

  get paginatedPatients(): PatientProfile[] {
    const safePage = Math.min(this.currentPage, this.totalPages);
    if (safePage !== this.currentPage) {
      this.currentPage = safePage;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedPatients.slice(start, start + this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get guardianOptions(): GuardianUser[] {
    return Object.values(this.guardianMap).sort((a, b) => this.guardianFullName(a).localeCompare(this.guardianFullName(b)));
  }

  get bloodTypeOptions(): string[] {
    return ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', '-'];
  }

  get totalPatients(): number {
    return this.allPatients.length;
  }

  get maleCount(): number {
    return this.allPatients.filter(p => p.sex === 'MALE').length;
  }

  get femaleCount(): number {
    return this.allPatients.filter(p => p.sex === 'FEMALE').length;
  }

  get withBloodTypeCount(): number {
    return this.allPatients.filter(p => !!p.bloodType && p.bloodType.trim().length > 0).length;
  }

  get withAllergiesCount(): number {
    return this.allPatients.filter(p => !!p.allergies && p.allergies.trim().length > 0).length;
  }

  get minorsCount(): number {
    return this.allPatients.filter(p => this.patientAge(p.dateOfBirth) < 18).length;
  }

  get linkedGuardianCount(): number {
    return new Set(this.allPatients.map(p => p.guardianUserId)).size;
  }

  get patientsWithoutKnownGuardianCount(): number {
    return this.allPatients.filter((patient) => !this.guardianMap[patient.guardianUserId]).length;
  }

  get newPatientsThisMonthCount(): number {
    const now = new Date();
    return this.allPatients.filter((patient) => {
      const created = new Date(patient.createdAt ?? '');
      return !Number.isNaN(created.getTime())
        && created.getFullYear() === now.getFullYear()
        && created.getMonth() === now.getMonth();
    }).length;
  }

  get missingClinicalBasicsCount(): number {
    return this.allPatients.filter((patient) => {
      const bloodType = (patient.bloodType ?? '').trim();
      const notes = (patient.medicalNotes ?? '').trim();
      return !bloodType || bloodType === '-' || !notes || notes === '-';
    }).length;
  }

  onFiltersChanged(): void {
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }

  openDetails(patient: PatientProfile): void {
    this.detailsPatient = patient;
  }

  closeDetails(): void {
    this.detailsPatient = null;
  }

  startEdit(patient: PatientProfile): void {
    if (!this.isReceptionist) return;
    const guardian = this.guardianMap[patient.guardianUserId];
    this.editingPatientId = patient.id;
    this.editMode = 'PATIENT';
    this.patientEditForm = {
      firstName: patient.firstName ?? '',
      lastName: patient.lastName ?? '',
      dateOfBirth: patient.dateOfBirth ?? '',
      sex: patient.sex ?? '',
      bloodType: patient.bloodType ?? '',
      allergies: patient.allergies ?? '',
      chronicConditions: patient.chronicConditions ?? '',
      medicalNotes: patient.medicalNotes ?? ''
    };
    this.guardianEditForm = {
      firstName: guardian?.firstName ?? '',
      lastName: guardian?.lastName ?? '',
      email: guardian?.email ?? '',
      phone: guardian?.phone ?? ''
    };
  }

  cancelEdit(): void {
    this.editingPatientId = null;
    this.savingEdit = false;
  }

  isEditing(patientId: number): boolean {
    return this.editingPatientId === patientId;
  }

  private validatePatientEdit(): string | null {
    if (this.patientEditForm.firstName.trim().length < 3) return 'Patient first name must be at least 3 letters.';
    if (this.patientEditForm.lastName.trim().length < 3) return 'Patient last name must be at least 3 letters.';
    if (!this.patientEditForm.dateOfBirth) return 'Patient date of birth is required.';
    if (!this.patientEditForm.sex) return 'Patient sex is required.';
    if (new Date(this.patientEditForm.dateOfBirth).getTime() > Date.now()) return 'Patient date of birth cannot be in the future.';
    return null;
  }

  private validateGuardianEdit(): string | null {
    if (this.guardianEditForm.firstName.trim().length < 3) return 'Guardian first name must be at least 3 letters.';
    if (this.guardianEditForm.lastName.trim().length < 3) return 'Guardian last name must be at least 3 letters.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.guardianEditForm.email.trim())) return 'Guardian email format is invalid.';
    return null;
  }

  async saveEdit(patient: PatientProfile): Promise<void> {
    if (!this.isReceptionist || this.savingEdit) return;
    this.errorMessage = '';
    this.savingEdit = true;
    try {
      const validationError = this.editMode === 'PATIENT'
        ? this.validatePatientEdit()
        : this.validateGuardianEdit();
      if (validationError) {
        this.errorMessage = validationError;
        return;
      }

      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      });

      if (this.editMode === 'PATIENT') {
        const payload = {
          guardianUserId: patient.guardianUserId,
          firstName: this.patientEditForm.firstName.trim(),
          lastName: this.patientEditForm.lastName.trim(),
          dateOfBirth: this.patientEditForm.dateOfBirth,
          sex: this.patientEditForm.sex,
          bloodType: this.patientEditForm.bloodType.trim() || null,
          allergies: this.patientEditForm.allergies.trim() || null,
          chronicConditions: this.patientEditForm.chronicConditions.trim() || null,
          medicalNotes: this.patientEditForm.medicalNotes.trim() || null
        };
        await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/patients/${patient.id}`, payload, { headers }));
      } else {
        const payload = {
          firstName: this.guardianEditForm.firstName.trim(),
          lastName: this.guardianEditForm.lastName.trim(),
          email: this.guardianEditForm.email.trim(),
          phone: this.guardianEditForm.phone.trim() || null
        };
        await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/users/guardian/${patient.guardianUserId}`, payload, { headers }));
      }

      await this.loadPatients();
      this.editingPatientId = null;
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to update data.';
    } finally {
      this.savingEdit = false;
      this.cdr.detectChanges();
    }
  }

  toPercent(value: number, total: number): number {
    if (!total) return 0;
    return Math.round((value / total) * 100);
  }

  circleStyle(percent: number, color: string): string {
    const safe = Math.max(0, Math.min(100, percent));
    const angle = Math.round((safe / 100) * 360);
    return `conic-gradient(${color} 0deg ${angle}deg, #e2e8f0 ${angle}deg 360deg)`;
  }

  patientFullName(patient: PatientProfile): string {
    return `${patient.firstName} ${patient.lastName}`.trim();
  }

  guardianFullName(guardian?: GuardianUser): string {
    if (!guardian) return 'Unknown Guardian';
    const name = `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim();
    return name || guardian.username;
  }

  guardianDisplay(patient: PatientProfile): string {
    const guardian = this.guardianMap[patient.guardianUserId];
    if (!guardian) return `Guardian #${patient.guardianUserId}`;
    return this.guardianFullName(guardian);
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

  patientAgeGroup(dateOfBirth: string): AgeGroup {
    const age = this.patientAge(dateOfBirth);
    if (age <= 2) return 'BABY';
    if (age <= 12) return 'CHILD';
    return 'TEEN';
  }

  async loadPatients(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

      const response = await firstValueFrom(forkJoin({
        patients: this.http.get<PatientProfile[]>(`${environment.apiBaseUrl}/api/patients`, { headers }),
        guardians: this.http.get<GuardianUser[]>(`${environment.apiBaseUrl}/api/users/guardians`, { headers })
      }));
      const patients = Array.isArray(response?.patients) ? response.patients : [];
      const guardians = Array.isArray(response?.guardians) ? response.guardians : [];
      this.allPatients = patients;
      this.guardianMap = {};
      guardians.forEach((guardian) => {
        this.guardianMap[guardian.id] = guardian;
      });
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { status?: number; error?: { message?: string }; message?: string };
      this.errorMessage = error instanceof Error
        ? (
            err?.error?.message ||
            (err?.status === 401 || err?.status === 403
              ? 'Your session is not valid anymore. Please login again.'
              : err?.message) ||
            error.message
          )
        : 'Authentication problem. Please login again.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private patientSearchTokens(patient: PatientProfile, guardian: GuardianUser | undefined, group: AgeGroup): string[] {
    return [
      patient.firstName ?? '',
      patient.lastName ?? '',
      `${patient.firstName ?? ''} ${patient.lastName ?? ''}`.trim(),
      patient.dateOfBirth ?? '',
      patient.sex ?? '',
      patient.bloodType ?? '',
      patient.allergies ?? '',
      patient.chronicConditions ?? '',
      patient.medicalNotes ?? '',
      String(this.patientAge(patient.dateOfBirth)),
      group,
      guardian?.username ?? '',
      guardian?.firstName ?? '',
      guardian?.lastName ?? '',
      this.guardianFullName(guardian),
      guardian?.email ?? '',
      guardian?.phone ?? ''
    ].map((v) => String(v).toLowerCase());
  }
}




