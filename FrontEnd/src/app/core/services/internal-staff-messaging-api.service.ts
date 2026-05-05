import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  StaffConversationSummary,
  StaffMessage,
  StaffMessagingUser
} from '../models/internal-staff-messaging.models';

@Injectable({
  providedIn: 'root'
})
export class InternalStaffMessagingApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/communication/staff`;

  constructor(private http: HttpClient) {}

  listStaffUsers(query = '', limit = 20): Observable<StaffMessagingUser[]> {
    let params = new HttpParams().set('limit', String(limit));
    const trimmed = query.trim();
    if (trimmed) {
      params = params.set('q', trimmed);
    }

    return this.http.get<StaffMessagingUser[]>(`${this.baseUrl}/users`, { params });
  }

  openDirectConversation(targetUserId: string): Observable<StaffConversationSummary> {
    return this.http.post<StaffConversationSummary>(`${this.baseUrl}/conversations/direct`, {
      targetUserId
    });
  }

  listConversations(): Observable<StaffConversationSummary[]> {
    return this.http.get<StaffConversationSummary[]>(`${this.baseUrl}/conversations`);
  }

  listMessages(conversationId: string): Observable<StaffMessage[]> {
    return this.http.get<StaffMessage[]>(`${this.baseUrl}/conversations/${conversationId}/messages`);
  }

  sendMessage(conversationId: string, content: string): Observable<StaffMessage> {
    return this.http.post<StaffMessage>(`${this.baseUrl}/conversations/${conversationId}/messages`, {
      content
    });
  }

  markConversationRead(conversationId: string): Observable<StaffConversationSummary> {
    return this.http.put<StaffConversationSummary>(`${this.baseUrl}/conversations/${conversationId}/read`, {});
  }
}
