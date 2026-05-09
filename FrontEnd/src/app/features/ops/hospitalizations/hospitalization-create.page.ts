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
type TaskTemplate = {
  label: string;
  type: HospitalizationTaskType;
  title: string;
  instructions: string;
  measurementKind: HospitalizationMeasurementKind;
  expectedUnit?: string;
};

type TaskDefinition = {
  label: string;
  defaultTitle: string;
  defaultInstructions: string;
  measurementKind: HospitalizationMeasurementKind;
  expectedUnit?: string;
};

const TASK_DEFINITIONS: Record<HospitalizationTaskType, TaskDefinition> = {
  WEIGHT_CHECK: {
    label: 'Weight check',
    defaultTitle: 'Record current weight',
    defaultInstructions: 'Measure and record the patient weight during the shift and alert the team to rapid change.',
    measurementKind: 'NUMERIC',
    expectedUnit: 'kg'
  },
  MEDICATION: {
    label: 'Medication administration',
    defaultTitle: 'Administer prescribed medication',
    defaultInstructions: 'Confirm medication administration or document why the dose was not given.',
    measurementKind: 'TEXT'
  },
  TEMPERATURE: {
    label: 'Temperature watch',
    defaultTitle: 'Record body temperature',
    defaultInstructions: 'Check temperature according to the nursing schedule and escalate persistent fever or instability.',
    measurementKind: 'NUMERIC',
    expectedUnit: '°C'
  },
  BLOOD_MONITORING: {
    label: 'Lab follow-up',
    defaultTitle: 'Follow blood test status',
    defaultInstructions: 'Track ordered blood work and document important updates for the clinical team.',
    measurementKind: 'TEXT'
  },
  PATIENT_MONITORING: {
    label: 'Clinical monitoring',
    defaultTitle: 'Observe patient clinical condition',
    defaultInstructions: 'Observe the patient and document any improvement, deterioration, or new concern.',
    measurementKind: 'TEXT'
  },
  CUSTOM: {
    label: 'Custom handoff task',
    defaultTitle: '',
    defaultInstructions: '',
    measurementKind: 'TEXT'
  }
};

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
  taskTemplates: TaskTemplate[] = [
    {
      label: 'Vitals monitoring',
      type: 'PATIENT_MONITORING',
      title: 'Monitor vital signs and clinical status',
      instructions: 'Observe the patient during the shift and document any significant change that should be escalated.',
      measurementKind: 'TEXT'
    },
    {
      label: 'Medication administration',
      type: 'MEDICATION',
      title: 'Administer prescribed medication safely',
      instructions: 'Follow the prescription plan, confirm administration times, and report any intolerance or missed dose.',
      measurementKind: 'TEXT'
    },
    {
      label: 'Fluid balance / urine output',
      type: 'PATIENT_MONITORING',
      title: 'Monitor hydration and urine output',
      instructions: 'Track intake/output, observe edema or dehydration signs, and alert the team if values are concerning.',
      measurementKind: 'TEXT'
    },
    {
      label: 'Temperature watch',
      type: 'TEMPERATURE',
      title: 'Watch for fever or temperature instability',
      instructions: 'Check temperature according to the nursing schedule and report persistent fever or abrupt change.',
      measurementKind: 'NUMERIC',
      expectedUnit: '°C'
    },
    {
      label: 'Lab follow-up',
      type: 'BLOOD_MONITORING',
      title: 'Follow pending lab-related monitoring',
      instructions: 'Ensure ordered tests are followed up and notify the doctor if results suggest clinical deterioration.',
      measurementKind: 'TEXT'
    }
  ];

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
    this.addTaskFromTemplate(this.taskTemplates[0]);
  }

  addTask(partial?: Partial<CreateHospitalizationTaskPayload>): void {
    const type = partial?.type || 'CUSTOM';
    const definition = TASK_DEFINITIONS[type];
    this.tasks = [
      ...this.tasks,
      {
        localId: ++this.taskCounter,
        type,
        title: partial?.title || definition.defaultTitle,
        instructions: partial?.instructions || definition.defaultInstructions,
        measurementKind: partial?.measurementKind || definition.measurementKind,
        expectedUnit: partial?.expectedUnit || definition.expectedUnit || '',
        displayOrder: this.tasks.length
      }
    ];
  }

  addTaskFromTemplate(template: TaskTemplate): void {
    this.addTask({
      type: template.type,
      title: template.title,
      instructions: template.instructions
    });
  }

  removeTask(localId: number): void {
    this.tasks = this.tasks
      .filter((task) => task.localId !== localId)
      .map((task, index) => ({ ...task, displayOrder: index }));
  }

  taskTypeLabel(type: HospitalizationTaskType): string {
    return TASK_DEFINITIONS[type]?.label || type.replace(/_/g, ' ');
  }

  taskTypeHint(task: TaskDraft): string {
    const definition = TASK_DEFINITIONS[task.type];
    if (!definition) return '';
    const valueMode = definition.measurementKind === 'NUMERIC'
      ? `Nurse records a numeric value${definition.expectedUnit ? ` in ${definition.expectedUnit}` : ''}.`
      : 'Nurse records an observed status and note.';
    return valueMode;
  }

  onTaskTypeChange(task: TaskDraft): void {
    const definition = TASK_DEFINITIONS[task.type];
    if (!definition) return;
    task.measurementKind = definition.measurementKind;
    task.expectedUnit = definition.expectedUnit || '';
    if (!task.title?.trim()) {
      task.title = definition.defaultTitle;
    }
    if (!task.instructions?.trim()) {
      task.instructions = definition.defaultInstructions;
    }
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
      .map((task, index) => {
        const definition = TASK_DEFINITIONS[task.type];
        return {
          type: task.type,
          title: task.title.trim(),
          instructions: (task.instructions || '').trim(),
          measurementKind: definition.measurementKind,
          expectedUnit: definition.expectedUnit || '',
          displayOrder: index
        };
      });

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
