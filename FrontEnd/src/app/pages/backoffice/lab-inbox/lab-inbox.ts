import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { environment } from '../../../../environments/environment';

interface LabRequest {
  id: string;
  doctorId: string;
  patientId: number;
  testType: string;
  urgency: string;
  status: string;
  notes?: string;
  createdAt: string;
}

@Component({
  selector: 'app-lab-inbox',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lab-inbox.html',
  styleUrl: './lab-inbox.scss'
})
export class LabInboxComponent implements OnInit {
  loading = true;
  errorMessage = '';
  successMessage = '';
  
  labRequests: LabRequest[] = [];
  filteredRequests: LabRequest[] = [];
  
  filterStatus = 'PENDING';
  filterUrgency = 'ALL';
  
  showUploadModal = false;
  selectedRequest: LabRequest | null = null;
  filePath = '';
  fileName = '';
  uploadingId = '';

  constructor(
    private http: HttpClient,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    this.loadLabRequests();
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
    this.filePath = '';
    this.fileName = '';
    this.showUploadModal = true;
  }

  closeUploadModal(): void {
    this.showUploadModal = false;
    this.selectedRequest = null;
  }

  uploadResult(): void {
    if (!this.selectedRequest || !this.filePath || !this.fileName) {
      this.errorMessage = 'Please fill in all fields';
      return;
    }

    this.uploadingId = this.selectedRequest.id;
    const token = this.authStorage.getAccessToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.post(
      `${environment.apiBaseUrl}/api/clinical/lab-requests/${this.selectedRequest.id}/results`,
      { filePath: this.filePath, fileName: this.fileName },
      { headers }
    ).subscribe({
      next: () => {
        this.successMessage = 'Lab result uploaded successfully';
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
