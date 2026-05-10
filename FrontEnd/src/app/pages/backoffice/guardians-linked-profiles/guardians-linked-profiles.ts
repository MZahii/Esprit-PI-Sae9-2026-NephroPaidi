import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { firstValueFrom, forkJoin } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { CountUpDirective } from '../../../shared/directives/count-up.directive';

interface PatientProfile {
  id: number;
  guardianUserId: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  sex: 'MALE' | 'FEMALE';
  bloodType?: string | null;
  allergies?: string | null;
  chronicConditions?: string | null;
  medicalNotes?: string | null;
}

interface GuardianUser {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  createdAt?: string | null;
}

interface GuardianLinkedRow {
  guardian: GuardianUser;
  patients: PatientProfile[];
}

type LinkFilter = 'ALL' | 'WITH_PATIENTS' | 'WITHOUT_PATIENTS';

@Component({
  selector: 'app-guardians-linked-profiles',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CountUpDirective],
  templateUrl: './guardians-linked-profiles.html',
  styleUrl: './guardians-linked-profiles.scss'
})
export class GuardiansLinkedProfiles implements OnInit {
  loading = false;
  errorMessage = '';
  searchTerm = '';
  linkedFilter: LinkFilter = 'ALL';
  sexFilter: 'ALL' | 'MALE' | 'FEMALE' = 'ALL';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageSize = 5;
  currentPage = 1;
  readonly pageSizeOptions: number[] = [5, 10, 20];
  detailsRow: GuardianLinkedRow | null = null;

  allRows: GuardianLinkedRow[] = [];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadGuardiansAndProfiles();
  }

  get filteredRows(): GuardianLinkedRow[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.allRows.filter((row) => {
      const matchesSearch = !term || this.guardianSearchTokens(row).some((value) => value.includes(term));
      const matchesLinked = this.linkedFilter === 'ALL'
        || (this.linkedFilter === 'WITH_PATIENTS' && row.patients.length > 0)
        || (this.linkedFilter === 'WITHOUT_PATIENTS' && row.patients.length === 0);
      const matchesSex = this.sexFilter === 'ALL'
        || row.patients.some((patient) => patient.sex === this.sexFilter);

      return matchesSearch && matchesLinked && matchesSex;
    });
  }

  get sortedRows(): GuardianLinkedRow[] {
    const rows = [...this.filteredRows];
    rows.sort((a, b) => {
      const result = this.guardianFullName(a.guardian).toLowerCase().localeCompare(this.guardianFullName(b.guardian).toLowerCase());
      return this.sortDirection === 'asc' ? result : -result;
    });
    return rows;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.sortedRows.length / this.pageSize));
  }

  get paginatedRows(): GuardianLinkedRow[] {
    const safePage = Math.min(this.currentPage, this.totalPages);
    if (safePage !== this.currentPage) {
      this.currentPage = safePage;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedRows.slice(start, start + this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get totalGuardians(): number {
    return this.allRows.length;
  }

  get withPatientsCount(): number {
    return this.allRows.filter(row => row.patients.length > 0).length;
  }

  get withoutPatientsCount(): number {
    return this.totalGuardians - this.withPatientsCount;
  }

  get totalLinkedProfiles(): number {
    return this.allRows.reduce((sum, row) => sum + row.patients.length, 0);
  }

  get averageProfilesPerGuardian(): number {
    if (!this.totalGuardians) return 0;
    return Number((this.totalLinkedProfiles / this.totalGuardians).toFixed(2));
  }

  get linkedGuardianRate(): number {
    if (!this.totalGuardians) return 0;
    return Math.round((this.withPatientsCount / this.totalGuardians) * 100);
  }

  get recentGuardiansCount(): number {
    const now = new Date();
    const since = new Date(now);
    since.setDate(now.getDate() - 30);
    return this.allRows.filter((row) => {
      const created = new Date(row.guardian.createdAt ?? '');
      return !Number.isNaN(created.getTime()) && created >= since && created <= now;
    }).length;
  }

  get topLinkedGuardians(): GuardianLinkedRow[] {
    return [...this.allRows]
      .sort((a, b) => b.patients.length - a.patients.length)
      .slice(0, 3);
  }

  onFiltersChanged(): void {
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }

  openDetails(row: GuardianLinkedRow): void {
    this.detailsRow = row;
  }

  closeDetails(): void {
    this.detailsRow = null;
  }

  guardianFullName(guardian: GuardianUser): string {
    const full = `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim();
    return full || guardian.username;
  }

  patientNames(row: GuardianLinkedRow): string {
    if (row.patients.length === 0) return '-';
    return row.patients.map((p) => `${p.firstName} ${p.lastName}`).join(', ');
  }

  async loadGuardiansAndProfiles(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

      const response = await firstValueFrom(forkJoin({
        guardians: this.http.get<GuardianUser[]>(`${environment.apiBaseUrl}/api/users/guardians`, { headers }),
        patients: this.http.get<PatientProfile[]>(`${environment.apiBaseUrl}/api/patients`, { headers })
      }));
      const guardians = Array.isArray(response?.guardians) ? response.guardians : [];
      const patients = Array.isArray(response?.patients) ? response.patients : [];
      const groupedPatients: Record<number, PatientProfile[]> = {};
      patients.forEach((patient) => {
        if (!groupedPatients[patient.guardianUserId]) {
          groupedPatients[patient.guardianUserId] = [];
        }
        groupedPatients[patient.guardianUserId].push(patient);
      });

      this.allRows = guardians.map((guardian) => ({
        guardian,
        patients: groupedPatients[guardian.id] ?? []
      }));
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

  private guardianSearchTokens(row: GuardianLinkedRow): string[] {
    const patientTokens = row.patients.flatMap((patient) => [
      patient.firstName ?? '',
      patient.lastName ?? '',
      `${patient.firstName ?? ''} ${patient.lastName ?? ''}`.trim(),
      patient.dateOfBirth ?? '',
      patient.sex ?? '',
      patient.bloodType ?? '',
      patient.allergies ?? '',
      patient.chronicConditions ?? ''
    ]);

    return [
      row.guardian.username ?? '',
      row.guardian.firstName ?? '',
      row.guardian.lastName ?? '',
      this.guardianFullName(row.guardian),
      row.guardian.email ?? '',
      row.guardian.phone ?? '',
      String(row.patients.length),
      ...patientTokens
    ].map((value) => String(value).toLowerCase());
  }
}




