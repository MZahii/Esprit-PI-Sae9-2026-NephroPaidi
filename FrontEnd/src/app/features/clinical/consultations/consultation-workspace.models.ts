import { DiagnosisItem, LabRequestItem, PrescriptionItem, CarePlanDoseItem } from './consultation-workspace.service';

export interface ClinicalAssessmentDraft {
  soap: {
    subjective: string;
    objective: string;
    assessment: string;
    plan: string;
  };
  treatmentPlan: string;
  guardianInstructions: string;
  diagnosisList: DiagnosisItem[];
}

export interface ConsultationSummaryViewModel {
  consultationId: string;
  patientName: string;
  subtitle: string;
  scheduledAt?: string | null;
  statusLabel: string;
  statusClass: string;
  completenessScore: number;
  completenessLabel: string;
  completenessClass: string;
  hasUnsavedChanges: boolean;
  renalRiskLabel: string;
}

export interface PatientSnapshotViewModel {
  patientName: string;
  subtitle: string;
  allergiesLabel: string;
  ageLabel: string;
  sexLabel: string;
  growthNarrative: string;
  timelineSummary: string;
  activeProblemCount: number;
  followUpStatusLabel: string;
}

export interface RenalMetricsViewModel {
  egfrLabel: string;
  egfrFormulaLabel: string;
  ckdStage: string;
  bloodPressurePercentileLabel: string;
  heightPercentileLabel: string;
  weightPercentileLabel: string;
  trajectoryLabel: string;
  previousEgfrLabel: string;
  latestDeltaLabel: string;
  clinicalAiRecommendationLabel: string;
  clinicalAiConfidencePercent: string;
  clinicalAiSummary: string;
  egfrAiRiskLabel: string;
  egfrAiProbabilityLabel: string;
  egfrAiConfidenceLabel: string;
  egfrAiPredicted3mLabel: string;
  egfrAiPredicted6mLabel: string;
  egfrAiPredicted12mLabel: string;
  egfrAiHint: string;
  latestLabSourceFileName: string;
  latestLabSourceAvailable: boolean;
  trendPoints: Array<{ dateTime: string; egfr: number | null }>;
}

export interface ClinicalAlertViewModel {
  title: string;
  severity: 'critical' | 'warning' | 'info';
}

export interface DispositionState {
  followUpStatusLabel: string;
  followUpStatusClass: string;
  followUpPreviewDate: string;
  followUpEnabled: boolean;
  followUpQueued: boolean;
  hospitalizationActive: boolean;
  activeMedicationCount: number;
}

export interface FollowUpDecisionDraft {
  enabled: boolean;
  offsetAmount: number;
  offsetUnit: 'DAYS' | 'WEEKS' | 'MONTHS';
  previewDate: string;
  requestSent: boolean;
  message: string;
  treatmentPlanReady: boolean;
  guardianInstructionsReady: boolean;
}

export interface HospitalizationLaunchDraft {
  active: boolean;
  carePlanDoses: CarePlanDoseItem[];
  queryParams: Record<string, string>;
}

export interface MedicationReviewViewModel {
  prescriptions: PrescriptionItem[];
  suggestions: Record<number, any[]>;
  doseAlerts: string[];
}

export interface LabOrdersViewModel {
  labRequests: LabRequestItem[];
  submitting: boolean;
  selectedCount: number;
}
