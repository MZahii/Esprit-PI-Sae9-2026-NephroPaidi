import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { getValidToken } from '../auth/keycloak.service';
import { StaffMessagingSocketEvent } from '../models/internal-staff-messaging.models';

@Injectable({
  providedIn: 'root'
})
export class InternalStaffMessagingRealtimeService implements OnDestroy {
  private socket?: WebSocket;
  private reconnectTimer?: ReturnType<typeof setTimeout>;
  private reconnectAttempts = 0;
  private manualDisconnect = false;

  private readonly eventsSubject = new Subject<StaffMessagingSocketEvent>();
  private readonly connectionStateSubject = new BehaviorSubject<boolean>(false);

  readonly events$ = this.eventsSubject.asObservable();
  readonly connected$ = this.connectionStateSubject.asObservable();

  constructor(private ngZone: NgZone) {}

  async connect(): Promise<void> {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    this.manualDisconnect = false;
    clearTimeout(this.reconnectTimer);

    try {
      const token = await getValidToken();
      const socketUrl = this.buildSocketUrl(token);
      this.socket = new WebSocket(socketUrl);

      this.socket.onopen = () => {
        this.reconnectAttempts = 0;
        this.connectionStateSubject.next(true);
      };

      this.socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data) as StaffMessagingSocketEvent;
          this.ngZone.run(() => {
            this.eventsSubject.next(payload);
          });
        } catch (error) {
          console.error('Failed to parse staff messaging event:', error);
        }
      };

      this.socket.onerror = () => {
        this.connectionStateSubject.next(false);
      };

      this.socket.onclose = () => {
        this.connectionStateSubject.next(false);
        this.socket = undefined;
        if (!this.manualDisconnect) {
          this.scheduleReconnect();
        }
      };
    } catch (error) {
      this.connectionStateSubject.next(false);
      this.scheduleReconnect();
      console.error('Failed to connect staff messaging websocket:', error);
    }
  }

  disconnect(): void {
    this.manualDisconnect = true;
    clearTimeout(this.reconnectTimer);
    this.connectionStateSubject.next(false);
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.close();
    }
    this.socket = undefined;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventsSubject.complete();
    this.connectionStateSubject.complete();
  }

  private scheduleReconnect(): void {
    clearTimeout(this.reconnectTimer);
    const delayMs = Math.min(1000 * Math.max(1, this.reconnectAttempts + 1), 10000);
    this.reconnectAttempts += 1;
    this.reconnectTimer = setTimeout(() => {
      void this.connect();
    }, delayMs);
  }

  private buildSocketUrl(token: string): string {
    const explicitBase = environment.communicationWsBaseUrl?.trim();
    const baseUrl = explicitBase || this.deriveSocketBaseUrl();
    const separator = baseUrl.includes('?') ? '&' : '?';
    return `${baseUrl}${separator}token=${encodeURIComponent(token)}`;
  }

  private deriveSocketBaseUrl(): string {
    try {
      const apiUrl = new URL(environment.apiBaseUrl);
      apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:';
      if (apiUrl.port === '8083') {
        apiUrl.port = '8085';
      }
      apiUrl.pathname = '/ws/staff-messaging';
      apiUrl.search = '';
      return apiUrl.toString();
    } catch {
      return 'ws://localhost:8085/ws/staff-messaging';
    }
  }
}
