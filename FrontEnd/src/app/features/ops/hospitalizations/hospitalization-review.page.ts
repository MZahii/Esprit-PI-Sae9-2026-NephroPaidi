import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HospitalizationCaseDto, OpsApiService } from '../../../core/services/ops-api.service';
import { DrugSafetyService, DrugSafetySignal } from '../../../core/services/drug-safety.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-hospitalization-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './hospitalization-review.page.html',
  styleUrl: './hospitalization-review.page.scss'
})
export class HospitalizationReviewPage implements OnInit {
  hospitalization: HospitalizationCaseDto | null = null;
  loading = false;
  error = '';
  safetyError = '';
  loadingSafety = false;
  safetySignal: DrugSafetySignal | null = null;

  constructor(
    private route: ActivatedRoute,
    private opsApi: OpsApiService,
    private drugSafety: DrugSafetyService
  ) {}

  ngOnInit(): void {
    const hospitalizationId = this.route.snapshot.paramMap.get('id');
    if (!hospitalizationId) {
      this.error = 'Hospitalization id is missing.';
      return;
    }
    this.load(hospitalizationId);
  }

  load(hospitalizationId: string): void {
    this.loading = true;
    this.error = '';
    this.opsApi.getHospitalizationProgress(hospitalizationId).subscribe({
      next: (item) => {
        this.hospitalization = item;
        this.loading = false;
        this.safetyError = '';
        this.safetySignal = null;
        this.loadingSafety = false;
        this.loadSafetyMetric(item);
      },
      error: () => {
        this.error = 'Unable to load hospitalization progress.';
        this.loading = false;
      }
    });
  }

  taskStatusClass(status?: string): string {
    if (status === 'DONE') return 'bg-soft-success text-success';
    if (status === 'NOT_DONE') return 'bg-soft-danger text-danger';
    return 'bg-soft-warning text-warning';
  }

  caseStatusClass(status?: string): string {
    if (status === 'ACTIVE') return 'bg-soft-primary text-primary';
    if (status === 'COMPLETED') return 'bg-soft-success text-success';
    if (status === 'CANCELLED') return 'bg-soft-danger text-danger';
    return 'bg-soft-warning text-warning';
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
