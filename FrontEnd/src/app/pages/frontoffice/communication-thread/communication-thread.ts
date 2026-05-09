import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommunicationApiService, FollowUpMessage } from '../../../core/services/communication-api.service';
import { Subscription, finalize, timeout } from 'rxjs';

@Component({
  selector: 'app-communication-thread',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './communication-thread.html',
  styleUrl: './communication-thread.scss'
})
export class CommunicationThreadComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private routeSub?: Subscription;
  private readonly feedbackStorageKey = 'guardian-message-satisfaction-v1';

  loading = true;
  loadFailed = false;
  actionLoading = false;
  closeConfirmOpen = false;
  savingFeedback = false;
  errorMessage = '';
  successMessage = '';
  message?: FollowUpMessage;
  private feedbackStore: Record<string, 'UP' | 'DOWN'> = {};

  replyForm = this.fb.group({
    replyText: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private route: ActivatedRoute,
    private communicationApi: CommunicationApiService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.feedbackStore = this.loadFeedbackStore();
    this.routeSub = this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      this.load(id);
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  load(idFromRoute?: string | null): void {
    const id = idFromRoute ?? this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.runInView(() => {
        this.loading = false;
        this.loadFailed = true;
        this.errorMessage = 'Message ID is missing. Please return to the messages list and try again.';
        this.message = undefined;
      });
      return;
    }

    this.loading = true;
    this.loadFailed = false;
    this.errorMessage = '';
    this.communicationApi.getMessageById(id).pipe(
      timeout(15000),
      finalize(() => {
        this.runInView(() => {
          this.loading = false;
        });
      })
    ).subscribe({
      next: (msg) => {
        this.runInView(() => {
          this.message = msg;
          this.loadFailed = false;
          this.closeConfirmOpen = false;
        });
      },
      error: (err) => {
        this.runInView(() => {
          this.message = undefined;
          this.loadFailed = true;
          this.errorMessage = err?.name === 'TimeoutError'
            ? 'Message details did not load in time. Please retry.'
            : err?.error?.message || err?.message || 'Failed to load message.';
        });
      }
    });
  }

  sendReply(): void {
    if (!this.message || this.replyForm.invalid || this.actionLoading) {
      this.replyForm.markAllAsTouched();
      return;
    }

    this.actionLoading = true;
    this.successMessage = '';
    this.communicationApi.replyMessage(this.message.id, this.replyForm.value.replyText!).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (updated) => {
        this.message = updated;
        this.replyForm.reset();
        this.successMessage = 'Reply sent.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Reply failed.';
      }
    });
  }

  closeMessage(): void {
    if (!this.message || this.actionLoading) return;
    this.actionLoading = true;
    this.successMessage = '';

    this.communicationApi.closeMessage(this.message.id).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (updated) => {
        this.message = updated;
        this.successMessage = 'Conversation closed successfully.';
        this.closeConfirmOpen = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Close failed.';
      }
    });
  }

  get replyLength(): number {
    return this.replyForm.value.replyText?.length ?? 0;
  }

  get staffReplies() {
    if (!this.message) {
      return [];
    }

    return [...this.message.replies]
      .filter(reply => reply.senderRole !== 'GUARDIAN')
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
  }

  get isClosed(): boolean {
    return this.message?.status === 'CLOSED';
  }

  get hasSatisfactionFeedback(): boolean {
    if (!this.message) {
      return false;
    }
    return !!this.feedbackStore[this.message.id];
  }

  openCloseConfirm(): void {
    if (!this.message || this.isClosed) {
      return;
    }
    this.closeConfirmOpen = true;
  }

  cancelCloseConfirm(): void {
    this.closeConfirmOpen = false;
  }

  saveSatisfaction(choice: 'UP' | 'DOWN'): void {
    if (!this.message || this.savingFeedback) {
      return;
    }

    this.savingFeedback = true;
    this.feedbackStore[this.message.id] = choice;
    this.persistFeedbackStore();
    this.savingFeedback = false;
    this.successMessage = 'Thank you for your feedback.';
  }

  getRelativeTimestamp(dateInput: string): string {
    const date = new Date(dateInput);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;

    if (diffMs < hour) {
      const mins = Math.max(1, Math.floor(diffMs / minute));
      return `${mins} minute${mins > 1 ? 's' : ''} ago`;
    }

    if (diffMs < day) {
      const hours = Math.floor(diffMs / hour);
      return `${hours} hour${hours > 1 ? 's' : ''} ago`;
    }

    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    if (date.toDateString() === yesterday.toDateString()) {
      return `Yesterday at ${date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}`;
    }

    return date.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  }

  private loadFeedbackStore(): Record<string, 'UP' | 'DOWN'> {
    try {
      const raw = localStorage.getItem(this.feedbackStorageKey);
      if (!raw) {
        return {};
      }
      const parsed = JSON.parse(raw) as Record<string, 'UP' | 'DOWN'>;
      return parsed ?? {};
    } catch {
      return {};
    }
  }

  private persistFeedbackStore(): void {
    try {
      localStorage.setItem(this.feedbackStorageKey, JSON.stringify(this.feedbackStore));
    } catch {
      // ignore persistence failures on restricted browsers
    }
  }

  private runInView(update: () => void): void {
    this.ngZone.run(() => {
      update();
      this.cdr.detectChanges();
    });
  }
}
