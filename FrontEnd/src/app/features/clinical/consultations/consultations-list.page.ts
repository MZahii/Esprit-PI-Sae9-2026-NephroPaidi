import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';

type ConsultationStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

@Component({
  selector: 'app-consultations-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultations-list.page.html',
  styleUrl: './consultations-list.page.scss'
})
export class ConsultationsListPage implements OnInit {
  loading = false;
  error = '';

  consultations: any[] = [];
  readonly pageSize = 10;
  page = 1;

  filters = {
    patientQuery: '',
    status: 'ALL' as ConsultationStatus | 'ALL'
  };

  statuses: Array<ConsultationStatus | 'ALL'> = ['ALL', 'OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  constructor(private api: ClinicalApiService) {}

  ngOnInit(): void {
    this.loadConsultations();
  }

  loadConsultations(): void {
    this.loading = true;
    this.error = '';
    const status = this.filters.status === 'ALL' ? undefined : this.filters.status;
    this.api.listMyConsultations({
      patientQuery: this.filters.patientQuery?.trim() || undefined,
      status
    }).subscribe({
      next: (items) => {
        this.consultations = items || [];
        this.page = 1;
        this.loading = false;
      },
      error: () => {
        this.consultations = [];
        this.loading = false;
        this.error = 'Failed to load consultations.';
      }
    });
  }

  clearFilters(): void {
    this.filters = { patientQuery: '', status: 'ALL' };
    this.page = 1;
    this.loadConsultations();
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'CANCELLED') return 'bg-soft-danger text-danger';
    if (status === 'IN_PROGRESS' || status === 'CONFIRMED') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  statusLabel(status?: string): string {
    const normalized = String(status || 'OPEN').trim().toUpperCase();
    switch (normalized) {
      case 'OPEN':
      case 'SCHEDULED':
        return 'Scheduled';
      case 'IN_PROGRESS':
      case 'CONFIRMED':
        return 'In progress';
      case 'COMPLETED':
        return 'Concluded';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return normalized
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase());
    }
  }

  statusOptionLabel(status: ConsultationStatus | 'ALL'): string {
    return status === 'ALL' ? 'All statuses' : this.statusLabel(status);
  }

  get totalCount(): number {
    return this.consultations.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.consultations.length / this.pageSize));
  }

  get pagedConsultations(): any[] {
    const safePage = Math.min(this.page, this.totalPages);
    if (safePage !== this.page) {
      this.page = safePage;
    }
    const start = (this.page - 1) * this.pageSize;
    return this.consultations.slice(start, start + this.pageSize);
  }

  get fromItem(): number {
    if (!this.consultations.length) return 0;
    return (this.page - 1) * this.pageSize + 1;
  }

  get toItem(): number {
    if (!this.consultations.length) return 0;
    return Math.min(this.page * this.pageSize, this.consultations.length);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.page = page;
  }

  previousPage(): void {
    this.goToPage(this.page - 1);
  }

  nextPage(): void {
    this.goToPage(this.page + 1);
  }

  get inProgressCount(): number {
    return this.consultations.filter(item => ['IN_PROGRESS', 'CONFIRMED'].includes(String(item.status || '').toUpperCase())).length;
  }

  get completedCount(): number {
    return this.consultations.filter(item => item.status === 'COMPLETED').length;
  }

  /**
   * Generate return URL to preserve current filter/page state when drilling into details or workspace.
   * Example: /backoffice/consultations?patientQuery=John&status=IN_PROGRESS&page=2
   */
  getReturnUrl(): string {
    const params: string[] = ['/backoffice/consultations'];
    if (this.filters.patientQuery?.trim()) {
      params.push(`patientQuery=${encodeURIComponent(this.filters.patientQuery.trim())}`);
    }
    if (this.filters.status !== 'ALL') {
      params.push(`status=${this.filters.status}`);
    }
    if (this.page > 1) {
      params.push(`page=${this.page}`);
    }
    return params.length > 1 ? params[0] + '?' + params.slice(1).join('&') : params[0];
  }
}
