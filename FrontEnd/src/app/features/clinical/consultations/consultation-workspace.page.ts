import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import {
  ConsultationWorkspaceDraft,
  ConsultationWorkspaceService,
  DiagnosisItem,
  LabRequestItem,
  PrescriptionItem
} from './consultation-workspace.service';

type WorkspaceTab = 'notes' | 'diagnosis' | 'plan' | 'labs' | 'prescriptions';

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
    metrics: {
      heightCm: undefined,
      creatinineMgDl: undefined,
      weightKg: undefined,
      ageYears: undefined
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
    this.minFollowUpDate = this.toLocalDateTimeMin(new Date());
    this.loadConsultation();
    this.loadDraft();
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
      next: () => {
        this.saving = false;
        this.infoMessage = 'Draft saved locally.';
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
      next: () => {
        this.api.updateConsultation(this.consultationId, { status: 'COMPLETED' }).subscribe({
          next: () => {
            this.saving = false;
            this.completed = true;
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
    return [...this.egfrAlerts, ...this.doseAlerts];
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
      return;
    }

    const history = (this.history || [])
      .filter(item => Number(item.patientId) === Number(patientId) && item.dateTime)
      .sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime());

    const previous = history.find(item => item.id !== this.consultationId);
    if (!previous) {
      this.previousEgfr = null;
      this.previousEgfrDate = null;
      return;
    }

    this.workspace.getDraft(previous.id).subscribe((draft) => {
      this.previousEgfr = this.calculateEgfr(draft.metrics.heightCm, draft.metrics.creatinineMgDl);
      this.previousEgfrDate = previous.dateTime;
    });
  }

  private calculateEgfr(heightCm?: number, creatinineMgDl?: number): number | null {
    const height = Number(heightCm);
    const creatinine = Number(creatinineMgDl);
    if (!Number.isFinite(height) || height <= 0) return null;
    if (!Number.isFinite(creatinine) || creatinine <= 0) return null;
    const value = (0.413 * height) / creatinine;
    return Math.round(value * 10) / 10;
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
}
