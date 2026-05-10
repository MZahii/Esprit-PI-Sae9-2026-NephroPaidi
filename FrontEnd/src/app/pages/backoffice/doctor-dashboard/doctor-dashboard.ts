import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';

interface DashboardData {
  todayAppointments: any[];
  pendingAppointmentRequests: number;
  pendingLabRequests: number;
  completedLabRequests: number;
  surgeriesIndicationsSent: number;
  hospitalizedPatients: number;
}

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './doctor-dashboard.html',
  styleUrl: './doctor-dashboard.scss'
})
export class DoctorDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';
  dashboardData: DashboardData | null = null;
  lastRefresh = new Date();

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
    // Auto-refresh every 5 minutes
    setInterval(() => this.loadDashboard(), 5 * 60 * 1000);
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = '';

    const token = this.authStorage.getAccessToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get<DashboardData>(
      `${environment.apiBaseUrl}/api/clinical/dashboard/my`,
      { headers }
    ).subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.lastRefresh = new Date();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load dashboard data';
        console.error(err);
        this.loading = false;
      }
    });
  }

  refresh(): void {
    this.loadDashboard();
  }
}
