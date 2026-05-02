import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { ConsultationWorkspaceService, ConsultationWorkspaceDraft } from './consultation-workspace.service';

@Component({
  selector: 'app-consultation-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './consultation-details.page.html',
  styleUrl: './consultation-details.page.scss'
})
export class ConsultationDetailsPage implements OnInit {
  consultationId = '';
  consultation: any | null = null;
  draft: ConsultationWorkspaceDraft | null = null;
  returnUrl: string | null = null;

  loading = false;
  error = '';

  // Expose Number for template usage
  readonly Number = Number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ClinicalApiService,
    private workspace: ConsultationWorkspaceService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Consultation id missing.';
      return;
    }
    this.consultationId = id;
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || null;
    this.loadConsultation();
  }

  loadConsultation(): void {
    this.loading = true;
    this.error = '';
    this.api.getConsultation(this.consultationId).subscribe({
      next: (item) => {
        this.consultation = item;
        this.loadDraft();
      },
      error: () => {
        this.api.listMyConsultations().subscribe({
          next: (items) => {
            this.consultation = (items || []).find(c => c.id === this.consultationId) || null;
            this.loadDraft();
            if (!this.consultation) {
              this.error = 'Consultation not found.';
              this.loading = false;
            }
          },
          error: () => {
            this.loading = false;
            this.error = 'Failed to load consultation details.';
          }
        });
      }
    });
  }

  private loadDraft(): void {
    this.workspace.getDraft(this.consultationId).subscribe({
      next: (draftData) => {
        this.draft = draftData;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        // Draft may not exist yet; that's fine for details view
      }
    });
  }

  getPatientLabel(id: number | string | undefined): string {
    if (!id) return '-';
    if (this.consultation?.patientName && String(this.consultation?.patientId) === String(id)) {
      return this.consultation.patientName;
    }
    return String(id);
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'CANCELLED') return 'bg-soft-danger text-danger';
    if (status === 'IN_PROGRESS') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  goBack(): void {
    if (this.returnUrl) {
      this.router.navigateByUrl(this.returnUrl);
    } else {
      this.router.navigate(['/backoffice/consultations']);
    }
  }

  get hasDraftData(): boolean {
    return !!(this.draft && (
      (this.draft.soap?.subjective && this.draft.soap.subjective.trim()) ||
      (this.draft.soap?.objective && this.draft.soap.objective.trim()) ||
      (this.draft.soap?.assessment && this.draft.soap.assessment.trim()) ||
      (this.draft.soap?.plan && this.draft.soap.plan.trim())
    ));
  }

  get hasDiagnosis(): boolean {
    return !!(this.draft?.diagnosisList && this.draft.diagnosisList.length > 0);
  }

  get hasTreatmentPlan(): boolean {
    return !!(this.draft && (
      (this.draft.treatmentPlan && this.draft.treatmentPlan.trim()) ||
      (this.draft.guardianInstructions && this.draft.guardianInstructions.trim())
    ));
  }

  get hasMetrics(): boolean {
    return !!(this.draft?.metrics && (
      Number.isFinite(this.draft.metrics.heightCm) ||
      Number.isFinite(this.draft.metrics.weightKg) ||
      Number.isFinite(this.draft.metrics.creatinineMgDl) ||
      Number.isFinite(this.draft.metrics.ageYears)
    ));
  }

  formatDiagnosis(): string {
    if (!this.draft?.diagnosisList || this.draft.diagnosisList.length === 0) {
      return 'No diagnoses recorded.';
    }
    return this.draft.diagnosisList
      .map(d => `${d.label}${d.code ? ` (${d.code})` : ''}${d.severity ? ` - ${d.severity}` : ''}`)
      .join('; ');
  }

}
