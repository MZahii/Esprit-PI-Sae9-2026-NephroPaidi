import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  DialysisPlan,
  DialysisOutcome,
  DialysisSession,
  ProcedureApiService
} from '../../../core/services/procedure-api.service';

@Component({
  selector: 'app-procedure-dialysis-outcomes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procedure-dialysis-outcomes.html',
  styleUrl: './procedure-dialysis-outcomes.scss'
})
export class ProcedureDialysisOutcomesComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  sessions: DialysisSession[] = [];
  plans: DialysisPlan[] = [];
  outcomes: DialysisOutcome[] = [];

  selectedSessionId = '';
  searchTerm = '';

  editValidated: Record<number, boolean> = {};
  editSummary: Record<number, string> = {};

  constructor(
    private procedureApi: ProcedureApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  get filteredOutcomes(): DialysisOutcome[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.outcomes.filter((outcome) => {
      if (!term) {
        return true;
      }
      return this.getSessionDisplay(outcome.sessionId).toLowerCase().includes(term);
    });
  }

  getSessionDisplay(sessionId: number): string {
    const session = this.sessions.find((s) => s.id === sessionId);
    if (!session) {
      return 'Unknown session';
    }

    const plan = this.plans.find((p) => p.id === session.planId);
    const patientLabel = plan ? `${plan.firstName || '-'} ${plan.lastName || ''}`.trim() : 'Unknown patient';
    return `${patientLabel} | ${session.sessionDate}`;
  }

  loadInitialData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.procedureApi.getDialysisSessions().subscribe({
      next: (sessions) => {
        this.sessions = sessions ?? [];
        if (!this.selectedSessionId && this.sessions.length > 0) {
          this.selectedSessionId = String(this.sessions[0].id);
        }
        this.procedureApi.getDialysisPlans().subscribe({
          next: (plans) => {
            this.plans = plans ?? [];
            this.loadOutcomes();
            this.refreshView();
          },
          error: (err: { error?: { message?: string }; message?: string }) => {
            this.loading = false;
            this.errorMessage = this.formatApiError(err, 'Failed to load dialysis plans.');
            this.refreshView();
          }
        });
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load dialysis sessions.');
        this.refreshView();
      }
    });
  }

  loadOutcomes(): void {
    this.procedureApi.getDialysisOutcomes().subscribe({
      next: (outcomes) => {
        this.outcomes = outcomes ?? [];
        this.editValidated = {};
        this.editSummary = {};

        for (const outcome of this.outcomes) {
          this.editValidated[outcome.id] = outcome.validated;
          this.editSummary[outcome.id] = outcome.summary ?? '';
        }

        this.loading = false;
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load dialysis outcomes.');
        this.refreshView();
      }
    });
  }

  createOutcome(): void {
    const sessionId = Number(this.selectedSessionId);

    if (!sessionId || Number.isNaN(sessionId)) {
      this.errorMessage = 'Session is required.';
      this.refreshView();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.createDialysisOutcomeForSession(sessionId).subscribe({
      next: () => {
        this.successMessage = 'Dialysis outcome created successfully.';
        this.saving = false;
        this.refreshView();
        this.loadOutcomes();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = this.formatApiError(err, 'Failed to create dialysis outcome.');
        this.refreshView();
      }
    });
  }

  validateOutcome(outcome: DialysisOutcome): void {
    const validated = this.editValidated[outcome.id] ?? false;
    const summary = (this.editSummary[outcome.id] ?? '').trim();

    if (validated && summary.length < 5) {
      this.errorMessage = 'Validated outcomes require a summary of at least 5 characters.';
      this.refreshView();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.validateDialysisOutcome(outcome.id, { validated, summary }).subscribe({
      next: () => {
        this.successMessage = 'Dialysis outcome updated successfully.';
        this.refreshView();
        this.loadOutcomes();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to validate dialysis outcome.');
        this.refreshView();
      }
    });
  }

  private refreshView(): void {
    this.cdr.markForCheck();
  }

  private formatApiError(
    err: { error?: { message?: string }; message?: string },
    fallback: string
  ): string {
    const message = err?.error?.message || err?.message || '';
    if (message.includes('503') || message.includes('Service Unavailable')) {
      return 'Procedure service is temporarily unavailable. Verify procedure-service and the API Gateway, then retry.';
    }
    return message || fallback;
  }
}
