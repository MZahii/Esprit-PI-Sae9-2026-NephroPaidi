import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, NgZone, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, forkJoin } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { DocumentExportService } from '../../../core/services/document-export.service';

interface UserRow {
  id: number;
  firstName?: string;
  lastName?: string;
  role?: string;
  username?: string;
}

interface AuditRow {
  id: number;
  action: string;
  actor?: string;
  oldValue?: string;
  newValue?: string;
  createdAt: string;
  userId?: number;
  scopeId?: number;
}

interface UnifiedLog {
  id: string;
  source: 'ACCOUNT' | 'CONTRACT';
  action: string;
  actor: string;
  createdAt: string;
  oldValue?: string;
  newValue?: string;
  userId: number;
  fullName: string;
  role: string;
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

interface UserLogGroup {
  key: string;
  userId: number;
  fullName: string;
  role: string;
  actorSet: string[];
  totalLogs: number;
  latestAt: string;
  accountLogs: number;
  contractLogs: number;
  entries: UnifiedLog[];
}

@Component({
  selector: 'app-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './logs.html',
  styleUrl: './logs.scss'
})
export class LogsComponent implements OnInit, AfterViewInit, OnDestroy {
  loading = false;
  errorMessage = '';
  search = '';
  sourceFilter: 'ALL' | 'ACCOUNT' | 'CONTRACT' = 'ALL';
  logs: UnifiedLog[] = [];
  private diffCache = new Map<string, LogDiffInfo>();
  currentPage = 1;
  pageSize = 5;
  readonly pageSizeOptions = [5, 8, 12, 20];
  expandedGroups = new Set<string>();
  private readonly nativeGroupToolClick = (event: Event) => this.handleNativeGroupToolClick(event);
  private lastGroupToolPointerActionAt = 0;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private elementRef: ElementRef<HTMLElement>,
    private ngZone: NgZone,
    private authStorage: AuthStorageService,
    private documentExportService: DocumentExportService
  ) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  ngAfterViewInit(): void {
    this.elementRef.nativeElement.addEventListener('click', this.nativeGroupToolClick, true);
  }

  ngOnDestroy(): void {
    this.elementRef.nativeElement.removeEventListener('click', this.nativeGroupToolClick, true);
  }

  get filteredLogs(): UnifiedLog[] {
    const term = this.search.trim().toLowerCase();
    return this.logs.filter((log) => {
      const sourceMatch = this.sourceFilter === 'ALL' || log.source === this.sourceFilter;
      const termMatch = !term || this.logSearchTokens(log).some((value) => value.includes(term));
      return sourceMatch && termMatch;
    });
  }

  get groupedLogs(): UserLogGroup[] {
    const map = new Map<string, UserLogGroup>();
    for (const log of this.filteredLogs) {
      const key = `${log.userId}-${log.role}-${log.fullName}`;
      const existing = map.get(key);
      if (!existing) {
        map.set(key, {
          key,
          userId: log.userId,
          fullName: log.fullName,
          role: log.role,
          actorSet: log.actor ? [log.actor] : [],
          totalLogs: 1,
          latestAt: log.createdAt,
          accountLogs: log.source === 'ACCOUNT' ? 1 : 0,
          contractLogs: log.source === 'CONTRACT' ? 1 : 0,
          entries: [log]
        });
      } else {
        existing.totalLogs++;
        if (new Date(log.createdAt).getTime() > new Date(existing.latestAt).getTime()) {
          existing.latestAt = log.createdAt;
        }
        if (log.source === 'ACCOUNT') existing.accountLogs++;
        if (log.source === 'CONTRACT') existing.contractLogs++;
        if (log.actor && !existing.actorSet.includes(log.actor)) {
          existing.actorSet.push(log.actor);
        }
        existing.entries.push(log);
      }
    }
    return Array.from(map.values())
      .map((group) => ({
        ...group,
        entries: group.entries.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      }))
      .sort((a, b) => new Date(b.latestAt).getTime() - new Date(a.latestAt).getTime());
  }

  get paginatedGroups(): UserLogGroup[] {
    if (this.currentPage > this.totalPages) {
      this.currentPage = this.totalPages;
    }
    const start = (this.currentPage - 1) * this.pageSize;
    return this.groupedLogs.slice(start, start + this.pageSize);
  }

  isGroupExpanded(groupKey: string): boolean {
    return this.expandedGroups.has(groupKey);
  }

  handleGroupToolClick(group: UserLogGroup, event: Event): void {
    if (Date.now() - this.lastGroupToolPointerActionAt < 450) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    this.runGroupToolAction(group, event);
  }

  handleGroupToolPointerDown(group: UserLogGroup, event: PointerEvent): void {
    if (event.button !== 0) return;
    const handled = this.runGroupToolAction(group, event);
    if (handled) {
      this.lastGroupToolPointerActionAt = Date.now();
    }
  }

  private runGroupToolAction(group: UserLogGroup, event: Event): boolean {
    const target = event.target as HTMLElement | null;
    const button = target?.closest<HTMLButtonElement>('button[data-log-action]');
    if (!button) return false;

    event.preventDefault();
    event.stopPropagation();

    const action = button.dataset['logAction'];
    if (action === 'print') {
      this.printUserLogs(group);
      return true;
    }
    if (action === 'pdf') {
      this.exportUserPdf(group);
      return true;
    }
    if (action === 'toggle') {
      this.toggleGroup(group.key);
      return true;
    }
    return false;
  }

  private handleNativeGroupToolClick(event: Event): void {
    const target = event.target as HTMLElement | null;
    const button = target?.closest<HTMLButtonElement>('button[data-log-action]');
    if (!button || !this.elementRef.nativeElement.contains(button)) return;

    const groupKey = button.closest<HTMLElement>('.group-tools')?.dataset['groupKey'];
    const group = this.groupedLogs.find((item) => item.key === groupKey);
    if (!group) return;

    event.preventDefault();
    event.stopPropagation();
    this.ngZone.run(() => this.handleGroupToolClick(group, event));
  }

  toggleGroup(groupKey: string, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    const nextExpandedGroups = new Set(this.expandedGroups);
    if (this.expandedGroups.has(groupKey)) {
      nextExpandedGroups.delete(groupKey);
    } else {
      nextExpandedGroups.add(groupKey);
    }
    this.expandedGroups = nextExpandedGroups;
    this.cdr.detectChanges();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.groupedLogs.length / this.pageSize));
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get rangeStart(): number {
    if (this.groupedLogs.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.currentPage * this.pageSize, this.groupedLogs.length);
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

  previousPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  printLogs(): void {
    const config = this.buildExportConfig();
    this.documentExportService.printDocument(config);
  }

  downloadPdf(): void {
    const config = this.buildExportConfig();
    this.documentExportService.exportPdf(config);
  }

  printUserLogs(group: UserLogGroup, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.documentExportService.printDocument(this.buildUserExportConfig(group));
  }

  exportUserPdf(group: UserLogGroup, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.documentExportService.exportPdf(this.buildUserExportConfig(group));
  }

  sourceClass(source: UnifiedLog['source']): string {
    return source === 'ACCOUNT' ? 'source-account' : 'source-contract';
  }

  actionClass(action: string): string {
    const normalized = (action || '').toLowerCase();
    if (normalized.includes('password') || normalized.includes('status')) return 'action-security';
    if (normalized.includes('create') || normalized.includes('restore')) return 'action-positive';
    if (normalized.includes('delete') || normalized.includes('archive') || normalized.includes('suspend') || normalized.includes('end')) return 'action-negative';
    return 'action-neutral';
  }

  getChangedFields(log: UnifiedLog): LogDiffRow[] {
    return this.getDiffInfo(log).rows;
  }

  hasParsedDetails(log: UnifiedLog): boolean {
    return this.getDiffInfo(log).parsed;
  }

  getDiffInfo(log: UnifiedLog): LogDiffInfo {
    const cached = this.diffCache.get(log.id);
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
    this.diffCache.set(log.id, info);
    return info;
  }

  hasRawDetails(log: UnifiedLog): boolean {
    return !!(log.oldValue || log.newValue);
  }

  getRawOld(log: UnifiedLog): string {
    return log.oldValue || '-';
  }

  getRawNew(log: UnifiedLog): string {
    return log.newValue || '-';
  }

  private async loadLogs(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

      const result = await firstValueFrom(forkJoin({
        users: this.http.get<UserRow[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers }),
        userAudit: this.http.get<AuditRow[] | unknown>(`${environment.apiBaseUrl}/api/users/audit`, { headers }),
        contractAudit: this.http.get<AuditRow[] | unknown>(`${environment.apiBaseUrl}/api/observability/contracts/timeline`, { headers })
      }));

      const users = Array.isArray(result.users) ? result.users : [];
      const userMap = new Map<number, UserRow>(users.map((u) => [u.id, u]));

      const userLogs = (Array.isArray(result.userAudit) ? result.userAudit : []).map((log) => {
        const userId = Number(log.userId ?? 0);
        const user = userMap.get(userId);
        const fullName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.username || `User #${userId}`;
        return {
          id: `U-${log.id}`,
          source: 'ACCOUNT' as const,
          action: log.action,
          actor: log.actor || '-',
          createdAt: log.createdAt,
          oldValue: log.oldValue,
          newValue: log.newValue,
          userId,
          fullName,
          role: user?.role || '-'
        };
      });

      const contractLogs = (Array.isArray(result.contractAudit) ? result.contractAudit : []).map((log) => {
        const userId = Number(log.scopeId ?? 0);
        const user = userMap.get(userId);
        const fullName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.username || `User #${userId}`;
        return {
          id: `C-${log.id}`,
          source: 'CONTRACT' as const,
          action: log.action,
          actor: log.actor || '-',
          createdAt: log.createdAt,
          oldValue: log.oldValue,
          newValue: log.newValue,
          userId,
          fullName,
          role: user?.role || '-'
        };
      });

      this.logs = [...userLogs, ...contractLogs]
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      this.diffCache.clear();
      this.currentPage = 1;
      this.expandedGroups.clear();
      this.groupedLogs.slice(0, 3).forEach((group) => this.expandedGroups.add(group.key));
    } catch (error: unknown) {
      const err = error as { error?: { message?: string }; message?: string };
      this.errorMessage = err?.error?.message || err?.message || 'Failed to load audit logs.';
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
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
      staffUserId: 'Staff User ID',
      contractReference: 'Contract Reference',
      contractType: 'Contract Type',
      accountStatus: 'Account Status',
      firstName: 'First Name',
      lastName: 'Last Name',
      dateOfBirth: 'Date Of Birth',
      hoursPerWeek: 'Hours / Week'
    };
    if (labels[field]) return labels[field];
    return field
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  private logSearchTokens(log: UnifiedLog): string[] {
    return [
      log.source ?? '',
      log.action ?? '',
      log.actor ?? '',
      log.fullName ?? '',
      log.role ?? '',
      String(log.userId ?? ''),
      log.createdAt ?? '',
      log.oldValue ?? '',
      log.newValue ?? ''
    ].map((value) => String(value).toLowerCase());
  }

  get accountSourceCount(): number {
    return this.filteredLogs.filter((log) => log.source === 'ACCOUNT').length;
  }

  get contractSourceCount(): number {
    return this.filteredLogs.filter((log) => log.source === 'CONTRACT').length;
  }

  get uniqueActorsCount(): number {
    return new Set(this.filteredLogs.map((log) => log.actor).filter(Boolean)).size;
  }

  get logsTodayCount(): number {
    const today = new Date().toISOString().slice(0, 10);
    return this.filteredLogs.filter((log) => (log.createdAt ?? '').slice(0, 10) === today).length;
  }

  get logsThisWeekCount(): number {
    const now = new Date();
    const start = new Date(now);
    start.setDate(now.getDate() - 7);
    return this.filteredLogs.filter((log) => {
      const created = new Date(log.createdAt);
      return !Number.isNaN(created.getTime()) && created >= start && created <= now;
    }).length;
  }

  get sensitiveActionCount(): number {
    return this.filteredLogs.filter((log) => {
      const action = (log.action ?? '').toLowerCase();
      return ['delete', 'archive', 'suspend', 'status', 'password', 'restore'].some((term) => action.includes(term));
    }).length;
  }

  get topActionType(): string {
    const counters = new Map<string, number>();
    for (const log of this.filteredLogs) {
      const action = (log.action || 'UNKNOWN').split(/[.:_-]/)[0].toUpperCase();
      counters.set(action, (counters.get(action) ?? 0) + 1);
    }
    let top = '-';
    let topCount = 0;
    for (const [action, count] of counters.entries()) {
      if (count > topCount) {
        top = action;
        topCount = count;
      }
    }
    return topCount ? `${top} (${topCount})` : '-';
  }

  private buildExportConfig() {
    const currentUser = this.authStorage.getUser();
    const generatedBy = currentUser
      ? `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim() || currentUser.username || 'System'
      : 'System';

    return {
      title: 'Audit Logs Report',
      subtitle: 'Unified account and contract timeline',
      generatedBy,
      summary: [
        { label: 'Total Logs', value: this.filteredLogs.length },
        { label: 'Account Logs', value: this.accountSourceCount },
        { label: 'Contract Logs', value: this.contractSourceCount },
        { label: 'Unique Actors', value: this.uniqueActorsCount },
        { label: 'Logs Today', value: this.logsTodayCount },
        { label: 'Sensitive Actions', value: this.sensitiveActionCount }
      ],
      columns: [
        { key: 'createdAt', label: 'Date / Time' },
        { key: 'source', label: 'Source' },
        { key: 'fullName', label: 'User' },
        { key: 'role', label: 'Role' },
        { key: 'action', label: 'Action' },
        { key: 'actor', label: 'Actor' }
      ],
      rows: this.filteredLogs.map((log) => ({
        createdAt: new Date(log.createdAt).toLocaleString(),
        source: log.source,
        fullName: log.fullName,
        role: log.role,
        action: log.action,
        actor: log.actor
      })),
      emptyText: 'No logs match the selected filters.'
    };
  }

  private buildUserExportConfig(group: UserLogGroup) {
    const currentUser = this.authStorage.getUser();
    const generatedBy = currentUser
      ? `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim() || currentUser.username || 'System'
      : 'System';

    return {
      title: `User Audit Report · ${group.fullName}`,
      subtitle: `Detailed timeline for ${group.role} account`,
      generatedBy,
      summary: [
        { label: 'User', value: group.fullName },
        { label: 'Role', value: group.role },
        { label: 'Total Events', value: group.totalLogs },
        { label: 'Account Events', value: group.accountLogs },
        { label: 'Contract Events', value: group.contractLogs },
        { label: 'Latest Event', value: new Date(group.latestAt).toLocaleString() }
      ],
      columns: [
        { key: 'createdAt', label: 'Date / Time' },
        { key: 'source', label: 'Source' },
        { key: 'action', label: 'Action' },
        { key: 'actor', label: 'Actor' },
        { key: 'details', label: 'Details' }
      ],
      rows: group.entries.map((log) => {
        const diffs = this.getChangedFields(log);
        const details = diffs.length
          ? diffs.map((d) => `${d.field}: ${d.oldValue} -> ${d.newValue}`).join(' | ')
          : (this.hasRawDetails(log) ? `Old: ${this.getRawOld(log)} | New: ${this.getRawNew(log)}` : 'No field changes');
        return {
          createdAt: new Date(log.createdAt).toLocaleString(),
          source: log.source,
          action: log.action,
          actor: log.actor,
          details
        };
      }),
      emptyText: 'No events found for this user.'
    };
  }
}





