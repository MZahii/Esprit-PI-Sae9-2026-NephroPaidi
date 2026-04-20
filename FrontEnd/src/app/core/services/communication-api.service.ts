import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthStorageService } from '../auth/auth-storage.service';
import { environment } from '../../../environments/environment';

export type MessageType = 'ADMINISTRATIVE' | 'MEDICAL' | 'LAB_RESULT' | 'APPOINTMENT' | 'QUESTION' | 'COMPLAINT' | 'OTHER';
export type PriorityLevel = 'NORMAL' | 'HIGH';
export type MessageQueue = 'RECEPTIONIST' | 'NURSE' | 'DOCTOR';
export type MessageStatus = 'PENDING' | 'READ' | 'IN_PROGRESS' | 'ESCALATED' | 'RESPONDED' | 'CLOSED';
export type SenderRole = 'GUARDIAN' | 'RECEPTIONIST' | 'NURSE' | 'DOCTOR';
export type BulkMessageAction = 'TAKE' | 'MARK_READ' | 'UNASSIGN' | 'CLOSE';

export interface CreateMessagePayload {
  patientId?: number | null;
  messageType: MessageType;
  priority: PriorityLevel;
  subject?: string | null;
  messageText: string;
}

export interface GuardianPatientItem {
  patientId: number;
  fullName: string;
  dob: string;
}

export interface PatientDirectoryItem {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
}

export interface StaffDirectoryItem {
  keycloakId: string;
  username: string;
}

export interface MessageReply {
  id: string;
  messageId: string;
  senderKeycloakId: string;
  senderRole: SenderRole;
  replyText: string;
  createdAt: string;
}

export interface FollowUpMessage {
  id: string;
  patientId: number;
  guardianKeycloakId: string;
  assignedDoctorKeycloakId?: string | null;
  messageType: MessageType;
  priority: PriorityLevel;
  queue: MessageQueue;
  status: MessageStatus;
  assignedToUserKeycloakId?: string | null;
  assignedToRole?: MessageQueue | null;
  subject?: string | null;
  messageText: string;
  createdAt: string;
  readAt?: string | null;
  lastUpdatedAt: string;
  closedAt?: string | null;
  replies: MessageReply[];
}

export interface AuditLogItem {
  id: string;
  messageId: string;
  actorKeycloakId: string;
  actorRole: SenderRole;
  action: string;
  details?: string | null;
  createdAt: string;
}

export interface BulkMessageOperationFailure {
  messageId: string;
  error: string;
}

export interface BulkMessageOperationResponse {
  action: BulkMessageAction;
  requestedCount: number;
  successCount: number;
  failedCount: number;
  succeededIds: string[];
  failures: BulkMessageOperationFailure[];
}

export interface QuickReplyTemplate {
  id: string;
  name: string;
  messageType: MessageType;
  templateText: string;
  usageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface QuickReplyTemplatePayload {
  name: string;
  messageType: MessageType;
  templateText: string;
}

export interface InboxFilters {
  queue?: MessageQueue;
  status?: MessageStatus;
  priority?: PriorityLevel;
  messageType?: MessageType;
  patientId?: number;
  dateFrom?: string;
  dateTo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CommunicationApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/communication`;

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService
  ) {}

  private authHeaders(): { headers?: Record<string, string> } {
    const token = this.authStorage.getAccessToken();
    return token ? { headers: { Authorization: `Bearer ${token}` } } : {};
  }

  createMessage(payload: CreateMessagePayload): Observable<{ id: string; status: MessageStatus; queue: MessageQueue; createdAt: string }> {
    return this.http.post<{ id: string; status: MessageStatus; queue: MessageQueue; createdAt: string }>(
      `${this.baseUrl}/messages`,
      payload
    );
  }

  getMyPatients(): Observable<GuardianPatientItem[]> {
    return this.http.get<GuardianPatientItem[]>(
      `${this.baseUrl}/patients/my`,
      this.authHeaders()
    ).pipe(catchError(() => of([])));
  }

  getMyMessages(): Observable<FollowUpMessage[]> {
    return this.http.get<FollowUpMessage[]>(`${this.baseUrl}/messages/my`);
  }

  getMessageById(id: string): Observable<FollowUpMessage> {
    return this.http.get<FollowUpMessage>(`${this.baseUrl}/messages/${id}`);
  }

  replyMessage(id: string, replyText: string): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(
      `${this.baseUrl}/messages/${id}/reply`,
      { replyText }
    );
  }

  closeMessage(id: string): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(`${this.baseUrl}/messages/${id}/close`, {});
  }

  takeMessage(id: string): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(`${this.baseUrl}/messages/${id}/take`, {});
  }

  unassignMessage(id: string): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(`${this.baseUrl}/messages/${id}/unassign`, {});
  }

  markRead(id: string): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(`${this.baseUrl}/messages/${id}/mark-read`, {});
  }

  escalate(id: string, doctorKeycloakId?: string | null): Observable<FollowUpMessage> {
    return this.http.post<FollowUpMessage>(
      `${this.baseUrl}/messages/${id}/escalate`,
      { doctorKeycloakId: doctorKeycloakId ?? null }
    );
  }

  getInbox(filters: InboxFilters): Observable<FollowUpMessage[]> {
    let params = new HttpParams();
    if (filters.queue) params = params.set('queue', filters.queue);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.priority) params = params.set('priority', filters.priority);
    if (filters.messageType) params = params.set('messageType', filters.messageType);
    if (filters.patientId) params = params.set('patientId', String(filters.patientId));
    if (filters.dateFrom) params = params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params = params.set('dateTo', filters.dateTo);

    return this.http.get<FollowUpMessage[]>(`${this.baseUrl}/backoffice/inbox`, {
      params
    });
  }

  getAudit(id: string): Observable<AuditLogItem[]> {
    return this.http.get<AuditLogItem[]>(`${this.baseUrl}/messages/${id}/audit`);
  }

  bulkOperateMessages(action: BulkMessageAction, messageIds: string[]): Observable<BulkMessageOperationResponse> {
    return this.http.post<BulkMessageOperationResponse>(`${this.baseUrl}/backoffice/messages/bulk`, {
      action,
      messageIds
    });
  }

  getPatientsDirectory(): Observable<PatientDirectoryItem[]> {
    return this.http.get<PatientDirectoryItem[]>(`${environment.apiBaseUrl}/api/patients`);
  }

  getDoctorsDirectory(): Observable<StaffDirectoryItem[]> {
    return this.http.get<StaffDirectoryItem[]>(`${this.baseUrl}/api/users/public/doctors`);
  }

  listTemplates(messageType?: MessageType): Observable<QuickReplyTemplate[]> {
    let params = new HttpParams();
    if (messageType) {
      params = params.set('messageType', messageType);
    }
    return this.http.get<QuickReplyTemplate[]>(`${this.baseUrl}/templates`, { params });
  }

  createTemplate(payload: QuickReplyTemplatePayload): Observable<QuickReplyTemplate> {
    return this.http.post<QuickReplyTemplate>(`${this.baseUrl}/templates`, payload);
  }

  updateTemplate(id: string, payload: QuickReplyTemplatePayload): Observable<QuickReplyTemplate> {
    return this.http.put<QuickReplyTemplate>(`${this.baseUrl}/templates/${id}`, payload);
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/templates/${id}`);
  }

  useTemplate(id: string): Observable<QuickReplyTemplate> {
    return this.http.post<QuickReplyTemplate>(`${this.baseUrl}/templates/${id}/use`, {});
  }
}
