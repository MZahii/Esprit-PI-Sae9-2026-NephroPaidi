import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { GuardianPatientsService } from '../../../features/administrative/api/guardian-patients.service';

@Component({
  selector: 'app-guardian-consultation-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './consultation-details.page.html',
  styleUrl: './consultation-details.page.scss'
})
export class ConsultationDetailsPage implements OnInit {
  consultationId = '';
  consultation: any | null = null;
  outcome: any | null = null;
  loading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private api: ClinicalApiService,
    private guardianPatients: GuardianPatientsService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Consultation id missing.';
      return;
    }
    this.consultationId = id;
    this.loadConsultation();
  }

  loadConsultation(): void {
    this.loading = true;
    this.error = '';
    this.guardianPatients.getGuardianPatientIds().pipe(
      switchMap((patientIds: number[]) => {
        if (!patientIds.length) return of([]);
        const requests = patientIds.map((patientId: number) =>
          this.api.listGuardianConsultations({ patientId }).pipe(
            catchError(() => of([]))
          )
        );
        return forkJoin(requests).pipe(map((sets: any[][]) => sets.flat()));
      }),
      map((items: any[]) => items.find((c: any) => String(c.id) === this.consultationId) || null),
      switchMap((item: any) => {
        if (!item) {
          return of({ consultation: null, outcome: null });
        }
        return this.api.getGuardianConsultationOutcome(this.consultationId).pipe(
          map((outcome) => ({ consultation: item, outcome })),
          catchError(() => of({ consultation: item, outcome: null }))
        );
      }),
      catchError(() => of({ consultation: null, outcome: null }))
    ).subscribe({
      next: (result) => {
        this.consultation = result.consultation;
        this.outcome = result.outcome;
        this.loading = false;
        if (!result.consultation) {
          this.error = 'Consultation not found.';
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load consultation.';
      }
    });
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'badge bg-success';
    if (status === 'CANCELLED') return 'badge bg-danger';
    if (status === 'IN_PROGRESS') return 'badge bg-primary';
    return 'badge bg-warning text-dark';
  }

  get consultationTitle(): string {
    const formattedDate = this.formatConsultationDate(this.consultation?.dateTime, 'longDate');
    return formattedDate ? `Consultation on ${formattedDate}` : 'Consultation details';
  }

  formatConsultationDate(value: any, format: 'longDate' | 'medium' = 'medium'): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('en-US', format === 'longDate'
      ? { year: 'numeric', month: 'short', day: 'numeric' }
      : { year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' }
    ).format(date);
  }
}
