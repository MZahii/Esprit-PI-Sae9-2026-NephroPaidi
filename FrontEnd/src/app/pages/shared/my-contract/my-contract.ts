import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { DocumentExportService } from '../../../core/services/document-export.service';

interface ContractRow {
  id: number;
  contractReference?: string;
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

@Component({
  selector: 'app-my-contract',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-contract.html',
  styleUrl: './my-contract.scss'
})
export class MyContractComponent implements OnInit {
  loading = false;
  errorMessage = '';
  contracts: ContractRow[] = [];
  role = '';

  constructor(
    private authStorage: AuthStorageService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private documentExportService: DocumentExportService
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.loadMyContracts();
  }

  get isBackoffice(): boolean {
    return this.router.url.startsWith('/backoffice');
  }

  get backRoute(): string {
    return this.isBackoffice ? '/backoffice/dashboard' : '/frontoffice/home';
  }

  get pageTitle(): string {
    return 'My Contract';
  }

  get accountUserId(): number | null {
    const userId = Number(this.authStorage.getUser()?.userId);
    return Number.isFinite(userId) && userId > 0 ? userId : null;
  }

  statusClass(status: string): string {
    if (status === 'ACTIVE') return 'badge-active';
    if (status === 'SUSPENDED') return 'badge-suspended';
    if (status === 'ENDED' || status === 'EXPIRED') return 'badge-ended';
    return 'badge-default';
  }

  printContract(): void {
    this.documentExportService.printDocument(this.buildExportConfig());
  }

  exportContractPdf(): void {
    this.documentExportService.exportPdf(this.buildExportConfig());
  }

  private async loadMyContracts(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    this.contracts = [];

    try {
      const userId = this.accountUserId;
      if (!userId) {
        throw new Error('Unable to detect current user.');
      }

      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(
        this.http.get<ContractRow[] | unknown>(`${environment.apiBaseUrl}/api/contracts?staffUserId=${userId}`, { headers })
      );
      this.contracts = Array.isArray(response) ? response : [];
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to load contract details.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private buildExportConfig() {
    const user = this.authStorage.getUser();
    const generatedBy = user
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() || user.username || 'System'
      : 'System';

    return {
      title: 'My Contract Details',
      subtitle: 'Personal contract document snapshot',
      generatedBy,
      summary: [
        { label: 'Total Contracts', value: this.contracts.length },
        { label: 'Role', value: this.role || '-' },
        { label: 'User', value: generatedBy }
      ],
      columns: [
        { key: 'contractReference', label: 'Reference' },
        { key: 'contractType', label: 'Type' },
        { key: 'status', label: 'Status' },
        { key: 'jobTitle', label: 'Job Title' },
        { key: 'department', label: 'Department' },
        { key: 'startDate', label: 'Start Date' },
        { key: 'endDate', label: 'End Date' },
        { key: 'hoursPerWeek', label: 'Hours/Week' },
        { key: 'salary', label: 'Salary' }
      ],
      rows: this.contracts.map((contract) => ({
        contractReference: contract.contractReference || '-',
        contractType: contract.contractType,
        status: contract.status,
        jobTitle: contract.jobTitle || '-',
        department: contract.department || '-',
        startDate: contract.startDate || '-',
        endDate: contract.endDate || '-',
        hoursPerWeek: contract.hoursPerWeek ?? '-',
        salary: `${contract.salary ?? '-'} ${contract.currency || 'TND'}`
      })),
      emptyText: 'No contract found for the current account.'
    };
  }
}
