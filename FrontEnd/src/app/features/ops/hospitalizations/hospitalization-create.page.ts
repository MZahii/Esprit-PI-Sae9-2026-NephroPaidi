import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  CreateHospitalizationPayload,
  CreateHospitalizationTaskPayload,
  HospitalizationMeasurementKind,
  HospitalizationTaskType,
  OpsApiService
} from '../../../core/services/ops-api.service';

type TaskDraft = CreateHospitalizationTaskPayload & { localId: number };

@Component({
  selector: 'app-hospitalization-create-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './hospitalization-create.page.html',
  styleUrl: './hospitalization-create.page.scss'
})
export class HospitalizationCreatePage implements OnInit {
  loading = false;
  saving = false;
  error = '';
  success = '';

  consultationId = '';
  patientId: number | null = null;

  form = {
    reason: ''
  };

  taskCounter = 0;
  taskTypeOptions: HospitalizationTaskType[] = [
    'WEIGHT_CHECK',
    'MEDICATION',
    'TEMPERATURE',
    'BLOOD_MONITORING',
    'PATIENT_MONITORING',
    'CUSTOM'
  ];
  measurementKindOptions: HospitalizationMeasurementKind[] = ['NONE', 'NUMERIC', 'TEXT'];

  tasks: TaskDraft[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private opsApi: OpsApiService
  ) {}

  ngOnInit(): void {
    this.consultationId = this.route.snapshot.queryParamMap.get('consultationId') || '';
    const patientIdParam = this.route.snapshot.queryParamMap.get('patientId');
    this.patientId = patientIdParam ? Number(patientIdParam) : null;
    this.addTask({
      type: 'PATIENT_MONITORING',
      title: 'Monitor patient condition',
      instructions: 'Observe the patient during the shift and record notable changes.',
      measurementKind: 'TEXT'
    });
  }

  addTask(partial?: Partial<CreateHospitalizationTaskPayload>): void {
    this.tasks = [
      ...this.tasks,
      {
        localId: ++this.taskCounter,
        type: partial?.type || 'CUSTOM',
        title: partial?.title || '',
        instructions: partial?.instructions || '',
        measurementKind: partial?.measurementKind || 'NONE',
        expectedUnit: partial?.expectedUnit || '',
        displayOrder: this.tasks.length
      }
    ];
  }

  removeTask(localId: number): void {
    this.tasks = this.tasks
      .filter((task) => task.localId !== localId)
      .map((task, index) => ({ ...task, displayOrder: index }));
  }

  submit(): void {
    this.error = '';
    this.success = '';

    if (!this.patientId || this.patientId <= 0) {
      this.error = 'Patient id is required.';
      return;
    }

    const normalizedTasks = this.tasks
      .filter((task) => (task.title || '').trim().length > 0)
      .map((task, index) => ({
        type: task.type,
        title: task.title.trim(),
        instructions: (task.instructions || '').trim(),
        measurementKind: task.measurementKind || 'NONE',
        expectedUnit: (task.expectedUnit || '').trim(),
        displayOrder: index
      }));

    const payload: CreateHospitalizationPayload = {
      patientId: this.patientId,
      consultationId: this.consultationId || undefined,
      reason: this.form.reason.trim(),
      tasks: normalizedTasks
    };

    if (!payload.reason) {
      this.error = 'Hospitalization reason is required.';
      return;
    }

    this.saving = true;
    this.opsApi.createHospitalization(payload).subscribe({
      next: (created) => {
        this.saving = false;
        this.success = 'Hospitalization workflow created.';
        this.router.navigate(['/backoffice/hospitalizations', created.id]);
      },
      error: () => {
        this.saving = false;
        this.error = 'Unable to create hospitalization workflow.';
      }
    });
  }
}
