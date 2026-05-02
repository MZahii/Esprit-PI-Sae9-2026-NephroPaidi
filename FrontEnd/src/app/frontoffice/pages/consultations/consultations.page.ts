import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { AppointmentsApiService } from '../../../core/services/appointments-api.service';
import { GuardianPatientsService } from '../../../features/administrative/api/guardian-patients.service';

type ConsultationStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

@Component({
  selector: 'app-guardian-consultations',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './consultations.page.html',
  styleUrl: './consultations.page.scss'
})
export class ConsultationsPage implements OnInit {
  consultations: any[] = [];
  loading = false;
  error = '';

  filterStatus: ConsultationStatus | 'ALL' = 'ALL';
  statuses: Array<ConsultationStatus | 'ALL'> = ['ALL', 'OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  constructor(
    private api: ClinicalApiService,
    private appointmentsApi: AppointmentsApiService,
    private guardianPatients: GuardianPatientsService
  ) {}

  ngOnInit(): void {
    this.loadConsultations();
  }

  loadConsultations(): void {
    this.loading = true;
    this.error = '';

    const clinicalConsultations$ = this.guardianPatients.getGuardianPatientIds().pipe(
      switchMap((patientIds: number[]) => {
        if (!patientIds.length) return of([]);
        const requests = patientIds.map((patientId: number) =>
          this.api.listGuardianConsultations({ patientId, status: this.filterStatus }).pipe(
            catchError(() =>
              this.api.listAppointments({ patientId }).pipe(
                map((appointments) => this.mapAppointmentsToConsultations(appointments, patientId)),
                catchError(() => of([]))
              )
            )
          )
        );
        return forkJoin(requests).pipe(map((sets: any[][]) => sets.flat()));
      }),
      catchError(() => of([]))
    );

    const approvedRequests$ = this.appointmentsApi.getMyRequests().pipe(
      map((items) => (items || [])
        .filter((item) => item.status === 'APPROVED')
        .map((item) => ({
          id: `REQ-${item.id}`,
          patientId: item.patientId,
          dateTime: item.scheduledDate || item.requestedDate,
          status: 'OPEN',
          appointmentId: item.id,
          source: 'REQUEST',
          reason: item.reason
        }))
        .filter((item) => !!item.dateTime)
      ),
      catchError(() => of([]))
    );

    forkJoin([clinicalConsultations$, approvedRequests$]).pipe(
      map(([clinicalItems, requestItems]) => this.mergeAndDedupeConsultations(clinicalItems as any[], requestItems as any[])),
      map((items: any[]) => this.dedupe(items)),
      catchError(() => {
        this.error = 'Failed to load consultations.';
        return of([]);
      })
    ).subscribe({
      next: (items: any[]) => {
        this.consultations = items || [];
        this.loading = false;
      },
      error: () => {
        this.consultations = [];
        this.loading = false;
        this.error = 'Failed to load consultations.';
      }
    });
  }

  get filteredConsultations(): any[] {
    if (this.filterStatus === 'ALL') return this.consultations;
    return this.consultations.filter(c => c.status === this.filterStatus);
  }

  statusBadge(status?: string): string {
    if (status === 'COMPLETED') return 'badge bg-success';
    if (status === 'CANCELLED') return 'badge bg-danger';
    if (status === 'IN_PROGRESS') return 'badge bg-primary';
    return 'badge bg-warning text-dark';
  }

  private mapAppointmentsToConsultations(appointments: any[], patientId: number): any[] {
    return (appointments || []).map((appt) => ({
      id: appt.consultationId || `APPT-${appt.id}`,
      patientId,
      dateTime: appt.scheduledAt,
      status: appt.status === 'CANCELLED' ? 'CANCELLED' : 'OPEN',
      appointmentId: appt.id
    }));
  }

  private dedupe(items: any[]): any[] {
    const seen = new Set<string>();
    return (items || []).filter(item => {
      const key = String(item?.id ?? `${item?.patientId}-${item?.dateTime}`);
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  private mergeAndDedupeConsultations(clinicalItems: any[], requestItems: any[]): any[] {
    const clinical = this.dedupe(clinicalItems || []);
    const requests = this.dedupe(requestItems || []);

    const clinicalTimeKeys = new Set(
      clinical.map((item) => `${item?.patientId ?? ''}|${this.toMinuteKey(item?.dateTime)}`)
    );

    const filteredRequests = requests.filter((item) => {
      const key = `${item?.patientId ?? ''}|${this.toMinuteKey(item?.dateTime)}`;
      return !clinicalTimeKeys.has(key);
    });

    return [...clinical, ...filteredRequests].sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime());
  }

  private toMinuteKey(value: any): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
