import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import {
  AuditLogItem,
  CommunicationApiService,
  FollowUpMessage,
  PatientDirectoryItem,
  QuickReplyTemplate,
  StaffDirectoryItem,
  StaffRole
} from '../../../core/services/communication-api.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-communication-details',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './communication-details.html',
  styleUrl: './communication-details.scss'
})
export class CommunicationDetailsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  loading = false;
  auditLoading = false;
  actionLoading = false;
  templatesLoading = false;
  errorMessage = '';
  successMessage = '';
  selectedTemplateId = '';
  showCloseConfirm = false;
  showEscalationModal = false;
  patientsById: Record<number, PatientDirectoryItem> = {};
  templates: QuickReplyTemplate[] = [];
  doctors: StaffDirectoryItem[] = [];

  message?: FollowUpMessage;
  auditItems: AuditLogItem[] = [];

  replyForm = this.fb.group({
    replyText: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  escalationForm = this.fb.group({
    doctorKeycloakId: ['', [Validators.required]],
    reason: ['', [Validators.required, Validators.maxLength(500)]]
  });

  constructor(
    private route: ActivatedRoute,
    private authStorage: AuthStorageService,
    private communicationApi: CommunicationApiService
  ) {}

  ngOnInit(): void {
    this.loadPatientsDirectory();
    this.loadDoctorsDirectory();
    this.load();
  }

  private loadDoctorsDirectory(): void {
    this.communicationApi.getDoctorsDirectory().subscribe({
      next: (doctors) => {
        this.doctors = doctors;
      },
      error: () => {
        this.doctors = [];
      }
    });
  }

  get isNurse(): boolean {
    return this.authStorage.getRole() === 'NURSE';
  }

  get currentStaffRole(): StaffRole {
    const role = this.authStorage.getRole();
    if (role === 'DOCTOR' || role === 'NURSE') {
      return role;
    }
    return 'RECEPTIONIST';
  }

  get canEscalate(): boolean {
    return this.isNurse && !!this.message && this.message.status !== 'CLOSED';
  }

  get canUnassign(): boolean {
    if (!this.message?.assignedToUserKeycloakId) {
      return false;
    }
    const currentUser = this.authStorage.getUser()?.keycloakId;
    return this.message.assignedToUserKeycloakId === currentUser || this.authStorage.getRole() === 'RECEPTIONIST';
  }

  get canClose(): boolean {
    if (!this.message) {
      return false;
    }
    return this.message.status !== 'CLOSED';
  }

  get messageTypeTemplates(): QuickReplyTemplate[] {
    if (!this.message) {
      return [];
    }
    return this.templates.filter((template) => template.messageType === this.message!.messageType);
  }

  get patientName(): string {
    if (!this.message) {
      return '-';
    }
    const patient = this.patientsById[this.message.patientId];
    if (!patient) {
      return `#${this.message.patientId}`;
    }
    return `${patient.firstName} ${patient.lastName}`.trim();
  }

  get patientDob(): string {
    if (!this.message) {
      return '-';
    }
    return this.patientsById[this.message.patientId]?.dateOfBirth ?? '-';
  }

  get replyLength(): number {
    return this.replyForm.value.replyText?.length ?? 0;
  }

  private loadPatientsDirectory(): void {
    this.communicationApi.getPatientsDirectory().subscribe({
      next: (patients) => {
        this.patientsById = patients.reduce((acc: Record<number, PatientDirectoryItem>, patient: PatientDirectoryItem) => {
          acc[patient.id] = patient;
          return acc;
        }, {});
      },
      error: () => {
        this.patientsById = {};
      }
    });
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.communicationApi.getMessageById(id).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: (msg) => {
        this.message = msg;
        this.loadTemplatesForMessageType();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to load message.';
      }
    });

    this.auditLoading = true;
    this.communicationApi.getAudit(id).pipe(
      finalize(() => {
        this.auditLoading = false;
      })
    ).subscribe({
      next: (items) => this.auditItems = items,
      error: () => this.auditItems = []
    });
  }

  private loadTemplatesForMessageType(): void {
    if (!this.message) {
      this.templates = [];
      return;
    }
    this.templatesLoading = true;
    this.communicationApi.listTemplates(this.message.messageType)
      .pipe(finalize(() => {
        this.templatesLoading = false;
      }))
      .subscribe({
        next: (templates) => {
          this.templates = templates;
        },
        error: () => {
          this.templates = [];
        }
      });
  }

  applyTemplate(): void {
    if (!this.selectedTemplateId) {
      return;
    }
    const template = this.templates.find((entry) => entry.id === this.selectedTemplateId);
    if (!template) {
      return;
    }

    const current = this.replyForm.value.replyText?.trim() ?? '';
    const merged = current ? `${current}\n\n${template.templateText}` : template.templateText;
    this.replyForm.patchValue({ replyText: merged });
    this.communicationApi.useTemplate(template.id).subscribe();
  }

  take(): void {
    if (!this.message || this.actionLoading) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;
    this.communicationApi.takeMessage(this.message.id).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => { this.message = msg; this.successMessage = 'Message assigned.'; },
      error: (err) => { this.errorMessage = err?.error?.message || 'Take failed.'; }
    });
  }

  markRead(): void {
    if (!this.message || this.actionLoading) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;
    this.communicationApi.markRead(this.message.id).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => { this.message = msg; this.successMessage = 'Message marked as read.'; },
      error: (err) => { this.errorMessage = err?.error?.message || 'Mark-read failed.'; }
    });
  }

  reply(): void {
    if (!this.message || this.replyForm.invalid || this.actionLoading) {
      this.replyForm.markAllAsTouched();
      return;
    }
    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;
    this.communicationApi.replyMessage(this.message.id, this.replyForm.value.replyText!).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => {
        this.message = msg;
        this.replyForm.reset();
        this.successMessage = 'Reply sent.';
        this.load();
      },
      error: (err) => { this.errorMessage = err?.error?.message || 'Reply failed.'; }
    });
  }

  escalate(): void {
    if (!this.message || !this.isNurse || this.actionLoading || this.escalationForm.invalid) {
      this.escalationForm.markAllAsTouched();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;

    const reason = this.escalationForm.value.reason?.trim() ?? '';
    const doctorKeycloakId = this.escalationForm.value.doctorKeycloakId?.trim() || null;
    this.communicationApi.escalate(this.message.id, doctorKeycloakId, reason).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => {
        this.message = msg;
        this.successMessage = 'Message escalated to doctor review.';
        this.showEscalationModal = false;
        this.escalationForm.reset({ doctorKeycloakId: '', reason: '' });
      },
      error: (err) => { this.errorMessage = err?.error?.message || 'Escalate failed.'; }
    });
  }

  close(): void {
    if (!this.message || this.actionLoading || !this.canClose) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;
    this.communicationApi.closeMessage(this.message.id).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => {
        this.message = msg;
        this.successMessage = 'Conversation closed successfully.';
        this.showCloseConfirm = false;
      },
      error: (err) => { this.errorMessage = err?.error?.message || 'Close failed.'; }
    });
  }

  openCloseConfirm(): void {
    if (!this.canClose) {
      return;
    }
    this.showCloseConfirm = true;
  }

  cancelCloseConfirm(): void {
    this.showCloseConfirm = false;
  }

  openEscalationModal(): void {
    if (!this.canEscalate) {
      return;
    }
    const defaultDoctor = this.message?.assignedDoctorKeycloakId ?? '';
    this.escalationForm.patchValue({ doctorKeycloakId: defaultDoctor, reason: '' });
    this.showEscalationModal = true;
  }

  cancelEscalationModal(): void {
    this.showEscalationModal = false;
    this.escalationForm.reset({ doctorKeycloakId: '', reason: '' });
  }

  unassign(): void {
    if (!this.message || !this.canUnassign || this.actionLoading) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.actionLoading = true;
    this.communicationApi.unassignMessage(this.message.id).pipe(
      finalize(() => {
        this.actionLoading = false;
      })
    ).subscribe({
      next: (msg) => {
        this.message = msg;
        this.successMessage = 'Message unassigned.';
        this.load();
      },
      error: (err) => { this.errorMessage = err?.error?.message || 'Unassign failed.'; }
    });
  }
}
