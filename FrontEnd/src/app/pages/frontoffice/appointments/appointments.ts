import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentsApiService, AppointmentRequestItem, AppointmentStatus } from '../../../core/services/appointments-api.service';
import { CommunicationApiService, GuardianPatientItem } from '../../../core/services/communication-api.service';
import { Subscription, finalize } from 'rxjs';

@Component({
  selector: 'app-frontoffice-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './appointments.html',
  styleUrl: './appointments.scss'
})
export class FrontofficeAppointmentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private redirectTimer?: ReturnType<typeof setTimeout>;
  private bootstrapTimer?: ReturnType<typeof setTimeout>;
  private querySub?: Subscription;

  loading = true;
  loadingPatients = true;
  submitting = false;
  errorMessage = '';
  successMessage = '';
  patientSearch = '';
  actionById: Record<string, boolean> = {};
  listSearch = '';
  dateFilter = '';
  statusFilter: AppointmentStatus | 'ALL' = 'ALL';
  page = 1;
  readonly pageSize = 8;
  readonly appointmentTypes = ['CONSULTATION', 'DIALYSIS', 'LAB_TEST', 'FOLLOW_UP', 'EMERGENCY'] as const;
  readonly timeSlots = [
    { value: 'MORNING', label: 'Morning (8:00 - 12:00)' },
    { value: 'AFTERNOON', label: 'Afternoon (14:00 - 18:00)' },
    { value: 'NO_PREFERENCE', label: 'No Preference' }
  ] as const;

  items: AppointmentRequestItem[] = [];
  patients: GuardianPatientItem[] = [];
  hasPatientLink = true;
  private readonly noLinkMessage = 'No linked patient found for this account';
  readonly statuses: Array<AppointmentStatus | 'ALL'> = ['ALL', 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED'];

  form = this.fb.group({
    patientId: [null as number | null],
    appointmentType: ['CONSULTATION' as 'CONSULTATION' | 'DIALYSIS' | 'LAB_TEST' | 'FOLLOW_UP' | 'EMERGENCY', Validators.required],
    requestedDate: ['', Validators.required],
    preferredTimeSlot: ['NO_PREFERENCE' as 'MORNING' | 'AFTERNOON' | 'NO_PREFERENCE', Validators.required],
    reason: ['', [Validators.required, Validators.maxLength(500)]]
  });

  constructor(
    private appointmentsApi: AppointmentsApiService,
    private communicationApi: CommunicationApiService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('created') === '1') {
      setTimeout(() => this.runInView(() => {
        this.successMessage = 'Request submitted successfully! You will receive confirmation within 24 hours.';
      }));
    }

    this.bootstrapTimer = setTimeout(() => this.loadPatients(), 0);
  }

  ngOnDestroy(): void {
    this.querySub?.unsubscribe();
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
    }
    if (this.bootstrapTimer) {
      clearTimeout(this.bootstrapTimer);
    }
  }

  get shouldSelectPatient(): boolean {
    return this.patients.length > 1;
  }

  get filteredPatients(): GuardianPatientItem[] {
    const q = this.patientSearch.trim().toLowerCase();
    if (!q) {
      return this.patients;
    }

    return this.patients.filter(patient =>
      patient.fullName.toLowerCase().includes(q) || patient.dob.includes(q)
    );
  }

  get reasonLength(): number {
    return this.form.controls.reason.value?.length ?? 0;
  }

  get minRequestedDate(): string {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    return new Date(now.getTime() - offset * 60000).toISOString().slice(0, 10);
  }

  get requestedDateInvalid(): boolean {
    const control = this.form.controls.requestedDate;
    const value = control.value;
    if (!control.touched || !value) {
      return false;
    }
    return this.isRequestedDateInPast(value);
  }

  private isRequestedDateInPast(requestedDate: string): boolean {
    const selectedDate = new Date(`${requestedDate}T00:00:00`);
    if (Number.isNaN(selectedDate.getTime())) {
      return true;
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selectedDate.getTime() < today.getTime();
  }

  private toRequestedDateTimeIso(date: string, timeSlot: 'MORNING' | 'AFTERNOON' | 'NO_PREFERENCE'): string {
    const hour = timeSlot === 'MORNING' ? '09' : timeSlot === 'AFTERNOON' ? '15' : '10';
    return `${date}T${hour}:00:00`;
  }

  get filteredItems(): AppointmentRequestItem[] {
    const q = this.listSearch.trim().toLowerCase();
    return this.items
      .filter(item => this.statusFilter === 'ALL' || item.status === this.statusFilter)
      .filter(item => {
        if (!this.dateFilter) {
          return true;
        }

        const createdDate = new Date(item.createdAt).toISOString().slice(0, 10);
        const requestedDate = item.requestedDate ? new Date(item.requestedDate).toISOString().slice(0, 10) : '';
        return createdDate === this.dateFilter || requestedDate === this.dateFilter;
      })
      .filter(item => {
        if (!q) {
          return true;
        }

        return (
          item.reason.toLowerCase().includes(q) ||
          item.status.toLowerCase().includes(q)
        );
      })
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredItems.length / this.pageSize));
  }

  get pagedItems(): AppointmentRequestItem[] {
    const safePage = Math.min(this.page, this.totalPages);
    const start = (safePage - 1) * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  onFiltersChange(): void {
    this.page = 1;
  }

  previousPage(): void {
    this.page = Math.max(1, this.page - 1);
  }

  nextPage(): void {
    this.page = Math.min(this.totalPages, this.page + 1);
  }

  private loadPatients(): void {
    this.loadingPatients = true;
    this.hasPatientLink = true;
    this.communicationApi.getMyPatients().pipe(
      finalize(() => {
        this.runInView(() => {
          this.loadingPatients = false;
        });
      })
    ).subscribe({
      next: (patients) => {
        this.runInView(() => {
          this.patients = patients;

          if (patients.length === 0) {
            this.hasPatientLink = false;
            this.items = [];
            this.errorMessage = this.noLinkMessage;
            this.loading = false;
            return;
          }

          if (patients.length === 1) {
            this.form.patchValue({ patientId: patients[0].patientId });
            this.form.controls.patientId.clearValidators();
            this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
          } else if (patients.length > 1) {
            this.form.controls.patientId.setValidators([Validators.required]);
            this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
          }
        });

        this.load();
      },
      error: (err) => {
        // Fallback: still allow guardian to view/request using patient IDs present in existing requests.
        this.loadRequestsAsPatientFallback(err);
      }
    });
  }

  private loadRequestsAsPatientFallback(originalError: any): void {
    this.loading = true;
    this.appointmentsApi.getMyRequests().pipe(
      finalize(() => {
        this.runInView(() => {
          this.loading = false;
          this.loadingPatients = false;
        });
      })
    ).subscribe({
      next: (items) => {
        this.runInView(() => {
          this.items = [...items].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

          const uniquePatientIds = Array.from(new Set((items || []).map(item => Number(item.patientId)).filter((id) => Number.isFinite(id) && id > 0)));
          this.patients = uniquePatientIds.map((id) => ({
            patientId: id,
            fullName: `Patient #${id}`,
            dob: '-'
          }));

          if (this.patients.length === 1) {
            this.form.patchValue({ patientId: this.patients[0].patientId });
            this.form.controls.patientId.clearValidators();
            this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
            this.hasPatientLink = true;
            this.errorMessage = originalError?.status === 403 ? this.noLinkMessage : 'Unable to load linked patients. Existing requests are still visible.';
            return;
          }

          if (this.patients.length > 1) {
            this.form.controls.patientId.setValidators([Validators.required]);
            this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
            this.hasPatientLink = true;
            this.errorMessage = 'Unable to load linked patient profiles. Please select patient by ID from existing requests.';
            return;
          }

          this.hasPatientLink = false;
          this.items = [];
          this.errorMessage = this.noLinkMessage;
        });
      },
      error: (fallbackErr) => {
        this.runInView(() => {
          this.hasPatientLink = false;
          this.patients = [];
          this.items = [];
          this.errorMessage = fallbackErr?.error?.message || originalError?.error?.message || 'Unable to load linked patients.';
        });
      }
    });
  }

  load(): void {
    if (!this.hasPatientLink) {
      this.loading = false;
      this.items = [];
      this.errorMessage = this.noLinkMessage;
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.appointmentsApi.getMyRequests().pipe(
      finalize(() => {
        this.runInView(() => {
          this.loading = false;
        });
      })
    ).subscribe({
      next: (items) => {
        this.runInView(() => {
          this.items = [...items].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
          this.page = 1;
        });
      },
      error: (err) => {
        this.runInView(() => {
          if (err?.status === 403) {
            this.hasPatientLink = false;
            this.items = [];
            this.errorMessage = this.noLinkMessage;
            return;
          }
          this.errorMessage = err?.error?.message || 'Failed to load appointments.';
        });
      }
    });
  }

  create(): void {
    if (this.form.invalid || this.submitting || this.loadingPatients || this.patients.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    const requestedDate = this.form.controls.requestedDate.value;
    if (!requestedDate || this.isRequestedDateInPast(requestedDate)) {
      this.form.controls.requestedDate.markAsTouched();
      this.errorMessage = 'Please select a valid future date.';
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const value = this.form.getRawValue();
    const requestedDateTime = this.toRequestedDateTimeIso(
      value.requestedDate!,
      value.preferredTimeSlot!
    );

    this.appointmentsApi.createRequest({
      patientId: value.patientId!,
      requestedDate: requestedDateTime,
      reason: value.reason!
    }).pipe(
      finalize(() => {
        this.submitting = false;
      })
    ).subscribe({
      next: () => {
        this.runInView(() => {
          this.successMessage = 'Request submitted successfully! You will receive confirmation within 24 hours.';
          this.form.patchValue({ requestedDate: '', preferredTimeSlot: 'NO_PREFERENCE', appointmentType: 'CONSULTATION', reason: '' });
        });
        void this.router.navigate(['/frontoffice/schedule/appointments'], { queryParams: { created: '1' } });
        this.load();
      },
      error: (err) => {
        this.runInView(() => {
          this.errorMessage = err?.error?.message || 'Failed to create request.';
        });
      }
    });
  }

  cancelRequest = (id: string): void => {
    if (this.actionById[id]) {
      return;
    }

    this.actionById[id] = true;
    this.appointmentsApi.cancel(id).pipe(
      finalize(() => {
        this.actionById[id] = false;
      })
    ).subscribe({
      next: () => {
        this.runInView(() => {
          this.successMessage = 'Appointment request cancelled.';
        });
        this.load();
      },
      error: (err) => {
        this.runInView(() => {
          this.errorMessage = err?.error?.message || 'Cancel failed.';
        });
      }
    });
  };

  private runInView(update: () => void): void {
    this.ngZone.run(() => {
      update();
      this.cdr.detectChanges();
    });
  }
}
