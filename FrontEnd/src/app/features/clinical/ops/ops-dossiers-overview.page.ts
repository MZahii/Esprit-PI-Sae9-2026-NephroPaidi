import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OpsApiService } from '../../../core/services/ops-api.service';
import { HospitalizationSummaryResponse } from '../../../core/models/ops.models';

@Component({
  selector: 'app-ops-dossiers-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ops-dossiers-overview.page.html',
  styleUrl: './ops-dossiers-overview.page.scss'
})
export class OpsDossiersOverviewPage implements OnInit {
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

    this.opsApi.listActiveHospitalizationsForNurse().subscribe({
      next: (res) => {
        this.hospitalizations = res ?? [];
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Unable to load hospitalizations.';
      }
    });
  }

  openHospitalization(item: HospitalizationSummaryResponse): void {
    this.router.navigate(['/backoffice/ops/dossiers', item.id], {
      queryParams: {
        returnUrl: '/backoffice/ops/dossiers'
      }
    });
  }
}
