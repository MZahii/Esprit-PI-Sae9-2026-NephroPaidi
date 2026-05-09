import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  DialysisPlan,
  DialysisSession,
  ProcedureApiService
} from '../../../core/services/procedure-api.service';

@Component({
  selector: 'app-procedure-dialysis-sessions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procedure-dialysis-sessions.html',
  styleUrl: './procedure-dialysis-sessions.scss'
})
export class ProcedureDialysisSessionsComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  plans: DialysisPlan[] = [];
  sessions: DialysisSession[] = [];

  searchTerm = '';
  selectedPlanId = '';

  constructor(
    private procedureApi: ProcedureApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  get filteredSessions(): DialysisSession[] {
    const planTerm = this.searchTerm.trim().toLowerCase();

    return this.sessions.filter((session) => {
      if (!planTerm) {
        return true;
      }
      const label = this.getPlanDisplay(session.planId).toLowerCase();
      return label.includes(planTerm);
    });
  }

  getPlanDisplay(planId: number): string {
    const plan = this.plans.find((p) => p.id === planId);
    if (!plan) {
      return 'Unknown patient';
    }

    return `${plan.firstName || '-'} ${plan.lastName || ''}`.trim();
  }

  loadInitialData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.procedureApi.getDialysisPlans().subscribe({
      next: (plans) => {
        this.plans = plans ?? [];
        if (!this.selectedPlanId && this.plans.length > 0) {
          this.selectedPlanId = String(this.plans[0].id);
        }
        this.loadSessions();
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load dialysis plans.');
        this.refreshView();
      }
    });
  }

  loadSessions(): void {
    this.procedureApi.getDialysisSessions().subscribe({
      next: (sessions) => {
        this.sessions = sessions ?? [];
        this.loading = false;
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load dialysis sessions.');
        this.refreshView();
      }
    });
  }

  get selectedPlan(): DialysisPlan | undefined {
    const id = Number(this.selectedPlanId);
    return this.plans.find((p) => p.id === id);
  }

  generateSessionsFromPlan(): void {
    const planId = Number(this.selectedPlanId);
    if (!planId || Number.isNaN(planId)) {
      this.errorMessage = 'Please select a dialysis plan.';
      this.refreshView();
      return;
    }

    if (this.selectedPlan && ['CANCELLED', 'ARCHIVED'].includes(this.selectedPlan.status)) {
      this.errorMessage = 'Sessions can only be generated for active dialysis plans.';
      this.refreshView();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.generateDialysisSessionsFromPlan(planId).subscribe({
      next: () => {
        this.successMessage = 'Dialysis sessions generated from plan successfully.';
        this.saving = false;
        this.refreshView();
        this.loadSessions();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = this.formatApiError(err, 'Failed to generate sessions from plan.');
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
