import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ClinicalApiService, ConsultationMetricsRequest } from '../../../core/services/clinical-api.service';

export interface SoapNotes {
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
}

export interface DiagnosisItem {
  label: string;
  code?: string;
  severity?: string;
  notes?: string;
}

export interface LabRequestItem {
  test: string;
  urgency: 'Routine' | 'Urgent' | 'STAT';
  note?: string;
}

export interface PrescriptionItem {
  medication: string;
  dosage: string;
  frequency: string;
  durationDays?: number;
  note?: string;
  doseMgPerKg?: number;
  minDoseMgPerKg?: number;
  maxDoseMgPerKg?: number;
}

export interface ConsultationMetrics {
  heightCm?: number;
  creatinineMgDl?: number;
  weightKg?: number;
  ageYears?: number;
  systolicBpMmHg?: number;
  diastolicBpMmHg?: number;
  // CKD-EPI formula fields
  sex?: string;  // 'M' or 'F' - REQUIRED for CKD-EPI
  creatinineUmol?: number;  // SI units (calculated by backend)
  serumCreatinineUnit?: string;  // "MICROMOL_L" or "MG_DL"
  egfrFormulaUsed?: string;  // "CKD_EPI_2021" or "COCKCROFT_GAULT"
  ckdEpiEgfr?: number;  // CKD-EPI eGFR result
  egfr?: number;  // eGFR value (display)
  ckdStage?: string;  // KDIGO classification: "NORMAL", "STAGE_1", "STAGE_2", "STAGE_3", "STAGE_4"
  previousEgfr?: number;  // Previous eGFR for trend
  egfrChange?: number;  // Absolute change from previous
  egfrChangePercent?: number;  // Percentage change from previous
  egfrTrend?: string;  // "STABLE", "IMPROVING", "DECLINING", "RAPIDLY_DECLINING"
  egfrQualityIndicator?: string;  // "HIGH_QUALITY", "MEDIUM_QUALITY", "LOW_QUALITY"
  egfrLastUpdatedAt?: string;  // ISO timestamp when eGFR was calculated
  alertLowEgfr?: boolean;  // Alert flag for low eGFR
  alertRapidDecline?: boolean;  // Alert flag for rapid decline
  alertMessage?: string;  // Clinical alert message
}

export interface CarePlanDoseItem {
  medication: string;
  scheduleTime: string;
  taken: boolean;
  missedReason?: string;
}

export interface ConsultationWorkspaceDraft {
  soap: SoapNotes;
  diagnosisList: DiagnosisItem[];
  treatmentPlan: string;
  guardianInstructions: string;
  followUpDate?: string;
  labRequests: LabRequestItem[];
  prescriptions: PrescriptionItem[];
  carePlanDoses: CarePlanDoseItem[];
  metrics: ConsultationMetrics;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ConsultationWorkspaceService {
  constructor(private api: ClinicalApiService) {}

  getDraft(consultationId: string): Observable<ConsultationWorkspaceDraft> {
    const raw = localStorage.getItem(this.storageKey(consultationId));
    if (raw) {
      try {
        return of(JSON.parse(raw) as ConsultationWorkspaceDraft);
      } catch {
        // ignore parse errors
      }
    }

    return of(this.emptyDraft());
  }

  saveDraft(consultationId: string, draft: ConsultationWorkspaceDraft): Observable<boolean> {
    const payload = { ...draft, updatedAt: new Date().toISOString() };

    return forkJoin([
      this.api.updateConsultationNotes(consultationId, this.serializeSoap(draft.soap)),
      this.api.updateConsultationDiagnosis(consultationId, this.serializeDiagnosis(draft.diagnosisList)),
      this.api.updateConsultationTreatmentPlan(consultationId, this.serializeTreatmentPlan(draft)),
      this.api.updateConsultationLabRequests(consultationId, this.serializeLabRequests(draft.labRequests)),
      this.api.updateConsultationPrescriptions(consultationId, this.serializePrescriptions(draft.prescriptions)),
      this.api.upsertConsultationMetrics(consultationId, this.normalizeMetrics(draft.metrics))
    ]).pipe(
      map(() => {
        this.saveLocalCopy(consultationId, payload);
        return true;
      }),
      catchError(() => {
        this.saveLocalCopy(consultationId, payload);
        return of(false);
      })
    );
  }

  completeConsultation(consultationId: string, draft: ConsultationWorkspaceDraft): Observable<boolean> {
    return this.saveDraft(consultationId, draft);
  }

  private saveLocalCopy(consultationId: string, payload: ConsultationWorkspaceDraft): void {
    localStorage.setItem(this.storageKey(consultationId), JSON.stringify(payload));
  }

  private serializeSoap(soap: SoapNotes): string {
    return JSON.stringify({
      subjective: (soap?.subjective ?? '').trim(),
      objective: (soap?.objective ?? '').trim(),
      assessment: (soap?.assessment ?? '').trim(),
      plan: (soap?.plan ?? '').trim()
    });
  }

  private serializeDiagnosis(items: DiagnosisItem[]): string {
    return JSON.stringify((items ?? []).map((item) => ({
      label: (item?.label ?? '').trim(),
      code: (item?.code ?? '').trim(),
      severity: (item?.severity ?? '').trim(),
      notes: (item?.notes ?? '').trim()
    })).filter((item) => item.label.length > 0 || item.code.length > 0));
  }

  private serializeLabRequests(items: LabRequestItem[]): string {
    return JSON.stringify((items ?? []).map((item) => ({
      test: (item?.test ?? '').trim(),
      urgency: item?.urgency ?? 'Routine',
      note: (item?.note ?? '').trim()
    })).filter((item) => item.test.length > 0));
  }

  private serializePrescriptions(items: PrescriptionItem[]): string {
    return JSON.stringify((items ?? []).map((item) => ({
      medication: (item?.medication ?? '').trim(),
      dosage: (item?.dosage ?? '').trim(),
      frequency: (item?.frequency ?? '').trim(),
      durationDays: item?.durationDays,
      note: (item?.note ?? '').trim(),
      doseMgPerKg: item?.doseMgPerKg,
      minDoseMgPerKg: item?.minDoseMgPerKg,
      maxDoseMgPerKg: item?.maxDoseMgPerKg
    })).filter((item) => item.medication.length > 0));
  }

  private serializeTreatmentPlan(draft: ConsultationWorkspaceDraft): string {
    return JSON.stringify({
      treatmentPlan: (draft?.treatmentPlan ?? '').trim(),
      guardianInstructions: (draft?.guardianInstructions ?? '').trim(),
      followUpDate: (draft?.followUpDate ?? '').trim()
    });
  }

  private normalizeMetrics(metrics: ConsultationMetrics): ConsultationMetricsRequest {
    const normalized: ConsultationMetricsRequest = {};

    // Input/output biometric fields
    if (Number.isFinite(Number(metrics?.heightCm))) normalized.heightCm = Number(metrics?.heightCm);
    if (Number.isFinite(Number(metrics?.creatinineMgDl))) normalized.creatinineMgDl = Number(metrics?.creatinineMgDl);
    if (Number.isFinite(Number(metrics?.weightKg))) normalized.weightKg = Number(metrics?.weightKg);
    if (Number.isFinite(Number(metrics?.ageYears))) normalized.ageYears = Number(metrics?.ageYears);
    if (Number.isFinite(Number(metrics?.systolicBpMmHg))) normalized.systolicBpMmHg = Number(metrics?.systolicBpMmHg);
    if (Number.isFinite(Number(metrics?.diastolicBpMmHg))) normalized.diastolicBpMmHg = Number(metrics?.diastolicBpMmHg);

    // CKD-EPI formula requirement: sex is REQUIRED
    if (metrics?.sex) normalized.sex = metrics.sex;

    // Backend-calculated fields (read-only, returned by server)
    if (Number.isFinite(Number(metrics?.creatinineUmol))) normalized.creatinineUmol = Number(metrics?.creatinineUmol);
    if (metrics?.serumCreatinineUnit) normalized.serumCreatinineUnit = metrics.serumCreatinineUnit;
    if (metrics?.egfrFormulaUsed) normalized.egfrFormulaUsed = metrics.egfrFormulaUsed;
    if (Number.isFinite(Number(metrics?.ckdEpiEgfr))) normalized.ckdEpiEgfr = Number(metrics?.ckdEpiEgfr);
    if (Number.isFinite(Number(metrics?.egfr))) normalized.egfr = Number(metrics?.egfr);
    if (metrics?.ckdStage) normalized.ckdStage = metrics.ckdStage;
    if (Number.isFinite(Number(metrics?.previousEgfr))) normalized.previousEgfr = Number(metrics?.previousEgfr);
    if (Number.isFinite(Number(metrics?.egfrChange))) normalized.egfrChange = Number(metrics?.egfrChange);
    if (Number.isFinite(Number(metrics?.egfrChangePercent))) normalized.egfrChangePercent = Number(metrics?.egfrChangePercent);
    if (metrics?.egfrTrend) normalized.egfrTrend = metrics.egfrTrend;
    if (metrics?.egfrQualityIndicator) normalized.egfrQualityIndicator = metrics.egfrQualityIndicator;
    if (metrics?.egfrLastUpdatedAt) normalized.egfrLastUpdatedAt = metrics.egfrLastUpdatedAt;
    if (typeof metrics?.alertLowEgfr === 'boolean') normalized.alertLowEgfr = metrics.alertLowEgfr;
    if (typeof metrics?.alertRapidDecline === 'boolean') normalized.alertRapidDecline = metrics.alertRapidDecline;
    if (metrics?.alertMessage) normalized.alertMessage = metrics.alertMessage;

    return normalized;
  }

  private emptyDraft(): ConsultationWorkspaceDraft {
    return {
      soap: {
        subjective: '',
        objective: '',
        assessment: '',
        plan: ''
      },
      diagnosisList: [],
      treatmentPlan: '',
      guardianInstructions: '',
      followUpDate: '',
      labRequests: [],
      prescriptions: [],
      carePlanDoses: [],
      metrics: {
        heightCm: undefined,
        creatinineMgDl: undefined,
        weightKg: undefined,
        ageYears: undefined,
        systolicBpMmHg: undefined,
        diastolicBpMmHg: undefined,
        // CKD-EPI fields
        sex: undefined,  // Required: 'M' or 'F'
        creatinineUmol: undefined,  // Calculated by backend
        serumCreatinineUnit: undefined,
        egfrFormulaUsed: undefined,
        ckdEpiEgfr: undefined,
        egfr: undefined,
        ckdStage: undefined,
        previousEgfr: undefined,
        egfrChange: undefined,
        egfrChangePercent: undefined,
        egfrTrend: undefined,
        egfrQualityIndicator: undefined,
        egfrLastUpdatedAt: undefined,
        alertLowEgfr: undefined,
        alertRapidDecline: undefined,
        alertMessage: undefined
      }
    };
  }

  private storageKey(consultationId: string): string {
    return `clinical_workspace_${consultationId}`;
  }
}
