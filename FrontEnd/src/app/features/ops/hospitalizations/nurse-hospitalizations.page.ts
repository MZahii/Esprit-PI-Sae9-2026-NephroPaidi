import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
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
import { catchError, of } from 'rxjs';

interface TaskDraftState {
  status: HospitalizationTaskStatus;
  note: string;
  numericValue: number | null;
  textValue: string;
  unit: string;
}

@Component({
  selector: 'app-nurse-hospitalizations-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nurse-hospitalizations.page.html',
  styleUrl: './nurse-hospitalizations.page.scss'
})
export class NurseHospitalizationsPage implements OnInit {
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

  taskStatuses: HospitalizationTaskStatus[] = ['PENDING', 'DONE', 'NOT_DONE'];

  constructor(
    private opsApi: OpsApiService,
    private drugSafety: DrugSafetyService
  ) {}

  ngOnInit(): void {
    this.loadActiveHospitalizations();
  }

  loadActiveHospitalizations(): void {
    this.loading = true;
    this.error = '';
    this.opsApi.listActiveHospitalizationsForNurse().subscribe({
      next: (items) => {
        this.summaries = items || [];
        this.loading = false;
        if (this.summaries.length > 0) {
          this.openHospitalization(this.summaries[0].id);
        } else {
          this.selectedHospitalization = null;
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load active hospitalization cases.';
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
      error: () => {
        this.detailError = 'Unable to load hospitalization details.';
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
      unit: draft.unit
    }).subscribe({
      next: () => {
        this.savingTaskId = '';
        if (this.selectedHospitalization?.id) {
          this.openHospitalization(this.selectedHospitalization.id);
        }
        this.loadActiveHospitalizations();
      },
      error: () => {
        this.savingTaskId = '';
        this.detailError = 'Unable to save task update.';
      }
    });
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
      unit: task.latestUnit || task.expectedUnit || ''
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
}
