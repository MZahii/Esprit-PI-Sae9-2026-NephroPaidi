import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HospitalizationCaseDto, OpsApiService } from '../../../core/services/ops-api.service';
import { DrugSafetyService, DrugSafetySignal } from '../../../core/services/drug-safety.service';
import { catchError, of } from 'rxjs';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

@Component({
  selector: 'app-hospitalization-review-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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
  savingLocation = false;
  locationForm = {
    roomNumber: '',
    bedNumber: ''
  };

  constructor(
    private route: ActivatedRoute,
    private opsApi: OpsApiService,
    private drugSafety: DrugSafetyService,
    private authStorage: AuthStorageService
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
    this.opsApi.getHospitalizationById(hospitalizationId).subscribe({
      next: (item) => {
        this.hospitalization = item;
        this.locationForm = {
          roomNumber: item.roomNumber || '',
          bedNumber: item.bedNumber || ''
        };
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

  get currentRole(): string {
    return String(this.authStorage.getRole() ?? '');
  }

  get isReceptionist(): boolean {
    return this.currentRole === 'RECEPTIONIST';
  }

  saveLocation(): void {
    if (!this.hospitalization || this.savingLocation) {
      return;
    }

    const roomNumber = this.locationForm.roomNumber.trim();
    const bedNumber = this.locationForm.bedNumber.trim();
    if (!roomNumber || !bedNumber) {
      this.error = 'Room number and bed number are required.';
      return;
    }

    this.savingLocation = true;
    this.error = '';
    this.opsApi.assignHospitalizationLocation(this.hospitalization.id, { roomNumber, bedNumber }).subscribe({
      next: (updated) => {
        this.hospitalization = updated;
        this.locationForm = {
          roomNumber: updated.roomNumber || '',
          bedNumber: updated.bedNumber || ''
        };
        this.savingLocation = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Unable to assign room and bed.';
        this.savingLocation = false;
      }
    });
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
