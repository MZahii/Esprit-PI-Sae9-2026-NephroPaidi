import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  HospitalizationCaseDto,
  HospitalizationSummaryDto,
  HospitalizationTaskDto,
  HospitalizationTaskStatus,
  OpsApiService
} from '../../../core/services/ops-api.service';
import { DrugSafetyService, DrugSafetySignal } from '../../../core/services/drug-safety.service';
import { Subscription, catchError, interval, of } from 'rxjs';

interface TaskDraftState {
  status: HospitalizationTaskStatus;
  note: string;
  numericValue: number | null;
  textValue: string;
  unit: string;
}

type ObservationOption = {
  value: string;
  label: string;
};

type TaskUiConfig = {
  label: string;
  unit?: string;
  observationOptions?: ObservationOption[];
};

const TASK_UI_CONFIG: Record<string, TaskUiConfig> = {
  WEIGHT_CHECK: {
    label: 'Weight check',
    unit: 'kg'
  },
  MEDICATION: {
    label: 'Medication administration',
    observationOptions: [
      { value: 'Administered', label: 'Administered' },
      { value: 'Delayed', label: 'Delayed' },
      { value: 'Refused', label: 'Refused by patient / guardian' },
      { value: 'Held', label: 'Held / not given' }
    ]
  },
  TEMPERATURE: {
    label: 'Temperature watch',
    unit: '°C'
  },
  BLOOD_MONITORING: {
    label: 'Lab follow-up',
    observationOptions: [
      { value: 'Sample sent', label: 'Sample sent' },
      { value: 'Result received', label: 'Result received' },
      { value: 'Abnormal result flagged', label: 'Abnormal result flagged' },
      { value: 'Awaiting result', label: 'Awaiting result' }
    ]
  },
  PATIENT_MONITORING: {
    label: 'Clinical monitoring',
    observationOptions: [
      { value: 'Stable', label: 'Stable' },
      { value: 'Improving', label: 'Improving' },
      { value: 'Needs review', label: 'Needs doctor review' },
      { value: 'Worsening', label: 'Worsening' }
    ]
  },
  CUSTOM: {
    label: 'Custom handoff task'
  }
};

@Component({
  selector: 'app-nurse-hospitalizations-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nurse-hospitalizations.page.html',
  styleUrl: './nurse-hospitalizations.page.scss'
})
export class NurseHospitalizationsPage implements OnInit, OnDestroy {
  loading = false;
  savingTaskId = '';
  error = '';
  detailError = '';
  safetyError = '';
  summaries: HospitalizationSummaryDto[] = [];
  selectedHospitalization: HospitalizationCaseDto | null = null;
  taskDrafts: Record<string, TaskDraftState> = {};
  safetySignal: DrugSafetySignal | null = null;
  loadingSafety = false;
  private refreshSub?: Subscription;

  constructor(
    private opsApi: OpsApiService,
    private drugSafety: DrugSafetyService
  ) {}

  ngOnInit(): void {
    this.loadActiveHospitalizations();
    this.refreshSub = interval(15000).subscribe(() => {
      this.loadActiveHospitalizations();
      if (this.selectedHospitalization?.id) {
        this.openHospitalization(this.selectedHospitalization.id);
      }
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  refreshNow(): void {
    this.loadActiveHospitalizations();
    if (this.selectedHospitalization?.id) {
      this.openHospitalization(this.selectedHospitalization.id);
    }
  }

  loadActiveHospitalizations(): void {
    this.loading = true;
    this.error = '';
    this.opsApi.listActiveHospitalizationsForNurse().subscribe({
      next: (items) => {
        this.summaries = items || [];
        this.loading = false;
        const selectedStillExists = this.selectedHospitalization?.id
          ? this.summaries.some((item) => item.id === this.selectedHospitalization?.id)
          : false;

        if (!selectedStillExists && this.summaries.length > 0) {
          this.openHospitalization(this.summaries[0].id);
        } else if (!this.summaries.length) {
          this.selectedHospitalization = null;
        }
      },
      error: () => {
        this.loading = false;
        this.error = this.timeoutMessage('Unable to load active hospitalization cases.');
      }
    });
  }

  openHospitalization(id: string): void {
    this.detailError = '';
    this.opsApi.getHospitalization(id).subscribe({
      next: (item) => {
        this.selectedHospitalization = item;
        this.taskDrafts = {};
        this.safetyError = '';
        this.safetySignal = null;
        this.loadingSafety = false;
        (item.tasks || []).forEach((task) => {
          this.taskDrafts[task.id] = this.createDraft(task);
        });
        this.loadSafetyMetric(item);
      },
      error: (err) => {
        this.detailError = this.timeoutMessage('Unable to load hospitalization details.', err);
      }
    });
  }

  submitTask(task: HospitalizationTaskDto): void {
    const draft = this.taskDrafts[task.id];
    if (!draft) return;

    this.savingTaskId = task.id;
    this.opsApi.updateTask(task.id, {
      status: draft.status,
      note: draft.note,
      numericValue: draft.numericValue,
      textValue: draft.textValue,
      unit: this.resolvedUnit(task) || draft.unit
    }).subscribe({
      next: () => {
        this.savingTaskId = '';
        if (this.selectedHospitalization?.id) {
          this.openHospitalization(this.selectedHospitalization.id);
        }
        this.loadActiveHospitalizations();
      },
      error: (err) => {
        this.savingTaskId = '';
        this.detailError = this.timeoutMessage('Unable to save task update.', err);
      }
    });
  }

  get activeCasesCount(): number {
    return this.summaries.length;
  }

  get completedTasksCount(): number {
    return this.summaries.reduce((sum, item) => sum + Number(item.completedTasks ?? 0), 0);
  }

  get pendingTasksCount(): number {
    return this.summaries.reduce((sum, item) => sum + Number(item.pendingTasks ?? 0), 0);
  }

  get selectedCompletionPercent(): number {
    if (!this.selectedHospitalization?.tasks?.length) {
      return 0;
    }
    const done = this.selectedHospitalization.tasks.filter((task) => task.status === 'DONE').length;
    return Math.round((done / this.selectedHospitalization.tasks.length) * 100);
  }

  get selectedPendingTasksCount(): number {
    if (!this.selectedHospitalization?.tasks?.length) {
      return 0;
    }
    return this.selectedHospitalization.tasks.filter((task) => task.status !== 'DONE').length;
  }

  get selectedDoneTasksCount(): number {
    if (!this.selectedHospitalization?.tasks?.length) {
      return 0;
    }
    return this.selectedHospitalization.tasks.filter((task) => task.status === 'DONE').length;
  }

  taskTypeLabel(type: string): string {
    return TASK_UI_CONFIG[type || 'CUSTOM']?.label
      || (type || 'CUSTOM')
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (char) => char.toUpperCase());
  }

  taskStatusClass(status: HospitalizationTaskStatus): string {
    switch (status) {
      case 'DONE':
        return 'done';
      case 'NOT_DONE':
        return 'not-done';
      default:
        return 'pending';
    }
  }

  taskStatusLabel(status: HospitalizationTaskStatus): string {
    switch (status) {
      case 'DONE':
        return 'Completed';
      case 'NOT_DONE':
        return 'Could not complete';
      default:
        return 'Pending';
    }
  }

  taskStatusOptions(): Array<{ value: HospitalizationTaskStatus; label: string }> {
    return [
      { value: 'PENDING', label: 'Pending' },
      { value: 'DONE', label: 'Completed' },
      { value: 'NOT_DONE', label: 'Could not complete' }
    ];
  }

  resolvedUnit(task: HospitalizationTaskDto): string {
    return TASK_UI_CONFIG[task.type]?.unit || task.expectedUnit || '';
  }

  observationOptions(task: HospitalizationTaskDto): ObservationOption[] {
    return TASK_UI_CONFIG[task.type]?.observationOptions || [];
  }

  shouldUseObservationDropdown(task: HospitalizationTaskDto): boolean {
    return task.measurementKind === 'TEXT' && this.observationOptions(task).length > 0;
  }

  taskValueLabel(task: HospitalizationTaskDto): string {
    if (task.measurementKind === 'NUMERIC') {
      return `Measured value${this.resolvedUnit(task) ? ` (${this.resolvedUnit(task)})` : ''}`;
    }
    return 'Observed value';
  }

  trackSummary(_: number, item: HospitalizationSummaryDto): string {
    return item.id;
  }

  trackTask(_: number, item: HospitalizationTaskDto): string {
    return item.id;
  }

  private createDraft(task: HospitalizationTaskDto): TaskDraftState {
    return {
      status: task.status || 'PENDING',
      note: task.latestNote || '',
      numericValue: task.latestNumericValue ?? null,
      textValue: task.latestTextValue || '',
      unit: task.latestUnit || this.resolvedUnit(task)
    };
  }

  private loadSafetyMetric(item: HospitalizationCaseDto): void {
    const medicationTask = (item.tasks || []).find((task) => task.type === 'MEDICATION');
    const candidate = this.extractDrugCandidate(medicationTask?.title ?? '');

    if (!candidate) {
      return;
    }

    this.loadingSafety = true;
    this.drugSafety.getSignal(candidate).pipe(
      catchError(() => {
        this.safetyError = 'Medication safety insight unavailable right now.';
        return of(null);
      })
    ).subscribe((signal) => {
      this.safetySignal = signal;
      this.loadingSafety = false;
    });
  }

  private extractDrugCandidate(title: string): string {
    const cleaned = title.trim();
    if (!cleaned) return '';

    const token = cleaned.split(/\s+/)[0];
    return token.replace(/[^a-zA-Z0-9-]/g, '');
  }

  private timeoutMessage(defaultMessage: string, err?: unknown): string {
    const maybeTimeout = (err as { name?: string; message?: string } | undefined);
    if (maybeTimeout?.name === 'TimeoutError' || (maybeTimeout?.message ?? '').includes('Timeout has occurred')) {
      return 'Request timed out after 12 seconds. Please retry or check that ops-service is running.';
    }
    return defaultMessage;
  }
}
