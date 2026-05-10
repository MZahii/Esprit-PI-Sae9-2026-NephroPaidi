import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';

import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import {
  StaffConversationParticipant,
  StaffConversationSummary,
  StaffMessage,
  StaffMessagingSocketEvent,
  StaffMessagingUser
} from '../../../core/models/internal-staff-messaging.models';
import { InternalStaffMessagingApiService } from '../../../core/services/internal-staff-messaging-api.service';
import { InternalStaffMessagingRealtimeService } from '../../../core/services/internal-staff-messaging-realtime.service';

@Component({
  selector: 'app-internal-staff-messaging',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './internal-staff-messaging.html',
  styleUrl: './internal-staff-messaging.scss'
})
export class InternalStaffMessagingComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() compactMode = false;
  @ViewChild('messageViewport') private messageViewport?: ElementRef<HTMLDivElement>;

  conversationsLoading = true;
  messagesLoading = false;
  searchLoading = false;
  sending = false;
  wsConnected = false;

  pageError = '';
  threadError = '';
  sendError = '';

  conversations: StaffConversationSummary[] = [];
  messages: StaffMessage[] = [];
  staffResults: StaffMessagingUser[] = [];

  selectedConversationId = '';
  draftMessage = '';
  searchTerm = '';
  compactThreadOpen = false;

  private realtimeSub?: Subscription;
  private connectionSub?: Subscription;
  private refreshTimer?: ReturnType<typeof setInterval>;
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private authStorage: AuthStorageService,
    private api: InternalStaffMessagingApiService,
    private realtime: InternalStaffMessagingRealtimeService
  ) {}

  get currentUserId(): string {
    return this.authStorage.getUser()?.keycloakId ?? '';
  }

  get selectedConversation(): StaffConversationSummary | null {
    return this.conversations.find((conversation) => conversation.id === this.selectedConversationId) ?? null;
  }

  get selectedPeer(): StaffConversationParticipant | null {
    const conversation = this.selectedConversation;
    if (!conversation) {
      return null;
    }

    return this.getPeer(conversation);
  }

  get selectedConversationLabel(): string {
    return this.selectedPeer?.displayName || 'Select a conversation';
  }

  get liveStatusLabel(): string {
    return this.wsConnected ? 'Live updates on' : 'REST fallback active';
  }

  ngOnInit(): void {
    this.loadConversations();
    this.loadStaffUsers();
    this.connectionSub = this.realtime.connected$.subscribe((connected) => {
      this.wsConnected = connected;
    });
    this.realtimeSub = this.realtime.events$.subscribe((event) => {
      this.handleRealtimeEvent(event);
    });
    void this.realtime.connect();

    this.refreshTimer = setInterval(() => {
      if (!this.wsConnected) {
        this.loadConversations(false);
        if (this.selectedConversationId) {
          this.loadMessages(this.selectedConversationId, false);
        }
      }
    }, 15000);
  }

  ngAfterViewInit(): void {
    this.scrollMessagesToBottom();
  }

  ngOnDestroy(): void {
    this.realtimeSub?.unsubscribe();
    this.connectionSub?.unsubscribe();
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.realtime.disconnect();
  }

  scheduleUserSearch(): void {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.loadStaffUsers();
    }, 250);
  }

  loadConversations(resetSelection = true): void {
    this.conversationsLoading = resetSelection;
    if (resetSelection) {
      this.pageError = '';
    }

    this.api.listConversations()
      .pipe(finalize(() => {
        this.conversationsLoading = false;
      }))
      .subscribe({
        next: (conversations) => {
          this.conversations = this.sortConversations(conversations);

          if (this.selectedConversationId) {
            const updatedSelection = this.selectedConversation;
            if (!updatedSelection) {
              this.selectedConversationId = '';
              this.messages = [];
            }
          }

          if (!this.compactMode && !this.selectedConversationId && this.conversations.length > 0) {
            void this.openConversation(this.conversations[0]);
          }
        },
        error: (err) => {
          this.pageError = err?.error?.message || 'Failed to load internal staff conversations.';
        }
      });
  }

  loadMessages(conversationId: string, showLoading = true): void {
    if (!conversationId) {
      return;
    }

    if (showLoading) {
      this.messagesLoading = true;
      this.threadError = '';
    }

    this.api.listMessages(conversationId)
      .pipe(finalize(() => {
        this.messagesLoading = false;
      }))
      .subscribe({
        next: (messages) => {
          this.messages = [...messages];
          this.scrollMessagesToBottom();
        },
        error: (err) => {
          this.threadError = err?.error?.message || 'Failed to load conversation history.';
        }
      });
  }

  loadStaffUsers(): void {
    this.searchLoading = true;
    this.api.listStaffUsers(this.searchTerm, 8)
      .pipe(finalize(() => {
        this.searchLoading = false;
      }))
      .subscribe({
        next: (users) => {
          this.staffResults = users;
        },
        error: () => {
          this.staffResults = [];
        }
      });
  }

  async openConversation(conversation: StaffConversationSummary): Promise<void> {
    if (!conversation?.id) {
      return;
    }

    this.selectedConversationId = conversation.id;
    this.compactThreadOpen = true;
    this.loadMessages(conversation.id);
    this.markConversationRead(conversation.id);
  }

  backToDiscussions(): void {
    this.compactThreadOpen = false;
  }

  startDirectConversation(user: StaffMessagingUser): void {
    this.pageError = '';
    this.api.openDirectConversation(user.userId).subscribe({
      next: (conversation) => {
        this.mergeConversation(conversation);
        this.searchTerm = '';
        this.staffResults = [];
        void this.openConversation(conversation);
      },
      error: (err) => {
        this.pageError = err?.error?.message || 'Failed to open direct conversation.';
      }
    });
  }

  sendMessage(): void {
    const conversationId = this.selectedConversationId;
    const content = this.draftMessage.trim();
    if (!conversationId || !content || this.sending) {
      return;
    }

    this.sending = true;
    this.sendError = '';

    this.api.sendMessage(conversationId, content)
      .pipe(finalize(() => {
        this.sending = false;
      }))
      .subscribe({
        next: (message) => {
          this.draftMessage = '';
          this.upsertMessage(message);
          this.loadConversations(false);
        },
        error: (err) => {
          this.sendError = err?.error?.message || 'Failed to send internal message.';
        }
      });
  }

  refreshAll(): void {
    this.loadConversations();
    if (this.selectedConversationId) {
      this.loadMessages(this.selectedConversationId);
    }
  }

  trackConversation(_: number, conversation: StaffConversationSummary): string {
    return conversation.id;
  }

  trackMessage(_: number, message: StaffMessage): string {
    return message.id;
  }

  isMine(message: StaffMessage): boolean {
    return message.senderId === this.currentUserId;
  }

  getConversationPreview(conversation: StaffConversationSummary): string {
    const preview = conversation.lastMessage?.content?.trim();
    if (!preview) {
      return 'No messages yet.';
    }

    return preview.length > 72 ? `${preview.slice(0, 72)}...` : preview;
  }

  getPeer(conversation: StaffConversationSummary): StaffConversationParticipant | null {
    return conversation.participants.find((participant) => participant.userId !== this.currentUserId)
      ?? conversation.participants[0]
      ?? null;
  }

  getRoleLabel(role: string | undefined | null): string {
    return (role || '').replace(/_/g, ' ');
  }

  getParticipantInitial(conversation: StaffConversationSummary): string {
    const name = this.getPeer(conversation)?.displayName || 'S';
    return name.trim().charAt(0).toUpperCase() || 'S';
  }

  private markConversationRead(conversationId: string): void {
    this.api.markConversationRead(conversationId).subscribe({
      next: (conversation) => {
        this.mergeConversation(conversation);
      },
      error: () => {
        // REST remains best-effort here to keep the thread usable.
      }
    });
  }

  private handleRealtimeEvent(event: StaffMessagingSocketEvent): void {
    if (event.conversation) {
      this.mergeConversation(event.conversation);
    }

    if (!event.message || event.conversationId !== this.selectedConversationId) {
      return;
    }

    this.upsertMessage(event.message);
    if (event.message.senderId !== this.currentUserId) {
      this.markConversationRead(event.conversationId);
    }
  }

  private mergeConversation(conversation: StaffConversationSummary): void {
    const existingIndex = this.conversations.findIndex((item) => item.id === conversation.id);
    if (existingIndex === -1) {
      this.conversations = this.sortConversations([conversation, ...this.conversations]);
      return;
    }

    const updated = [...this.conversations];
    updated[existingIndex] = conversation;
    this.conversations = this.sortConversations(updated);
  }

  private upsertMessage(message: StaffMessage): void {
    const existingIndex = this.messages.findIndex((item) => item.id === message.id);
    if (existingIndex === -1) {
      this.messages = [...this.messages, message];
    } else {
      const updated = [...this.messages];
      updated[existingIndex] = message;
      this.messages = updated;
    }
    this.scrollMessagesToBottom();
  }

  private sortConversations(conversations: StaffConversationSummary[]): StaffConversationSummary[] {
    return [...conversations].sort((left, right) => {
      const leftTime = left.lastMessageAt || left.updatedAt || left.createdAt;
      const rightTime = right.lastMessageAt || right.updatedAt || right.createdAt;
      return new Date(rightTime).getTime() - new Date(leftTime).getTime();
    });
  }

  private scrollMessagesToBottom(): void {
    setTimeout(() => {
      const viewport = this.messageViewport?.nativeElement;
      if (viewport) {
        viewport.scrollTop = viewport.scrollHeight;
      }
    }, 0);
  }
}
