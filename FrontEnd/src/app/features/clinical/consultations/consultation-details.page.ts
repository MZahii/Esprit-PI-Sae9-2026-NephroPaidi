import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { ConsultationWorkspaceService, ConsultationWorkspaceDraft } from './consultation-workspace.service';
import { ConsultationMedicalDossier } from '../models/clinical.models';

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
  medicalDossier: ConsultationMedicalDossier | null = null;
  returnUrl: string | null = null;

  loading = false;
  dossierLoading = false;
  error = '';
  dossierError = '';

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
        this.loadMedicalDossier();
        this.loadDraft();
      },
      error: () => {
        this.api.listMyConsultations().subscribe({
          next: (items) => {
            this.consultation = (items || []).find(c => c.id === this.consultationId) || null;
            if (this.consultation) {
              this.loadMedicalDossier();
            }
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

  private loadMedicalDossier(): void {
    this.dossierLoading = true;
    this.dossierError = '';
    this.api.getConsultationMedicalDossier(this.consultationId).subscribe({
      next: (dossier) => {
        this.medicalDossier = dossier;
        this.dossierLoading = false;
      },
      error: () => {
        this.medicalDossier = null;
        this.dossierLoading = false;
        this.dossierError = 'Unable to load the medical dossier timeline.';
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
    if (status === 'IN_PROGRESS' || status === 'CONFIRMED') return 'bg-soft-primary text-primary';
    return 'bg-soft-warning text-warning';
  }

  statusLabel(status?: string): string {
    const normalized = String(status || 'OPEN').trim().toUpperCase();
    switch (normalized) {
      case 'OPEN':
      case 'SCHEDULED':
        return 'Scheduled';
      case 'IN_PROGRESS':
      case 'CONFIRMED':
        return 'In progress';
      case 'COMPLETED':
        return 'Concluded';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return normalized
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase());
    }
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

  get hasMedicalDossier(): boolean {
    return !!(this.medicalDossier && (
      (this.medicalDossier.consultations?.length ?? 0) > 0 ||
      (this.medicalDossier.timeline?.length ?? 0) > 0
    ));
  }

  dossierBadgeClass(kind?: string): string {
    const normalized = String(kind || '').trim().toUpperCase();
    if (normalized === 'CONSULTATION') return 'bg-soft-primary text-primary';
    if (normalized === 'LAB_REQUEST') return 'bg-soft-info text-info';
    if (normalized === 'PRESCRIPTION') return 'bg-soft-success text-success';
    return 'bg-soft-warning text-warning';
  }

  dossierKindLabel(kind?: string): string {
    const normalized = String(kind || '').trim().toUpperCase();
    switch (normalized) {
      case 'CONSULTATION': return 'Consultation';
      case 'LAB_REQUEST': return 'Lab request';
      case 'PRESCRIPTION': return 'Prescription';
      case 'DISCHARGE': return 'Discharge';
      default:
        return String(kind || 'Event')
          .toLowerCase()
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase());
    }
  }

  consultationBadgeClass(status?: string): string {
    return this.statusBadge(status);
  }

  formatSections(value?: string | null): string[] {
    return (value ?? '')
      .split(/\s*\|\s*/)
      .map((part) => part.trim())
      .filter((part) => part.length > 0);
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
