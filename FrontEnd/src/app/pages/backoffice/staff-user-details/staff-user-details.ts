import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, forkJoin } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';

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
  accountStatus: string;
  enabled: boolean;
  avatarUrl?: string;
  deleted?: boolean;
}

interface ContractRow {
  id: number;
  staffUserId: number;
  contractReference: string;
  contractType: string;
  status: string;
  jobTitle: string;
  department?: string;
  startDate: string;
  endDate: string;
  salary: number;
  currency?: string;
  hoursPerWeek: number;
  notes?: string;
}

interface UserAuditLogRow {
  id: number;
  userId: number;
  action: string;
  actor?: string;
  oldValue?: string;
  newValue?: string;
  createdAt: string;
}

interface LogDiffRow {
  field: string;
  oldValue: string;
  newValue: string;
}

interface LogDiffInfo {
  rows: LogDiffRow[];
  parsed: boolean;
}

type StaffRole = 'DOCTOR' | 'NURSE' | 'SURGEON' | 'PHARMACIST' | 'RECEPTIONIST' | 'LAB_AGENT';
type Sex = 'MALE' | 'FEMALE';
type ContractType = 'CDI' | 'CDD' | 'INTERNSHIP' | 'PART_TIME' | 'TEMPORARY';

@Component({
  selector: 'app-staff-user-details',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './staff-user-details.html',
  styleUrl: './staff-user-details.scss'
})
export class StaffUserDetails implements OnInit {
  @Input() modalMode = false;
  @Input() userId: number | null = null;
  @Output() closeModal = new EventEmitter<void>();

  loading = false;
  savingProfile = false;
  savingContractId: number | null = null;
  lifecycleLoading = false;
  auditLoading = false;
  errorMessage = '';
  successMessage = '';
  profileStatsOpen = false;
  role = '';
  returnTo: 'staff' | 'staff-details' | 'contracts' | 'hr-list' = 'staff';
  backRoute = '/backoffice/staff';
  backLabel = 'Back To Staff List';
  user: UserRow | null = null;
  contracts: ContractRow[] = [];
  auditLogs: UserAuditLogRow[] = [];
  editProfile = false;
  editingContractId: number | null = null;
  auditSearch = '';
  auditActionFilter = 'ALL';
  private auditDiffCache = new Map<number, LogDiffInfo>();

  profileForm = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    sex: 'MALE' as Sex,
    role: 'DOCTOR' as StaffRole
  };

  contractForm = {
    contractType: 'CDI' as ContractType,
    jobTitle: '',
    department: '',
    startDate: '',
    endDate: '',
    salary: 0,
    hoursPerWeek: '' as number | '',
    notes: ''
  };

  readonly staffRoles: StaffRole[] = ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];
  readonly sexOptions: Sex[] = ['MALE', 'FEMALE'];
  readonly contractTypeOptions: ContractType[] = ['CDI', 'CDD', 'INTERNSHIP', 'PART_TIME', 'TEMPORARY'];
  readonly hoursPerWeekOptionsByType: Record<ContractType, number[]> = {
    CDI: [35, 40, 45],
    CDD: [30, 35, 40],
    INTERNSHIP: [20, 25, 30, 35],
    PART_TIME: [10, 15, 20, 25, 30],
    TEMPORARY: [20, 30, 40]
  };

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private authStorage: AuthStorageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.resolveReturnNavigation();
    this.loadUserDetails();
  }

  openProfileStats(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.profileStatsOpen = true;
  }

  closeProfileStats(): void {
    this.profileStatsOpen = false;
  }

  private resolveReturnNavigation(): void {
    const requested = this.route.snapshot.queryParamMap.get('returnTo');
    if (requested === 'contracts') {
      this.returnTo = 'contracts';
      this.backRoute = '/backoffice/contracts';
      this.backLabel = 'Back To Contracts';
      return;
    }

    if (requested === 'hr-list') {
      this.returnTo = 'hr-list';
      this.backRoute = '/backoffice/hr-list';
      this.backLabel = 'Back To HR List';
      return;
    }

    if (requested === 'staff-details' || this.role === 'ADMIN') {
      this.returnTo = 'staff-details';
      this.backRoute = '/backoffice/staff-details';
      this.backLabel = 'Back To Staff Details';
      return;
    }

    this.returnTo = 'staff';
    this.backRoute = '/backoffice/staff';
    this.backLabel = 'Back To Staff List';
  }

  get fullName(): string {
    if (!this.user) return '-';
    const name = `${this.user.firstName ?? ''} ${this.user.lastName ?? ''}`.trim();
    return name || '-';
  }

  get isHr(): boolean {
    return this.role === 'HR';
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get isHrUser(): boolean {
    return this.user?.role === 'HR';
  }

  get canEditProfile(): boolean {
    if (!this.user) return false;
    if (this.user.deleted) return false;
    if (this.isAdmin) return this.isHrUser;
    if (this.isHr) return !this.isHrUser;
    return false;
  }

  get canToggleAccountStatus(): boolean {
    if (!this.user) return false;
    if (this.isHr) return this.staffRoles.includes(this.user.role as StaffRole);
    return this.isAdmin && this.isHrUser;
  }

  get canArchiveUser(): boolean {
    if (!this.user) return false;
    if (this.isHr) return this.staffRoles.includes(this.user.role as StaffRole) && !this.user.deleted;
    return this.isAdmin && this.isHrUser && !this.user.deleted;
  }

  get canRestoreUser(): boolean {
    if (!this.user) return false;
    if (this.isHr) return this.staffRoles.includes(this.user.role as StaffRole) && !!this.user.deleted;
    return this.isAdmin && this.isHrUser && !!this.user.deleted;
  }

  get canManageContracts(): boolean {
    if (this.user?.deleted) return false;
    if (this.isHr) return true;
    return this.role === 'ADMIN' && this.user?.role === 'HR';
  }

  get fixedContractJobTitle(): string {
    if (!this.user) return '';
    return this.resolveRoleDescriptor(this.user.role).jobTitle;
  }

  get fixedContractDepartment(): string {
    if (!this.user) return '';
    return this.resolveRoleDescriptor(this.user.role).department;
  }

  get hoursPerWeekOptions(): number[] {
    return this.hoursPerWeekOptionsByType[this.contractForm.contractType] ?? [];
  }

  get pageTitle(): string {
    return this.user?.role === 'HR' ? 'HR User Details' : 'Staff User Details';
  }

  get canViewAudit(): boolean {
    return this.isAdmin || this.isHr;
  }

  get auditActions(): string[] {
    return Array.from(new Set(this.auditLogs.map((item) => item.action).filter(Boolean))).sort();
  }

  get filteredAuditLogs(): UserAuditLogRow[] {
    const term = this.auditSearch.trim().toLowerCase();
    return this.auditLogs.filter((log) => {
      const actionMatch = this.auditActionFilter === 'ALL' || log.action === this.auditActionFilter;
      const termMatch = !term || this.auditTokens(log).some((value) => value.includes(term));
      return actionMatch && termMatch;
    });
  }

  get activeContractCount(): number {
    return this.contracts.filter((contract) => ['ACTIVE', 'SUSPENDED'].includes(contract.status)).length;
  }

  get endedContractCount(): number {
    return this.contracts.filter((contract) => ['ENDED', 'EXPIRED'].includes(contract.status)).length;
  }

  get latestContractStatus(): string {
    return this.contracts[0]?.status ?? 'NO CONTRACT';
  }

  get auditEventCount(): number {
    return this.auditLogs.length;
  }

  get profileCompleteness(): number {
    if (!this.user) return 0;
    const fields = [this.user.firstName, this.user.lastName, this.user.email, this.user.phone, this.user.dateOfBirth, this.user.sex, this.user.cin];
    const filled = fields.filter((value) => String(value ?? '').trim().length > 0).length;
    return Math.round((filled / fields.length) * 100);
  }

  startProfileEdit(): void {
    if (!this.user || !this.canEditProfile) return;
    this.editProfile = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.profileForm = {
      firstName: this.user.firstName ?? '',
      lastName: this.user.lastName ?? '',
      email: this.user.email ?? '',
      phone: this.user.phone ?? '',
      dateOfBirth: this.user.dateOfBirth ?? '',
      sex: (this.user.sex as Sex) || 'MALE',
      role: (this.user.role as StaffRole) || 'DOCTOR'
    };
  }

  cancelProfileEdit(): void {
    this.editProfile = false;
  }

  startContractEdit(contract: ContractRow): void {
    if (!this.canManageContracts) return;
    this.editingContractId = contract.id;
    this.successMessage = '';
    this.errorMessage = '';
    this.contractForm = {
      contractType: contract.contractType as ContractType,
      jobTitle: this.fixedContractJobTitle,
      department: this.fixedContractDepartment,
      startDate: contract.startDate ?? '',
      endDate: contract.endDate ?? '',
      salary: Number(contract.salary ?? 0),
      hoursPerWeek: Number(contract.hoursPerWeek ?? 40),
      notes: contract.notes ?? ''
    };
  }

  cancelContractEdit(): void {
    this.editingContractId = null;
  }

  async saveProfile(): Promise<void> {
    if (!this.user || !this.canEditProfile || this.savingProfile) return;
    this.savingProfile = true;
    this.successMessage = '';
    this.errorMessage = '';
    try {
      const firstName = this.profileForm.firstName.trim();
      const lastName = this.profileForm.lastName.trim();
      const email = this.profileForm.email.trim();
      const dateOfBirth = this.profileForm.dateOfBirth;

      if (firstName.length < 3 || lastName.length < 3) {
        throw new Error('First name and last name must have at least 3 letters.');
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        throw new Error('Please enter a valid email.');
      }
      if (!this.isAdult(dateOfBirth)) {
        throw new Error('Account age must be at least 18 years.');
      }

      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`
      });
      const isHrAccount = this.user.role === 'HR';
      const payload = isHrAccount
        ? {
            firstName,
            lastName,
            email,
            dateOfBirth,
            sex: this.profileForm.sex
          }
        : {
            firstName,
            lastName,
            email,
            phone: this.user.phone || null,
            dateOfBirth,
            sex: this.profileForm.sex,
            role: this.user.role
          };
      const endpoint = isHrAccount
        ? `${environment.apiBaseUrl}/api/users/hr/${this.user.id}`
        : `${environment.apiBaseUrl}/api/users/staff/${this.user.id}`;

      const updated = await firstValueFrom(
        this.http.patch<UserRow>(
          endpoint,
          payload,
          { headers }
        )
      );
      this.user = updated;
      this.editProfile = false;
      this.successMessage = 'Profile updated successfully.';
      await this.loadUserDetails();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to update profile.';
    } finally {
      this.savingProfile = false;
      this.cdr.detectChanges();
    }
  }

  async saveContract(contractId: number): Promise<void> {
    if (!this.canManageContracts || this.savingContractId !== null) return;
    this.savingContractId = contractId;
    this.successMessage = '';
    this.errorMessage = '';
    try {
      this.syncContractRoleFields();
      if (!this.contractForm.jobTitle || !this.contractForm.department) {
        throw new Error('Contract role metadata could not be resolved.');
      }
      if (this.contractForm.hoursPerWeek === '' || !this.hoursPerWeekOptions.includes(Number(this.contractForm.hoursPerWeek))) {
        throw new Error('Please select valid hours/week based on contract type.');
      }
      const salaryValue = Number(this.contractForm.salary);
      if (!Number.isFinite(salaryValue) || salaryValue <= 0) {
        throw new Error('Salary must be a positive number greater than 0.');
      }
      if (new Date(this.contractForm.endDate).getTime() < new Date(this.contractForm.startDate).getTime()) {
        throw new Error('End date must be after start date.');
      }

      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`
      });
      const payload = {
        contractType: this.contractForm.contractType,
        jobTitle: this.contractForm.jobTitle,
        department: this.contractForm.department || null,
        startDate: this.contractForm.startDate,
        endDate: this.contractForm.endDate,
        salary: salaryValue,
        currency: 'TND',
        hoursPerWeek: Number(this.contractForm.hoursPerWeek),
        notes: this.contractForm.notes || null
      };
      const updated = await firstValueFrom(
        this.http.put<ContractRow>(
          `${environment.apiBaseUrl}/api/contracts/${contractId}`,
          payload,
          { headers }
        )
      );
      this.contracts = this.contracts.map(c => c.id === contractId ? updated : c);
      this.editingContractId = null;
      this.successMessage = 'Contract updated successfully.';
      await this.loadUserDetails();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to update contract.';
    } finally {
      this.savingContractId = null;
      this.cdr.detectChanges();
    }
  }

  onContractTypeChange(): void {
    if (this.contractForm.hoursPerWeek === '') return;
    if (!this.hoursPerWeekOptions.includes(Number(this.contractForm.hoursPerWeek))) {
      this.contractForm.hoursPerWeek = '';
    }
  }

  private syncContractRoleFields(): void {
    if (!this.user) {
      this.contractForm.jobTitle = '';
      this.contractForm.department = '';
      return;
    }
    const descriptor = this.resolveRoleDescriptor(this.user.role);
    this.contractForm.jobTitle = descriptor.jobTitle;
    this.contractForm.department = descriptor.department;
  }

  private resolveRoleDescriptor(role: string): { jobTitle: string; department: string } {
    switch (role) {
      case 'HR':
        return { jobTitle: 'HR Manager', department: 'Human Resources' };
      case 'DOCTOR':
        return { jobTitle: 'Doctor', department: 'Medical' };
      case 'NURSE':
        return { jobTitle: 'Nurse', department: 'Nursing' };
      case 'SURGEON':
        return { jobTitle: 'Surgeon', department: 'Surgery' };
      case 'PHARMACIST':
        return { jobTitle: 'Pharmacist', department: 'Pharmacy' };
      case 'RECEPTIONIST':
        return { jobTitle: 'Receptionist', department: 'Reception' };
      case 'LAB_AGENT':
        return { jobTitle: 'Lab Agent', department: 'Laboratory' };
      default:
        return { jobTitle: '', department: '' };
    }
  }

  async toggleUserAccountStatus(): Promise<void> {
    if (!this.user || !this.canToggleAccountStatus || this.user.deleted) return;
    const hasRunningContract = this.contracts.some(c => c.status === 'ACTIVE' || c.status === 'SUSPENDED');
    const target = this.user.accountStatus === 'INACTIVE'
      ? (hasRunningContract ? 'ACTIVE' : 'PENDING_CONTRACT')
      : 'INACTIVE';
    const confirmed = window.confirm(`Confirm changing ${this.user.username} status to ${target}?`);
    if (!confirmed) return;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/contracts/staff/${this.user.id}/status?status=${target}`,
        {},
        { headers }
      ));
      await this.loadUserDetails();
      this.successMessage = `Account status changed to ${target}.`;
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to update account status.';
    } finally {
      this.cdr.detectChanges();
    }
  }

  async softDeleteUser(): Promise<void> {
    if (!this.user || !this.canArchiveUser || this.lifecycleLoading) return;
    const confirmed = window.confirm(
      `Confirm archiving "${this.user.username}"? This disables the account and removes it from normal backend listings until restored.`
    );
    if (!confirmed) return;

    this.lifecycleLoading = true;
    this.successMessage = '';
    this.errorMessage = '';
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const archived = await firstValueFrom(
        this.http.patch<UserRow>(
          `${environment.apiBaseUrl}/api/users/${this.user.id}/soft-delete`,
          {},
          { headers }
        )
      );
      this.user = {
        ...archived,
        deleted: true,
        enabled: false,
        accountStatus: 'INACTIVE'
      };
      this.successMessage = 'User archived successfully. This details page stays available so you can restore it immediately if needed.';
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to archive user.';
    } finally {
      this.lifecycleLoading = false;
      this.cdr.detectChanges();
    }
  }

  async restoreUser(): Promise<void> {
    if (!this.user || !this.canRestoreUser || this.lifecycleLoading) return;
    const confirmed = window.confirm(`Confirm restoring archived user "${this.user.username}"?`);
    if (!confirmed) return;

    this.lifecycleLoading = true;
    this.successMessage = '';
    this.errorMessage = '';
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const restored = await firstValueFrom(
        this.http.patch<UserRow>(
          `${environment.apiBaseUrl}/api/users/${this.user.id}/restore`,
          {},
          { headers }
        )
      );
      this.user = {
        ...restored,
        deleted: false
      };
      this.successMessage = 'User restored successfully.';
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to restore user.';
    } finally {
      this.lifecycleLoading = false;
      this.cdr.detectChanges();
    }
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

  async runContractAction(contractId: number, action: 'suspend' | 'resume' | 'end'): Promise<void> {
    if (!this.canManageContracts) return;
    const confirmed = window.confirm(`Confirm ${action.toUpperCase()} contract?`);
    if (!confirmed) return;

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/contracts/${contractId}/${action}`,
        {},
        { headers }
      ));
      await this.loadUserDetails();
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || `Failed to ${action} contract.`;
      this.cdr.detectChanges();
    }
  }

  onAuditFiltersChanged(): void {
    this.cdr.detectChanges();
  }

  getChangedFields(log: UserAuditLogRow): LogDiffRow[] {
    return this.getAuditDiffInfo(log).rows;
  }

  hasParsedAuditDetails(log: UserAuditLogRow): boolean {
    return this.getAuditDiffInfo(log).parsed;
  }

  hasRawAuditDetails(log: UserAuditLogRow): boolean {
    return !!(log.oldValue || log.newValue);
  }

  getRawOld(log: UserAuditLogRow): string {
    return log.oldValue || '-';
  }

  getRawNew(log: UserAuditLogRow): string {
    return log.newValue || '-';
  }

  private async loadUserDetails(): Promise<void> {
    this.loading = true;
    this.auditLoading = false;
    this.errorMessage = '';
    this.successMessage = '';
    this.user = null;
    this.contracts = [];
    this.auditLogs = [];

    try {
      const userId = Number(this.userId ?? this.route.snapshot.paramMap.get('id'));
      if (!Number.isFinite(userId) || userId <= 0) {
        throw new Error('Invalid staff user id.');
      }

      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`
      });

      const response = await firstValueFrom(forkJoin({
        users: this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers }),
        contracts: this.http.get<ContractRow[] | unknown>(
          `${environment.apiBaseUrl}/api/contracts?staffUserId=${userId}`,
          { headers }
        )
      }));

      const users = Array.isArray(response?.users) ? response.users : [];
      const selected = users.find(u => u.id === userId) ?? null;
      if (!selected) {
        throw new Error('Staff user not found.');
      }

      this.user = { ...selected, deleted: false };
      this.contracts = Array.isArray(response?.contracts) ? response.contracts : [];
      this.auditLogs = [];
      this.auditDiffCache.clear();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to load staff details.';
    } finally {
      this.loading = false;
      this.auditLoading = false;
      this.cdr.detectChanges();
    }
  }

  private getAuditDiffInfo(log: UserAuditLogRow): LogDiffInfo {
    const cached = this.auditDiffCache.get(log.id);
    if (cached) return cached;

    const oldObj = this.parseJsonObject(log.oldValue);
    const newObj = this.parseJsonObject(log.newValue);
    const parsed = !!oldObj || !!newObj;
    const keys = new Set<string>([
      ...Object.keys(oldObj ?? {}),
      ...Object.keys(newObj ?? {})
    ]);

    const diffs: LogDiffRow[] = [];
    keys.forEach((key) => {
      if (key === 'id') return;
      const oldVal = oldObj ? oldObj[key] : undefined;
      const newVal = newObj ? newObj[key] : undefined;
      if (!this.valuesEqual(oldVal, newVal)) {
        diffs.push({
          field: this.humanizeField(key),
          oldValue: this.stringifyValue(oldVal),
          newValue: this.stringifyValue(newVal)
        });
      }
    });

    const info: LogDiffInfo = { rows: diffs, parsed };
    this.auditDiffCache.set(log.id, info);
    return info;
  }

  private parseJsonObject(raw?: string): Record<string, unknown> | null {
    if (!raw) return null;
    const value = raw.trim();
    try {
      const parsed = JSON.parse(value);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed as Record<string, unknown>;
      if (typeof parsed === 'string') {
        const nested = parsed.trim();
        if (nested.startsWith('{') && nested.endsWith('}')) {
          const nestedParsed = JSON.parse(nested);
          if (nestedParsed && typeof nestedParsed === 'object' && !Array.isArray(nestedParsed)) {
            return nestedParsed as Record<string, unknown>;
          }
        }
      }
      if (value.startsWith('{') && value.endsWith('}')) {
        const direct = JSON.parse(value);
        if (direct && typeof direct === 'object' && !Array.isArray(direct)) {
          return direct as Record<string, unknown>;
        }
      }
    } catch {
      return null;
    }
    return null;
  }

  private valuesEqual(a: unknown, b: unknown): boolean {
    return JSON.stringify(a) === JSON.stringify(b);
  }

  private stringifyValue(value: unknown): string {
    if (value === null || value === undefined || value === '') return '-';
    if (typeof value === 'object') {
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    }
    return String(value);
  }

  private humanizeField(field: string): string {
    const labels: Record<string, string> = {
      accountStatus: 'Account Status',
      firstName: 'First Name',
      lastName: 'Last Name',
      dateOfBirth: 'Date Of Birth',
      keycloakId: 'Keycloak ID'
    };
    if (labels[field]) return labels[field];
    return field
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  private auditTokens(log: UserAuditLogRow): string[] {
    return [
      log.action ?? '',
      log.actor ?? '',
      log.createdAt ?? '',
      log.oldValue ?? '',
      log.newValue ?? '',
      String(log.userId ?? '')
    ].map((value) => String(value).toLowerCase());
  }
}
