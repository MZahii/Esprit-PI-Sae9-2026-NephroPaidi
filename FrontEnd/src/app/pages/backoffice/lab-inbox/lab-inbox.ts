import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';

interface LabRequest {
  id: string;
  doctorId: string;
  patientId: number;
  consultationId?: string;
  testType: string;
  urgency: string;
  status: string;
  notes?: string;
  latestAiRecommendation?: string;
  latestAiConfidence?: number;
  latestAiRequiresDoctorReview?: boolean;
  latestAiSummary?: string;
  createdAt: string;
}

@Component({
  selector: 'app-lab-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './lab-inbox.html',
  styleUrl: './lab-inbox.scss'
})
export class LabInboxComponent implements OnInit, OnDestroy {
  loading = true;
  errorMessage = '';
  successMessage = '';
  
  labRequests: LabRequest[] = [];
  filteredRequests: LabRequest[] = [];
  
  filterStatus = 'PENDING';
  filterUrgency = 'ALL';
  
  showUploadModal = false;
  selectedRequest: LabRequest | null = null;
  selectedFile: File | null = null;
  uploadingId = '';
  private refreshSub?: Subscription;

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    this.loadLabRequests();
    this.refreshSub = interval(15000).subscribe(() => {
      if (!this.showUploadModal && !this.uploadingId) {
        this.loadLabRequests();
      }
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadLabRequests(): void {
    this.loading = true;
    this.errorMessage = '';

    const token = this.authStorage.getAccessToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.get<LabRequest[]>(
      `${environment.apiBaseUrl}/api/clinical/lab-requests/pending`,
      { headers }
    ).subscribe({
      next: (data) => {
        this.labRequests = data || [];
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load lab requests';
        console.error(err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredRequests = this.labRequests.filter(req => {
      if (this.filterStatus !== 'ALL' && req.status !== this.filterStatus) return false;
      if (this.filterUrgency !== 'ALL' && req.urgency !== this.filterUrgency) return false;
      return true;
    });
  }

  openUploadModal(request: LabRequest): void {
    this.selectedRequest = request;
    this.selectedFile = null;
    this.showUploadModal = true;
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
    this.selectedRequest = null;
    this.selectedFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  uploadResult(): void {
    if (!this.selectedRequest || !this.selectedFile) {
      this.errorMessage = 'Please choose a result file';
      return;
    }

    this.uploadingId = this.selectedRequest.id;
    const token = this.authStorage.getAccessToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    const formData = new FormData();
    formData.append('file', this.selectedFile, this.selectedFile.name);

    this.http.post(
      `${environment.apiBaseUrl}/api/clinical/lab-requests/${this.selectedRequest.id}/results`,
      formData,
      { headers }
    ).subscribe({
      next: () => {
        this.successMessage = 'Lab result uploaded and analyzed successfully';
        this.closeUploadModal();
        this.loadLabRequests();
      },
      error: (err) => {
        this.errorMessage = 'Failed to upload lab result';
        console.error(err);
      },
      complete: () => {
        this.uploadingId = '';
      }
    });
  }

  getUrgencyBadgeClass(urgency: string): string {
    switch (urgency) {
      case 'STAT':
        return 'bg-danger';
      case 'URGENT':
        return 'bg-warning';
      default:
        return 'bg-info';
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'bg-success';
      case 'IN_PROGRESS':
        return 'bg-primary';
      default:
        return 'bg-warning';
    }
  }
}
