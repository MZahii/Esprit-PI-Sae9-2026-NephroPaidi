import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  CareTask,
  Complication,
  PostOpObservation,
  PreOpAssessment,
  ProcedureApiService,
  SurgicalCase
} from '../../../core/services/procedure-api.service';

@Component({
  selector: 'app-procedure-surgical-advanced',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procedure-surgical-advanced.html',
  styleUrl: './procedure-surgical-advanced.scss'
})
export class ProcedureSurgicalAdvancedComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';
  partialLoadWarnings: string[] = [];

  surgicalCases: SurgicalCase[] = [];
  preOps: PreOpAssessment[] = [];
  postOps: PostOpObservation[] = [];
  complications: Complication[] = [];
  careTasks: CareTask[] = [];

  selectedCaseId = '';
  editingComplicationId: number | null = null;
  editingCareTaskId: number | null = null;

  preOpForm = {
    hemodynamicsOk: false,
    infectionScreenOk: false,
    anesthesiaClearanceOk: false,
    consentSigned: false,
    note: ''
  };

  postOpForm = {
    hemodynamicsStable: false,
    bleedingControlled: false,
    painControlled: false,
    consciousnessNormal: false,
    note: ''
  };

  complicationForm = {
    description: ''
  };

  careTaskForm = {
    title: '',
    done: false
  };

  constructor(
    private procedureApi: ProcedureApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  get selectedCase(): SurgicalCase | undefined {
    const id = Number(this.selectedCaseId);
    return this.surgicalCases.find((c) => c.id === id);
  }

  get preOpEligible(): boolean {
    return this.preOpForm.hemodynamicsOk
      && this.preOpForm.infectionScreenOk
      && this.preOpForm.anesthesiaClearanceOk
      && this.preOpForm.consentSigned;
  }

  get preOpDecisionLabel(): string {
    return this.preOpEligible ? 'ELIGIBLE FOR INTERVENTION' : 'BLOCKED - PRE-OP NOT VALIDATED';
  }

  get postOpStable(): boolean {
    return this.postOpForm.hemodynamicsStable
      && this.postOpForm.bleedingControlled
      && this.postOpForm.painControlled
      && this.postOpForm.consciousnessNormal;
  }

  get postOpDecisionLabel(): string {
    return this.postOpStable ? 'PATIENT STABLE' : 'PATIENT NOT STABLE';
  }

  get preOpHistoryForSelectedCase(): PreOpAssessment[] {
    const id = Number(this.selectedCaseId);
    return this.preOps.filter((p) => p.surgicalCaseId === id);
  }

  get postOpHistoryForSelectedCase(): PostOpObservation[] {
    const id = Number(this.selectedCaseId);
    return this.postOps.filter((p) => p.surgicalCaseId === id);
  }

  get complicationsForSelectedCase(): Complication[] {
    const id = Number(this.selectedCaseId);
    return this.complications.filter((item) => item.surgicalCaseId === id);
  }

  get careTasksForSelectedCase(): CareTask[] {
    const id = Number(this.selectedCaseId);
    return this.careTasks.filter((item) => item.surgicalCaseId === id);
  }

  parseRecord(notes: string | null | undefined): Record<string, string> {
    const parsed: Record<string, string> = {};
    const raw = (notes ?? '').trim();
    if (!raw.includes(';') || !raw.includes('=')) {
      parsed['note'] = raw;
      return parsed;
    }

    for (const part of raw.split(';')) {
      const index = part.indexOf('=');
      if (index <= 0) continue;
      const key = part.slice(0, index).trim();
      const value = part.slice(index + 1).trim();
      parsed[key] = value;
    }

    return parsed;
  }

  yesNo(value: string | undefined): string {
    return value === 'true' ? 'Yes' : 'No';
  }

  loadAll(): void {
    this.loading = true;
    this.errorMessage = '';
    this.partialLoadWarnings = [];

    this.procedureApi.getSurgicalCases().subscribe({
      next: (cases) => {
        this.surgicalCases = cases ?? [];
        this.syncSelectedCase();
        this.loadWorkflowData();
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatCasesLoadError(err);
        this.refreshView();
      }
    });
  }

  onSelectedCaseChange(value: string): void {
    this.selectedCaseId = value;
    this.errorMessage = '';
    this.successMessage = '';
    this.partialLoadWarnings = [];
    this.cancelComplicationEdit();
    this.cancelCareTaskEdit();
    this.refreshView();
  }

  loadWorkflowData(): void {
    const warnings: string[] = [];

    forkJoin({
      preOps: this.procedureApi.getPreOpAssessments().pipe(
        catchError((err: { error?: { message?: string }; message?: string }) => {
          warnings.push(this.describePartialLoadFailure('pre-op history', err));
          return of([]);
        })
      ),
      postOps: this.procedureApi.getPostOpObservations().pipe(
        catchError((err: { error?: { message?: string }; message?: string }) => {
          warnings.push(this.describePartialLoadFailure('post-op history', err));
          return of([]);
        })
      ),
      complications: this.procedureApi.getComplications().pipe(
        catchError((err: { error?: { message?: string }; message?: string }) => {
          warnings.push(this.describePartialLoadFailure('complications', err));
          return of([]);
        })
      ),
      careTasks: this.procedureApi.getCareTasks().pipe(
        catchError((err: { error?: { message?: string }; message?: string }) => {
          warnings.push(this.describePartialLoadFailure('care tasks', err));
          return of([]);
        })
      )
    }).subscribe({
      next: ({ preOps, postOps, complications, careTasks }) => {
        this.preOps = preOps ?? [];
        this.postOps = postOps ?? [];
        this.complications = complications ?? [];
        this.careTasks = careTasks ?? [];
        this.partialLoadWarnings = warnings;
        this.loading = false;
        this.refreshView();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to load surgical workflow.';
        this.refreshView();
      }
    });
  }

  startComplicationEdit(item: Complication): void {
    this.editingComplicationId = item.id;
    this.complicationForm.description = item.description ?? '';
    this.errorMessage = '';
    this.successMessage = '';
    this.refreshView();
  }

  cancelComplicationEdit(): void {
    this.editingComplicationId = null;
    this.complicationForm.description = '';
    this.refreshView();
  }

  startCareTaskEdit(item: CareTask): void {
    this.editingCareTaskId = item.id;
    this.careTaskForm.title = item.title ?? '';
    this.careTaskForm.done = item.done;
    this.errorMessage = '';
    this.successMessage = '';
    this.refreshView();
  }

  cancelCareTaskEdit(): void {
    this.editingCareTaskId = null;
    this.careTaskForm.title = '';
    this.careTaskForm.done = false;
    this.refreshView();
  }

  submitComplication(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    const description = this.complicationForm.description.trim();

    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    if (description.length < 5) {
      this.errorMessage = 'Complication description must contain at least 5 characters.';
      this.refreshView();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request$ = this.editingComplicationId
      ? this.procedureApi.updateComplication(this.editingComplicationId, { description })
      : this.procedureApi.createComplication({ surgicalCaseId, description });

    request$.subscribe({
      next: () => {
        this.successMessage = this.editingComplicationId
          ? 'Complication updated successfully.'
          : 'Complication recorded successfully.';
        this.saving = false;
        this.editingComplicationId = null;
        this.complicationForm.description = '';
        this.refreshView();
        this.loadAll();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to save complication.';
        this.refreshView();
      }
    });
  }

  submitCareTask(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    const title = this.careTaskForm.title.trim();

    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    if (title.length < 3) {
      this.errorMessage = 'Care task title must contain at least 3 characters.';
      this.refreshView();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request$ = this.editingCareTaskId
      ? this.procedureApi.updateCareTask(this.editingCareTaskId, {
          title,
          done: this.careTaskForm.done
        })
      : this.procedureApi.createCareTask({ surgicalCaseId, title });

    request$.subscribe({
      next: () => {
        this.successMessage = this.editingCareTaskId
          ? 'Care task updated successfully.'
          : 'Care task created successfully.';
        this.saving = false;
        this.cancelCareTaskEdit();
        this.refreshView();
        this.loadAll();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to save care task.';
        this.refreshView();
      }
    });
  }

  toggleCareTaskDone(item: CareTask): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateCareTask(item.id, {
      title: item.title,
      done: !item.done
    }).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = item.done
          ? 'Care task reopened successfully.'
          : 'Care task marked as done.';
        if (this.editingCareTaskId === item.id) {
          this.cancelCareTaskEdit();
        }
        this.refreshView();
        this.loadAll();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to update care task.';
        this.refreshView();
      }
    });
  }

  submitPreOp(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    const note = this.preOpForm.note.trim();
    const formattedNotes =
      `hemodynamicsOk=${this.preOpForm.hemodynamicsOk};` +
      `infectionScreenOk=${this.preOpForm.infectionScreenOk};` +
      `anesthesiaClearanceOk=${this.preOpForm.anesthesiaClearanceOk};` +
      `consentSigned=${this.preOpForm.consentSigned};` +
      `decision=${this.preOpDecisionLabel};` +
      `note=${note}`;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.createPreOpAssessment({ surgicalCaseId, notes: formattedNotes }).subscribe({
      next: () => {
        this.successMessage = `Pre-Op submitted. Decision: ${this.preOpDecisionLabel}. Case status updated automatically.`;
        this.saving = false;
        this.refreshView();
        this.loadAll();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to submit pre-op.';
        this.refreshView();
      }
    });
  }

  submitPostOp(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    const note = this.postOpForm.note.trim();
    const formattedNotes =
      `hemodynamicsStable=${this.postOpForm.hemodynamicsStable};` +
      `bleedingControlled=${this.postOpForm.bleedingControlled};` +
      `painControlled=${this.postOpForm.painControlled};` +
      `consciousnessNormal=${this.postOpForm.consciousnessNormal};` +
      `decision=${this.postOpDecisionLabel};` +
      `note=${note}`;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.createPostOpObservation({ surgicalCaseId, notes: formattedNotes }).subscribe({
      next: () => {
        this.successMessage = `Post-Op submitted. Decision: ${this.postOpDecisionLabel}. Care tasks may be auto-generated if unstable.`;
        this.saving = false;
        this.refreshView();
        this.loadAll();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to submit post-op.';
        this.refreshView();
      }
    });
  }

  private refreshView(): void {
    this.cdr.markForCheck();
  }

  private syncSelectedCase(): void {
    if (this.surgicalCases.length === 0) {
      this.selectedCaseId = '';
      return;
    }

    const selectedId = Number(this.selectedCaseId);
    const stillExists = this.surgicalCases.some((item) => item.id === selectedId);
    if (!this.selectedCaseId || Number.isNaN(selectedId) || !stillExists) {
      this.selectedCaseId = String(this.surgicalCases[0].id);
    }
  }

  private formatCasesLoadError(err: { error?: { message?: string }; message?: string }): string {
    const message = err?.error?.message || err?.message || '';
    if (message.includes('503') || message.includes('Service Unavailable')) {
      return 'Procedure service is temporarily unavailable. Verify procedure-service and the API Gateway, then retry.';
    }
    return message || 'Failed to load surgical cases.';
  }

  private describePartialLoadFailure(
    section: string,
    err: { error?: { message?: string }; message?: string }
  ): string {
    const message = err?.error?.message || err?.message || 'Temporary error.';
    if (message.includes('503') || message.includes('Service Unavailable')) {
      return `Unable to load ${section}: procedure-service is unavailable.`;
    }
    return `Unable to load ${section}: ${message}`;
  }
}
