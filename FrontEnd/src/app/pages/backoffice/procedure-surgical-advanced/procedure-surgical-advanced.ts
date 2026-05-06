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
  SurgicalPrediction,
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
  predictions: SurgicalPrediction[] = [];

  selectedCaseId = '';
  editingComplicationId: number | null = null;
  editingCareTaskId: number | null = null;
  predictionRunningPhase: 'PRE_OP' | 'POST_OP' | '' = '';

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

  get preOpPredictionsForSelectedCase(): SurgicalPrediction[] {
    const id = Number(this.selectedCaseId);
    return this.predictions.filter((item) => item.surgicalCaseId === id && item.phase === 'PRE_OP');
  }

  get postOpPredictionsForSelectedCase(): SurgicalPrediction[] {
    const id = Number(this.selectedCaseId);
    return this.predictions.filter((item) => item.surgicalCaseId === id && item.phase === 'POST_OP');
  }

  get latestPreOpPrediction(): SurgicalPrediction | undefined {
    return this.preOpPredictionsForSelectedCase[0];
  }

  get latestPostOpPrediction(): SurgicalPrediction | undefined {
    return this.postOpPredictionsForSelectedCase[0];
  }

  predictionScore(prediction: SurgicalPrediction | undefined): number {
    return Math.round((prediction?.probability ?? 0) * 100);
  }

  predictionTone(prediction: SurgicalPrediction | undefined): 'high' | 'medium' | 'low' {
    const risk = String(prediction?.riskLevel ?? '').toUpperCase();
    if (risk === 'HIGH') {
      return 'high';
    }
    if (risk === 'MEDIUM') {
      return 'medium';
    }
    return 'low';
  }

  predictionSummary(prediction: SurgicalPrediction | undefined, phase: 'PRE_OP' | 'POST_OP'): string {
    if (!prediction) {
      return phase === 'PRE_OP'
        ? 'Run the model to get a pre-operative risk reading for this surgical case.'
        : 'Run the model to estimate post-operative complication risk for this surgical case.';
    }

    const label = String(prediction.predictionLabel ?? '').replaceAll('_', ' ').trim();
    if (phase === 'PRE_OP') {
      return `${label} detected before intervention. Use this score to validate readiness and escalation level.`;
    }
    return `${label} detected after intervention. Use this score to guide monitoring and complication response.`;
  }

  predictionExplanations(prediction: SurgicalPrediction | undefined): Array<{ key: string; value: number }> {
    const parsed = this.parsePredictionPayload(prediction?.outputJson);
    const explanations = parsed?.['explanations'];
    if (!explanations || typeof explanations !== 'object') {
      return [];
    }

    return Object.entries(explanations)
      .map(([key, value]) => ({ key: this.humanizeExplanationKey(key), value: Number(value ?? 0) }))
      .filter((item) => !Number.isNaN(item.value))
      .sort((left, right) => right.value - left.value)
      .slice(0, 5);
  }

  predictionGeneratedAt(prediction: SurgicalPrediction | undefined): string {
    if (!prediction?.createdAt) {
      return '-';
    }
    const parsed = new Date(prediction.createdAt);
    return Number.isNaN(parsed.getTime()) ? String(prediction.createdAt) : parsed.toLocaleString();
  }

  predictionHistory(phase: 'PRE_OP' | 'POST_OP'): SurgicalPrediction[] {
    return phase === 'PRE_OP' ? this.preOpPredictionsForSelectedCase : this.postOpPredictionsForSelectedCase;
  }

  clinicalInterpretation(prediction: SurgicalPrediction | undefined, phase: 'PRE_OP' | 'POST_OP'): string {
    if (!prediction) {
      return phase === 'PRE_OP'
        ? 'No pre-operative interpretation yet. Run the model after validating the pre-op criteria.'
        : 'No post-operative interpretation yet. Run the model after recording the immediate recovery state.';
    }

    const risk = String(prediction.riskLevel ?? '').toUpperCase();
    if (phase === 'PRE_OP') {
      if (risk === 'HIGH') {
        return 'The patient should be considered high-risk for intervention and requires reinforced surgical review before proceeding.';
      }
      if (risk === 'MEDIUM') {
        return 'The patient presents a moderate pre-operative risk profile and should proceed only after targeted clinical review.';
      }
      return 'The patient currently presents a low pre-operative risk signal based on the available structured inputs.';
    }

    if (risk === 'HIGH') {
      return 'The post-operative pattern suggests a significant complication signal and supports escalation in recovery monitoring.';
    }
    if (risk === 'MEDIUM') {
      return 'The post-operative pattern suggests a moderate complication risk and supports closer observation with reassessment.';
    }
    return 'The post-operative pattern currently suggests a stable recovery signal under the available documented observations.';
  }

  suggestedWorkflowStatus(prediction: SurgicalPrediction | undefined, phase: 'PRE_OP' | 'POST_OP'): string {
    if (!prediction) {
      return 'Awaiting ML run';
    }

    const risk = String(prediction.riskLevel ?? '').toUpperCase();
    if (phase === 'PRE_OP') {
      if (risk === 'HIGH') return 'Hold intervention';
      if (risk === 'MEDIUM') return 'Needs clinical review';
      return 'Proceed with caution';
    }

    if (risk === 'HIGH') return 'Escalate monitoring';
    if (risk === 'MEDIUM') return 'Reassess in recovery';
    return 'Continue standard follow-up';
  }

  modelDisplayName(prediction: SurgicalPrediction | undefined, phase: 'PRE_OP' | 'POST_OP'): string {
    if (prediction?.modelName) {
      if (phase === 'PRE_OP') {
        return 'Pre-Op Risk Model';
      }
      return 'Post-Op Complication Model';
    }
    return phase === 'PRE_OP' ? 'Pre-Op Risk Model' : 'Post-Op Complication Model';
  }

  modelTechnique(prediction: SurgicalPrediction | undefined, phase: 'PRE_OP' | 'POST_OP'): string {
    const version = String(prediction?.modelVersion ?? '').toLowerCase();
    if (version.includes('preop')) {
      return 'XGBoost pipeline';
    }
    if (version.includes('postop')) {
      return 'Decision tree pipeline';
    }
    return phase === 'PRE_OP' ? 'Structured pre-op inference' : 'Structured post-op inference';
  }

  decisionSupportStrength(prediction: SurgicalPrediction | undefined): string {
    if (!prediction) {
      return 'Not evaluated';
    }
    const risk = String(prediction.riskLevel ?? '').toUpperCase();
    if (risk === 'HIGH') return 'Strong signal';
    if (risk === 'MEDIUM') return 'Moderate signal';
    return 'Low signal';
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
    this.loadPredictionsForSelectedCase();
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
        this.loadPredictionsForSelectedCase();
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

  runPreOpPrediction(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    this.predictionRunningPhase = 'PRE_OP';
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.runPreOpPrediction(surgicalCaseId).subscribe({
      next: (prediction) => {
        this.predictionRunningPhase = '';
        this.upsertPrediction(prediction);
        this.successMessage = 'Pre-op ML prediction generated.';
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.predictionRunningPhase = '';
        this.errorMessage = err?.error?.message || err?.message || 'Unable to generate pre-op prediction.';
        this.refreshView();
      }
    });
  }

  runPostOpPrediction(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.errorMessage = 'Please select a surgical case.';
      this.refreshView();
      return;
    }

    this.predictionRunningPhase = 'POST_OP';
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.runPostOpPrediction(surgicalCaseId).subscribe({
      next: (prediction) => {
        this.predictionRunningPhase = '';
        this.upsertPrediction(prediction);
        this.successMessage = 'Post-op ML prediction generated.';
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.predictionRunningPhase = '';
        this.errorMessage = err?.error?.message || err?.message || 'Unable to generate post-op prediction.';
        this.refreshView();
      }
    });
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

  private loadPredictionsForSelectedCase(): void {
    const surgicalCaseId = Number(this.selectedCaseId);
    if (!surgicalCaseId || Number.isNaN(surgicalCaseId)) {
      this.predictions = [];
      this.refreshView();
      return;
    }

    this.procedureApi.getSurgicalPredictions(surgicalCaseId).subscribe({
      next: (predictions) => {
        this.predictions = predictions ?? [];
        this.refreshView();
      },
      error: () => {
        this.predictions = [];
        this.refreshView();
      }
    });
  }

  private upsertPrediction(prediction: SurgicalPrediction): void {
    this.predictions = [prediction, ...this.predictions.filter((item) => item.id !== prediction.id)]
      .sort((left, right) => String(right.createdAt).localeCompare(String(left.createdAt)));
  }

  private parsePredictionPayload(raw: string | null | undefined): Record<string, unknown> | null {
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private humanizeExplanationKey(key: string): string {
    const normalized = String(key ?? '').replace(/^keyword:/, '').replaceAll('_', ' ').trim();
    if (!normalized) {
      return 'Signal';
    }
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
  }
}
