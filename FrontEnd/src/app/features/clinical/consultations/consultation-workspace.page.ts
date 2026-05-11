import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  ClinicalApiService,
  ClinicalLabRequestResponse,
  EgfrMlClassificationResponse,
  EgfrMlRegressionResponse
} from '../../../core/services/clinical-api.service';
import {
  CarePlanDoseItem,
  ConsultationWorkspaceDraft,
  ConsultationWorkspaceService,
  DiagnosisItem,
  LabRequestItem,
  PrescriptionItem
} from './consultation-workspace.service';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { EncounterHeaderComponent } from './components/encounter-header.component';
import { PatientSnapshotCardComponent } from './components/patient-snapshot-card.component';
import { ClinicalAssessmentFormComponent } from './components/clinical-assessment-form.component';
import { RenalMetricsPanelComponent } from './components/renal-metrics-panel.component';
import { ClinicalAlertCenterComponent } from './components/clinical-alert-center.component';
import { DispositionPanelComponent } from './components/disposition-panel.component';
import { FollowUpRequestDialogComponent } from './components/follow-up-request-dialog.component';
import { HospitalizationActionDialogComponent } from './components/hospitalization-action-dialog.component';
import { MedicationReviewPanelComponent } from './components/medication-review-panel.component';
import { LabOrdersPanelComponent } from './components/lab-orders-panel.component';
import {
  ClinicalAlertViewModel,
  ConsultationSummaryViewModel,
  DispositionState,
  FollowUpDecisionDraft,
  HospitalizationLaunchDraft,
  LabOrdersViewModel,
  MedicationReviewViewModel,
  PatientSnapshotViewModel,
  RenalMetricsViewModel
} from './consultation-workspace.models';
import {
  Consultation,
  ConsultationOutcomeResponse,
  PatientProfile
} from '../models/clinical.models';

type WorkspaceTab = 'overview' | 'assessment' | 'orders' | 'medications' | 'review';
type WorkspaceDialog = 'patient-profile' | 'alert-center' | 'follow-up' | 'medication-review' | null;
type WorkflowSectionKey =
  | 'patient-context'
  | 'vitals'
  | 'nephrology'
  | 'hospitalization'
  | 'discharge'
  | 'alerts'
  | 'review';

interface EgfrTrendPoint {
  consultationId: string;
  dateTime: string;
  egfr: number | null;
  creatinineMgDl: number | null;
}

interface EgfrMlPredictionState {
  loading: boolean;
  available: boolean;
  error: string;
  regression: EgfrMlRegressionResponse | null;
  classification: EgfrMlClassificationResponse | null;
  payloadPreview: Record<string, unknown> | null;
}

interface WorkflowSection {
  key: WorkflowSectionKey;
  label: string;
  hint: string;
}

interface LabRequestFileSelection {
  testKey: string;
  testLabel: string;
  files: File[];
}

interface ClinicalHistoryConsultationItem {
  id: string;
  dateTime: string;
  status?: string;
  diagnosisSummary: string;
}

interface ClinicalHistoryLabItem {
  consultationId?: string;
  requestId: string;
  title: string;
  urgency: string;
  status: string;
  createdAt?: string;
  latestResultFileName?: string;
  latestResultUploadedAt?: string;
  latestAiSummary?: string;
}

type WorkflowIntakeForm = FormGroup<{
  ageYears: FormControl<number | null>;
  sex: FormControl<string>;
  heightCm: FormControl<number | null>;
  weightKg: FormControl<number | null>;
  systolicBpMmHg: FormControl<number | null>;
  diastolicBpMmHg: FormControl<number | null>;
  heartRateBpm: FormControl<number | null>;
  respiratoryRateBpm: FormControl<number | null>;
  temperatureC: FormControl<number | null>;
  oxygenSaturationPct: FormControl<number | null>;
  creatinineMgDl: FormControl<number | null>;
}>;

const WORKFLOW_SECTIONS: WorkflowSection[] = [
  { key: 'patient-context', label: 'Patient Context', hint: 'Identity, history, allergies, and encounter framing.' },
  { key: 'vitals', label: 'Vitals', hint: 'Anthropometrics, blood pressure, and immediate measurements.' },
  { key: 'nephrology', label: 'Nephrology', hint: 'Creatinine, eGFR, CKD stage, and renal findings.' },
  { key: 'hospitalization', label: 'Hospitalization', hint: 'Escalate when inpatient workflow or nurse handoff is needed.' },
  { key: 'discharge', label: 'Discharge & Follow-up', hint: 'Plan, prescriptions, follow-up, and home instructions.' },
  { key: 'alerts', label: 'Alerts', hint: 'Surface renal risk, adherence, and blood-pressure concerns.' },
  { key: 'review', label: 'Review', hint: 'Generate summary and confirm the consultation is complete.' }
];

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
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    EncounterHeaderComponent,
    PatientSnapshotCardComponent,
    ClinicalAssessmentFormComponent,
    RenalMetricsPanelComponent,
    ClinicalAlertCenterComponent,
    DispositionPanelComponent,
    FollowUpRequestDialogComponent,
    HospitalizationActionDialogComponent,
    MedicationReviewPanelComponent,
    LabOrdersPanelComponent
  ],
  templateUrl: './consultation-workspace.page.html',
  styleUrl: './consultation-workspace.page.scss'
})
export class ConsultationWorkspacePage implements OnInit, OnDestroy {
  consultationId = '';
  consultation: Consultation | null = null;
  patientProfile: PatientProfile | null = null;
  history: Consultation[] = [];
  consultationHistoryCards: ClinicalHistoryConsultationItem[] = [];
  labHistoryItems: ClinicalHistoryLabItem[] = [];
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
  generatedSummary = '';
  openingAiSource = false;
  egfrMl: EgfrMlPredictionState = {
    loading: false,
    available: false,
    error: '',
    regression: null,
    classification: null,
    payloadPreview: null
  };

  activeTab: WorkspaceTab = 'overview';
  activeWorkflowSection: WorkflowSectionKey = 'patient-context';
  workflowSections = WORKFLOW_SECTIONS;
  workflowIntakeForm: WorkflowIntakeForm;
  workspaceDialog: WorkspaceDialog = null;
  followUpDecisionDraftVm: FollowUpDecisionDraft = {
    enabled: true,
    offsetAmount: 5,
    offsetUnit: 'DAYS',
    previewDate: '',
    requestSent: false,
    message: '',
    treatmentPlanReady: false,
    guardianInstructionsReady: false
  };
  hospitalizationLaunchDraftVm: HospitalizationLaunchDraft = {
    active: false,
    carePlanDoses: [],
    queryParams: {}
  };
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
      heartRateBpm: undefined,
      respiratoryRateBpm: undefined,
      temperatureC: undefined,
      oxygenSaturationPct: undefined,
      egfr: undefined,  // Auto-calculated by backend
      egfrQualityIndicator: undefined,  // HIGH_QUALITY, MEDIUM_QUALITY, LOW_QUALITY
      ckdStage: undefined,  // From backend
      egfrLastUpdatedAt: undefined
    }
  };

  /** Unlocks Adherence tab after doctor chooses Hospitalize Patient */
  adherenceUnlocked = false;

  /** Doctor → receptionist follow-up (clinical-service) */
  doctorFollowUpEnabled = true;
  followUpOffsetAmount = 5;
  followUpOffsetUnit: 'DAYS' | 'WEEKS' | 'MONTHS' = 'DAYS';
  followUpRequestMessage = '';
  followUpRequestSubmitting = false;
  doctorFollowUpRequestSent = false;

  labSubmitting = false;
  labRequestFiles: Record<string, File[]> = {};

  showHospitalizeModal = false;

  medicationSuggestions: Record<number, any[]> = {};
  private workflowFormSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ClinicalApiService,
    private workspace: ConsultationWorkspaceService,
    private formBuilder: FormBuilder
  ) {
    this.workflowIntakeForm = this.formBuilder.group({
      ageYears: [null as number | null],
      sex: [''],
      heightCm: [null as number | null],
      weightKg: [null as number | null],
      systolicBpMmHg: [null as number | null],
      diastolicBpMmHg: [null as number | null],
      heartRateBpm: [null as number | null],
      respiratoryRateBpm: [null as number | null],
      temperatureC: [null as number | null],
      oxygenSaturationPct: [null as number | null],
      creatinineMgDl: [null as number | null]
    }) as WorkflowIntakeForm;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Consultation id missing.';
      return;
    }
    this.consultationId = id;
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;
    this.bindWorkflowIntakeForm();
    this.loadConsultation();
    this.loadDraft();
  }

  ngOnDestroy(): void {
    this.workflowFormSubscription?.unsubscribe();
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

  openWorkspaceDialog(dialog: Exclude<WorkspaceDialog, null>): void {
    this.workspaceDialog = dialog;
  }

  closeWorkspaceDialog(): void {
    this.workspaceDialog = null;
  }

  openFollowUpPlanner(): void {
    this.activeWorkflowSection = 'discharge';
    this.setTab('review');
    this.syncFollowUpDraftVm();
    this.openWorkspaceDialog('follow-up');
  }

  openMedicationReview(): void {
    this.activeWorkflowSection = 'alerts';
    this.setTab('medications');
    this.openWorkspaceDialog('medication-review');
  }

  openWorkflowSection(section: WorkflowSectionKey): void {
    this.activeWorkflowSection = section;
    switch (section) {
      case 'patient-context':
      case 'vitals':
      case 'nephrology':
      case 'alerts':
        this.setTab('overview');
        break;
      case 'hospitalization':
        this.openHospitalizeModal();
        break;
      case 'discharge':
        this.openFollowUpPlanner();
        break;
      case 'review':
        this.setTab('review');
        break;
    }
  }

  loadConsultation(): void {
    this.loading = true;
    this.error = '';
    this.api.getConsultation(this.consultationId).subscribe({
      next: (item) => {
        this.consultation = item;
        this.loadPatientProfile();
        this.loading = false;
        this.loadHistory();
      },
      error: () => {
        this.api.listMyConsultations().subscribe({
          next: (items) => {
            this.consultation = (items || []).find(c => c.id === this.consultationId) || null;
            this.loadPatientProfile();
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
    this.workspace
      .getDraft(this.consultationId)
      .pipe(
        switchMap((local) =>
          this.api.getConsultationOutcome(this.consultationId).pipe(
            map((outcome) => this.workspace.mergeServerOutcome(local, outcome)),
            catchError(() => of(local))
          )
        ),
        switchMap((draft) =>
          this.api.getConsultationMetrics(this.consultationId).pipe(
            map((m) => this.workspace.mergeServerMetrics(draft, m)),
            catchError(() => of(draft))
          )
        )
      )
      .subscribe((draft) => {
        this.draft = draft;
        this.adherenceUnlocked = (draft.carePlanDoses?.length ?? 0) > 0;
        this.syncWorkflowIntakeFormFromDraft();
        this.lastSavedSnapshot = this.buildDraftSnapshot();
        this.loadConsultationLabRequests();
        this.runEgfrMlPrediction();
      });
  }

  get followUpPreviewDate(): string {
    return this.computePreviewDate(this.followUpOffsetAmount, this.followUpOffsetUnit);
  }

  submitDoctorFollowUpRequest(): void {
    if (!this.consultationId) return;
    this.doctorFollowUpEnabled = this.followUpDecisionDraftVm.enabled;
    this.followUpOffsetAmount = this.followUpDecisionDraftVm.offsetAmount;
    this.followUpOffsetUnit = this.followUpDecisionDraftVm.offsetUnit;
    this.followUpRequestMessage = '';
    this.followUpRequestSubmitting = true;
    this.api
      .createDoctorFollowUpRequest(this.consultationId, {
        offsetAmount: Math.max(1, Number(this.followUpOffsetAmount) || 1),
        offsetUnit: this.followUpOffsetUnit,
        notes: this.draft.treatmentPlan?.trim() ? `Plan context: ${this.draft.treatmentPlan.trim().slice(0, 200)}` : undefined
      })
      .subscribe({
        next: () => {
          this.followUpRequestSubmitting = false;
          this.followUpRequestMessage = 'Follow-up request sent to receptionist queue.';
          this.doctorFollowUpRequestSent = true;
          this.followUpDecisionDraftVm.requestSent = true;
          this.followUpDecisionDraftVm.message = this.followUpRequestMessage;
          this.followUpDecisionDraftVm.previewDate = this.followUpPreviewDate;
        },
        error: () => {
          this.followUpRequestSubmitting = false;
          this.followUpRequestMessage = 'Could not send follow-up request.';
          this.followUpDecisionDraftVm.message = this.followUpRequestMessage;
        }
      });
  }

  sendLabsToBackend(): void {
    if (!this.consultationId) return;
    const patientId = Number(this.consultation?.patientId);
    if (!Number.isFinite(patientId) || patientId <= 0) {
      this.infoMessage = 'Cannot create lab requests because the patient record is missing.';
      return;
    }
    this.labSubmitting = true;
    this.infoMessage = '';
    this.workspace.submitLabRequests(this.consultationId, patientId, this.draft).subscribe({
      next: (result) => {
        if (!result.ok) {
          this.labSubmitting = false;
          this.infoMessage = 'Failed to save lab requests.';
          return;
        }
        const requestId = result.requestId || this.draft.labRequests.find((item) => item.backendRequestId)?.backendRequestId;
        if (!requestId) {
          this.labSubmitting = false;
          this.lastSavedSnapshot = this.buildDraftSnapshot();
          this.infoMessage = 'Lab requests were saved, but no lab inbox request id was returned.';
          return;
        }

        const uploads = Object.entries(this.labRequestFiles)
          .flatMap(([testKey, files]) => {
            const item = this.draft.labRequests.find((candidate) => (candidate.key || this.toItemKey(candidate.test)) === testKey);
            if (!item || !files.length) {
              return [];
            }
            return files.map((file) =>
              this.api.uploadLabSupportingFile(requestId, file, testKey, item.test).pipe(
                map(() => true),
                catchError(() => of(false))
              )
            );
          });

        const finishMessage = (uploadedAll: boolean) => {
          this.labSubmitting = false;
          this.lastSavedSnapshot = this.buildDraftSnapshot();
          this.labRequestFiles = {};
          this.loadConsultationLabRequests();
          this.infoMessage = uploadedAll
            ? 'Grouped lab request sent to the lab inbox with test attachments.'
            : 'Grouped lab request sent, but one or more supporting files could not be uploaded.';
        };

        if (!uploads.length) {
          finishMessage(true);
          return;
        }

        forkJoin(uploads).subscribe({
          next: (statuses) => {
            finishMessage(statuses.every(Boolean));
          },
          error: () => {
            finishMessage(false);
          }
        });
      },
      error: () => {
        this.labSubmitting = false;
        this.infoMessage = 'Failed to save lab requests.';
      }
    });
  }

  openHospitalizeModal(): void {
    this.adherenceUnlocked = true;
    this.prefillCarePlanFromPrescriptions();
    this.hospitalizationLaunchDraftVm = {
      active: true,
      carePlanDoses: this.draft.carePlanDoses,
      queryParams: this.hospitalizationQueryParams
    };
    this.showHospitalizeModal = true;
  }

  openMedicalDossier(): void {
    this.router.navigate(['/backoffice/consultations', this.consultationId], {
      queryParams: {
        returnUrl: `/backoffice/consultations/${this.consultationId}/workspace`
      }
    });
  }

  closeHospitalizeModal(): void {
    this.showHospitalizeModal = false;
  }

  prefillCarePlanFromPrescriptions(): void {
    const lines = (this.draft.prescriptions || []).filter((p) => (p.medication || '').trim().length > 0);
    if (lines.length === 0) return;
    const existing = new Set((this.draft.carePlanDoses || []).map((d) => (d.medication || '').toLowerCase()));
    const toAdd: CarePlanDoseItem[] = [];
    for (const p of lines) {
      const name = (p.medication || '').trim();
      if (existing.has(name.toLowerCase())) continue;
      toAdd.push({ medication: name, scheduleTime: '08:00', taken: false });
    }
    this.draft.carePlanDoses = [...(this.draft.carePlanDoses || []), ...toAdd];
  }

  onMedicationSearch(index: number, event: Event): void {
    const q = ((event.target as HTMLInputElement)?.value ?? '').trim();
    if (q.length < 2) {
      this.medicationSuggestions[index] = [];
      return;
    }
    this.api.searchMedications(q, 15).subscribe({
      next: (list) => {
        this.medicationSuggestions[index] = list || [];
      },
      error: () => {
        this.medicationSuggestions[index] = [];
      }
    });
  }

  pickMedication(index: number, med: any): void {
    const row = this.draft.prescriptions[index];
    if (!row || !med) return;
    row.medication = med.name ?? '';
    row.medicationId = med.medicationId ?? med.id;
    if (med.pediatricDosage && !(row.dosage || '').trim()) {
      row.dosage = med.pediatricDosage;
    }
    this.medicationSuggestions[index] = [];
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
        if (!saved) {
          this.infoMessage = 'Backend save failed. A local backup was kept.';
          return;
        }
        this.infoMessage = 'Encounter draft saved to backend.';
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
      this.infoMessage = 'Required: Fill SOAP notes, add a diagnosis, and enter treatment plan to complete.';
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
      key: '',
      test: '',
      category: '',
      urgency: 'Routine',
      note: '',
      uploadedFileNames: []
    };
    this.draft.labRequests = [...(this.draft.labRequests || []), item];
  }

  removeLabRequest(index: number): void {
    const item = (this.draft.labRequests || [])[index];
    if (item?.backendRequestId) {
      this.infoMessage = 'Submitted lab requests cannot be removed from the workspace draft. Review them in clinical history instead.';
      return;
    }
    const key = item ? (item.key || this.toItemKey(item.test)) : '';
    if (key) {
      delete this.labRequestFiles[key];
    }
    this.draft.labRequests = (this.draft.labRequests || []).filter((_, i) => i !== index);
  }

  setLabRequestFiles(selection: LabRequestFileSelection): void {
    const key = selection.testKey || this.toItemKey(selection.testLabel);
    this.labRequestFiles[key] = [...selection.files];
    this.draft.labRequests = (this.draft.labRequests || []).map((item) =>
      (item.key || this.toItemKey(item.test)) === key
        ? {
            ...item,
            key,
            uploadedFileNames: selection.files.map((file) => file.name)
          }
        : item
    );
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

  get quickStatCards(): Array<{ label: string; value: string; accent: string; detail: string }> {
    return [
      {
        label: 'Active alerts',
        value: `${this.alerts.length}`,
        accent: this.alerts.length > 0 ? 'critical' : 'calm',
        detail: this.alerts.length > 0 ? 'Needs clinician review' : 'No urgent warnings'
      },
      {
        label: 'CKD stage',
        value: this.ckdStage,
        accent: this.egfrValue !== null && this.egfrValue < 60 ? 'warning' : 'calm',
        detail: `${this.egfrFormulaLabel} classification`
      },
      {
        label: 'Visit timeline',
        value: `${this.patientHistory.length + (this.consultation ? 1 : 0)}`,
        accent: 'sky',
        detail: 'Consultations in chart'
      },
      {
        label: 'Medication lines',
        value: `${this.activeMedicationCount}`,
        accent: this.doseAlerts.length > 0 ? 'warning' : 'calm',
        detail: this.doseAlerts.length > 0 ? 'Dose review recommended' : 'Dosing profile stable'
      }
    ];
  }

  get activeMedicationCount(): number {
    return (this.draft.prescriptions || []).filter((item) => (item.medication || '').trim().length > 0).length;
  }

  get consultationSummaryVm(): ConsultationSummaryViewModel {
    return {
      consultationId: this.consultationId,
      patientName: this.getPatientLabel(this.consultation?.patientId),
      subtitle: this.patientSnapshotSubtitle,
      scheduledAt: this.consultation?.dateTime ?? null,
      statusLabel: this.consultationStatusLabel,
      statusClass: this.statusBadge(this.consultation?.status),
      completenessScore: this.completenessScore,
      completenessLabel: this.completenessLabel,
      completenessClass: this.completenessClass,
      hasUnsavedChanges: this.hasUnsavedChanges,
      renalRiskLabel: this.alerts.length > 0 ? `${this.alerts.length} renal/clinical alert(s)` : 'No urgent renal risk'
    };
  }

  get patientSnapshotVm(): PatientSnapshotViewModel {
    return {
      patientName: this.getPatientLabel(this.consultation?.patientId),
      subtitle: this.patientSnapshotSubtitle,
      allergiesLabel: this.patientAllergiesLabel,
      ageLabel: this.patientAgeLabel,
      sexLabel: this.patientSexLabel,
      growthNarrative: this.growthNarrative,
      timelineSummary: this.patientTimelineSummary,
      activeProblemCount: this.activeProblemCount,
      followUpStatusLabel: this.followUpStatusLabel
    };
  }

  get renalMetricsVm(): RenalMetricsViewModel {
    return {
      egfrLabel: this.egfrValue !== null ? `${this.egfrValue} mL/min/1.73m2` : 'Awaiting calculation',
      egfrFormulaLabel: this.egfrFormulaLabel,
      ckdStage: this.ckdStage,
      bloodPressurePercentileLabel: this.bloodPressurePercentileLabel,
      heightPercentileLabel: this.heightPercentileLabel,
      weightPercentileLabel: this.weightPercentileLabel,
      trajectoryLabel: this.egfrTrajectoryLabel,
      previousEgfrLabel: this.previousEgfr !== null ? `${this.previousEgfr} mL/min/1.73m2` : 'N/A',
      latestDeltaLabel: this.latestEgfrDeltaPct !== null ? `${this.latestEgfrDeltaPct}%` : 'N/A',
      clinicalAiRecommendationLabel: this.aiRecommendationLabel,
      clinicalAiConfidencePercent: this.aiConfidencePercent,
      clinicalAiSummary: this.draft.metrics.aiSummary?.trim() || 'No clinical AI interpretation has been attached to this encounter yet.',
      egfrAiRiskLabel: this.egfrMlRiskLabel,
      egfrAiProbabilityLabel: this.egfrMlProbabilityLabel,
      egfrAiConfidenceLabel: this.egfrMlConfidenceLabel,
      egfrAiPredicted3mLabel: this.egfrMlPredicted3mLabel,
      egfrAiPredicted6mLabel: this.egfrMlPredicted6mLabel,
      egfrAiPredicted12mLabel: this.egfrMlPredicted12mLabel,
      egfrAiHint: this.egfrMlClinicalHint,
      latestLabSourceFileName: this.draft.metrics.aiSourceFileName || '',
      latestLabSourceAvailable: !!this.draft.metrics.aiSourceFileName,
      trendPoints: this.egfrTrendPoints.map((point) => ({ dateTime: point.dateTime, egfr: point.egfr }))
    };
  }

  get clinicalAlertsVm(): ClinicalAlertViewModel[] {
    return this.alerts.map((title) => ({
      title,
      severity: title.toLowerCase().includes('rapid') || title.toLowerCase().includes('hypertension')
        ? 'critical'
        : title.toLowerCase().includes('dose') || title.toLowerCase().includes('adherence')
          ? 'warning'
          : 'info'
    }));
  }

  get dispositionStateVm(): DispositionState {
    return {
      followUpStatusLabel: this.followUpStatusLabel,
      followUpStatusClass: this.followUpStatusClass,
      followUpPreviewDate: this.followUpPreviewDate,
      followUpEnabled: this.doctorFollowUpEnabled,
      followUpQueued: this.doctorFollowUpRequestSent,
      hospitalizationActive: this.adherenceUnlocked || (this.draft.carePlanDoses?.length ?? 0) > 0,
      activeMedicationCount: this.activeMedicationCount
    };
  }

  get medicationReviewVm(): MedicationReviewViewModel {
    return {
      prescriptions: this.draft.prescriptions,
      suggestions: this.medicationSuggestions,
      doseAlerts: this.doseAlerts
    };
  }

  get labOrdersVm(): LabOrdersViewModel {
    return {
      labRequests: this.draft.labRequests,
      submitting: this.labSubmitting,
      selectedCount: this.draft.labRequests.length
    };
  }

  get recentLabHistory(): ClinicalHistoryLabItem[] {
    return this.labHistoryItems.slice(0, 6);
  }

  syncFollowUpDialogPreview(): void {
    this.followUpDecisionDraftVm.previewDate = this.computePreviewDate(
      this.followUpDecisionDraftVm.offsetAmount,
      this.followUpDecisionDraftVm.offsetUnit
    );
  }

  get growthNarrative(): string {
    const parts = [this.heightPercentileLabel, this.weightPercentileLabel].filter((value) => value !== 'N/A');
    if (parts.length === 0) {
      return 'Growth data is not complete yet.';
    }
    return `Height ${this.heightPercentileLabel} and weight ${this.weightPercentileLabel}.`;
  }

  get followUpStatusLabel(): string {
    if (this.doctorFollowUpRequestSent) {
      return 'Queued for receptionist';
    }
    if (this.doctorFollowUpEnabled) {
      return this.followUpPreviewDate ? `Drafted for ${this.followUpPreviewDate}` : 'Draft follow-up requested';
    }
    return 'No follow-up requested';
  }

  get followUpStatusClass(): string {
    if (this.doctorFollowUpRequestSent) return 'status-chip status-chip-success';
    if (this.doctorFollowUpEnabled) return 'status-chip status-chip-warning';
    return 'status-chip status-chip-neutral';
  }

  get patientTimelineSummary(): string {
    if (this.patientHistory.length === 0) {
      return 'First documented consultation in this workspace.';
    }
    const latest = this.patientHistory[0];
    const latestDate = latest?.dateTime ? new Date(latest.dateTime).toLocaleDateString() : 'prior visit';
    return `${this.patientHistory.length} prior consultation(s), latest on ${latestDate}.`;
  }

  get activeProblemCount(): number {
    return (this.draft.diagnosisList || []).filter((item) => (item.label || item.code || '').trim().length > 0).length;
  }

  get abnormalClinicalSignals(): string[] {
    const signals: string[] = [];

    if (this.egfrValue !== null && this.egfrValue < 60) {
      signals.push(`Reduced renal filtration: eGFR ${this.egfrValue} mL/min/1.73m2.`);
    }

    if (this.bloodPressurePercentileLabel !== 'N/A' && !this.bloodPressurePercentileLabel.includes('Expected')) {
      signals.push(`Blood pressure requires review: ${this.bloodPressurePercentileLabel}.`);
    }

    if (this.carePlanMissedCount > 0) {
      signals.push(`${this.carePlanMissedCount} scheduled medication dose(s) were missed.`);
    }

    if (this.doctorFollowUpEnabled && !this.doctorFollowUpRequestSent) {
      signals.push('Follow-up is prepared but not yet sent to the receptionist queue.');
    }

    return signals;
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
    if (typeof this.draft.metrics.egfr === 'number' && Number.isFinite(this.draft.metrics.egfr)) {
      return this.draft.metrics.egfr;
    }
    return this.calculateEgfr(this.draft.metrics.heightCm, this.draft.metrics.creatinineMgDl);
  }

  get ckdStage(): string {
    if (this.draft.metrics.ckdStage) {
      return this.getCkdStageName(this.draft.metrics.ckdStage);
    }
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

  get aiRecommendationLabel(): string {
    const value = (this.draft.metrics.aiRecommendation || '').trim();
    if (!value) return 'No AI analysis yet';
    return value.replace(/_/g, ' ').replace(/\b\w/g, (c: string) => c.toUpperCase());
  }

  get aiConfidencePercent(): string {
    const confidence = Number(this.draft.metrics.aiConfidence);
    if (!Number.isFinite(confidence)) return '-';
    return `${Math.round(confidence * 100)}%`;
  }

  get egfrFormulaLabel(): string {
    const formula = this.draft.metrics.egfrFormulaUsed;
    if (!formula) {
      const age = Number(this.draft.metrics.ageYears);
      return Number.isFinite(age) && age < 18 ? 'Schwartz' : 'CKD-EPI';
    }
    return formula.replace(/_/g, ' ');
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

  /**
   * REQUIRED fields (60 points) — must have to complete:
   * - SOAP notes (any part filled): 20
   * - Diagnosis (at least one): 20
   * - Treatment Plan: 20
   *
   * OPTIONAL fields (40 points) — nice to have:
   * - Guardian Instructions: 10
   * - Prescriptions (if applicable): 10
   * - Lab Requests (if applicable): 10
   * - Follow-up Request: 10
   *
   * Rule: Can complete when REQUIRED ≥ 60 (i.e., all three required fields filled).
   * Completeness percentage shows quality beyond minimum.
   */
  get completenessScore(): number {
    let requiredScore = 0;
    let optionalScore = 0;

    // REQUIRED: SOAP (any part filled)
    const soapFilled = [
      this.draft.soap.subjective,
      this.draft.soap.objective,
      this.draft.soap.assessment,
      this.draft.soap.plan
    ].some(value => (value || '').trim().length > 0);
    if (soapFilled) requiredScore += 20;

    // REQUIRED: Diagnosis (at least one)
    const diagnosisFilled = (this.draft.diagnosisList || []).some(item =>
      (item.label || '').trim().length > 0 || (item.code || '').trim().length > 0
    );
    if (diagnosisFilled) requiredScore += 20;

    // REQUIRED: Treatment Plan
    if ((this.draft.treatmentPlan || '').trim().length > 0) requiredScore += 20;

    // OPTIONAL: Guardian Instructions
    if ((this.draft.guardianInstructions || '').trim().length > 0) optionalScore += 10;

    // OPTIONAL: Prescriptions (only count if provided — not all patients need meds)
    const prescriptionsFilled = (this.draft.prescriptions || []).some(item =>
      (item.medication || '').trim().length > 0
    );
    if (prescriptionsFilled) optionalScore += 10;

    // OPTIONAL: Lab Requests (only count if provided — not all patients need labs)
    const labsFilled = (this.draft.labRequests || []).some(item =>
      (item.test || '').trim().length > 0
    );
    if (labsFilled) optionalScore += 10;

    return requiredScore + optionalScore;
  }

  /** Minimum required score to mark consultation as complete */
  get minimumRequiredScore(): number {
    let required = 0;
    const soapFilled = [
      this.draft.soap.subjective,
      this.draft.soap.objective,
      this.draft.soap.assessment,
      this.draft.soap.plan
    ].some(value => (value || '').trim().length > 0);
    if (soapFilled) required += 20;

    const diagnosisFilled = (this.draft.diagnosisList || []).some(item =>
      (item.label || '').trim().length > 0 || (item.code || '').trim().length > 0
    );
    if (diagnosisFilled) required += 20;

    if ((this.draft.treatmentPlan || '').trim().length > 0) required += 20;

    return required;
  }

  get completenessLabel(): string {
    const score = this.completenessScore;
    if (score >= 90) return 'Complete & Excellent';
    if (score >= 80) return 'Complete & Good';
    if (score >= 70) return 'Complete';
    if (score >= 60) return 'Clinically Ready';
    if (score >= 50) return 'Mostly Ready';
    return 'Incomplete';
  }

  get completenessClass(): string {
    const score = this.completenessScore;
    if (score >= 90) return 'bg-soft-success text-success';
    if (score >= 70) return 'bg-soft-success text-success';
    if (score >= 60) return 'bg-soft-info text-info';
    if (score >= 50) return 'bg-soft-warning text-warning';
    return 'bg-soft-danger text-danger';
  }

  /** Can complete if all REQUIRED fields are filled (SOAP, Diagnosis, Treatment Plan) */
  get canComplete(): boolean {
    const soapFilled = [
      this.draft.soap.subjective,
      this.draft.soap.objective,
      this.draft.soap.assessment,
      this.draft.soap.plan
    ].some(value => (value || '').trim().length > 0);

    const diagnosisFilled = (this.draft.diagnosisList || []).some(item =>
      (item.label || '').trim().length > 0 || (item.code || '').trim().length > 0
    );

    const treatmentPlanFilled = (this.draft.treatmentPlan || '').trim().length > 0;

    return soapFilled && diagnosisFilled && treatmentPlanFilled;
  }

  workflowSectionStatus(section: WorkflowSectionKey): 'Complete' | 'In progress' | 'Pending' {
    if (this.isWorkflowSectionComplete(section)) return 'Complete';
    if (this.isWorkflowSectionStarted(section)) return 'In progress';
    return 'Pending';
  }

  workflowSectionBadgeClass(section: WorkflowSectionKey): string {
    const status = this.workflowSectionStatus(section);
    if (status === 'Complete') return 'bg-soft-success text-success';
    if (status === 'In progress') return 'bg-soft-warning text-warning';
    return 'bg-soft-secondary text-muted';
  }

  get completedWorkflowSections(): number {
    return this.workflowSections.filter((section) => this.isWorkflowSectionComplete(section.key)).length;
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
    const profileName = [this.patientProfile?.firstName, this.patientProfile?.lastName]
      .filter((value) => String(value || '').trim().length > 0)
      .join(' ')
      .trim();
    if (profileName) return profileName;
    if (this.consultation?.patientName && String(this.consultation?.patientId) === String(id)) {
      return this.consultation.patientName;
    }
    const match = (this.history || []).find(item => String(item.patientId) === String(id) && item.patientName);
    if (match?.patientName) return match.patientName;
    return String(id);
  }

  get consultationStatusLabel(): string {
    return this.formatWorkflowStatus(this.consultation?.status);
  }

  get patientSnapshotSubtitle(): string {
    const parts: string[] = [];
    if (this.patientAgeLabel !== '-') parts.push(this.patientAgeLabel);
    if (this.patientSexLabel !== '-') parts.push(this.patientSexLabel);
    if (this.patientProfile?.bloodType) parts.push(`Blood ${this.patientProfile.bloodType}`);
    return parts.join(' • ') || 'Details pending';
  }

  get patientAgeLabel(): string {
    const direct = Number(this.patientProfile?.age ?? this.draft.metrics.ageYears);
    if (Number.isFinite(direct) && direct > 0) {
      return `${direct} year${direct === 1 ? '' : 's'}`;
    }
    const dob = this.patientProfile?.dateOfBirth || this.patientProfile?.birthDate;
    if (!dob) return '-';
    const birth = new Date(dob);
    if (Number.isNaN(birth.getTime())) return '-';
    const now = new Date();
    let age = now.getFullYear() - birth.getFullYear();
    const monthDelta = now.getMonth() - birth.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < birth.getDate())) age--;
    return age >= 0 ? `${age} year${age === 1 ? '' : 's'}` : '-';
  }

  get patientSexLabel(): string {
    const raw = String(this.patientProfile?.gender ?? this.patientProfile?.sex ?? this.draft.metrics.sex ?? '').trim().toUpperCase();
    if (raw === 'M' || raw === 'MALE') return 'Male';
    if (raw === 'F' || raw === 'FEMALE') return 'Female';
    return raw ? raw.charAt(0) + raw.slice(1).toLowerCase() : '-';
  }

  get patientAllergiesLabel(): string {
    const raw = String(this.patientProfile?.allergies ?? '').trim();
    return raw || 'No allergies documented';
  }

  openAiSourceFile(): void {
    if (!this.consultationId || !this.draft.metrics.aiSourceFileName || this.openingAiSource) {
      return;
    }
    this.router.navigate(
      ['/backoffice/consultations', this.consultationId, 'lab-source'],
      { queryParams: { fileName: this.draft.metrics.aiSourceFileName } }
    );
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'CANCELLED') return 'bg-soft-danger text-danger';
    if (status === 'IN_PROGRESS') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  formatWorkflowStatus(status?: string): string {
    const normalized = String(status || 'OPEN').trim().toUpperCase();
    switch (normalized) {
      case 'OPEN':
      case 'SCHEDULED':
        return 'Scheduled';
      case 'IN_PROGRESS':
      case 'CONFIRMED':
        return 'In progress';
      case 'COMPLETED':
        return 'Concluded';
      case 'CANCELLED':
        return 'Cancelled';
      case 'ARCHIVED':
        return 'Archived';
      case 'NO_SHOW':
        return 'No show';
      default:
        return normalized
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase());
    }
  }

  private loadHistory(): void {
    this.api.listMyConsultations().subscribe({
      next: (items) => {
        this.setHistory(items || []);
        this.loadPatientLabHistory();
      },
      error: () => {
        this.history = [];
        this.consultationHistoryCards = [];
        this.labHistoryItems = [];
        this.previousEgfr = null;
        this.previousEgfrDate = null;
      }
    });
  }

  private setHistory(items: any[]): void {
    this.history = items || [];
    this.consultationHistoryCards = (this.patientHistory || [])
      .filter((item) => String(item.id) !== String(this.consultationId))
      .slice(0, 8)
      .map((item) => ({
        id: String(item.id),
        dateTime: String(item.dateTime || ''),
        status: item.status,
        diagnosisSummary: String(item.summary || item.patientName || '').trim()
      }));
    this.computePreviousEgfr();
  }

  private loadConsultationLabRequests(): void {
    if (!this.consultationId) {
      return;
    }
    this.api.getConsultationLabRequests(this.consultationId).subscribe({
      next: (requests) => {
        this.mergeLabRequestsFromBackend(requests || []);
      },
      error: () => {
        // Keep draft state if backend lab request fetch fails.
      }
    });
  }

  private loadPatientLabHistory(): void {
    const patientId = Number(this.consultation?.patientId);
    if (!Number.isFinite(patientId) || patientId <= 0) {
      this.labHistoryItems = [];
      return;
    }
    this.api.getPatientLabRequests(patientId).subscribe({
      next: (requests) => {
        this.labHistoryItems = (requests || [])
          .flatMap((request) => {
            const items = request.testItems?.length
              ? request.testItems.map((item) => ({
                  consultationId: request.consultationId,
                  requestId: request.id,
                  title: item.label,
                  urgency: request.urgency,
                  status: request.status,
                  createdAt: request.createdAt,
                  latestResultFileName: request.latestResultFileName,
                  latestResultUploadedAt: request.latestResultUploadedAt,
                  latestAiSummary: request.latestAiSummary
                }))
              : [{
                  consultationId: request.consultationId,
                  requestId: request.id,
                  title: request.testType,
                  urgency: request.urgency,
                  status: request.status,
                  createdAt: request.createdAt,
                  latestResultFileName: request.latestResultFileName,
                  latestResultUploadedAt: request.latestResultUploadedAt,
                  latestAiSummary: request.latestAiSummary
                }];
            return items;
          })
          .sort((left, right) => new Date(right.latestResultUploadedAt || right.createdAt || 0).getTime() - new Date(left.latestResultUploadedAt || left.createdAt || 0).getTime())
          .slice(0, 12);
      },
      error: () => {
        this.labHistoryItems = [];
      }
    });
  }

  private mergeLabRequestsFromBackend(requests: ClinicalLabRequestResponse[]): void {
    if (!requests.length) {
      return;
    }
    const mergedItems = requests.flatMap((request) => {
      const base = {
        backendRequestId: request.id,
        urgency: this.normalizeUrgencyForDraft(request.urgency),
        note: request.notes || '',
        status: request.status,
        latestAiSummary: request.latestAiSummary,
        latestAiRecommendation: request.latestAiRecommendation,
        latestResultFileName: request.latestResultFileName,
        latestResultUploadedAt: request.latestResultUploadedAt,
        latestResultAvailable: !!request.latestResultAvailable
      };

      if (request.testItems?.length) {
        return request.testItems.map((item) => ({
          ...base,
          key: item.key || this.toItemKey(item.label),
          test: item.label,
          category: '',
          note: item.note || request.notes || '',
          uploadedFileNames: request.latestResultFileName ? [request.latestResultFileName] : []
        }));
      }

      return [{
        ...base,
        key: this.toItemKey(request.testType),
        test: request.testType,
        category: '',
        uploadedFileNames: request.latestResultFileName ? [request.latestResultFileName] : []
      }];
    });

    const currentByKey = new Map((this.draft.labRequests || []).map((item) => [(item.key || this.toItemKey(item.test)), item]));
    this.draft.labRequests = mergedItems.map((item) => {
      const current = currentByKey.get(item.key || this.toItemKey(item.test));
      return {
        ...current,
        ...item,
        uploadedFileNames: item.uploadedFileNames?.length ? item.uploadedFileNames : [...(current?.uploadedFileNames ?? [])]
      };
    });
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

  generateSummary(): void {
    const historyContext = this.patientHistory
      .slice(0, 3)
      .map((item) => `${item.dateTime ? new Date(item.dateTime).toLocaleDateString() : 'Prior visit'} (${this.formatWorkflowStatus(item.status)})`)
      .join('; ');
    const diagnosis = (this.draft.diagnosisList || [])
      .map((item) => [item.label, item.code].filter(Boolean).join(' '))
      .filter(Boolean)
      .join(', ');
    const labs = (this.draft.labRequests || [])
      .map((item) => `${item.test} (${item.urgency})`)
      .join(', ');
    const prescriptions = (this.draft.prescriptions || [])
      .map((item) => [item.medication, item.dosage, item.frequency].filter(Boolean).join(' '))
      .filter(Boolean)
      .join('; ');
    const metricsSummary = [
      Number.isFinite(Number(this.draft.metrics.heightCm)) ? `Height ${this.draft.metrics.heightCm} cm` : '',
      Number.isFinite(Number(this.draft.metrics.weightKg)) ? `Weight ${this.draft.metrics.weightKg} kg` : '',
      Number.isFinite(Number(this.draft.metrics.heartRateBpm)) ? `HR ${this.draft.metrics.heartRateBpm} bpm` : '',
      Number.isFinite(Number(this.draft.metrics.respiratoryRateBpm)) ? `RR ${this.draft.metrics.respiratoryRateBpm}/min` : '',
      Number.isFinite(Number(this.draft.metrics.temperatureC)) ? `Temp ${this.draft.metrics.temperatureC} °C` : '',
      Number.isFinite(Number(this.draft.metrics.oxygenSaturationPct)) ? `SpO2 ${this.draft.metrics.oxygenSaturationPct}%` : '',
      Number.isFinite(Number(this.draft.metrics.creatinineMgDl)) ? `Creatinine ${this.draft.metrics.creatinineMgDl} mg/dL` : '',
      this.egfrValue !== null ? `eGFR ${this.egfrValue} mL/min/1.73m2 (${this.egfrFormulaLabel})` : '',
      this.ckdStage && this.ckdStage !== 'N/A' ? `CKD ${this.ckdStage}` : ''
    ].filter(Boolean).join(' • ');
    const nextStep = this.adherenceUnlocked
      ? 'Hospitalization workflow has been opened for nurse follow-up.'
      : (this.draft.labRequests || []).some((item) => (item.test || '').trim().length > 0)
        ? 'Lab order branch is active; doctor review should continue after results are uploaded.'
        : this.doctorFollowUpEnabled
          ? `Recommended follow-up target: ${this.followUpPreviewDate || 'To be scheduled'}`
          : 'No follow-up branch selected yet.';

    const lines = [
      `Patient: ${this.getPatientLabel(this.consultation?.patientId)}`,
      `Consultation status: ${this.consultationStatusLabel}`,
      historyContext ? `Recent history: ${historyContext}` : '',
      this.draft.soap.subjective ? `Subjective: ${this.draft.soap.subjective.trim()}` : '',
      this.draft.soap.objective ? `Objective: ${this.draft.soap.objective.trim()}` : '',
      this.draft.soap.assessment ? `Assessment: ${this.draft.soap.assessment.trim()}` : '',
      diagnosis ? `Diagnoses: ${diagnosis}` : '',
      metricsSummary ? `Renal snapshot: ${metricsSummary}` : '',
      this.draft.treatmentPlan?.trim() ? `Treatment plan: ${this.draft.treatmentPlan.trim()}` : '',
      prescriptions ? `Prescriptions: ${prescriptions}` : '',
      labs ? `Lab orders: ${labs}` : '',
      nextStep ? `Next step: ${nextStep}` : '',
      this.draft.metrics.aiSummary ? `AI summary: ${this.draft.metrics.aiSummary}` : '',
      this.draft.metrics.aiRecommendation ? `AI recommendation: ${this.aiRecommendationLabel} (${this.aiConfidencePercent})` : ''
    ].filter(Boolean);

    this.generatedSummary = lines.join('\n');
    this.infoMessage = 'Clinical summary generated below. Review it before sharing or copying.';
  }

  private toItemKey(label: string): string {
    return String(label ?? '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  private normalizeUrgencyForDraft(value?: string | null): 'Routine' | 'Urgent' | 'STAT' {
    const normalized = String(value || '').trim().toUpperCase();
    if (normalized === 'STAT') return 'STAT';
    if (normalized === 'URGENT') return 'Urgent';
    return 'Routine';
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
    // Local fallback only. The backend remains the source of truth.
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
      case 'G1':
        return 'text-success';  // Green
      case 'STAGE_1':
      case 'STAGE1':
      case 'G2':
        return 'text-success';  // Green
      case 'STAGE_2':
      case 'STAGE2':
      case 'G3A':
        return 'text-warning';  // Yellow
      case 'STAGE_3':
      case 'STAGE3':
      case 'G3B':
        return 'text-danger';   // Red
      case 'STAGE_4':
      case 'STAGE4':
      case 'G4':
        return 'text-danger';   // Dark red
      case 'STAGE_5':
      case 'STAGE5':
      case 'G5':
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
    const normalized = stage.toUpperCase();
    if (normalized === 'G1') return 'G1 (Normal to High)';
    if (normalized === 'G2') return 'G2 (Mildly Decreased)';
    if (normalized === 'G3A') return 'G3a (Mild to Moderate)';
    if (normalized === 'G3B') return 'G3b (Moderate to Severe)';
    if (normalized === 'G4') return 'G4 (Severely Decreased)';
    if (normalized === 'G5') return 'G5 (Kidney Failure)';
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

  private loadPatientProfile(): void {
    const patientId = Number(this.consultation?.patientId);
    if (!Number.isFinite(patientId) || patientId <= 0) {
      this.patientProfile = null;
      return;
    }

    this.api.getPatient(patientId).subscribe({
      next: (patient) => {
        this.patientProfile = patient;
        this.patchWorkflowPatientProfile();
        this.runEgfrMlPrediction();
      },
      error: () => {
        this.patientProfile = null;
        this.runEgfrMlPrediction();
      }
    });
  }

  get egfrMlRiskLabel(): string {
    return this.egfrMl.classification?.risk_label || 'Not available';
  }

  get egfrMlRiskClass(): string {
    const label = (this.egfrMl.classification?.risk_label || '').toUpperCase();
    if (label === 'HIGH') return 'text-danger';
    if (label === 'LOW') return 'text-success';
    return 'text-secondary';
  }

  get egfrMlProbabilityLabel(): string {
    const value = this.egfrMl.classification?.rapid_decline_probability;
    if (!Number.isFinite(Number(value))) return '-';
    return `${(Number(value) * 100).toFixed(1)}%`;
  }

  get egfrMlConfidenceLabel(): string {
    const value = this.egfrMl.classification?.confidence_score;
    if (!Number.isFinite(Number(value))) return '-';
    return `${(Number(value) * 100).toFixed(1)}%`;
  }

  get egfrMlLowConfidence(): boolean {
    const value = Number(this.egfrMl.classification?.confidence_score);
    return Number.isFinite(value) && value < 0.2;
  }

  get egfrMlGuardrailText(): string {
    if (!this.egfrMlLowConfidence) {
      return '';
    }
    return 'Low confidence: model probability is close to the decision threshold. Recheck labs and clinical context before acting.';
  }

  get egfrMlPredictedEgfrLabel(): string {
    const value = this.egfrMl.regression?.predicted_egfr_12_months;
    if (!Number.isFinite(Number(value))) return '-';
    return `${Number(value).toFixed(1)} mL/min/1.73m2`;
  }

  get egfrMlPredicted3mLabel(): string {
    const value = this.egfrMl.regression?.predicted_egfr_3_months;
    if (!Number.isFinite(Number(value))) return '-';
    return `${Number(value).toFixed(1)} mL/min/1.73m2`;
  }

  get egfrMlPredicted6mLabel(): string {
    const value = this.egfrMl.regression?.predicted_egfr_6_months;
    if (!Number.isFinite(Number(value))) return '-';
    return `${Number(value).toFixed(1)} mL/min/1.73m2`;
  }

  get egfrMlPredicted12mLabel(): string {
    return this.egfrMlPredictedEgfrLabel;
  }

  get egfrMlTrendSummary(): string {
    const p3 = Number(this.egfrMl.regression?.predicted_egfr_3_months);
    const p6 = Number(this.egfrMl.regression?.predicted_egfr_6_months);
    const p12 = Number(this.egfrMl.regression?.predicted_egfr_12_months);
    if (!Number.isFinite(p3) || !Number.isFinite(p6) || !Number.isFinite(p12)) {
      return 'Trend unavailable.';
    }

    if (p12 < p6 && p6 < p3) {
      return 'Projected trajectory is declining over 3 to 12 months.';
    }
    if (p12 > p6 && p6 > p3) {
      return 'Projected trajectory is improving over 3 to 12 months.';
    }
    return 'Projected trajectory is mixed; monitor with repeat labs.';
  }

  get egfrMlClinicalHint(): string {
    const risk = (this.egfrMl.classification?.risk_label || '').toUpperCase();
    const prob = Number(this.egfrMl.classification?.rapid_decline_probability ?? 0);
    const confidence = Number(this.egfrMl.classification?.confidence_score ?? 0);
    const pred12 = Number(this.egfrMl.regression?.predicted_egfr_12_months ?? NaN);

    if (Number.isFinite(confidence) && confidence < 0.2) {
      return 'Prediction is near the threshold; prioritize repeat measurements and trend review before escalation.';
    }

    if (risk === 'HIGH') {
      return `High rapid-decline risk (${(prob * 100).toFixed(1)}%). Prioritize closer follow-up and nephrology reassessment.`;
    }
    if (Number.isFinite(pred12) && pred12 < 45) {
      return '12-month forecast is low; consider tighter monitoring plan even with low decline-risk label.';
    }
    if (risk === 'LOW') {
      return `Low rapid-decline risk (${(prob * 100).toFixed(1)}%). Continue standard follow-up with periodic renal labs.`;
    }
    return 'Use prediction as decision support with clinical judgment and lab trend review.';
  }

  refreshEgfrMlPrediction(): void {
    this.runEgfrMlPrediction(true);
  }

  private runEgfrMlPrediction(force = false): void {
    const payload = this.buildEgfrMlPayload();
    this.egfrMl.payloadPreview = payload;
    this.egfrMl.error = '';

    if (!payload) {
      this.egfrMl.available = false;
      this.egfrMl.regression = null;
      this.egfrMl.classification = null;
      if (force) {
        this.egfrMl.error = 'Missing required inputs: age, height, creatinine, blood pressure, or consultation date.';
      }
      return;
    }

    this.egfrMl.loading = true;
    this.egfrMl.available = true;

    forkJoin({
      regression: this.api.predictEgfrRegression(payload),
      classification: this.api.predictEgfrClassification(payload, 0.5)
    }).subscribe({
      next: ({ regression, classification }) => {
        this.egfrMl.loading = false;
        this.egfrMl.regression = regression;
        this.egfrMl.classification = classification;
      },
      error: () => {
        this.egfrMl.loading = false;
        this.egfrMl.regression = null;
        this.egfrMl.classification = null;
        this.egfrMl.error = 'Model service unavailable. Check egfr-ml-service container and gateway route.';
      }
    });
  }

  private buildEgfrMlPayload(): Record<string, unknown> | null {
    const ageYears = this.resolvePatientAgeYears();
    const heightCm = this.toFiniteNumber(this.draft.metrics.heightCm);
    const weightKg = this.toFiniteNumber(this.draft.metrics.weightKg);
    const creatinineValue = this.toFiniteNumber(this.draft.metrics.creatinineMgDl);
    const systolic = this.toFiniteNumber(this.draft.metrics.systolicBpMmHg);
    const diastolic = this.toFiniteNumber(this.draft.metrics.diastolicBpMmHg);
    const consultationDate = this.consultation?.dateTime ? new Date(this.consultation.dateTime) : null;

    if (
      ageYears === null ||
      heightCm === null ||
      creatinineValue === null ||
      systolic === null ||
      diastolic === null ||
      !consultationDate ||
      Number.isNaN(consultationDate.getTime())
    ) {
      return null;
    }

    const ageMonths = Math.round(ageYears * 12);
    const sexM = this.resolveSexM();
    const monthSinceFirst = this.resolveMonthsSinceFirst(consultationDate);
    const adherenceScore = Math.round(this.carePlanAdherenceRate);
    const previousValues = this.egfrTrendPoints
      .map((p) => p.egfr)
      .filter((v): v is number => v !== null);
    const currentEgfr = this.egfrValue;
    const egfr3 = previousValues.length >= 1 ? previousValues[previousValues.length - 1] : currentEgfr;
    const egfr6 = previousValues.length >= 2 ? previousValues[previousValues.length - 2] : currentEgfr;
    const egfr12 = previousValues.length >= 3 ? previousValues[previousValues.length - 3] : currentEgfr;

    const payload: Record<string, unknown> = {
      Age_Months: ageMonths,
      Age_Years: ageYears,
      Measure_Seq: Math.max(1, this.egfrTrendPoints.length),
      Height_cm: heightCm,
      Weight_kg: weightKg ?? null,
      Creatinine_Value: creatinineValue,
      Urea_Value: null,
      Proteinuria_Value: null,
      Systolic_BP: systolic,
      Diastolic_BP: diastolic,
      Sodium_mmolL: null,
      Potassium_mmolL: null,
      Chloride_mmolL: null,
      Bicarbonate_mmolL: null,
      Hemoglobin_gdL: null,
      Albumin_gdL: null,
      eGFR_Calculated: currentEgfr,
      Months_Since_First: monthSinceFirst,
      Adherence_Score: adherenceScore,
      egfr_3_months: egfr3,
      egfr_6_months: egfr6,
      egfr_12_months: egfr12,
      Measure_Year: consultationDate.getFullYear(),
      Measure_Month: consultationDate.getMonth() + 1,
      Measure_DayOfYear: this.dayOfYear(consultationDate),
      Sex_M: sexM
    };

    return payload;
  }

  private resolvePatientAgeYears(): number | null {
    const direct = this.toFiniteNumber(this.draft.metrics.ageYears);
    if (direct !== null && direct > 0) return direct;
    const fromProfile = this.toFiniteNumber(this.patientProfile?.age);
    if (fromProfile !== null && fromProfile > 0) return fromProfile;
    const dob = this.patientProfile?.dateOfBirth || this.patientProfile?.birthDate;
    if (!dob) return null;
    const birth = new Date(dob);
    if (Number.isNaN(birth.getTime())) return null;
    const now = new Date();
    const years = (now.getTime() - birth.getTime()) / (365.25 * 24 * 60 * 60 * 1000);
    return years > 0 ? Math.round(years * 10) / 10 : null;
  }

  private resolveSexM(): boolean {
    const raw = String(this.patientProfile?.gender ?? this.patientProfile?.sex ?? this.draft.metrics.sex ?? '')
      .trim()
      .toUpperCase();
    return raw === 'M' || raw === 'MALE';
  }

  private resolveMonthsSinceFirst(currentDate: Date): number {
    const dates = this.patientHistory
      .map((item) => new Date(item.dateTime))
      .filter((d) => !Number.isNaN(d.getTime()))
      .sort((a, b) => a.getTime() - b.getTime());
    if (!dates.length) return 0;
    const first = dates[0];
    const months = (currentDate.getFullYear() - first.getFullYear()) * 12
      + (currentDate.getMonth() - first.getMonth());
    return Math.max(0, months);
  }

  private dayOfYear(date: Date): number {
    const start = new Date(date.getFullYear(), 0, 0);
    const diff = date.getTime() - start.getTime();
    const oneDay = 1000 * 60 * 60 * 24;
    return Math.floor(diff / oneDay);
  }

  private toFiniteNumber(value: unknown): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  searchMedicationQuery(index: number, query: string): void {
    const q = (query || '').trim();
    if (q.length < 2) {
      this.medicationSuggestions[index] = [];
      return;
    }
    this.api.searchMedications(q, 15).subscribe({
      next: (list) => {
        this.medicationSuggestions[index] = list || [];
      },
      error: () => {
        this.medicationSuggestions[index] = [];
      }
    });
  }

  private bindWorkflowIntakeForm(): void {
    this.workflowFormSubscription = this.workflowIntakeForm.valueChanges.subscribe((value) => {
      this.draft = {
        ...this.draft,
        metrics: {
          ...this.draft.metrics,
          ageYears: this.toFiniteNumber(value.ageYears) ?? undefined,
          sex: value.sex || undefined,
          heightCm: this.toFiniteNumber(value.heightCm) ?? undefined,
          weightKg: this.toFiniteNumber(value.weightKg) ?? undefined,
          systolicBpMmHg: this.toFiniteNumber(value.systolicBpMmHg) ?? undefined,
          diastolicBpMmHg: this.toFiniteNumber(value.diastolicBpMmHg) ?? undefined,
          heartRateBpm: this.toFiniteNumber(value.heartRateBpm) ?? undefined,
          respiratoryRateBpm: this.toFiniteNumber(value.respiratoryRateBpm) ?? undefined,
          temperatureC: this.toFiniteNumber(value.temperatureC) ?? undefined,
          oxygenSaturationPct: this.toFiniteNumber(value.oxygenSaturationPct) ?? undefined,
          creatinineMgDl: this.toFiniteNumber(value.creatinineMgDl) ?? undefined
        }
      };
      this.runEgfrMlPrediction();
    });
  }

  private syncWorkflowIntakeFormFromDraft(): void {
    this.workflowIntakeForm.patchValue({
      ageYears: this.draft.metrics.ageYears ?? null,
      sex: this.draft.metrics.sex ?? '',
      heightCm: this.draft.metrics.heightCm ?? null,
      weightKg: this.draft.metrics.weightKg ?? null,
      systolicBpMmHg: this.draft.metrics.systolicBpMmHg ?? null,
      diastolicBpMmHg: this.draft.metrics.diastolicBpMmHg ?? null,
      heartRateBpm: this.draft.metrics.heartRateBpm ?? null,
      respiratoryRateBpm: this.draft.metrics.respiratoryRateBpm ?? null,
      temperatureC: this.draft.metrics.temperatureC ?? null,
      oxygenSaturationPct: this.draft.metrics.oxygenSaturationPct ?? null,
      creatinineMgDl: this.draft.metrics.creatinineMgDl ?? null
    }, { emitEvent: false });
  }

  private syncFollowUpDraftVm(): void {
    this.followUpDecisionDraftVm = {
      enabled: this.doctorFollowUpEnabled,
      offsetAmount: this.followUpOffsetAmount,
      offsetUnit: this.followUpOffsetUnit,
      previewDate: this.followUpPreviewDate,
      requestSent: this.doctorFollowUpRequestSent,
      message: this.followUpRequestMessage,
      treatmentPlanReady: !!this.draft.treatmentPlan?.trim(),
      guardianInstructionsReady: !!this.draft.guardianInstructions?.trim()
    };
  }

  private computePreviewDate(offsetAmount: number, offsetUnit: 'DAYS' | 'WEEKS' | 'MONTHS'): string {
    if (!this.consultation?.dateTime) return '';
    const anchor = new Date(this.consultation.dateTime);
    const d = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate());
    const n = Math.max(1, Number(offsetAmount) || 1);
    if (offsetUnit === 'DAYS') {
      d.setDate(d.getDate() + n);
    } else if (offsetUnit === 'WEEKS') {
      d.setDate(d.getDate() + n * 7);
    } else {
      d.setMonth(d.getMonth() + n);
    }
    return d.toLocaleDateString();
  }

  private patchWorkflowPatientProfile(): void {
    if (!this.patientProfile) {
      return;
    }

    const current = this.workflowIntakeForm.getRawValue();
    const fallbackAge = this.toFiniteNumber(this.patientProfile?.age);
    const fallbackSex = String(this.patientProfile?.gender ?? this.patientProfile?.sex ?? '').trim();

    if (this.draft.metrics.ageYears === undefined && fallbackAge !== null) {
      this.draft.metrics.ageYears = fallbackAge;
    }
    if (!this.draft.metrics.sex && fallbackSex) {
      this.draft.metrics.sex = fallbackSex;
    }

    this.workflowIntakeForm.patchValue({
      ageYears: current.ageYears ?? fallbackAge,
      sex: current.sex || fallbackSex
    }, { emitEvent: false });
  }

  private hasFiniteMetric(value: unknown): boolean {
    return this.toFiniteNumber(value) !== null;
  }

  private isWorkflowSectionComplete(section: WorkflowSectionKey): boolean {
    switch (section) {
      case 'patient-context':
        return !!this.consultationId && this.getPatientLabel(this.consultation?.patientId) !== '-';
      case 'vitals':
        return this.hasFiniteMetric(this.draft.metrics.heightCm)
          && this.hasFiniteMetric(this.draft.metrics.weightKg)
          && this.hasFiniteMetric(this.draft.metrics.systolicBpMmHg)
          && this.hasFiniteMetric(this.draft.metrics.diastolicBpMmHg)
          && this.hasFiniteMetric(this.draft.metrics.heartRateBpm)
          && this.hasFiniteMetric(this.draft.metrics.respiratoryRateBpm)
          && this.hasFiniteMetric(this.draft.metrics.temperatureC)
          && this.hasFiniteMetric(this.draft.metrics.oxygenSaturationPct);
      case 'nephrology':
        return this.hasFiniteMetric(this.draft.metrics.creatinineMgDl)
          && this.egfrValue !== null
          && !!this.ckdStage
          && this.ckdStage !== 'N/A';
      case 'hospitalization':
        return this.adherenceUnlocked && (this.draft.carePlanDoses?.length ?? 0) > 0;
      case 'discharge':
        return !!this.draft.treatmentPlan?.trim()
          && !!this.draft.guardianInstructions?.trim()
          && (this.draft.prescriptions || []).some((item) => (item.medication || '').trim().length > 0);
      case 'alerts':
        return this.hasFiniteMetric(this.draft.metrics.creatinineMgDl)
          && this.hasFiniteMetric(this.draft.metrics.systolicBpMmHg)
          && this.hasFiniteMetric(this.draft.metrics.diastolicBpMmHg);
      case 'review':
        return this.canComplete;
    }
  }

  private isWorkflowSectionStarted(section: WorkflowSectionKey): boolean {
    switch (section) {
      case 'patient-context':
        return !!this.consultation || !!this.patientProfile;
      case 'vitals':
        return this.hasFiniteMetric(this.draft.metrics.heightCm)
          || this.hasFiniteMetric(this.draft.metrics.weightKg)
          || this.hasFiniteMetric(this.draft.metrics.systolicBpMmHg)
          || this.hasFiniteMetric(this.draft.metrics.diastolicBpMmHg)
          || this.hasFiniteMetric(this.draft.metrics.heartRateBpm)
          || this.hasFiniteMetric(this.draft.metrics.respiratoryRateBpm)
          || this.hasFiniteMetric(this.draft.metrics.temperatureC)
          || this.hasFiniteMetric(this.draft.metrics.oxygenSaturationPct);
      case 'nephrology':
        return this.hasFiniteMetric(this.draft.metrics.creatinineMgDl)
          || this.egfrValue !== null
          || (!!this.ckdStage && this.ckdStage !== 'N/A');
      case 'hospitalization':
        return this.adherenceUnlocked || (this.draft.carePlanDoses?.length ?? 0) > 0;
      case 'discharge':
        return !!this.draft.treatmentPlan?.trim()
          || !!this.draft.guardianInstructions?.trim()
          || (this.draft.prescriptions || []).some((item) => (item.medication || '').trim().length > 0);
      case 'alerts':
        return this.alerts.length > 0 || this.hasFiniteMetric(this.draft.metrics.creatinineMgDl);
      case 'review':
        return this.completenessScore > 0 || !!this.generatedSummary;
    }
  }
}
