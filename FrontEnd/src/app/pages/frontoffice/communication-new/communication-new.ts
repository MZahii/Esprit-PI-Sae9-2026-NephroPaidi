import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommunicationApiService, GuardianPatientItem, MessageType } from '../../../core/services/communication-api.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-communication-new',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './communication-new.html',
  styleUrl: './communication-new.scss'
})
export class CommunicationNewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private redirectTimer?: ReturnType<typeof setTimeout>;
  private hasLoadedPatientsOnce = false;

  submitting = false;
  loadingPatients = true;
  errorMessage = '';
  successMessage = '';
  patientSearch = '';
  patients: GuardianPatientItem[] = [];

  readonly messageTypes: MessageType[] = ['ADMINISTRATIVE', 'APPOINTMENT', 'MEDICAL', 'LAB_RESULT', 'OTHER'];

  form = this.fb.group({
    patientId: [null as number | null],
    messageType: ['ADMINISTRATIVE' as MessageType, Validators.required],
    priority: ['NORMAL' as 'NORMAL' | 'HIGH', Validators.required],
    subject: ['', [Validators.maxLength(120)]],
    messageText: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private communicationApi: CommunicationApiService
  ) {}

  ngOnInit(): void {
    const queryType = this.route.snapshot.queryParamMap.get('type') as MessageType | null;
    if (queryType && this.messageTypes.includes(queryType)) {
      this.form.patchValue({ messageType: queryType });
    }

    this.loadPatients();
  }

  ngOnDestroy(): void {
    if (this.redirectTimer) {
      clearTimeout(this.redirectTimer);
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
      patient.fullName.toLowerCase().includes(q) ||
      patient.dob.includes(q)
    );
  }

  get subjectLength(): number {
    return this.form.controls.subject.value?.length ?? 0;
  }

  get messageLength(): number {
    return this.form.controls.messageText.value?.length ?? 0;
  }

  get isUrgent(): boolean {
    return this.form.controls.priority.value === 'HIGH';
  }

  setUrgent(isChecked: boolean): void {
    this.form.controls.priority.setValue(isChecked ? 'HIGH' : 'NORMAL');
  }

  private loadPatients(): void {
    if (this.hasLoadedPatientsOnce) {
      return;
    }
    this.hasLoadedPatientsOnce = true;

    this.loadingPatients = true;
    this.communicationApi.getMyPatients().pipe(
      finalize(() => {
        this.loadingPatients = false;
      })
    ).subscribe({
      next: (patients) => {
        this.patients = patients;

        if (patients.length === 1) {
          this.form.patchValue({ patientId: patients[0].patientId });
          this.form.controls.patientId.clearValidators();
          this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
          return;
        }

        if (patients.length > 1) {
          this.form.controls.patientId.setValidators([Validators.required]);
          this.form.controls.patientId.updateValueAndValidity({ emitEvent: false });
        }
      },
      error: (err) => {
        if (err?.status === 403) {
          this.patients = [];
          this.errorMessage = '';
          return;
        }
        this.errorMessage = err?.error?.message || 'Unable to load linked patients.';
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting || this.loadingPatients || this.patients.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    const value = this.form.getRawValue();
    this.communicationApi.createMessage({
      patientId: value.patientId,
      messageType: value.messageType!,
      priority: value.priority!,
      subject: value.subject || null,
      messageText: value.messageText!
    }).pipe(
      finalize(() => {
        this.submitting = false;
      })
    ).subscribe({
      next: () => {
        this.successMessage = 'Message sent successfully. Redirecting to your messages...';
        this.redirectTimer = setTimeout(() => {
          void this.router.navigate(['/frontoffice/communication'], { queryParams: { created: '1' } });
        }, 1500);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to create message.';
      }
    });
  }
}
