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
  patientName?: string;
}

export interface PatientProfile {
  id?: number;
  firstName?: string;
  lastName?: string;
  age?: number;
  gender?: string;
  sex?: string;
  bloodType?: string;
  allergies?: string;
  dateOfBirth?: string;
  birthDate?: string;
}

export interface ConsultationOutcomeResponse {
  notes?: string | null;
  diagnosis?: string | null;
  prescriptions?: string | null;
  labRequests?: string | null;
  treatmentPlan?: string | null;
  updatedAt?: string | null;
}

export interface ConsultationDossierDocumentItem {
  label: string;
  details: string;
}

export interface ConsultationDossierConsultationItem {
  consultationId: string;
  consultationDate: string;
  status?: ConsultationStatus;
  outcomeUpdatedAt?: string;
  notes?: string;
  diagnosis?: string;
  treatmentPlan?: string;
  labRequests: ConsultationDossierDocumentItem[];
  prescriptions: ConsultationDossierDocumentItem[];
}

export interface ConsultationDossierTimelineItem {
  kind: 'CONSULTATION' | 'LAB_REQUEST' | 'PRESCRIPTION' | string;
  occurredAt?: string;
  consultationId?: string;
  title: string;
  summary?: string;
  details?: string;
}

export interface ConsultationDossierSummary {
  consultationCount: number;
  labRequestCount: number;
  prescriptionCount: number;
  dossierEntryCount: number;
}

export interface ConsultationMedicalDossier {
  patientId: number;
  patientName?: string;
  sourceConsultationId: string;
  generatedAt: string;
  consultations: ConsultationDossierConsultationItem[];
  timeline: ConsultationDossierTimelineItem[];
  summary: ConsultationDossierSummary;
}
