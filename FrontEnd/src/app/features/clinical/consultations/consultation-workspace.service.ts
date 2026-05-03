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
  medicationId?: number;
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
  sex?: string;
  creatinineUmol?: number;
  serumCreatinineUnit?: string;
  egfrFormulaUsed?: string;
  ckdEpiEgfr?: number;
  egfr?: number;
  ckdStage?: string;
  previousEgfr?: number;
  egfrChange?: number;
  egfrChangePercent?: number;
  egfrTrend?: string;
  egfrQualityIndicator?: string;
  egfrLastUpdatedAt?: string;
  alertLowEgfr?: boolean;
  alertRapidDecline?: boolean;
  alertMessage?: string;
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

  /**
   * Merge server consultation outcome into local draft when server is newer or local has no timestamp.
   */
  mergeServerOutcome(
    local: ConsultationWorkspaceDraft,
    outcome: {
      notes?: string | null;
      diagnosis?: string | null;
      prescriptions?: string | null;
      labRequests?: string | null;
      treatmentPlan?: string | null;
      updatedAt?: string | null;
    } | null
  ): ConsultationWorkspaceDraft {
    if (!outcome) {
      return local;
    }

    const serverMs = outcome.updatedAt ? new Date(outcome.updatedAt).getTime() : 0;
    const localMs = local.updatedAt ? new Date(local.updatedAt).getTime() : 0;
    const preferServer = serverMs >= localMs || !local.updatedAt;

    if (!preferServer) {
      return local;
    }

    const merged: ConsultationWorkspaceDraft = {
      ...local,
      soap: { ...local.soap },
      diagnosisList: [...(local.diagnosisList || [])],
      labRequests: [...(local.labRequests || [])],
      prescriptions: [...(local.prescriptions || [])],
      carePlanDoses: [...(local.carePlanDoses || [])],
      metrics: { ...local.metrics }
    };

    if (outcome.notes) {
      const soap = this.tryParseJson<SoapNotes>(outcome.notes);
      if (soap && typeof soap === 'object') {
        merged.soap = {
          subjective: soap.subjective ?? '',
          objective: soap.objective ?? '',
          assessment: soap.assessment ?? '',
          plan: soap.plan ?? ''
        };
      } else {
        merged.soap = {
          ...merged.soap,
          subjective: outcome.notes
        };
      }
    }

    if (outcome.diagnosis) {
      const list = this.tryParseJson<DiagnosisItem[]>(outcome.diagnosis);
      if (Array.isArray(list)) {
        merged.diagnosisList = list;
      }
    }

    if (outcome.prescriptions) {
      const list = this.tryParseJson<PrescriptionItem[]>(outcome.prescriptions);
      if (Array.isArray(list)) {
        merged.prescriptions = list;
      }
    }

    if (outcome.labRequests) {
      const list = this.tryParseJson<LabRequestItem[]>(outcome.labRequests);
      if (Array.isArray(list)) {
        merged.labRequests = list;
      }
    }

    if (outcome.treatmentPlan) {
      const tp = this.tryParseJson<{
        treatmentPlan?: string;
        guardianInstructions?: string;
        followUpDate?: string;
      }>(outcome.treatmentPlan);
      if (tp) {
        merged.treatmentPlan = tp.treatmentPlan ?? merged.treatmentPlan;
        merged.guardianInstructions = tp.guardianInstructions ?? merged.guardianInstructions;
        merged.followUpDate = tp.followUpDate ?? merged.followUpDate;
      }
    }

    if (outcome.updatedAt) {
      merged.updatedAt = outcome.updatedAt;
    }

    return merged;
  }

  mergeServerMetrics(local: ConsultationWorkspaceDraft, metrics: ConsultationMetricsRequest | null): ConsultationWorkspaceDraft {
    if (!metrics || typeof metrics !== 'object') {
      return local;
    }
    return {
      ...local,
      metrics: {
        ...local.metrics,
        ...metrics
      }
    };
  }

  /** Persists notes, diagnosis, plan, prescriptions, metrics — not lab orders (use submitLabRequests). */
  saveDraft(consultationId: string, draft: ConsultationWorkspaceDraft): Observable<boolean> {
    const payload = { ...draft, updatedAt: new Date().toISOString() };

    const track = (obs: Observable<any>) =>
      obs.pipe(
        map(() => true),
        catchError(() => of(false))
      );

    return forkJoin({
      notes: track(this.api.updateConsultationNotes(consultationId, this.serializeSoap(draft.soap))),
      diagnosis: track(this.api.updateConsultationDiagnosis(consultationId, this.serializeDiagnosis(draft.diagnosisList))),
      treatmentPlan: track(this.api.updateConsultationTreatmentPlan(consultationId, this.serializeTreatmentPlan(draft))),
      prescriptions: track(this.api.updateConsultationPrescriptions(consultationId, this.serializePrescriptions(draft.prescriptions))),
      metrics: track(this.api.upsertConsultationMetrics(consultationId, this.normalizeMetrics(draft.metrics)))
    }).pipe(
      map((r) => {
        const coreOk = r.notes && r.diagnosis && r.treatmentPlan && r.prescriptions;
        this.saveLocalCopy(consultationId, payload);
        return coreOk;
      })
    );
  }

  /** Send lab request lines to clinical outcome (explicit action, not part of Save Draft). */
  submitLabRequests(consultationId: string, draft: ConsultationWorkspaceDraft): Observable<boolean> {
    const payload = { ...draft, updatedAt: new Date().toISOString() };
    return this.api
      .updateConsultationLabRequests(consultationId, this.serializeLabRequests(draft.labRequests))
      .pipe(
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

  private tryParseJson<T>(raw: string): T | null {
    try {
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
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
    return JSON.stringify(
      (items ?? [])
        .map((item) => ({
          label: (item?.label ?? '').trim(),
          code: (item?.code ?? '').trim(),
          severity: (item?.severity ?? '').trim(),
          notes: (item?.notes ?? '').trim()
        }))
        .filter((item) => item.label.length > 0 || item.code.length > 0)
    );
  }

  serializeLabRequests(items: LabRequestItem[]): string {
    return JSON.stringify(
      (items ?? [])
        .map((item) => ({
          test: (item?.test ?? '').trim(),
          urgency: item?.urgency ?? 'Routine',
          note: (item?.note ?? '').trim()
        }))
        .filter((item) => item.test.length > 0)
    );
  }

  private serializePrescriptions(items: PrescriptionItem[]): string {
    return JSON.stringify(
      (items ?? [])
        .map((item) => ({
          medication: (item?.medication ?? '').trim(),
          medicationId: item?.medicationId,
          dosage: (item?.dosage ?? '').trim(),
          frequency: (item?.frequency ?? '').trim(),
          durationDays: item?.durationDays,
          note: (item?.note ?? '').trim(),
          doseMgPerKg: item?.doseMgPerKg,
          minDoseMgPerKg: item?.minDoseMgPerKg,
          maxDoseMgPerKg: item?.maxDoseMgPerKg
        }))
        .filter((item) => item.medication.length > 0 || item.medicationId != null)
    );
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

    if (Number.isFinite(Number(metrics?.heightCm))) normalized.heightCm = Number(metrics?.heightCm);
    if (Number.isFinite(Number(metrics?.creatinineMgDl))) normalized.creatinineMgDl = Number(metrics?.creatinineMgDl);
    if (Number.isFinite(Number(metrics?.weightKg))) normalized.weightKg = Number(metrics?.weightKg);
    if (Number.isFinite(Number(metrics?.ageYears))) normalized.ageYears = Number(metrics?.ageYears);
    if (Number.isFinite(Number(metrics?.systolicBpMmHg))) normalized.systolicBpMmHg = Number(metrics?.systolicBpMmHg);
    if (Number.isFinite(Number(metrics?.diastolicBpMmHg))) normalized.diastolicBpMmHg = Number(metrics?.diastolicBpMmHg);

    if (metrics?.sex) normalized.sex = metrics.sex;

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
        sex: undefined,
        creatinineUmol: undefined,
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
