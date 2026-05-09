export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'NO_SHOW';

export interface Appointment {
  id: string;
  patientId: number;
  doctorId: string;
  scheduledAt: string;
  durationMinutes?: number;
  reason?: string;
  status?: AppointmentStatus;
  consultationId?: string;
}

export type ConsultationStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface Consultation {
  id: string;
  patientId: number;
  appointmentId?: string;
  dateTime: string;
  status?: ConsultationStatus;
  startedAt?: string;
  completedAt?: string;
  summary?: string;
}
