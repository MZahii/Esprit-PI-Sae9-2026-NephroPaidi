import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, forkJoin } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

type ContractType = 'CDI' | 'CDD' | 'INTERNSHIP' | 'PART_TIME' | 'TEMPORARY';

interface UserRow {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  role: string;
}

interface ContractRow {
  staffUserId: number;
  status?: 'ACTIVE' | 'SUSPENDED' | 'ENDED' | 'EXPIRED';
}

interface CreatedContractResponse {
  id: number;
  staffUserId: number;
  contractReference: string;
  contractType: ContractType;
  status: 'ACTIVE' | 'SUSPENDED' | 'ENDED' | 'EXPIRED';
  jobTitle: string;
  department?: string;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-create-contract',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './create-contract.html',
  styleUrl: './create-contract.scss'
})
export class CreateContract implements OnInit {
  loading = false;
  loadingStaff = false;
  successMessage = '';
  errorMessage = '';
  contractId: number | null = null;
  role = '';
  backRoute = '/backoffice/contracts';
  backLabel = 'BACK TO CONTRACTS';
  fromSource = '';
  lockedStaffUserId: number | null = null;

  staffUsers: UserRow[] = [];
  preselectedStaffName = '';
  selectedAccountLabel = 'Staff Account';

  readonly contractTypes: ContractType[] = ['CDI', 'CDD', 'INTERNSHIP', 'PART_TIME', 'TEMPORARY'];
  readonly hoursPerWeekOptionsByType: Record<ContractType, number[]> = {
    CDI: [35, 40, 45],
    CDD: [30, 35, 40],
    INTERNSHIP: [20, 25, 30, 35],
    PART_TIME: [10, 15, 20, 25, 30],
    TEMPORARY: [20, 30, 40]
  };

  form = {
    staffUserId: '' as number | '',
    contractReference: '',
    contractType: '' as ContractType | '',
    jobTitle: '',
    department: '',
    startDate: '',
    endDate: '',
    salary: '' as number | '',
    hoursPerWeek: '' as number | '',
    notes: ''
  };

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private authStorage: AuthStorageService
  ) {}

  private showError(message: string): void {
    this.successMessage = '';
    this.errorMessage = message;
  }

  private showSuccess(message: string): void {
    this.errorMessage = '';
    this.successMessage = message;
  }

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.selectedAccountLabel = this.isAdmin ? 'HR Account' : 'Staff Account';
    if (this.isAdmin) {
      this.backRoute = '/backoffice/hr-list';
      this.backLabel = 'BACK TO HR LIST';
    }

    this.route.queryParamMap.subscribe(params => {
      const staffUserId = params.get('staffUserId');
      this.preselectedStaffName = params.get('staffName') ?? '';
      this.fromSource = params.get('from') ?? '';
      const contractId = params.get('contractId');
      this.contractId = contractId ? Number(contractId) : null;
      this.lockedStaffUserId = staffUserId ? Number(staffUserId) : null;
      if (staffUserId) {
        this.form.staffUserId = Number(staffUserId);
      }
      if (this.contractId) {
        this.loadContractForEdit(this.contractId);
      }
    });

    this.loadStaffUsers();
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get isStaffLocked(): boolean {
    return Number.isFinite(this.lockedStaffUserId) && (this.lockedStaffUserId ?? 0) > 0;
  }

  get pageTitle(): string {
    const target = this.isAdmin ? 'HR' : 'Staff';
    return this.contractId ? `Edit ${target} Contract` : `Create ${target} Contract`;
  }

  get cardTitle(): string {
    const target = this.isAdmin ? 'HR Contract' : 'Contract';
    return this.contractId ? `Update ${target}` : `New ${target}`;
  }

  get selectedStaffDisplayName(): string {
    if (this.preselectedStaffName.trim()) {
      return this.preselectedStaffName.trim();
    }
    const selected = this.staffUsers.find(u => u.id === Number(this.form.staffUserId));
    return selected ? this.fullName(selected) : '-';
  }

  get backRouteQueryParams(): Record<string, string> | undefined {
    if (this.isAdmin) {
      return { scope: 'HR' };
    }
    return undefined;
  }

  get hoursPerWeekOptions(): number[] {
    if (!this.form.contractType) return [];
    return this.hoursPerWeekOptionsByType[this.form.contractType] ?? [];
  }

  get fixedJobTitle(): string {
    const selectedUser = this.staffUsers.find((user) => user.id === Number(this.form.staffUserId));
    return this.resolveRoleDescriptor(selectedUser?.role ?? '').jobTitle;
  }

  get fixedDepartment(): string {
    const selectedUser = this.staffUsers.find((user) => user.id === Number(this.form.staffUserId));
    return this.resolveRoleDescriptor(selectedUser?.role ?? '').department;
  }

  fullName(user: UserRow): string {
    const full = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return full || user.username;
  }

  onContractTypeChange(): void {
    if (this.form.hoursPerWeek === '') return;
    if (!this.hoursPerWeekOptions.includes(Number(this.form.hoursPerWeek))) {
      this.form.hoursPerWeek = '';
    }
  }

  async loadStaffUsers(): Promise<void> {
    this.loadingStaff = true;
    this.showError('');
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(forkJoin({
        users: this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers }),
        contracts: this.http.get<ContractRow[] | unknown>(`${environment.apiBaseUrl}/api/contracts`, { headers })
      }));
      const users = Array.isArray(response?.users) ? response.users : [];
      const contracts = Array.isArray(response?.contracts) ? response.contracts : [];
      const usersWithRunningContract = new Set(
        contracts
          .filter((c) => c.status === 'ACTIVE' || c.status === 'SUSPENDED')
          .map(c => c.staffUserId)
      );

      const allowedRoles = this.isAdmin
        ? ['HR']
        : ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];

      const contractUsers = users.filter((user) => allowedRoles.includes(user.role));
      const availableUsers = contractUsers.filter(user => !usersWithRunningContract.has(user.id));

      if (this.isStaffLocked && this.lockedStaffUserId) {
        const selected = contractUsers.find(u => u.id === this.lockedStaffUserId);
        this.staffUsers = selected ? [selected] : [];
      } else {
        this.staffUsers = availableUsers;
      }

      if (this.contractId && this.form.staffUserId !== '') {
        const selectedId = Number(this.form.staffUserId);
        const selected = contractUsers.find(u => u.id === selectedId);
        if (selected && !this.staffUsers.some(u => u.id === selected.id)) {
          this.staffUsers = [selected, ...this.staffUsers];
        }
      }
      this.syncFixedRoleFields();
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.showError(
        error instanceof Error
          ? err?.error?.message || err?.message || error.message || 'Failed to load staff users.'
          : 'Authentication problem. Please login again.'
      );
    } finally {
      this.loadingStaff = false;
      this.cdr.detectChanges();
    }
  }

  async submit(): Promise<void> {
    this.showError('');
    this.successMessage = '';
    if (this.loading) return;

    const selectedUser = this.staffUsers.find(u => u.id === Number(this.form.staffUserId));
    if (!selectedUser) {
      this.showError(`Please select a valid ${this.selectedAccountLabel.toLowerCase()}.`);
      return;
    }
    if (this.isAdmin && selectedUser.role !== 'HR') {
      this.showError('ADMIN can create contracts only for HR accounts from this page.');
      return;
    }
    if (!this.isAdmin && selectedUser.role === 'HR') {
      this.showError('HR users cannot create HR contracts.');
      return;
    }
    if (!this.form.contractType) {
      this.showError('Please select a contract type.');
      return;
    }
    this.syncFixedRoleFields();
    if (!this.form.jobTitle.trim() || !this.form.department.trim()) {
      this.showError('Job title and department are fixed by the selected staff role.');
      return;
    }
    if (!this.form.startDate || !this.form.endDate) {
      this.showError('Start date and end date are required.');
      return;
    }
    if (new Date(this.form.endDate).getTime() < new Date(this.form.startDate).getTime()) {
      this.showError('End date must be after start date.');
      return;
    }
    if (this.form.hoursPerWeek === '' || !this.hoursPerWeekOptions.includes(Number(this.form.hoursPerWeek))) {
      this.showError('Please select hours per week based on contract type.');
      return;
    }
    const salaryValue = Number(this.form.salary);
    if (!Number.isFinite(salaryValue) || salaryValue <= 0) {
      this.showError('Salary must be a positive number greater than 0.');
      return;
    }

    this.loading = true;

    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      });

      const payload = {
        staffUserId: this.form.staffUserId,
        contractType: this.form.contractType,
        jobTitle: this.form.jobTitle.trim(),
        department: this.form.department.trim() || null,
        startDate: this.form.startDate,
        endDate: this.form.endDate,
        salary: salaryValue,
        currency: 'TND',
        hoursPerWeek: typeof this.form.hoursPerWeek === 'number' ? this.form.hoursPerWeek : null,
        notes: this.form.notes.trim() || null
      };

      const request$ = this.contractId
        ? this.http.put<CreatedContractResponse>(`${environment.apiBaseUrl}/api/contracts/${this.contractId}`, payload, { headers })
        : this.http.post<CreatedContractResponse>(`${environment.apiBaseUrl}/api/contracts`, payload, { headers });

      const savedContract = await firstValueFrom(request$);
      this.showSuccess(
        this.contractId
          ? 'Contract updated successfully. Returning to the previous page...'
          : 'Contract created successfully. Returning to the previous page...'
      );
      this.cdr.detectChanges();
      setTimeout(() => {
        void this.router.navigate([this.backRoute], {
          queryParams: this.backRouteQueryParams,
          state: {
            actionMessage: this.contractId
              ? 'The contract was updated successfully.'
              : `The contract for ${this.selectedStaffDisplayName} was created successfully.`
          }
        });
      }, 1200);
    } catch (error: unknown) {
      if (error instanceof HttpErrorResponse) {
        const backendMessage = (error.error && typeof error.error === 'object' && 'message' in error.error)
          ? String((error.error as { message?: string }).message ?? '')
          : '';
        this.showError(
          backendMessage ||
          error.message ||
          `Request failed with status ${error.status}.`
        );
        return;
      }

      const err = error as { error?: { validationErrors?: Record<string, string>; message?: string }; message?: string };
      if (err?.error?.validationErrors) {
        this.showError(Object.values(err.error.validationErrors).join(' | '));
      } else {
        this.showError(
          error instanceof Error
            ? err?.error?.message || err?.message || error.message || 'Failed to create contract.'
            : 'Authentication problem. Please login again.'
        );
      }
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  goToBackRoute(): void {
    void this.router.navigate([this.backRoute], {
      queryParams: this.backRouteQueryParams
    });
  }

  private async loadContractForEdit(contractId: number): Promise<void> {
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const contract = await firstValueFrom(this.http.get<any>(`${environment.apiBaseUrl}/api/contracts/${contractId}`, { headers }));
      this.form.staffUserId = contract.staffUserId;
      this.form.contractReference = contract.contractReference ?? '';
      this.form.contractType = contract.contractType ?? '';
      this.syncFixedRoleFields();
      this.form.startDate = contract.startDate ?? '';
      this.form.endDate = contract.endDate ?? '';
      this.form.salary = contract.salary ?? '';
      this.form.hoursPerWeek = contract.hoursPerWeek ?? '';
      this.form.notes = contract.notes ?? '';
      this.cdr.detectChanges();
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.showError(
        error instanceof Error
          ? err?.error?.message || err?.message || error.message
          : 'Failed to load contract for editing.'
      );
    }
  }

  private syncFixedRoleFields(): void {
    const selectedUser = this.staffUsers.find((user) => user.id === Number(this.form.staffUserId));
    if (!selectedUser) {
      this.form.jobTitle = '';
      this.form.department = '';
      return;
    }
    const descriptor = this.resolveRoleDescriptor(selectedUser.role);
    this.form.jobTitle = descriptor.jobTitle;
    this.form.department = descriptor.department;
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

  onStaffUserChange(): void {
    this.syncFixedRoleFields();
  }
}
