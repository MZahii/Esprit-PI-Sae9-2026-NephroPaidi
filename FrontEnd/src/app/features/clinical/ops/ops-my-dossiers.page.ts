import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HospitalizationSummaryResponse } from '../../../core/models/ops.models';
import { OpsApiService } from '../../../core/services/ops-api.service';

@Component({
  selector: 'app-ops-my-dossiers',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ops-my-dossiers.page.html',
  styleUrl: './ops-my-dossiers.page.scss'
})
export class OpsMyDossiersPage implements OnInit {
  loading = false;
  error = '';
  hospitalizations: HospitalizationSummaryResponse[] = [];

  constructor(
    private opsApi: OpsApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.opsApi.listMyAssignedDossiers(0, 50).subscribe({
      next: (res) => {
        this.hospitalizations = (res?.content ?? []).map((item) => ({
          id: item.id,
          patientId: item.patientId,
          consultationId: '',
          doctorUsername: '-',
          reason: `Priority ${item.admissionPriority}`,
          status: item.status === 'DISCHARGED' || item.status === 'ARCHIVED'
            ? 'COMPLETED'
            : item.status === 'ACTIVE' || item.status === 'IN_PROGRESS' || item.status === 'READY_FOR_DISCHARGE'
              ? 'ACTIVE'
              : 'REQUESTED',
          totalTasks: 0,
          completedTasks: 0,
          pendingTasks: 0,
          createdAt: item.admittedAt,
          updatedAt: item.admittedAt
        }));
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Unable to load active hospitalizations.';
      }
    });
  }

  openHospitalization(item: HospitalizationSummaryResponse): void {
    this.router.navigate(['/backoffice/ops/dossiers', item.id], {
      queryParams: {
        returnUrl: '/backoffice/ops/my-dossiers'
      }
    });
  }
}
