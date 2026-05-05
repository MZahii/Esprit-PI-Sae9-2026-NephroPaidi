import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';
import { CommunicationApiService, FollowUpMessage, MessageStatus, MessageType } from '../../../core/services/communication-api.service';

type GuardianStatusFilter = 'ALL' | 'PENDING' | 'RESPONDED' | 'CLOSED';

@Component({
  selector: 'app-communication-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './communication-list.html',
  styleUrl: './communication-list.scss'
})
export class CommunicationListComponent implements OnInit {
  loading = true;
  errorMessage = '';
  successMessage = '';
  items: FollowUpMessage[] = [];
  hasPatientLink = true;

  search = '';
  activeFilter: GuardianStatusFilter = 'ALL';
  page = 1;
  readonly pageSize = 8;

  readonly filterButtons: Array<{ label: string; value: GuardianStatusFilter }> = [
    { label: 'All', value: 'ALL' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Replied', value: 'RESPONDED' },
    { label: 'Closed', value: 'CLOSED' }
  ];

  constructor(
    private communicationApi: CommunicationApiService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('created') === '1') {
      this.successMessage = 'Message created successfully.';
    }
    this.load();
  }

  private readonly noLinkMessage = 'No linked patient found for this account';

  get filteredItems(): FollowUpMessage[] {
    const search = this.search.trim().toLowerCase();
    return this.items
      .filter(item => this.matchesFilter(item.status))
      .filter(item => {
        if (!search) return true;
        return (
          (item.subject ?? '').toLowerCase().includes(search) ||
          item.messageText.toLowerCase().includes(search)
        );
      })
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  private matchesFilter(status: MessageStatus): boolean {
    if (this.activeFilter === 'ALL') return true;
    if (this.activeFilter === 'PENDING') {
      return status === 'PENDING' || status === 'READ' || status === 'IN_PROGRESS' || status === 'ESCALATED';
    }
    return status === this.activeFilter;
  }

  setFilter(filter: GuardianStatusFilter): void {
    this.activeFilter = filter;
    this.onFiltersChange();
  }

  getTypeBadgeClass(type: MessageType): string {
    switch (type) {
      case 'MEDICAL':
        return 'text-bg-danger';
      case 'ADMINISTRATIVE':
        return 'text-bg-primary';
      case 'LAB_RESULT':
        return 'text-bg-warning';
      case 'APPOINTMENT':
        return 'text-bg-info';
      default:
        return 'text-bg-secondary';
    }
  }

  getStatusBadgeClass(status: MessageStatus): string {
    if (status === 'RESPONDED') return 'text-bg-success';
    if (status === 'CLOSED') return 'text-bg-secondary';
    return 'text-bg-warning';
  }

  getPreviewText(item: FollowUpMessage): string {
    if (item.messageText.length <= 100) {
      return item.messageText;
    }
    return `${item.messageText.slice(0, 100)}...`;
  }

  getCardTimeIndicator(item: FollowUpMessage): string {
    if (item.status === 'CLOSED') {
      return 'Closed';
    }

    if (item.status === 'RESPONDED') {
      const responseMs = this.getFirstStaffResponseMs(item);
      if (responseMs === null) {
        return 'Replied';
      }
      const hours = Math.floor(responseMs / (1000 * 60 * 60));
      const minutes = Math.floor((responseMs % (1000 * 60 * 60)) / (1000 * 60));
      return `Replied · Response time: ${hours}h ${minutes}m`;
    }

    if (item.status === 'PENDING') {
      const diffMs = Date.now() - new Date(item.createdAt).getTime();
      const hours = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)));
      return `Awaiting response · Sent ${hours} hours ago`;
    }

    const diffMs = Date.now() - new Date(item.createdAt).getTime();
    const hours = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)));
    return `Awaiting response · Sent ${hours} hours ago`;
  }

  private getFirstStaffResponseMs(item: FollowUpMessage): number | null {
    const firstStaffReply = [...item.replies]
      .filter(reply => reply.senderRole !== 'GUARDIAN')
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())[0];

    if (!firstStaffReply) {
      return null;
    }

    const sentAt = new Date(item.createdAt).getTime();
    const repliedAt = new Date(firstStaffReply.createdAt).getTime();
    if (Number.isNaN(sentAt) || Number.isNaN(repliedAt) || repliedAt < sentAt) {
      return null;
    }

    return repliedAt - sentAt;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredItems.length / this.pageSize));
  }

  get pagedItems(): FollowUpMessage[] {
    const safePage = Math.min(this.page, this.totalPages);
    const start = (safePage - 1) * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  onFiltersChange(): void {
    this.page = 1;
  }

  previousPage(): void {
    this.page = Math.max(1, this.page - 1);
  }

  nextPage(): void {
    this.page = Math.min(this.totalPages, this.page + 1);
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.hasPatientLink = true;

    this.communicationApi.getMyPatients().pipe(
      switchMap((patients) => {
        const hasLink = patients.length > 0 && patients.some(patient => !!patient.patientId);
        if (!hasLink) {
          this.runInView(() => {
            this.hasPatientLink = false;
            this.items = [];
            this.errorMessage = this.noLinkMessage;
          });
          return of<FollowUpMessage[] | null>(null);
        }
        return this.communicationApi.getMyMessages();
      }),
      finalize(() => {
        this.runInView(() => {
          this.loading = false;
        });
      })
    ).subscribe({
      next: (items) => {
        this.runInView(() => {
          this.items = items ?? [];
          this.page = 1;
        });
      },
      error: (err) => {
        this.runInView(() => {
          if (err?.status === 403) {
            this.hasPatientLink = false;
            this.items = [];
            this.errorMessage = this.noLinkMessage;
            return;
          }
          this.errorMessage = err?.error?.message || 'Failed to load messages.';
        });
      }
    });
  }

  private runInView(update: () => void): void {
    this.ngZone.run(() => {
      update();
      this.cdr.detectChanges();
    });
  }
}
