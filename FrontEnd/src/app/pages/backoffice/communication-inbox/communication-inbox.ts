import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { finalize } from 'rxjs';
import {
  BulkMessageAction,
  BulkMessageOperationResponse,
  CommunicationApiService,
  FollowUpMessage,
  MessageQueue,
  MessageStatus,
  MessageType,
  PatientDirectoryItem,
  PriorityLevel
} from '../../../core/services/communication-api.service';

type InboxTab = 'MY_QUEUE' | 'ALL_MESSAGES' | 'URGENT';
type AssignedFilter = 'ALL' | 'ME' | 'UNASSIGNED' | 'ASSIGNED';

interface InboxFiltersState {
  search: string;
  patientId: number | null;
  dateFrom: string;
  dateTo: string;
  assigned: AssignedFilter;
  statuses: MessageStatus[];
  priorities: PriorityLevel[];
  types: MessageType[];
}

@Component({
  selector: 'app-communication-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './communication-inbox.html',
  styleUrl: './communication-inbox.scss'
})
export class CommunicationInboxComponent implements OnInit {
  loading = true;
  actionLoadingId = '';
  bulkActionLoading = false;
  errorMessage = '';
  successMessage = '';
  items: FollowUpMessage[] = [];
  activeTab: InboxTab = 'MY_QUEUE';
  showFilters = false;
  page = 1;
  readonly pageSize = 12;
  selectedMessageIds: string[] = [];

  patientsById: Record<number, string> = {};

  draftFilters: InboxFiltersState = this.createDefaultFilters();
  appliedFilters: InboxFiltersState = this.createDefaultFilters();

  readonly statuses: MessageStatus[] = ['PENDING', 'READ', 'IN_PROGRESS', 'ESCALATED', 'RESPONDED', 'CLOSED'];
  readonly priorities: PriorityLevel[] = ['HIGH', 'NORMAL'];
  readonly types: MessageType[] = ['ADMINISTRATIVE', 'APPOINTMENT', 'QUESTION', 'COMPLAINT', 'MEDICAL', 'LAB_RESULT', 'OTHER'];
  readonly assignedFilters: AssignedFilter[] = ['ALL', 'ME', 'UNASSIGNED', 'ASSIGNED'];

  get pendingCount(): number {
    return this.items.filter((item) => item.status === 'PENDING').length;
  }

  get urgentOpenCount(): number {
    return this.items.filter((item) => item.priority === 'HIGH' && !this.isClosed(item.status)).length;
  }

  get respondedTodayCount(): number {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    return this.items.filter((item) => {
      const firstStaffReply = this.getFirstStaffReplyDate(item);
      return !!firstStaffReply && firstStaffReply.getTime() >= start.getTime();
    }).length;
  }

  get avgResponseHours(): string {
    const samples = this.items
      .map((item) => {
        const firstReply = this.getFirstStaffReplyDate(item);
        if (!firstReply) {
          return null;
        }
        const created = new Date(item.createdAt);
        return (firstReply.getTime() - created.getTime()) / 3600000;
      })
      .filter((value): value is number => typeof value === 'number' && value >= 0);

    if (samples.length === 0) {
      return '-';
    }
    const average = samples.reduce((sum, value) => sum + value, 0) / samples.length;
    return `${average.toFixed(1)} h`;
  }

  get urgentTabCount(): number {
    return this.items.filter((item) => item.priority === 'HIGH').length;
  }

  get escalatedCount(): number {
    return this.items.filter((item) => item.status === 'ESCALATED').length;
  }

  get escalatedAssignedToMeCount(): number {
    if (!this.currentUserKeycloakId) {
      return 0;
    }
    return this.items.filter((item) => item.status === 'ESCALATED' && item.assignedToUserKeycloakId === this.currentUserKeycloakId).length;
  }

  get hasSelection(): boolean {
    return this.selectedMessageIds.length > 0;
  }

  get selectedCount(): number {
    return this.selectedMessageIds.length;
  }

  constructor(
    private authStorage: AuthStorageService,
    private communicationApi: CommunicationApiService
  ) {}

  ngOnInit(): void {
    this.loadPatientsDirectory();
    this.load();
  }

  private createDefaultFilters(): InboxFiltersState {
    return {
      search: '',
      patientId: null,
      dateFrom: '',
      dateTo: '',
      assigned: 'ALL',
      statuses: [],
      priorities: [],
      types: []
    };
  }

  private loadPatientsDirectory(): void {
    this.communicationApi.getPatientsDirectory().subscribe({
      next: (patients) => {
        this.patientsById = patients.reduce((acc: Record<number, string>, patient: PatientDirectoryItem) => {
          acc[patient.id] = `${patient.firstName} ${patient.lastName}`.trim();
          return acc;
        }, {});
      },
      error: () => {
        this.patientsById = {};
      }
    });
  }

  get currentUserKeycloakId(): string {
    return this.authStorage.getUser()?.keycloakId ?? '';
  }

  get patientOptions(): Array<{ id: number; name: string }> {
    return Object.entries(this.patientsById)
      .map(([id, name]) => ({ id: Number(id), name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  getPatientDisplay(patientId: number): string {
    return this.patientsById[patientId] ?? `#${patientId}`;
  }

  get queue(): MessageQueue {
    const role = this.authStorage.getRole();
    if (role === 'NURSE') return 'NURSE';
    if (role === 'DOCTOR') return 'DOCTOR';
    return 'RECEPTIONIST';
  }

  get filteredItems(): FollowUpMessage[] {
    const q = this.appliedFilters.search.trim().toLowerCase();
    return this.items
      .filter((item) => this.matchesTab(item))
      .filter((item) => this.appliedFilters.statuses.length === 0 || this.appliedFilters.statuses.includes(item.status))
      .filter((item) => this.appliedFilters.priorities.length === 0 || this.appliedFilters.priorities.includes(item.priority))
      .filter((item) => this.appliedFilters.types.length === 0 || this.appliedFilters.types.includes(item.messageType))
      .filter((item) => {
        if (this.appliedFilters.assigned === 'ALL') return true;
        if (this.appliedFilters.assigned === 'UNASSIGNED') return !item.assignedToUserKeycloakId;
        if (this.appliedFilters.assigned === 'ASSIGNED') return !!item.assignedToUserKeycloakId;
        return item.assignedToUserKeycloakId === this.currentUserKeycloakId;
      })
      .filter((item) => {
        if (!q) return true;
        const patientName = this.getPatientDisplay(item.patientId).toLowerCase();
        return (
          patientName.includes(q) ||
          (item.subject ?? '').toLowerCase().includes(q) ||
          item.messageText.toLowerCase().includes(q)
        );
      })
      .sort((a, b) => this.sortMessages(a, b));
  }

  get areAllVisibleSelected(): boolean {
    return this.pagedItems.length > 0 && this.pagedItems.every((item) => this.isSelected(item.id));
  }

  get activeFilterTags(): string[] {
    const tags: string[] = [];
    this.appliedFilters.statuses.forEach((status) => tags.push(`Status: ${status}`));
    this.appliedFilters.priorities.forEach((priority) => tags.push(`Priority: ${priority}`));
    this.appliedFilters.types.forEach((type) => tags.push(`Type: ${type}`));
    if (this.appliedFilters.patientId) {
      tags.push(`Patient: ${this.getPatientDisplay(this.appliedFilters.patientId)}`);
    }
    if (this.appliedFilters.dateFrom) {
      tags.push(`From: ${this.appliedFilters.dateFrom}`);
    }
    if (this.appliedFilters.dateTo) {
      tags.push(`To: ${this.appliedFilters.dateTo}`);
    }
    if (this.appliedFilters.assigned !== 'ALL') {
      tags.push(`Assigned: ${this.appliedFilters.assigned}`);
    }
    if (this.appliedFilters.search.trim()) {
      tags.push(`Search: ${this.appliedFilters.search.trim()}`);
    }
    return tags;
  }

  private matchesTab(item: FollowUpMessage): boolean {
    if (this.activeTab === 'URGENT') {
      return item.priority === 'HIGH';
    }
    if (this.activeTab === 'MY_QUEUE') {
      return !item.assignedToUserKeycloakId
        || (!!this.currentUserKeycloakId && item.assignedToUserKeycloakId === this.currentUserKeycloakId);
    }
    return true;
  }

  private sortMessages(a: FollowUpMessage, b: FollowUpMessage): number {
    if (a.status !== b.status) {
      if (a.status === 'ESCALATED') return -1;
      if (b.status === 'ESCALATED') return 1;
    }

    if (a.priority !== b.priority) {
      if (a.priority === 'HIGH') return -1;
      if (b.priority === 'HIGH') return 1;
    }

    const aPending = a.status === 'PENDING';
    const bPending = b.status === 'PENDING';
    if (aPending !== bPending) {
      return aPending ? -1 : 1;
    }

    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  }

  private getFirstStaffReplyDate(item: FollowUpMessage): Date | null {
    const staffReply = item.replies
      .filter((reply) => reply.senderRole !== 'GUARDIAN')
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())[0];
    return staffReply ? new Date(staffReply.createdAt) : null;
  }

  private isClosed(status: MessageStatus): boolean {
    return status === 'CLOSED';
  }

  getEscalationLabel(item: FollowUpMessage): string {
    if (item.status !== 'ESCALATED') {
      return '';
    }
    if (item.assignedToUserKeycloakId && item.assignedToUserKeycloakId === this.currentUserKeycloakId) {
      return 'Escalated to you';
    }
    if (item.assignedToUserKeycloakId) {
      return 'Escalated';
    }
    return 'Needs doctor review';
  }

  getAiUrgencyClass(item: FollowUpMessage): string {
    switch (item.aiUrgencyLevel) {
      case 'CRITICAL':
        return 'bg-danger';
      case 'HIGH':
        return 'bg-warning text-dark';
      case 'MEDIUM':
        return 'bg-info text-dark';
      case 'LOW':
        return 'bg-success';
      default:
        return 'bg-light text-dark';
    }
  }

  get showAiColumn(): boolean {
    return this.queue !== 'RECEPTIONIST';
  }

  shouldShowAiAdvisory(item: FollowUpMessage): boolean {
    return this.showAiColumn
      && item.messageType === 'MEDICAL'
      && item.aiTriageStatus !== null
      && item.aiTriageStatus !== undefined
      && item.aiTriageStatus !== 'SKIPPED';
  }

  shouldShowAiFailure(item: FollowUpMessage): boolean {
    return this.shouldShowAiAdvisory(item) && item.aiTriageStatus === 'FAILED';
  }

  shouldShowAiPrediction(item: FollowUpMessage): boolean {
    return this.shouldShowAiAdvisory(item)
      && item.aiTriageStatus !== 'FAILED'
      && item.aiUrgencyLevel !== null
      && item.aiUrgencyLevel !== undefined;
  }

  formatAiConfidence(item: FollowUpMessage): string {
    if (item.aiConfidence === null || item.aiConfidence === undefined) {
      return '-';
    }
    return `${Math.round(item.aiConfidence * 100)}%`;
  }

  selectTab(tab: InboxTab): void {
    this.activeTab = tab;
    this.page = 1;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  clearAllFilters(): void {
    this.draftFilters = this.createDefaultFilters();
    this.applyFilters();
  }

  applyFilters(): void {
    this.appliedFilters = {
      ...this.draftFilters,
      statuses: [...this.draftFilters.statuses],
      priorities: [...this.draftFilters.priorities],
      types: [...this.draftFilters.types]
    };
    this.page = 1;
    this.load();
  }

  removeTag(tag: string): void {
    if (tag.startsWith('Status: ')) {
      const value = tag.replace('Status: ', '') as MessageStatus;
      this.draftFilters.statuses = this.draftFilters.statuses.filter((entry) => entry !== value);
    } else if (tag.startsWith('Priority: ')) {
      const value = tag.replace('Priority: ', '') as PriorityLevel;
      this.draftFilters.priorities = this.draftFilters.priorities.filter((entry) => entry !== value);
    } else if (tag.startsWith('Type: ')) {
      const value = tag.replace('Type: ', '') as MessageType;
      this.draftFilters.types = this.draftFilters.types.filter((entry) => entry !== value);
    } else if (tag.startsWith('Patient: ')) {
      this.draftFilters.patientId = null;
    } else if (tag.startsWith('From: ')) {
      this.draftFilters.dateFrom = '';
    } else if (tag.startsWith('To: ')) {
      this.draftFilters.dateTo = '';
    } else if (tag.startsWith('Assigned: ')) {
      this.draftFilters.assigned = 'ALL';
    } else if (tag.startsWith('Search: ')) {
      this.draftFilters.search = '';
    }
    this.applyFilters();
  }

  isChecked<T>(value: T, selected: T[]): boolean {
    return selected.includes(value);
  }

  toggleChecked<T>(value: T, selected: T[]): void {
    const index = selected.indexOf(value);
    if (index === -1) {
      selected.push(value);
    } else {
      selected.splice(index, 1);
    }
  }

  getMessagePreview(item: FollowUpMessage): string {
    const source = item.messageText || '';
    if (source.length <= 80) {
      return source;
    }
    return `${source.slice(0, 80)}...`;
  }

  canTake(item: FollowUpMessage): boolean {
    return item.status === 'PENDING' && !item.assignedToUserKeycloakId;
  }

  take(item: FollowUpMessage): void {
    if (!this.canTake(item) || this.actionLoadingId) {
      return;
    }
    this.actionLoadingId = item.id;
    this.errorMessage = '';
    this.communicationApi.takeMessage(item.id)
      .pipe(finalize(() => {
        this.actionLoadingId = '';
      }))
      .subscribe({
        next: () => {
          this.load();
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to take message.';
        }
      });
  }

  isSelected(messageId: string): boolean {
    return this.selectedMessageIds.includes(messageId);
  }

  toggleSelection(messageId: string): void {
    if (this.isSelected(messageId)) {
      this.selectedMessageIds = this.selectedMessageIds.filter((id) => id !== messageId);
      return;
    }
    this.selectedMessageIds = [...this.selectedMessageIds, messageId];
  }

  toggleSelectVisible(): void {
    if (this.areAllVisibleSelected) {
      const visibleIds = new Set(this.pagedItems.map((item) => item.id));
      this.selectedMessageIds = this.selectedMessageIds.filter((id) => !visibleIds.has(id));
      return;
    }

    const merged = new Set(this.selectedMessageIds);
    this.pagedItems.forEach((item) => merged.add(item.id));
    this.selectedMessageIds = Array.from(merged);
  }

  clearSelection(): void {
    this.selectedMessageIds = [];
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredItems.length / this.pageSize));
  }

  get pagedItems(): FollowUpMessage[] {
    const safePage = Math.min(this.page, this.totalPages);
    const start = (safePage - 1) * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  previousPage(): void {
    this.page = Math.max(1, this.page - 1);
  }

  nextPage(): void {
    this.page = Math.min(this.totalPages, this.page + 1);
  }

  runBulkAction(action: BulkMessageAction): void {
    if (!this.hasSelection || this.bulkActionLoading) {
      return;
    }

    this.bulkActionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.communicationApi.bulkOperateMessages(action, this.selectedMessageIds)
      .pipe(finalize(() => {
        this.bulkActionLoading = false;
      }))
      .subscribe({
        next: (response) => {
          this.handleBulkActionSuccess(response);
          this.clearSelection();
          this.load();
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to process bulk message action.';
        }
      });
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.communicationApi.getInbox({
      queue: this.queue,
      patientId: this.appliedFilters.patientId ?? undefined,
      dateFrom: this.appliedFilters.dateFrom ? new Date(`${this.appliedFilters.dateFrom}T00:00:00`).toISOString() : undefined,
      dateTo: this.appliedFilters.dateTo ? new Date(`${this.appliedFilters.dateTo}T23:59:59`).toISOString() : undefined
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (items) => {
        this.items = items;
        const loadedIds = new Set(items.map((item) => item.id));
        this.selectedMessageIds = this.selectedMessageIds.filter((id) => loadedIds.has(id));
        this.page = 1;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load inbox.';
      }
    });
  }

  private handleBulkActionSuccess(response: BulkMessageOperationResponse): void {
    const actionLabel = this.formatBulkAction(response.action);

    if (response.failedCount === 0) {
      this.successMessage = `${actionLabel} completed for ${response.successCount} message${response.successCount === 1 ? '' : 's'}.`;
      return;
    }

    const firstFailures = response.failures
      .slice(0, 2)
      .map((failure) => failure.error)
      .join(' ');

    this.successMessage = `${actionLabel} completed for ${response.successCount} of ${response.requestedCount} messages.`;
    this.errorMessage = firstFailures || `${response.failedCount} messages could not be processed.`;
  }

  private formatBulkAction(action: BulkMessageAction): string {
    switch (action) {
      case 'TAKE':
        return 'Take ownership';
      case 'MARK_READ':
        return 'Mark read';
      case 'UNASSIGN':
        return 'Unassign';
      case 'CLOSE':
        return 'Close';
      default:
        return 'Bulk action';
    }
  }
}
