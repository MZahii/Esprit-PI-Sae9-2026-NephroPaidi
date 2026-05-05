import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import {
  HospitalizationCaseResponse,
  HospitalizationMeasurementKind,
  HospitalizationStatus,
  HospitalizationTaskRequest,
  HospitalizationTaskResponse,
  HospitalizationTaskStatus,
  HospitalizationTaskType,
  HospitalizationTaskUpdateRequest
} from '../../../core/models/ops.models';
import { OpsApiService } from '../../../core/services/ops-api.service';

type TaskExecutionDraft = {
  status: HospitalizationTaskStatus;
  note: string;
  numericValue: string;
  textValue: string;
  unit: string;
};

@Component({
  selector: 'app-ops-dossier-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './ops-dossier-detail.page.html',
  styleUrl: './ops-dossier-detail.page.scss'
})
export class OpsDossierDetailPage implements OnInit {
  hospitalizationId = '';
  returnUrl: string | null = null;

  loading = false;
  error = '';
  message = '';

  hospitalization: HospitalizationCaseResponse | null = null;
  tasks: HospitalizationTaskResponse[] = [];

  readonly taskTypes: HospitalizationTaskType[] = [
    'WEIGHT_CHECK',
    'MEDICATION',
    'TEMPERATURE',
    'BLOOD_MONITORING',
    'PATIENT_MONITORING',
    'CUSTOM'
  ];

  readonly measurementKinds: HospitalizationMeasurementKind[] = ['NONE', 'NUMERIC', 'TEXT'];
  readonly taskStatuses: HospitalizationTaskStatus[] = ['DONE', 'NOT_DONE'];

  taskDraft: HospitalizationTaskRequest = {
    type: 'CUSTOM',
    title: '',
    instructions: '',
    measurementKind: 'NONE',
    expectedUnit: '',
    displayOrder: 0
  };

  taskExecutionDrafts: Record<string, TaskExecutionDraft> = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private opsApi: OpsApiService,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Hospitalization id missing.';
      return;
    }

    this.hospitalizationId = id;
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.loadHospitalization();
  }

  loadHospitalization(): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.opsApi.getHospitalizationById(this.hospitalizationId).subscribe({
      next: (res) => {
        this.hospitalization = res;
        this.tasks = res.tasks ?? [];
        this.prepareTaskDrafts();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Unable to load hospitalization.';
      }
    });
  }

  addTask(): void {
    this.message = '';
    this.error = '';

    const title = (this.taskDraft.title || '').trim();
    if (!title) {
      this.error = 'Task title is required.';
      return;
    }

    this.opsApi.addHospitalizationTask(this.hospitalizationId, {
      type: this.taskDraft.type,
      title,
      instructions: (this.taskDraft.instructions || '').trim() || null,
      measurementKind: this.taskDraft.measurementKind,
      expectedUnit: (this.taskDraft.expectedUnit || '').trim() || null,
      displayOrder: Number.isFinite(Number(this.taskDraft.displayOrder)) ? Number(this.taskDraft.displayOrder) : null
    }).subscribe({
      next: () => {
        this.message = 'Task added to the hospitalization case.';
        this.taskDraft = {
          type: 'CUSTOM',
          title: '',
          instructions: '',
          measurementKind: 'NONE',
          expectedUnit: '',
          displayOrder: this.tasks.length + 1
        };
        this.loadHospitalization();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to add task.';
      }
    });
  }

  updateTask(task: HospitalizationTaskResponse): void {
    this.message = '';
    this.error = '';

    const draft = this.taskExecutionDrafts[task.id];
    if (!draft) {
      this.error = 'Task form is not available.';
      return;
    }

    const numericValue = draft.numericValue.trim().length > 0 ? Number(draft.numericValue) : null;
    if (task.measurementKind === 'NUMERIC' && !Number.isFinite(numericValue ?? NaN)) {
      this.error = 'This task requires a numeric value.';
      return;
    }

    if (task.measurementKind === 'TEXT' && !draft.textValue.trim()) {
      this.error = 'This task requires a text value.';
      return;
    }

    const payload: HospitalizationTaskUpdateRequest = {
      status: draft.status,
      note: draft.note.trim() || null,
      numericValue: Number.isFinite(numericValue ?? NaN) ? numericValue : null,
      textValue: draft.textValue.trim() || null,
      unit: draft.unit.trim() || null
    };

    this.opsApi.updateHospitalizationTask(task.id, payload).subscribe({
      next: () => {
        this.message = 'Task updated.';
        this.loadHospitalization();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update task.';
      }
    });
  }

  goBack(): void {
    if (this.returnUrl) {
      this.router.navigateByUrl(this.returnUrl);
      return;
    }

    if (this.isNurse) {
      this.router.navigate(['/backoffice/ops/my-dossiers']);
      return;
    }

    this.router.navigate(['/backoffice/ops/dossiers']);
  }

  prepareTaskDrafts(): void {
    const nextDrafts: Record<string, TaskExecutionDraft> = {};
    for (const task of this.tasks) {
      nextDrafts[task.id] = this.taskExecutionDrafts[task.id] ?? {
        status: 'DONE',
        note: '',
        numericValue: '',
        textValue: '',
        unit: task.expectedUnit || ''
      };
    }
    this.taskExecutionDrafts = nextDrafts;
  }

  taskForm(task: HospitalizationTaskResponse): TaskExecutionDraft {
    if (!this.taskExecutionDrafts[task.id]) {
      this.taskExecutionDrafts[task.id] = {
        status: 'DONE',
        note: '',
        numericValue: '',
        textValue: '',
        unit: task.expectedUnit || ''
      };
    }
    return this.taskExecutionDrafts[task.id];
  }

  get currentRole(): string {
    return String(this.authStorage.getRole() ?? '');
  }

  get isNurse(): boolean {
    return this.currentRole === 'NURSE';
  }

  get isDoctor(): boolean {
    return this.currentRole === 'DOCTOR';
  }

  get canAddTasks(): boolean {
    return false;
  }

  get canUpdateTasks(): boolean {
    return false;
  }

  statusClass(status?: HospitalizationStatus | string): string {
    if (status === 'CANCELLED') return 'bg-soft-secondary text-muted';
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'ACTIVE') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  taskStatusClass(status?: HospitalizationTaskStatus | string): string {
    if (status === 'DONE') return 'bg-soft-success text-success';
    if (status === 'NOT_DONE') return 'bg-soft-danger text-danger';
    return 'bg-soft-warning text-warning';
  }
}
