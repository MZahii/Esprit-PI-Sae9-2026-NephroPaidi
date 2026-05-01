import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import {
  CarePlanDoseItem,
  ConsultationWorkspaceDraft,
  ConsultationWorkspaceService,
  DiagnosisItem,
  LabRequestItem,
  PrescriptionItem
} from './consultation-workspace.service';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

type WorkspaceTab = 'notes' | 'diagnosis' | 'plan' | 'labs' | 'prescriptions' | 'adherence';

interface EgfrTrendPoint {
  consultationId: string;
  dateTime: string;
  egfr: number | null;
  creatinineMgDl: number | null;
}

const HEIGHT_MEDIAN_BY_AGE: Record<number, number> = {
  2: 87,
  3: 95,
  4: 102,
  5: 109,
  6: 116,
  7: 121,
  8: 127,
  9: 132,
  10: 138,
  11: 144,
  12: 150,
  13: 156,
  14: 161,
  15: 165,
  16: 167,
  17: 168,
  18: 168
};

const HEIGHT_SD_BY_AGE: Record<number, number> = {
  2: 3.6,
  3: 4,
  4: 4.3,
  5: 4.6,
  6: 4.9,
  7: 5.1,
  8: 5.4,
  9: 5.7,
  10: 6,
  11: 6.5,
  12: 7,
  13: 7.4,
  14: 7.8,
  15: 8.1,
  16: 8.4,
  17: 8.6,
  18: 8.8
};

const WEIGHT_MEDIAN_BY_AGE: Record<number, number> = {
  2: 12.5,
  3: 14.5,
  4: 16.5,
  5: 18.5,
  6: 21,
  7: 23,
  8: 26,
  9: 29,
  10: 32,
  11: 36,
  12: 41,
  13: 46,
  14: 51,
  15: 56,
  16: 60,
  17: 62,
  18: 63
};

const WEIGHT_SD_BY_AGE: Record<number, number> = {
  2: 1.8,
  3: 2.1,
  4: 2.4,
  5: 2.8,
  6: 3.2,
  7: 3.8,
  8: 4.2,
  9: 4.7,
  10: 5.3,
  11: 6,
  12: 7,
  13: 8,
  14: 8.8,
  15: 9.3,
  16: 9.7,
  17: 10,
  18: 10.2
};

const SBP_P90_BY_AGE: Record<number, number> = {
  2: 104,
  3: 106,
  4: 108,
  5: 109,
  6: 111,
  7: 113,
  8: 115,
  9: 117,
  10: 119,
  11: 121,
  12: 123,
  13: 125,
  14: 127,
  15: 129,
  16: 131,
  17: 132,
  18: 133
};

const SBP_P95_BY_AGE: Record<number, number> = {
  2: 107,
  3: 109,
  4: 111,
  5: 113,
  6: 115,
  7: 117,
  8: 119,
  9: 121,
  10: 123,
  11: 125,
  12: 127,
  13: 129,
  14: 131,
  15: 133,
  16: 135,
  17: 136,
  18: 137
};

const DBP_P90_BY_AGE: Record<number, number> = {
  2: 63,
  3: 64,
  4: 66,
  5: 67,
  6: 68,
  7: 69,
  8: 70,
  9: 71,
  10: 72,
  11: 73,
  12: 74,
  13: 75,
  14: 76,
  15: 77,
  16: 78,
  17: 79,
  18: 80
};

const DBP_P95_BY_AGE: Record<number, number> = {
  2: 66,
  3: 67,
  4: 69,
  5: 70,
  6: 72,
  7: 73,
  8: 74,
  9: 75,
  10: 76,
  11: 77,
  12: 78,
  13: 79,
  14: 80,
  15: 81,
  16: 82,
  17: 83,
  18: 84
};

@Component({
  selector: 'app-consultation-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultation-workspace.page.html',
  styleUrl: './consultation-workspace.page.scss'
})
export class ConsultationWorkspacePage implements OnInit {
  consultationId = '';
  consultation: any | null = null;
  history: any[] = [];
  previousEgfr: number | null = null;
  previousEgfrDate: string | null = null;
  egfrTrendPoints: EgfrTrendPoint[] = [];
  returnUrl: string | null = null;
  private lastSavedSnapshot = '';

  loading = false;
  error = '';
  saving = false;
  completed = false;
  infoMessage = '';

  activeTab: WorkspaceTab = 'notes';
  draft: ConsultationWorkspaceDraft = {
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
      creatinineMgDl: undefined,  // From lab results (read-only in UI)
      weightKg: undefined,
      ageYears: undefined,
      sex: undefined,  // 'M' or 'F' - required for CKD-EPI
      systolicBpMmHg: undefined,
      diastolicBpMmHg: undefined,
      egfr: undefined,  // Auto-calculated by backend
      egfrQualityIndicator: undefined,  // HIGH_QUALITY, MEDIUM_QUALITY, LOW_QUALITY
      ckdStage: undefined,  // From backend
      egfrLastUpdatedAt: undefined
    }
  };

  followUpForm = {
    scheduledAt: '',
    durationMinutes: 30,
    reason: ''
  };
  followUpError = '';
  followUpSuccess = '';
  minFollowUpDate = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ClinicalApiService,
    private workspace: ConsultationWorkspaceService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Consultation id missing.';
      return;
    }
    this.consultationId = id;
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;
    this.minFollowUpDate = this.toLocalDateTimeMin(new Date());
    this.loadConsultation();
    this.loadDraft();
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges) {
      event.preventDefault();
      event.returnValue = true;
    }
  }

  setTab(tab: WorkspaceTab): void {
    this.activeTab = tab;
  }

  loadConsultation(): void {
    this.loading = true;
    this.error = '';
    this.api.getConsultation(this.consultationId).subscribe({
      next: (item) => {
        this.consultation = item;
        this.loading = false;
        this.loadHistory();
      },
      error: () => {
        this.api.listMyConsultations().subscribe({
          next: (items) => {
            this.consultation = (items || []).find(c => c.id === this.consultationId) || null;
            this.loading = false;
            this.setHistory(items || []);
            if (!this.consultation) {
              this.error = 'Consultation not found.';
            }
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to load consultation.';
          }
        });
      }
    });
  }

  loadDraft(): void {
    this.workspace.getDraft(this.consultationId).subscribe((draft) => {
      this.draft = draft;
      this.lastSavedSnapshot = this.buildDraftSnapshot();
      if (draft.followUpDate && !this.followUpForm.scheduledAt) {
        this.followUpForm.scheduledAt = draft.followUpDate;
      }
    });
  }

  saveDraft(): void {
    if (!this.consultationId) return;
    this.saving = true;
    this.infoMessage = '';
    this.workspace.saveDraft(this.consultationId, this.draft).subscribe({
      next: (saved) => {
        this.saving = false;
        if (saved) {
          this.lastSavedSnapshot = this.buildDraftSnapshot();
        }
        this.infoMessage = saved
          ? 'Draft saved to backend.'
          : 'Backend save failed. A local backup was kept.';
      },
      error: () => {
        this.saving = false;
        this.infoMessage = 'Unable to save draft.';
      }
    });
  }

  completeConsultation(): void {
    if (!this.consultationId) return;
    if (!this.canComplete) {
      this.infoMessage = 'Completeness score must be at least 70 to mark completed.';
      return;
    }
    this.saving = true;
    this.infoMessage = '';
    this.workspace.completeConsultation(this.consultationId, this.draft).subscribe({
      next: (saved) => {
        if (!saved) {
          this.saving = false;
          this.infoMessage = 'Cannot complete consultation because backend save failed. Please retry.';
          return;
        }

        this.api.updateConsultation(this.consultationId, { status: 'COMPLETED' }).subscribe({
          next: () => {
            this.saving = false;
            this.completed = true;
            this.lastSavedSnapshot = this.buildDraftSnapshot();
            this.infoMessage = 'Consultation marked as completed.';
            this.loadConsultation();
          },
          error: () => {
            this.saving = false;
            this.infoMessage = 'Saved locally, but failed to update status.';
          }
        });
      },
      error: () => {
        this.saving = false;
        this.infoMessage = 'Unable to complete consultation.';
      }
    });
  }

  scheduleFollowUp(): void {
    this.followUpError = '';
    this.followUpSuccess = '';

    if (!this.consultation?.patientId || !this.consultation?.doctorId) {
      this.followUpError = 'Missing patient or doctor for follow-up.';
      return;
    }

    if (!this.followUpForm.scheduledAt) {
      this.followUpError = 'Follow-up date/time is required.';
      return;
    }

    if (this.isPastDatetime(this.followUpForm.scheduledAt)) {
      this.followUpError = 'Follow-up date must be in the future.';
      return;
    }

    const duration = Number(this.followUpForm.durationMinutes || 30);
    if (!Number.isFinite(duration) || duration < 10 || duration > 180) {
      this.followUpError = 'Duration must be between 10 and 180 minutes.';
      return;
    }

    this.api.createAppointment({
      patientId: this.consultation.patientId,
      doctorId: this.consultation.doctorId,
      scheduledAt: this.normalizeDateTime(this.followUpForm.scheduledAt),
      durationMinutes: duration,
      reason: this.followUpForm.reason || 'Follow-up visit'
    }).subscribe({
      next: () => {
        this.followUpSuccess = 'Follow-up appointment scheduled.';
      },
      error: () => {
        this.followUpError = 'Unable to schedule follow-up appointment.';
      }
    });
  }

  addDiagnosis(): void {
    const item: DiagnosisItem = {
      label: '',
      code: '',
      severity: 'Moderate',
      notes: ''
    };
    this.draft.diagnosisList = [...(this.draft.diagnosisList || []), item];
  }

  removeDiagnosis(index: number): void {
    this.draft.diagnosisList = (this.draft.diagnosisList || []).filter((_, i) => i !== index);
  }

  addLabRequest(): void {
    const item: LabRequestItem = {
      test: '',
      urgency: 'Routine',
      note: ''
    };
    this.draft.labRequests = [...(this.draft.labRequests || []), item];
  }

  removeLabRequest(index: number): void {
    this.draft.labRequests = (this.draft.labRequests || []).filter((_, i) => i !== index);
  }

  addPrescription(): void {
    const item: PrescriptionItem = {
      medication: '',
      dosage: '',
      frequency: '',
      durationDays: undefined,
      note: '',
      doseMgPerKg: undefined,
      minDoseMgPerKg: undefined,
      maxDoseMgPerKg: undefined
    };
    this.draft.prescriptions = [...(this.draft.prescriptions || []), item];
  }

  addCarePlanDose(): void {
    const item: CarePlanDoseItem = {
      medication: '',
      scheduleTime: '',
      taken: false,
      missedReason: ''
    };
    this.draft.carePlanDoses = [...(this.draft.carePlanDoses || []), item];
  }

  removeCarePlanDose(index: number): void {
    this.draft.carePlanDoses = (this.draft.carePlanDoses || []).filter((_, i) => i !== index);
  }

  get hasUnsavedChanges(): boolean {
    return this.buildDraftSnapshot() !== this.lastSavedSnapshot;
  }

  get egfrTrajectoryLabel(): string {
    const values = this.egfrTrendPoints
      .map((point) => point.egfr)
      .filter((value): value is number => value !== null);

    if (values.length < 2) return 'Insufficient data';

    const first = values[0];
    const last = values[values.length - 1];
    if (!first) return 'Insufficient data';

    const changePct = ((last - first) / first) * 100;
    if (changePct <= -15) return 'Worsening trend';
    if (changePct >= 10) return 'Improving trend';
    return 'Stable trend';
  }

  get latestEgfrDeltaPct(): number | null {
    const values = this.egfrTrendPoints
      .map((point) => point.egfr)
      .filter((value): value is number => value !== null);
    if (values.length < 2 || !values[values.length - 2]) return null;
    const previous = values[values.length - 2];
    const current = values[values.length - 1];
    return Math.round((((current - previous) / previous) * 100) * 10) / 10;
  }

  get weightPercentileLabel(): string {
    const percentile = this.computePercentileByAge(
      this.draft.metrics.ageYears,
      this.draft.metrics.weightKg,
      WEIGHT_MEDIAN_BY_AGE,
      WEIGHT_SD_BY_AGE
    );
    return percentile === null ? 'N/A' : `P${percentile}`;
  }

  get heightPercentileLabel(): string {
    const percentile = this.computePercentileByAge(
      this.draft.metrics.ageYears,
      this.draft.metrics.heightCm,
      HEIGHT_MEDIAN_BY_AGE,
      HEIGHT_SD_BY_AGE
    );
    return percentile === null ? 'N/A' : `P${percentile}`;
  }

  get bloodPressurePercentileLabel(): string {
    const age = Number(this.draft.metrics.ageYears);
    const sbp = Number(this.draft.metrics.systolicBpMmHg);
    const dbp = Number(this.draft.metrics.diastolicBpMmHg);
    if (!Number.isFinite(age) || !Number.isFinite(sbp) || !Number.isFinite(dbp)) return 'N/A';

    const sbpP90 = this.interpolateByAge(age, SBP_P90_BY_AGE);
    const sbpP95 = this.interpolateByAge(age, SBP_P95_BY_AGE);
    const dbpP90 = this.interpolateByAge(age, DBP_P90_BY_AGE);
    const dbpP95 = this.interpolateByAge(age, DBP_P95_BY_AGE);

    if (sbpP95 === null || dbpP95 === null || sbpP90 === null || dbpP90 === null) return 'N/A';

    if (sbp >= sbpP95 || dbp >= dbpP95) return '>=P95 (Hypertension range)';
    if (sbp >= sbpP90 || dbp >= dbpP90) return 'P90-P94 (Elevated range)';
    return '<P90 (Expected range)';
  }

  get carePlanAdherenceRate(): number {
    const doses = this.draft.carePlanDoses || [];
    if (doses.length === 0) return 0;
    const taken = doses.filter((dose) => dose.taken).length;
    return Math.round((taken / doses.length) * 100);
  }

  get carePlanMissedCount(): number {
    return (this.draft.carePlanDoses || []).filter((dose) => !dose.taken).length;
  }

  get adherenceAlerts(): string[] {
    const alerts: string[] = [];
    const doses = this.draft.carePlanDoses || [];
    if (doses.length === 0) return alerts;

    if (this.carePlanAdherenceRate < 80) {
      alerts.push(`Adherence at ${this.carePlanAdherenceRate}%: consider caregiver counseling.`);
    }

    const missedWithoutReason = doses.filter((dose) => !dose.taken && !(dose.missedReason || '').trim()).length;
    if (missedWithoutReason > 0) {
      alerts.push(`${missedWithoutReason} missed dose(s) without reason documented.`);
    }

    return alerts;
  }

  removePrescription(index: number): void {
    this.draft.prescriptions = (this.draft.prescriptions || []).filter((_, i) => i !== index);
  }

  get egfrValue(): number | null {
    return this.calculateEgfr(this.draft.metrics.heightCm, this.draft.metrics.creatinineMgDl);
  }

  get ckdStage(): string {
    const egfr = this.egfrValue;
    if (egfr === null) return 'N/A';
    if (egfr >= 90) return 'G1 (>=90)';
    if (egfr >= 60) return 'G2 (60-89)';
    if (egfr >= 45) return 'G3a (45-59)';
    if (egfr >= 30) return 'G3b (30-44)';
    if (egfr >= 15) return 'G4 (15-29)';
    return 'G5 (<15)';
  }

  get alerts(): string[] {
    return [...this.egfrAlerts, ...this.doseAlerts, ...this.adherenceAlerts];
  }

  get egfrAlerts(): string[] {
    const alerts: string[] = [];
    const egfr = this.egfrValue;
    if (egfr !== null && egfr < 60) {
      alerts.push('eGFR < 60: possible renal insufficiency.');
    }

    if (egfr !== null && this.previousEgfr !== null && this.previousEgfr > 0) {
      const deltaPct = ((egfr - this.previousEgfr) / this.previousEgfr) * 100;
      if (deltaPct <= -20) {
        const label = this.previousEgfrDate ? `since ${this.previousEgfrDate}` : 'since last visit';
        alerts.push(`Rapid progression: eGFR decreased by ${Math.abs(Math.round(deltaPct))}% ${label}.`);
      }
    }

    if (this.egfrTrajectoryLabel === 'Worsening trend') {
      alerts.push('Longitudinal trend shows worsening renal function across recent visits.');
    }

    if (this.bloodPressurePercentileLabel.includes('Hypertension')) {
      alerts.push('Blood pressure percentile is in hypertension range for age.');
    }

    return alerts;
  }

  get doseAlerts(): string[] {
    const alerts: string[] = [];
    const weight = Number(this.draft.metrics.weightKg);
    if (!Number.isFinite(weight) || weight <= 0) return alerts;

    (this.draft.prescriptions || []).forEach((item, index) => {
      const dose = Number(item.doseMgPerKg);
      if (!Number.isFinite(dose) || dose <= 0) return;
      const min = Number.isFinite(Number(item.minDoseMgPerKg)) ? Number(item.minDoseMgPerKg) : 1;
      const max = Number.isFinite(Number(item.maxDoseMgPerKg)) ? Number(item.maxDoseMgPerKg) : 20;
      if (dose < min || dose > max) {
        const label = item.medication ? item.medication : `Prescription ${index + 1}`;
        alerts.push(`${label}: dose out of range (${min}-${max} mg/kg/day).`);
      }
    });

    return alerts;
  }

  get completenessScore(): number {
    let score = 0;
    const soapFilled = [
      this.draft.soap.subjective,
      this.draft.soap.objective,
      this.draft.soap.assessment,
      this.draft.soap.plan
    ].some(value => (value || '').trim().length > 0);

    if (soapFilled) score += 20;

    const diagnosisFilled = (this.draft.diagnosisList || []).some(item =>
      (item.label || '').trim().length > 0 || (item.code || '').trim().length > 0
    );
    if (diagnosisFilled) score += 20;

    if ((this.draft.treatmentPlan || '').trim().length > 0) score += 20;

    const prescriptionsFilled = (this.draft.prescriptions || []).some(item =>
      (item.medication || '').trim().length > 0
    );
    if (prescriptionsFilled) score += 20;

    if ((this.draft.guardianInstructions || '').trim().length > 0) score += 10;

    if ((this.draft.followUpDate || '').trim().length > 0) score += 10;

    return score;
  }

  get completenessLabel(): string {
    if (this.completenessScore >= 70) return 'Excellent';
    if (this.completenessScore >= 50) return 'Acceptable';
    return 'Incomplete';
  }

  get completenessClass(): string {
    if (this.completenessScore >= 70) return 'bg-soft-success text-success';
    if (this.completenessScore >= 50) return 'bg-soft-warning text-warning';
    return 'bg-soft-danger text-danger';
  }

  get canComplete(): boolean {
    return this.completenessScore >= 70;
  }

  get hospitalizationQueryParams(): Record<string, string> {
    const params: Record<string, string> = {};
    if (this.consultationId) {
      params['consultationId'] = this.consultationId;
    }
    if (this.consultation?.patientId !== undefined && this.consultation?.patientId !== null) {
      params['patientId'] = String(this.consultation.patientId);
    }
    return params;
  }

  get patientHistory(): any[] {
    const patientId = this.consultation?.patientId;
    if (!patientId) return [];
    return (this.history || [])
      .filter(item => Number(item.patientId) === Number(patientId))
      .sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime());
  }

  getPatientLabel(id: number | string | undefined): string {
    if (!id) return '-';
    if (this.consultation?.patientName && String(this.consultation?.patientId) === String(id)) {
      return this.consultation.patientName;
    }
    const match = (this.history || []).find(item => String(item.patientId) === String(id) && item.patientName);
    if (match?.patientName) return match.patientName;
    return String(id);
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'CANCELLED') return 'bg-soft-danger text-danger';
    if (status === 'IN_PROGRESS') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  private loadHistory(): void {
    this.api.listMyConsultations().subscribe({
      next: (items) => {
        this.setHistory(items || []);
      },
      error: () => {
        this.history = [];
        this.previousEgfr = null;
        this.previousEgfrDate = null;
      }
    });
  }

  private setHistory(items: any[]): void {
    this.history = items || [];
    this.computePreviousEgfr();
  }

  private computePreviousEgfr(): void {
    const patientId = this.consultation?.patientId;
    if (!patientId) {
      this.previousEgfr = null;
      this.previousEgfrDate = null;
      this.egfrTrendPoints = [];
      return;
    }

    const history = (this.history || [])
      .filter(item => Number(item.patientId) === Number(patientId) && item.dateTime)
      .sort((a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime());

    if (history.length === 0) {
      this.egfrTrendPoints = [];
      return;
    }

    const requests = history.map((item) =>
      this.workspace.getDraft(item.id).pipe(
        map((draft) => ({
          consultationId: String(item.id),
          dateTime: String(item.dateTime),
          egfr: this.calculateEgfr(draft.metrics.heightCm, draft.metrics.creatinineMgDl),
          creatinineMgDl: Number.isFinite(Number(draft.metrics.creatinineMgDl))
            ? Number(draft.metrics.creatinineMgDl)
            : null
        } as EgfrTrendPoint)),
        catchError(() => of({
          consultationId: String(item.id),
          dateTime: String(item.dateTime),
          egfr: null,
          creatinineMgDl: null
        } as EgfrTrendPoint))
      )
    );

    forkJoin(requests).subscribe((points) => {
      this.egfrTrendPoints = points
        .sort((a, b) => new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime())
        .slice(-8);

      const previousPoint = [...this.egfrTrendPoints]
        .reverse()
        .find((point) => point.consultationId !== this.consultationId && point.egfr !== null);

      this.previousEgfr = previousPoint?.egfr ?? null;
      this.previousEgfrDate = previousPoint?.dateTime ?? null;
    });
  }

  private computePercentileByAge(
    ageYears: number | undefined,
    value: number | undefined,
    medianTable: Record<number, number>,
    sdTable: Record<number, number>
  ): number | null {
    const age = Number(ageYears);
    const numericValue = Number(value);
    if (!Number.isFinite(age) || !Number.isFinite(numericValue)) return null;

    const median = this.interpolateByAge(age, medianTable);
    const sd = this.interpolateByAge(age, sdTable);
    if (median === null || sd === null || sd <= 0) return null;

    const z = (numericValue - median) / sd;
    const percentile = 50 + z * 34;
    return Math.max(1, Math.min(99, Math.round(percentile)));
  }

  private interpolateByAge(ageYears: number, table: Record<number, number>): number | null {
    const keys = Object.keys(table).map(Number).sort((a, b) => a - b);
    if (keys.length === 0) return null;

    if (ageYears <= keys[0]) return table[keys[0]];
    if (ageYears >= keys[keys.length - 1]) return table[keys[keys.length - 1]];

    const lower = Math.floor(ageYears);
    const upper = Math.ceil(ageYears);
    if (lower === upper) return table[lower] ?? null;

    const lowerValue = table[lower];
    const upperValue = table[upper];
    if (!Number.isFinite(lowerValue) || !Number.isFinite(upperValue)) return null;

    const ratio = ageYears - lower;
    return lowerValue + (upperValue - lowerValue) * ratio;
  }

  private buildDraftSnapshot(): string {
    return JSON.stringify(this.draft);
  }

  private calculateEgfr(heightCm?: number, creatinineMgDl?: number): number | null {
    // LEGACY METHOD - eGFR is now calculated by backend using CKD-EPI formula
    // This method kept for backward compatibility with local estimation only
    const height = Number(heightCm);
    const creatinine = Number(creatinineMgDl);
    if (!Number.isFinite(height) || height <= 0) return null;
    if (!Number.isFinite(creatinine) || creatinine <= 0) return null;
    const value = (0.413 * height) / creatinine;
    return Math.round(value * 10) / 10;
  }

  // ============================================================
  // CKD-EPI Metrics Display Helpers
  // ============================================================

  /**
   * Get CKD stage display color based on stage value
   * Used for visual alerts in UI: green (normal) → yellow (stage 2) → red (stage 4)
   */
  getCkdStageColor(stage?: string): string {
    if (!stage) return 'text-secondary';  // Gray for unknown
    switch (stage.toUpperCase()) {
      case 'STAGE_NORMAL':
      case 'NORMAL':
        return 'text-success';  // Green
      case 'STAGE_1':
      case 'STAGE1':
        return 'text-success';  // Green
      case 'STAGE_2':
      case 'STAGE2':
        return 'text-warning';  // Yellow
      case 'STAGE_3':
      case 'STAGE3':
        return 'text-danger';   // Red
      case 'STAGE_4':
      case 'STAGE4':
        return 'text-danger';   // Dark red
      case 'STAGE_5':
      case 'STAGE5':
        return 'text-danger';   // Dark red
      default:
        return 'text-secondary';
    }
  }

  /**
   * Get CKD stage display name
   */
  getCkdStageName(stage?: string): string {
    if (!stage) return 'Unknown';
    return stage.replace(/_/g, ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * Format eGFR value with units and clinical interpretation
   */
  formatEgfr(egfr?: number): string {
    if (egfr === undefined || egfr === null) {
      return 'Awaiting lab results...';
    }
    const formatted = egfr.toFixed(1);
    if (egfr >= 90) return `${formatted} mL/min/1.73m² (Normal)`;
    if (egfr >= 60) return `${formatted} mL/min/1.73m² (Mild)`;
    if (egfr >= 30) return `${formatted} mL/min/1.73m² (Moderate)`;
    if (egfr >= 15) return `${formatted} mL/min/1.73m² (Severe)`;
    return `${formatted} mL/min/1.73m² (Kidney Failure)`;
  }

  /**
   * Get quality badge class for display
   */
  getQualityBadgeClass(quality?: string): string {
    if (!quality) return 'badge bg-secondary';
    switch (quality.toUpperCase()) {
      case 'HIGH_QUALITY':
        return 'badge bg-success';
      case 'MEDIUM_QUALITY':
        return 'badge bg-warning';
      case 'LOW_QUALITY':
        return 'badge bg-danger';
      default:
        return 'badge bg-secondary';
    }
  }

  /**
   * Format creatinine value with units
   */
  formatCreatinine(creatinine?: number, unit: string = 'mg/dL'): string {
    if (creatinine === undefined || creatinine === null) {
      return 'Awaiting lab results...';
    }
    return `${creatinine.toFixed(2)} ${unit}`;
  }

  /**
   * Format last updated timestamp
   */
  formatLastUpdated(timestamp?: string): string {
    if (!timestamp) return 'Not yet calculated';
    try {
      const date = new Date(timestamp);
      if (Number.isNaN(date.getTime())) return 'Invalid date';
      return date.toLocaleString();
    } catch {
      return 'Invalid date';
    }
  }

  private isPastDatetime(value: string): boolean {
    if (!value) return false;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return false;
    return date.getTime() < Date.now();
  }

  private toLocalDateTimeMin(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private normalizeDateTime(value: string): string {
    if (!value) return value;
    return value.length === 16 ? `${value}:00` : value;
  }

  goBack(): void {
    if (this.hasUnsavedChanges) {
      const confirmed = window.confirm('You have unsaved changes in this workspace. Leave without saving?');
      if (!confirmed) return;
    }
    if (this.returnUrl) {
      this.router.navigateByUrl(this.returnUrl);
    } else {
      this.router.navigate(['/backoffice/consultations']);
    }
  }
}
