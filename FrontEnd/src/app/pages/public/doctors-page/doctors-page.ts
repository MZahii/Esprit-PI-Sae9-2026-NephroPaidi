import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClinicalApiService, DoctorSearchResult } from '../../../core/services/clinical-api.service';

@Component({
  selector: 'app-doctors-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './doctors-page.html'
})
export class DoctorsPageComponent implements OnInit {
  doctors: DoctorSearchResult[] = [];
  loading = false;
  errorMessage = '';
  readonly defaultDoctorImage = 'assets/backoffice/images/general/full-avatar.png';

  constructor(private clinicalApi: ClinicalApiService) {}

  ngOnInit(): void {
    this.loadDoctors();
  }

  loadDoctors(): void {
    this.loading = true;
    this.errorMessage = '';

    this.clinicalApi.getPublicDoctors().subscribe({
      next: (doctors) => {
        this.doctors = doctors ?? [];
        this.loading = false;
      },
      error: () => {
        this.doctors = [];
        this.errorMessage = 'Unable to load doctors right now.';
        this.loading = false;
      }
    });
  }

  fullName(doctor: DoctorSearchResult): string {
    const name = `${doctor.firstName ?? ''} ${doctor.lastName ?? ''}`.trim();
    return name || doctor.username || 'Doctor';
  }

  doctorTitle(doctor: DoctorSearchResult): string {
    return doctor.role ? `${doctor.role} Specialist` : 'Medical Specialist';
  }

  doctorSummary(doctor: DoctorSearchResult): string {
    const parts: string[] = [];
    if (doctor.email) parts.push(doctor.email);
    if (doctor.phone) parts.push(doctor.phone);
    return parts.length > 0 ? parts.join(' • ') : 'Active member of the care team.';
  }

  handleImageError(event: Event): void {
    const target = event.target as HTMLImageElement | null;
    if (target) {
      target.src = this.defaultDoctorImage;
    }
  }
}