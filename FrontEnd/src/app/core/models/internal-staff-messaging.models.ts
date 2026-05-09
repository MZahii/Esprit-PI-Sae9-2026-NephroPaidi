export type InternalStaffRole =
  | 'ADMIN'
  | 'HR'
  | 'DOCTOR'
  | 'NURSE'
  | 'RECEPTIONIST'
  | 'PHARMACIST'
  | 'LAB_AGENT'
  | 'SURGEON';

export type StaffConversationType = 'DIRECT';
export type StaffMessageType = 'TEXT';
export type StaffMessagingSocketEventType = 'MESSAGE_CREATED' | 'CONVERSATION_READ';

export interface StaffMessagingUser {
  userId: string;
  username: string;
  displayName: string;
  role: InternalStaffRole;
  email?: string | null;
  avatarUrl?: string | null;
}

export interface StaffConversationParticipant {
  userId: string;
  userRole: InternalStaffRole;
  displayName: string;
  joinedAt: string;
  lastReadAt?: string | null;
  archived: boolean;
  muted: boolean;
}

export interface StaffMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderRole: InternalStaffRole;
  senderDisplayName: string;
  content: string;
  messageType: StaffMessageType;
  createdAt: string;
  editedAt?: string | null;
  deletedAt?: string | null;
}

export interface StaffConversationSummary {
  id: string;
  type: StaffConversationType;
  title?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  lastMessageAt?: string | null;
  unreadCount: number;
  lastMessage?: StaffMessage | null;
  participants: StaffConversationParticipant[];
}

export interface StaffMessagingSocketEvent {
  type: StaffMessagingSocketEventType;
  conversationId: string;
  conversation?: StaffConversationSummary | null;
  message?: StaffMessage | null;
}
