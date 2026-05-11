import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { Consultation, PatientProfile } from '../models/clinical.models';

interface DoctorPatientCard {
  id: number;
  profile: PatientProfile | null;
  name: string;
  ageLabel: string;
  bloodTypeLabel: string;
  consultationCount: number;
  latestConsultationAt?: string;
  statusSummary: string;
}

@Component({
  selector: 'app-doctor-patients-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './doctor-patients.page.html',
  styleUrl: './doctor-patients.page.scss'
})
export class DoctorPatientsPage implements OnInit {
  loading = false;
  error = '';
  query = '';
  patients: DoctorPatientCard[] = [];

  constructor(private api: ClinicalApiService) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading = true;
    this.error = '';
    this.api.listMyConsultations().subscribe({
      next: (consultations) => {
        const grouped = this.groupConsultationsByPatient(consultations ?? []);
        const ids = [...grouped.keys()];
        if (!ids.length) {
          this.patients = [];
          this.loading = false;
          return;
        }

        forkJoin(
          ids.map((id) =>
            this.api.getPatient(id).pipe(
              map((profile) => this.buildPatientCard(id, grouped.get(id) ?? [], profile)),
              catchError(() => of(this.buildPatientCard(id, grouped.get(id) ?? [], null)))
            )
          )
        ).subscribe({
          next: (cards) => {
            this.patients = cards.sort((a, b) => {
              const timeA = a.latestConsultationAt ? new Date(a.latestConsultationAt).getTime() : 0;
              const timeB = b.latestConsultationAt ? new Date(b.latestConsultationAt).getTime() : 0;
              return timeB - timeA;
            });
            this.loading = false;
          },
          error: () => {
            this.error = 'Unable to load your patient roster.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Unable to load your consultations.';
        this.loading = false;
      }
    });
  }

  get filteredPatients(): DoctorPatientCard[] {
    const term = this.query.trim().toLowerCase();
    if (!term) {
      return this.patients;
    }
    return this.patients.filter((patient) => {
      const values = [
        patient.name,
        patient.ageLabel,
        patient.bloodTypeLabel,
        patient.statusSummary,
        String(patient.id)
      ].join(' ').toLowerCase();
      return values.includes(term);
    });
  }

  private groupConsultationsByPatient(items: Consultation[]): Map<number, Consultation[]> {
    const grouped = new Map<number, Consultation[]>();
    for (const item of items) {
      const patientId = Number(item?.patientId);
      if (!Number.isFinite(patientId) || patientId <= 0) {
        continue;
      }
      const list = grouped.get(patientId) ?? [];
      list.push(item);
      grouped.set(patientId, list);
    }
    return grouped;
  }

  private buildPatientCard(id: number, consultations: Consultation[], profile: PatientProfile | null): DoctorPatientCard {
    const latest = [...consultations]
      .sort((a, b) => new Date(b.dateTime || 0).getTime() - new Date(a.dateTime || 0).getTime())[0];
    const name = [profile?.firstName, profile?.lastName]
      .filter((value) => String(value || '').trim().length > 0)
      .join(' ')
      .trim() || latest?.patientName || `Patient #${id}`;

    const age = Number(profile?.age);
    const ageLabel = Number.isFinite(age) && age > 0 ? `${age} years` : 'Age pending';
    const bloodTypeLabel = profile?.bloodType?.trim() || 'Blood type pending';
    const openCount = consultations.filter((item) => String(item.status || '').toUpperCase() !== 'COMPLETED').length;

    return {
      id,
      profile,
      name,
      ageLabel,
      bloodTypeLabel,
      consultationCount: consultations.length,
      latestConsultationAt: latest?.dateTime,
      statusSummary: openCount > 0 ? `${openCount} active encounter(s)` : 'Only concluded encounters'
    };
  }
}
