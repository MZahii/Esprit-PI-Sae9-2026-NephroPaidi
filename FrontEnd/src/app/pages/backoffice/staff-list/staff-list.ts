import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, forkJoin } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';

type StaffRole = 'DOCTOR' | 'NURSE' | 'SURGEON' | 'PHARMACIST' | 'RECEPTIONIST' | 'LAB_AGENT';
type AccountStatus = 'PENDING_CONTRACT' | 'ACTIVE' | 'INACTIVE';
type AccessFilter = 'ALL' | 'ENABLED' | 'DISABLED';
type ArchiveFilter = 'ALL' | 'LIVE' | 'ARCHIVED';
type ContractFilter = 'ALL' | 'WITH_CONTRACT' | 'WITHOUT_CONTRACT' | 'RUNNING_CONTRACT' | 'NO_RUNNING_CONTRACT';
type SortField = 'username' | 'name' | 'role' | 'status' | 'contract';

interface UserRow {
  id: number;
  username: string;
  cin?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  sex?: string;
  role: string;
  accountStatus: AccountStatus;
  enabled: boolean;
  deleted?: boolean;
}

interface ContractRow {
  id: number;
  staffUserId: number;
  contractReference: string;
  contractType: string;
  status: string;
  jobTitle: string;
  startDate: string;
  endDate: string;
}

interface RoleStats {
  role: StaffRole;
  total: number;
  active: number;
  pending: number;
  inactive: number;
  enabled: number;
  disabled: number;
  withContract: number;
  withoutContract: number;
  activeWithContract: number;
  activeWithoutContract: number;
  pendingWithContract: number;
  pendingWithoutContract: number;
  inactiveWithContract: number;
  inactiveWithoutContract: number;
}

@Component({
  selector: 'app-staff-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './staff-list.html',
  styleUrl: './staff-list.scss'
})
export class StaffList implements OnInit, OnDestroy {
  loading = false;
  errorMessage = '';
  role = '';

  searchTerm = '';
  roleFilter: StaffRole | 'ALL' = 'ALL';
  statusFilter: AccountStatus | 'ALL' = 'ALL';
  accessFilter: AccessFilter = 'ALL';
  archiveFilter: ArchiveFilter = 'ALL';
  contractFilter: ContractFilter = 'ALL';
  sortField: SortField = 'username';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageSize = 10;
  currentPage = 1;
  actionLoadingUserId: number | null = null;
  actionMessage = '';

  allStaff: UserRow[] = [];
  archivedStaff: UserRow[] = [];
  allContracts: ContractRow[] = [];
  contractsByStaffId: Record<number, ContractRow[]> = {};

  readonly staffRoles: StaffRole[] = ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];
  readonly pageSizeOptions: number[] = [5, 10, 20];
  private refreshTimer?: ReturnType<typeof setInterval>;

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.loadStaffAccounts();
    this.refreshTimer = setInterval(() => {
      if (!this.loading) {
        this.loadStaffAccounts();
      }
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  get isHr(): boolean {
    return this.role === 'HR';
  }

  get filteredStaff(): UserRow[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.allStaff.filter(user => {
      const matchesRole = this.roleFilter === 'ALL' || user.role === this.roleFilter;
      const matchesStatus = this.statusFilter === 'ALL' || user.accountStatus === this.statusFilter;
      const matchesAccess = this.accessFilter === 'ALL'
        || (this.accessFilter === 'ENABLED' && this.isEnabledFromStatus(user))
        || (this.accessFilter === 'DISABLED' && !this.isEnabledFromStatus(user));
      const matchesArchive = this.archiveFilter === 'ALL'
        || (this.archiveFilter === 'LIVE' && !user.deleted)
        || (this.archiveFilter === 'ARCHIVED' && !!user.deleted);
      const matchesContract = this.contractFilter === 'ALL'
        || (this.contractFilter === 'WITH_CONTRACT' && this.hasAnyContract(user.id))
        || (this.contractFilter === 'WITHOUT_CONTRACT' && !this.hasAnyContract(user.id))
        || (this.contractFilter === 'RUNNING_CONTRACT' && this.hasRunningContract(user.id))
        || (this.contractFilter === 'NO_RUNNING_CONTRACT' && !this.hasRunningContract(user.id));
      const matchesSearch = !term || this.staffSearchTokens(user).some(v => v.includes(term));

      return matchesRole && matchesStatus && matchesAccess && matchesArchive && matchesContract && matchesSearch;
    });
  }

  get hasActiveFilters(): boolean {
    return !!this.searchTerm.trim()
      || this.roleFilter !== 'ALL'
      || this.statusFilter !== 'ALL'
      || this.accessFilter !== 'ALL'
      || this.archiveFilter !== 'ALL'
      || this.contractFilter !== 'ALL'
      || this.sortField !== 'username'
      || this.sortDirection !== 'asc';
  }

  get emptyStateMessage(): string {
    if (this.loading) {
      return 'Loading staff accounts...';
    }

    if (this.totalStaff === 0) {
      return 'No staff accounts are available yet.';
    }

    return this.hasActiveFilters
      ? 'No staff accounts match the current filters.'
      : 'No staff accounts found.';
  }

  get totalStaff(): number {
    return this.allStaff.length;
  }

  get activeStaff(): number {
    return this.allStaff.filter(u => u.accountStatus === 'ACTIVE').length;
  }

  get pendingStaff(): number {
    return this.allStaff.filter(u => u.accountStatus === 'PENDING_CONTRACT').length;
  }

  get inactiveStaff(): number {
    return this.allStaff.filter(u => u.accountStatus === 'INACTIVE').length;
  }

  get archivedStaffCount(): number {
    return this.allStaff.filter(u => !!u.deleted).length;
  }

  get enabledStaff(): number {
    return this.allStaff.filter(u => this.isEnabledFromStatus(u)).length;
  }

  get disabledStaff(): number {
    return this.allStaff.filter(u => !this.isEnabledFromStatus(u)).length;
  }

  get withContractStaff(): number {
    return this.allStaff.filter(u => this.hasAnyContract(u.id)).length;
  }

  get withoutContractStaff(): number {
    return this.totalStaff - this.withContractStaff;
  }

  get activeWithContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'ACTIVE' && this.hasAnyContract(u.id)).length;
  }

  get activeWithoutContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'ACTIVE' && !this.hasAnyContract(u.id)).length;
  }

  get pendingWithContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'PENDING_CONTRACT' && this.hasAnyContract(u.id)).length;
  }

  get pendingWithoutContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'PENDING_CONTRACT' && !this.hasAnyContract(u.id)).length;
  }

  get inactiveWithContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'INACTIVE' && this.hasAnyContract(u.id)).length;
  }

  get inactiveWithoutContract(): number {
    return this.allStaff.filter(u => u.accountStatus === 'INACTIVE' && !this.hasAnyContract(u.id)).length;
  }

  get roleStats(): RoleStats[] {
    return this.staffRoles.map(role => {
      const users = this.allStaff.filter(u => u.role === role);
      return {
        role,
        total: users.length,
        active: users.filter(u => u.accountStatus === 'ACTIVE').length,
        pending: users.filter(u => u.accountStatus === 'PENDING_CONTRACT').length,
        inactive: users.filter(u => u.accountStatus === 'INACTIVE').length,
        enabled: users.filter(u => this.isEnabledFromStatus(u)).length,
        disabled: users.filter(u => !this.isEnabledFromStatus(u)).length,
        withContract: users.filter(u => this.hasAnyContract(u.id)).length,
        withoutContract: users.filter(u => !this.hasAnyContract(u.id)).length,
        activeWithContract: users.filter(u => u.accountStatus === 'ACTIVE' && this.hasAnyContract(u.id)).length,
        activeWithoutContract: users.filter(u => u.accountStatus === 'ACTIVE' && !this.hasAnyContract(u.id)).length,
        pendingWithContract: users.filter(u => u.accountStatus === 'PENDING_CONTRACT' && this.hasAnyContract(u.id)).length,
        pendingWithoutContract: users.filter(u => u.accountStatus === 'PENDING_CONTRACT' && !this.hasAnyContract(u.id)).length,
        inactiveWithContract: users.filter(u => u.accountStatus === 'INACTIVE' && this.hasAnyContract(u.id)).length,
        inactiveWithoutContract: users.filter(u => u.accountStatus === 'INACTIVE' && !this.hasAnyContract(u.id)).length
      };
    });
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.sortedStaff.length / this.pageSize));
  }

  get sortedStaff(): UserRow[] {
    const items = [...this.filteredStaff];
    items.sort((a, b) => {
      const result = this.compareStaff(a, b);
      return this.sortDirection === 'asc' ? result : -result;
    });
    return items;
  }

  get paginatedStaff(): UserRow[] {
    const safePage = Math.min(this.currentPage, this.totalPages);
    if (safePage !== this.currentPage) {
      this.currentPage = safePage;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedStaff.slice(start, start + this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
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

  archivedClass(isArchived: boolean): string {
    return isArchived ? 'badge-archived' : 'badge-live';
  }

  isEnabledFromStatus(user: UserRow): boolean {
    return user.accountStatus === 'ACTIVE';
  }

  hasAnyContract(staffUserId: number): boolean {
    return (this.contractsByStaffId[staffUserId]?.length ?? 0) > 0;
  }

  hasRunningContract(staffUserId: number): boolean {
    const contracts = this.contractsByStaffId[staffUserId] ?? [];
    return contracts.some(c => c.status === 'ACTIVE' || c.status === 'SUSPENDED');
  }

  getPrimaryContract(staffUserId: number): ContractRow | null {
    const contracts = [...(this.contractsByStaffId[staffUserId] ?? [])];
    if (!contracts.length) return null;

    const priority = (status: string): number => {
      if (status === 'ACTIVE') return 0;
      if (status === 'SUSPENDED') return 1;
      if (status === 'ENDED') return 2;
      return 3;
    };

    contracts.sort((a, b) => {
      const priorityResult = priority(a.status) - priority(b.status);
      if (priorityResult !== 0) return priorityResult;
      return (b.startDate ?? '').localeCompare(a.startDate ?? '');
    });

    return contracts[0] ?? null;
  }

  contractSummary(user: UserRow): string {
    const contract = this.getPrimaryContract(user.id);
    if (!contract) return 'No contract';
    return `${contract.jobTitle} (${contract.status})`;
  }

  contractStateClass(user: UserRow): string {
    if (this.hasRunningContract(user.id)) return 'badge-contract-running';
    if (this.hasAnyContract(user.id)) return 'badge-contract-history';
    return 'badge-contract-none';
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.roleFilter = 'ALL';
    this.statusFilter = 'ALL';
    this.accessFilter = 'ALL';
    this.archiveFilter = 'ALL';
    this.contractFilter = 'ALL';
    this.sortField = 'username';
    this.sortDirection = 'asc';
    this.currentPage = 1;
  }

  applyQuickFilter(filter: 'PENDING_WITHOUT_CONTRACT' | 'INACTIVE' | 'ARCHIVED' | 'NO_RUNNING_CONTRACT'): void {
    this.resetFilters();
    if (filter === 'PENDING_WITHOUT_CONTRACT') {
      this.statusFilter = 'PENDING_CONTRACT';
      this.contractFilter = 'WITHOUT_CONTRACT';
      return;
    }

    if (filter === 'INACTIVE') {
      this.statusFilter = 'INACTIVE';
      this.accessFilter = 'DISABLED';
      return;
    }

    if (filter === 'ARCHIVED') {
      this.archiveFilter = 'ARCHIVED';
      return;
    }

    this.contractFilter = 'NO_RUNNING_CONTRACT';
  }

  onFiltersChanged(): void {
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
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

  async toggleAccountStatus(user: UserRow): Promise<void> {
    if (!this.isHr || this.actionLoadingUserId === user.id) return;

    const nextStatus: AccountStatus = user.accountStatus === 'INACTIVE'
      ? (this.hasRunningContract(user.id) ? 'ACTIVE' : 'PENDING_CONTRACT')
      : 'INACTIVE';

    const confirmed = window.confirm(
      `Confirm changing account status for "${user.username}" to "${nextStatus}"?`
    );
    if (!confirmed) {
      return;
    }

    this.actionLoadingUserId = user.id;
    this.actionMessage = '';
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/contracts/staff/${user.id}/status?status=${nextStatus}`,
        {},
        {
          headers: new HttpHeaders({
            Authorization: `Bearer ${token}`
          })
        }
      ));
      await this.loadStaffAccounts();
      this.actionMessage = 'Account status updated successfully.';
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to update account status.';
    } finally {
      this.actionLoadingUserId = null;
      this.cdr.detectChanges();
    }
  }

  canSoftDelete(user: UserRow): boolean {
    return this.isHr && !user.deleted;
  }

  canRestore(user: UserRow): boolean {
    return this.isHr && !!user.deleted;
  }

  async softDeleteUser(user: UserRow): Promise<void> {
    if (!this.canSoftDelete(user) || this.actionLoadingUserId === user.id) return;

    const confirmed = window.confirm(
      `Confirm archiving staff user "${user.username}"? This will disable the account and remove it from normal backend listings until restored.`
    );
    if (!confirmed) return;

    this.actionLoadingUserId = user.id;
    this.actionMessage = '';
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const archived = await firstValueFrom(this.http.patch<UserRow>(
        `${environment.apiBaseUrl}/api/users/${user.id}/soft-delete`,
        {},
        { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
      ));

      const localArchived = {
        ...archived,
        deleted: true,
        enabled: false,
        accountStatus: 'INACTIVE' as AccountStatus
      };

      this.archivedStaff = [localArchived, ...this.archivedStaff.filter(item => item.id !== user.id)];
      this.allStaff = this.allStaff.map(item => item.id === user.id ? localArchived : item);
      this.actionMessage = 'User archived successfully. You can restore it from this list during the current session.';
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to archive user.';
    } finally {
      this.actionLoadingUserId = null;
      this.cdr.detectChanges();
    }
  }

  async restoreUser(user: UserRow): Promise<void> {
    if (!this.canRestore(user) || this.actionLoadingUserId === user.id) return;

    const confirmed = window.confirm(`Confirm restoring archived user "${user.username}"?`);
    if (!confirmed) return;

    this.actionLoadingUserId = user.id;
    this.actionMessage = '';
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const restored = await firstValueFrom(this.http.patch<UserRow>(
        `${environment.apiBaseUrl}/api/users/${user.id}/restore`,
        {},
        { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
      ));

      const liveUser = { ...restored, deleted: false };
      this.archivedStaff = this.archivedStaff.filter(item => item.id !== user.id);
      this.allStaff = this.allStaff.map(item => item.id === user.id ? liveUser : item);
      this.actionMessage = 'User restored successfully.';
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to restore user.';
    } finally {
      this.actionLoadingUserId = null;
      this.cdr.detectChanges();
    }
  }

  async loadStaffAccounts(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`
      });
      const response = await firstValueFrom(forkJoin({
        users: this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers }),
        contracts: this.http.get<ContractRow[] | unknown>(`${environment.apiBaseUrl}/api/contracts`, { headers })
      }));
      const users = Array.isArray(response?.users) ? response.users : [];
      const contracts = Array.isArray(response?.contracts) ? response.contracts : [];
      const liveStaff = users
        .filter(u => this.staffRoles.includes(u.role as StaffRole))
        .map(u => ({ ...u, deleted: false }));
      const liveIds = new Set(liveStaff.map(u => u.id));
      const preservedArchived = this.archivedStaff.filter(u => !liveIds.has(u.id));
      this.archivedStaff = preservedArchived;
      this.allStaff = [...liveStaff, ...preservedArchived];
      this.allContracts = contracts;
      this.contractsByStaffId = {};
      for (const contract of this.allContracts) {
        if (!this.contractsByStaffId[contract.staffUserId]) {
          this.contractsByStaffId[contract.staffUserId] = [];
        }
        this.contractsByStaffId[contract.staffUserId].push(contract);
      }
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

  private staffSearchTokens(user: UserRow): string[] {
    const contracts = this.contractsByStaffId[user.id] ?? [];
    return [
      user.username ?? '',
      user.cin ?? '',
      user.firstName ?? '',
      user.lastName ?? '',
      `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim(),
      user.email ?? '',
      user.phone ?? '',
      user.sex ?? '',
      user.dateOfBirth ?? '',
      user.role ?? '',
      user.accountStatus ?? '',
      user.deleted ? 'archived' : 'live',
      this.isEnabledFromStatus(user) ? 'enabled' : 'disabled',
      this.contractSummary(user),
      ...contracts.map(contract => contract.contractReference ?? ''),
      ...contracts.map(contract => contract.contractType ?? ''),
      ...contracts.map(contract => contract.jobTitle ?? ''),
      ...contracts.map(contract => contract.status ?? '')
    ].map(v => v.toLowerCase());
  }

  private compareStaff(a: UserRow, b: UserRow): number {
    if (this.sortField === 'name') {
      return this.fullName(a).toLowerCase().localeCompare(this.fullName(b).toLowerCase());
    }

    if (this.sortField === 'role') {
      return a.role.toLowerCase().localeCompare(b.role.toLowerCase());
    }

    if (this.sortField === 'status') {
      return a.accountStatus.toLowerCase().localeCompare(b.accountStatus.toLowerCase());
    }

    if (this.sortField === 'contract') {
      return this.contractSummary(a).toLowerCase().localeCompare(this.contractSummary(b).toLowerCase());
    }

    return a.username.toLowerCase().localeCompare(b.username.toLowerCase());
  }
}
