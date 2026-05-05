import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DocumentExportService } from '../../../core/services/document-export.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

interface ClinicalAuditEvent {
  id: number;
  entityType: string;
  entityId: string;
  action: string;
  actorId?: string;
  actorUsername?: string;
  actorRole?: string;
  details?: string;
  createdAt: string;
}

@Component({
  selector: 'app-clinical-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './clinical-audit-logs.html',
  styleUrl: './clinical-audit-logs.scss'
})
export class ClinicalAuditLogsComponent implements OnInit {
  loading = false;
  errorMessage = '';
  search = '';
  actionFilter = 'ALL';
  logs: ClinicalAuditEvent[] = [];
  currentPage = 1;
  pageSize = 20;
  readonly pageSizeOptions = [10, 20, 50];

  async ngOnInit(): Promise<void> {
    await this.loadLogs();
  }

  get actionOptions(): string[] {
    const actions = new Set(this.logs.map((l) => l.action).filter(Boolean));
    return ['ALL', ...Array.from(actions)];
  }

  get filteredLogs(): ClinicalAuditEvent[] {
    const term = this.search.trim().toLowerCase();
    return this.logs.filter((log) => {
      const actionMatch = this.actionFilter === 'ALL' || log.action === this.actionFilter;
      if (!actionMatch) return false;
      if (!term) return true;
      const tokens = [
        log.entityType,
        log.entityId,
        log.action,
        log.actorUsername,
        log.actorRole,
        log.details,
        log.createdAt
      ].map((v) => String(v ?? '').toLowerCase());
      return tokens.some((t) => t.includes(term));
    });
  }

  get paginatedLogs(): ClinicalAuditEvent[] {
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredLogs.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredLogs.length / this.pageSize));
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get uniqueActionsCount(): number {
    return new Set(this.filteredLogs.map((log) => log.action).filter(Boolean)).size;
  }

  get uniqueEntityTypesCount(): number {
    return new Set(this.filteredLogs.map((log) => log.entityType).filter(Boolean)).size;
  }

  onFiltersChanged(): void {
    this.currentPage = 1;
  }

  onPageSizeChanged(): void {
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }

  printLogs(): void {
    this.documentExportService.printDocument(this.buildExportConfig());
  }

  exportPdf(): void {
    this.documentExportService.exportPdf(this.buildExportConfig());
  }

  private async loadLogs(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      const response = await firstValueFrom(this.http.get<ClinicalAuditEvent[] | unknown>(
        `${environment.apiBaseUrl}/api/clinical/audit?limit=500`,
      ));
      this.logs = (Array.isArray(response) ? response : [])
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load clinical audit logs.';
    } finally {
      this.loading = false;
    }
  }

  constructor(
    private http: HttpClient,
    private documentExportService: DocumentExportService,
    private authStorage: AuthStorageService
  ) {}

  private buildExportConfig() {
    const currentUser = this.authStorage.getUser();
    const generatedBy = currentUser
      ? `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim() || currentUser.username || 'System'
      : 'System';
    return {
      title: 'Clinical Audit Logs Report',
      subtitle: 'Clinical service event timeline',
      generatedBy,
      summary: [
        { label: 'Total Events', value: this.filteredLogs.length },
        { label: 'Unique Actions', value: this.uniqueActionsCount },
        { label: 'Entity Types', value: this.uniqueEntityTypesCount },
        { label: 'Filtered By', value: this.actionFilter === 'ALL' ? 'All Actions' : this.actionFilter }
      ],
      columns: [
        { key: 'createdAt', label: 'Date / Time' },
        { key: 'entityType', label: 'Entity Type' },
        { key: 'entityId', label: 'Entity ID' },
        { key: 'action', label: 'Action' },
        { key: 'actorUsername', label: 'Actor' },
        { key: 'actorRole', label: 'Role' },
        { key: 'details', label: 'Details' }
      ],
      rows: this.filteredLogs.map((log) => ({
        createdAt: new Date(log.createdAt).toLocaleString(),
        entityType: log.entityType,
        entityId: log.entityId,
        action: log.action,
        actorUsername: log.actorUsername || '-',
        actorRole: log.actorRole || '-',
        details: log.details || '-'
      })),
      emptyText: 'No clinical events match the selected filters.'
    };
  }
}
