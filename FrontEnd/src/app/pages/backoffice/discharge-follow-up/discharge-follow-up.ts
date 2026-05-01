import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface FollowUpItem {
  itemType: string;
  description: string;
  frequency: string;
}

interface DischargeFollowUp {
  id: string;
  patientId: number;
  doctorId: string;
  status: string;
  createdAt: string;
  items: FollowUpItem[];
}

@Component({
  selector: 'app-discharge-follow-up',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './discharge-follow-up.html',
  styleUrls: ['./discharge-follow-up.scss']
})
export class DischargeFollowUpComponent implements OnInit, OnDestroy {
  followUpForm: FormGroup;
  followUps: DischargeFollowUp[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  showForm = false;
  refreshInterval: any;

  constructor(private http: HttpClient, private fb: FormBuilder) {
    this.followUpForm = this.fb.group({
      patientId: ['', [Validators.required, Validators.min(1)]],
      items: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.loadFollowUps();
    this.refreshInterval = setInterval(() => this.loadFollowUps(), 300000); // Refresh every 5 minutes
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  get items() {
    return this.followUpForm.get('items') as FormArray;
  }

  loadFollowUps(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<DischargeFollowUp[]>(`${environment.apiBaseUrl}/clinical/discharge-follow-ups/doctor/my`)
      .subscribe({
        next: (data) => {
          this.followUps = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading follow-ups:', err);
          this.errorMessage = 'Failed to load discharge follow-ups';
          this.loading = false;
        }
      });
  }

  createFollowUp(): void {
    if (this.followUpForm.invalid) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    this.loading = true;
    const payload = {
      patientId: this.followUpForm.get('patientId')?.value,
      items: this.followUpForm.get('items')?.value || []
    };

    this.http.post<DischargeFollowUp>(`${environment.apiBaseUrl}/clinical/discharge-follow-ups`, payload)
      .subscribe({
        next: (response) => {
          this.successMessage = 'Discharge follow-up created successfully';
          this.followUps.unshift(response);
          this.followUpForm.reset();
          this.showForm = false;
          this.loading = false;

          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          console.error('Error creating follow-up:', err);
          this.errorMessage = 'Failed to create discharge follow-up';
          this.loading = false;
        }
      });
  }

  addItem(): void {
    const itemsArray = this.followUpForm.get('items') as any;
    if (itemsArray) {
      itemsArray.push(this.fb.group({
        itemType: ['', Validators.required],
        description: ['', Validators.required],
        frequency: ['', Validators.required]
      }));
    }
  }

  removeItem(index: number): void {
    const itemsArray = this.followUpForm.get('items') as any;
    if (itemsArray) {
      itemsArray.removeAt(index);
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'badge bg-success';
      case 'COMPLETED': return 'badge bg-info';
      case 'CANCELLED': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.followUpForm.reset();
    }
  }
}
