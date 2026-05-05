import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, forkJoin, Subscription } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { DocumentExportService } from '../../../core/services/document-export.service';

type ContractStatus = 'ACTIVE' | 'SUSPENDED' | 'ENDED' | 'EXPIRED';
type ContractType = 'CDI' | 'CDD' | 'INTERNSHIP' | 'PART_TIME' | 'TEMPORARY';

interface ContractRow {
  id: number;
  staffUserId: number;
  contractReference: string;
  contractType: ContractType;
  status: ContractStatus;
  jobTitle: string;
  department?: string;
  startDate: string;
  endDate: string;
  salary?: number;
  currency?: string;
  hoursPerWeek?: number;
  notes?: string;
  deleted?: boolean;
  deletedAt?: string;
  deletedBy?: string;
}

interface UserRow {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  role: string;
  accountStatus?: 'PENDING_CONTRACT' | 'ACTIVE' | 'INACTIVE';
  enabled?: boolean;
}

@Component({
  selector: 'app-contracts-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './contracts-list.html',
  styleUrl: './contracts-list.scss'
})
export class ContractsList implements OnInit, OnDestroy {
  loading = false;
  errorMessage = '';
  actionMessage = '';
  role = '';

  searchTerm = '';
  statusFilter: ContractStatus | 'ALL' = 'ALL';
  roleFilter: 'ALL' | 'HR' | 'DOCTOR' | 'NURSE' | 'SURGEON' | 'PHARMACIST' | 'RECEPTIONIST' | 'LAB_AGENT' = 'ALL';
  pageSize = 10;
  currentPage = 1;
  sortField: 'name' | 'startDate' | 'endDate' = 'name';
  sortDirection: 'asc' | 'desc' = 'desc';
  contractScope: 'STAFF' | 'HR' = 'STAFF';

  allContracts: ContractRow[] = [];
  staffUsers: UserRow[] = [];
  staffMap: Record<number, UserRow> = {};
  private refreshTimer?: ReturnType<typeof setInterval>;
  private routeSub?: Subscription;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private authStorage: AuthStorageService,
    private cdr: ChangeDetectorRef,
    private documentExportService: DocumentExportService
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    const navigationState = history.state as { actionMessage?: string } | undefined;
    if (navigationState?.actionMessage) {
      this.actionMessage = navigationState.actionMessage;
    }
    this.routeSub = this.route.queryParamMap.subscribe(params => {
      const scope = (params.get('scope') ?? '').toUpperCase();
      if (this.isAdmin && scope === 'HR') {
        this.contractScope = 'HR';
      } else {
        this.contractScope = 'STAFF';
      }
      this.loadContracts();
    });

    this.refreshTimer = setInterval(() => {
      if (!this.loading) {
        this.loadContracts();
      }
    }, 10000);
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  get isHr(): boolean {
    return this.role === 'HR';
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get contractListTitle(): string {
    return this.contractScope === 'HR' ? 'HR Contracts Registry' : 'Staff Contracts Registry';
  }

  canManageContract(contract: ContractRow): boolean {
    if (contract.deleted) return false;
    if (this.isHr) return true;
    if (!this.isAdmin) return false;
    const user = this.staffMap[contract.staffUserId];
    return this.contractScope === 'HR' && user?.role === 'HR';
  }

  canArchiveContract(contract: ContractRow): boolean {
    if (contract.deleted) return false;
    const user = this.staffMap[contract.staffUserId];
    if (!user) return false;
    if (this.isAdmin) return user.role === 'HR';
    if (this.isHr) return user.role !== 'HR';
    return false;
  }

  canRestoreContract(contract: ContractRow): boolean {
    return this.isAdmin && !!contract.deleted;
  }

  get visibleContracts(): ContractRow[] {
    return this.allContracts.filter((contract) => {
      const staff = this.staffMap[contract.staffUserId];
      const isHrContract = staff?.role === 'HR';
      if (this.contractScope === 'HR') return isHrContract;
      return !isHrContract;
    });
  }

  get filteredContracts(): ContractRow[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.visibleContracts.filter((contract) => {
      const staff = this.staffMap[contract.staffUserId];
      const staffName = staff ? this.staffFullName(staff).toLowerCase() : '';

      const matchesStatus = this.statusFilter === 'ALL' || contract.status === this.statusFilter;
      const matchesRole = this.roleFilter === 'ALL' || (staff?.role === this.roleFilter);
      const matchesSearch = !term || this.contractSearchTokens(contract, staffName, staff).some((value) =>
        value.includes(term)
      );

      return matchesStatus && matchesRole && matchesSearch;
    });
  }

  get sortedContracts(): ContractRow[] {
    const items = [...this.filteredContracts];
    items.sort((a, b) => this.compareContracts(a, b));
    return items;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.sortedContracts.length / this.pageSize));
  }

  get paginatedContracts(): ContractRow[] {
    const safePage = Math.min(this.currentPage, this.totalPages);
    if (safePage !== this.currentPage) {
      this.currentPage = safePage;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.sortedContracts.slice(start, start + this.pageSize);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get totalContracts(): number {
    return this.visibleContracts.filter(c => !c.deleted).length;
  }

  get activeContracts(): number {
    return this.visibleContracts.filter(c => !c.deleted && c.status === 'ACTIVE').length;
  }

  get suspendedContracts(): number {
    return this.visibleContracts.filter(c => !c.deleted && c.status === 'SUSPENDED').length;
  }

  get endedContracts(): number {
    return this.visibleContracts.filter(c => !c.deleted && (c.status === 'ENDED' || c.status === 'EXPIRED')).length;
  }

  get archivedContracts(): number {
    return this.visibleContracts.filter(c => !!c.deleted).length;
  }

  get userActiveAccessContracts(): number {
    return this.visibleContracts.filter(c => !c.deleted && this.accountAccessStatus(c) === 'USER_ACTIVE').length;
  }

  get userInactiveAccessContracts(): number {
    return this.totalContracts - this.userActiveAccessContracts;
  }

  statusClass(status: ContractStatus): string {
    if (status === 'ACTIVE') return 'badge-active';
    if (status === 'SUSPENDED') return 'badge-suspended';
    if (status === 'ENDED') return 'badge-ended';
    return 'badge-expired';
  }

  contractStateClass(contract: ContractRow): string {
    if (contract.deleted) return 'badge-archived';
    return this.statusClass(contract.status);
  }

  contractStateLabel(contract: ContractRow): string {
    return contract.deleted ? 'ARCHIVED' : contract.status;
  }

  staffFullName(user?: UserRow): string {
    if (!user) return '-';
    const full = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return full || user.username;
  }

  staffDisplay(contract: ContractRow): string {
    const user = this.staffMap[contract.staffUserId];
    return user ? this.staffFullName(user) : `Staff #${contract.staffUserId}`;
  }

  staffRole(contract: ContractRow): string {
    return this.staffMap[contract.staffUserId]?.role ?? '-';
  }

  accountAccessStatus(contract: ContractRow): 'USER_ACTIVE' | 'USER_INACTIVE' {
    const user = this.staffMap[contract.staffUserId];
    if (!user) return 'USER_INACTIVE';
    const active = user.accountStatus === 'ACTIVE' && user.enabled === true;
    return active ? 'USER_ACTIVE' : 'USER_INACTIVE';
  }

  accountAccessClass(contract: ContractRow): string {
    return this.accountAccessStatus(contract) === 'USER_ACTIVE' ? 'badge-active' : 'badge-ended';
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

  printContracts(): void {
    this.documentExportService.printDocument(this.buildExportConfig());
  }

  exportContractsPdf(): void {
    this.documentExportService.exportPdf(this.buildExportConfig());
  }

  async loadContracts(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

      const response = await firstValueFrom(forkJoin({
        contracts: this.http.get<ContractRow[]>(
          `${environment.apiBaseUrl}/api/contracts${this.isAdmin ? '?includeDeleted=true' : ''}`,
          { headers }
        ),
        users: this.http.get<UserRow[]>(`${environment.apiBaseUrl}/api/users`, { headers })
      }));
      const users = Array.isArray(response?.users) ? response.users : [];
      const contracts = Array.isArray(response?.contracts) ? response.contracts : [];

      this.staffUsers = users.filter((user) =>
        ['HR', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'].includes(user.role)
      );
      this.staffMap = {};
      this.staffUsers.forEach((user) => {
        this.staffMap[user.id] = user;
      });
      this.allContracts = contracts;
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

  async runAction(contractId: number, action: 'suspend' | 'resume' | 'end'): Promise<void> {
    const target = this.allContracts.find(c => c.id === contractId);
    if (!target || !this.canManageContract(target)) return;
    const actionLabel = action.toUpperCase();
    const confirmed = window.confirm(`Confirm ${actionLabel} contract?`);
    if (!confirmed) return;

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/contracts/${contractId}/${action}`,
        {},
        { headers }
      ));
      await this.loadContracts();
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = error instanceof Error
        ? err?.error?.message || err?.message || error.message || `Failed to ${action} contract.`
        : 'Authentication problem. Please login again.';
    }
  }

  async runArchive(contractId: number): Promise<void> {
    const target = this.allContracts.find(c => c.id === contractId);
    if (!target || !this.canArchiveContract(target)) return;
    const confirmed = window.confirm('Confirm ARCHIVE contract?');
    if (!confirmed) return;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.delete(
        `${environment.apiBaseUrl}/api/contracts/${contractId}`,
        { headers }
      ));
      await this.loadContracts();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to archive contract.';
      this.cdr.detectChanges();
    }
  }

  async runRestore(contractId: number): Promise<void> {
    const target = this.allContracts.find(c => c.id === contractId);
    if (!target || !this.canRestoreContract(target)) return;
    const confirmed = window.confirm('Confirm RESTORE archived contract?');
    if (!confirmed) return;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/contracts/${contractId}/restore`,
        {},
        { headers }
      ));
      await this.loadContracts();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to restore contract.';
      this.cdr.detectChanges();
    }
  }

  private compareContracts(a: ContractRow, b: ContractRow): number {
    const staffA = this.staffDisplay(a).toLowerCase();
    const staffB = this.staffDisplay(b).toLowerCase();
    let valueA: string | number = '';
    let valueB: string | number = '';

    switch (this.sortField) {
      case 'name':
        valueA = staffA;
        valueB = staffB;
        break;
      case 'endDate':
        valueA = a.endDate;
        valueB = b.endDate;
        break;
      case 'startDate':
      default:
        valueA = a.startDate;
        valueB = b.startDate;
        break;
    }

    let result = 0;
    if (typeof valueA === 'number' && typeof valueB === 'number') {
      result = valueA - valueB;
    } else {
      result = String(valueA).localeCompare(String(valueB));
    }

    return this.sortDirection === 'asc' ? result : -result;
  }

  private contractSearchTokens(contract: ContractRow, staffName: string, staff?: UserRow): string[] {
    return [
      contract.contractReference ?? '',
      contract.contractType ?? '',
      contract.status ?? '',
      contract.jobTitle ?? '',
      contract.department ?? '',
      contract.startDate ?? '',
      contract.endDate ?? '',
      contract.currency ?? '',
      String(contract.salary ?? ''),
      String(contract.hoursPerWeek ?? ''),
      staffName,
      staff?.username ?? '',
      staff?.email ?? '',
      staff?.role ?? '',
      staff?.accountStatus ?? ''
    ]
      .map((value) => String(value).toLowerCase());
  }

  private buildExportConfig() {
    const user = this.authStorage.getUser();
    const generatedBy = user
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || user.username || 'System'
      : 'System';

    return {
      title: this.contractListTitle,
      subtitle: this.contractScope === 'HR'
        ? 'HR contract governance report'
        : 'Staff contract governance report',
      generatedBy,
      summary: [
        { label: 'Total Contracts', value: this.totalContracts },
        { label: 'Active', value: this.activeContracts },
        { label: 'Suspended', value: this.suspendedContracts },
        { label: 'Ended/Expired', value: this.endedContracts },
        { label: 'Archived', value: this.archivedContracts }
      ],
      columns: [
        { key: 'staff', label: 'Account' },
        { key: 'role', label: 'Role' },
        { key: 'reference', label: 'Reference' },
        { key: 'type', label: 'Type' },
        { key: 'state', label: 'Contract Status' },
        { key: 'access', label: 'Account Access' },
        { key: 'startDate', label: 'Start Date' },
        { key: 'endDate', label: 'End Date' }
      ],
      rows: this.sortedContracts.map((contract) => ({
        staff: this.staffDisplay(contract),
        role: this.staffRole(contract),
        reference: contract.contractReference || '-',
        type: contract.contractType,
        state: this.contractStateLabel(contract),
        access: this.accountAccessStatus(contract) === 'USER_ACTIVE' ? 'USER ACTIVE' : 'USER INACTIVE',
        startDate: contract.startDate,
        endDate: contract.endDate
      })),
      emptyText: 'No contracts match the selected filters.'
    };
  }
}
