import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type StaffRole = 'DOCTOR' | 'NURSE' | 'SURGEON' | 'PHARMACIST' | 'RECEPTIONIST' | 'LAB_AGENT';
type AccountStatus = 'PENDING_CONTRACT' | 'ACTIVE' | 'INACTIVE';

interface UserRow {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  role: string;
  accountStatus: AccountStatus;
  enabled: boolean;
}

interface StaffSearchResponse {
  items: UserRow[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

@Component({
  selector: 'app-staff-roles-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './staff-roles-details.html',
  styleUrl: './staff-roles-details.scss'
})
export class StaffRolesDetails implements OnInit, OnDestroy {
  loading = false;
  errorMessage = '';

  searchTerm = '';
  roleFilter: StaffRole | 'ALL' = 'ALL';
  statusFilter: AccountStatus | 'ALL' = 'ALL';
  sortBy: 'firstName' | 'lastName' | 'username' | 'email' = 'firstName';
  sortDir: 'asc' | 'desc' = 'asc';
  currentPage = 1;
  pageSize = 10;
  readonly pageSizeOptions = [5, 10, 20, 50];

  readonly staffRoles: StaffRole[] = ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];
  allStaff: UserRow[] = [];
  allStaffForStats: UserRow[] = [];
  totalElements = 0;
  totalPages = 1;
  private refreshTimer?: ReturnType<typeof setInterval>;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRoleStats();
    this.loadStaffSearch();
    this.refreshTimer = setInterval(() => {
      if (!this.loading) {
        this.loadStaffSearch();
      }
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  get paginatedStaff(): UserRow[] {
    return this.allStaff;
  }

  get roleStats(): Array<{ role: StaffRole; total: number; active: number; pending: number; inactive: number }> {
    return this.staffRoles.map(role => {
      const users = this.allStaffForStats.filter(u => u.role === role);

      return {
        role,
        total: users.length,
        active: users.filter(u => u.accountStatus === 'ACTIVE').length,
        pending: users.filter(u => u.accountStatus === 'PENDING_CONTRACT').length,
        inactive: users.filter(u => u.accountStatus === 'INACTIVE').length
      };
    });
  }

  fullName(user: UserRow): string {
    const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return name || '-';
  }

  statusClass(status: AccountStatus): string {
    if (status === 'ACTIVE') return 'badge-active';
    if (status === 'PENDING_CONTRACT') return 'badge-pending';
    return 'badge-inactive';
  }

  enabledClass(enabled: boolean): string {
    return enabled ? 'badge-enabled' : 'badge-disabled';
  }

  onFiltersChanged(): void {
    this.currentPage = 1;
    this.loadStaffSearch();
  }

  onPageSizeChanged(): void {
    this.currentPage = 1;
    this.loadStaffSearch();
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.loadStaffSearch();
  }

  previousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  private async loadRoleStats(): Promise<void> {
    try {
      const token = await getValidToken();
      const response = await firstValueFrom(this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`
        })
      }));
      const users = Array.isArray(response) ? response : [];
      this.allStaffForStats = users.filter(u => this.staffRoles.includes(u.role as StaffRole));
    } catch {
      this.allStaffForStats = [];
    } finally {
      this.cdr.detectChanges();
    }
  }

  async loadStaffSearch(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const response = await firstValueFrom(this.http.post<StaffSearchResponse | unknown>(`${environment.apiBaseUrl}/api/users/staff/search`, {
        query: this.searchTerm.trim() || null,
        roles: this.roleFilter === 'ALL' ? [] : [this.roleFilter],
        statuses: this.statusFilter === 'ALL' ? [] : [this.statusFilter],
        enabled: null,
        sortBy: this.sortBy,
        sortDir: this.sortDir,
        page: this.currentPage - 1,
        size: this.pageSize
      }, {
        headers: new HttpHeaders({
          Authorization: `Bearer ${token}`
        })
      }));
      const payload = (response && typeof response === 'object') ? response as StaffSearchResponse : null;
      this.allStaff = Array.isArray(payload?.items) ? payload.items : [];
      this.totalElements = Number(payload?.totalElements ?? 0);
      this.totalPages = Math.max(1, Number(payload?.totalPages ?? 1));
      this.currentPage = Math.min(Math.max(1, Number(payload?.page ?? 0) + 1), this.totalPages);
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
}
