import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DialysisPlan, ProcedureApiService } from '../../../core/services/procedure-api.service';
import { ClinicalApiService, DoctorSearchResult } from '../../../core/services/clinical-api.service';

interface PatientLookupItem {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  sex?: 'MALE' | 'FEMALE' | string | null;
  bloodType?: string | null;
  allergies?: string | null;
}

@Component({
  selector: 'app-procedure-dialysis',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procedure-dialysis.html',
  styleUrl: './procedure-dialysis.scss'
})
export class ProcedureDialysisComponent implements OnInit {
  private readonly allowedStatusTransitions: Record<string, string[]> = {
    PLANNED: ['PLANNED', 'IN_PROGRESS', 'CANCELLED', 'ARCHIVED'],
    IN_PROGRESS: ['IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ARCHIVED'],
    COMPLETED: ['COMPLETED', 'ARCHIVED'],
    CANCELLED: ['CANCELLED', 'ARCHIVED'],
    ARCHIVED: ['ARCHIVED']
  };

  loading = false;
  saving = false;
  downloadingPdfId: number | null = null;
  showEditModal = false;
  savingEdit = false;
  editingPlanId: number | null = null;
  errorMessage = '';
  successMessage = '';

  plans: DialysisPlan[] = [];
  searchTerm = '';
  statusFilter = 'ALL';
  showArchived = false;
  readonly planStatuses = ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ARCHIVED'];
  readonly dialysisTypes = ['HEMODIALYSIS', 'PERITONEAL_DIALYSIS'];
  availablePatients: PatientLookupItem[] = [];
  selectedPatient: PatientLookupItem | null = null;
  patientLoading = false;
  patientError = '';
  availableDoctors: DoctorSearchResult[] = [];
  selectedDoctor: DoctorSearchResult | null = null;
  doctorLoading = false;
  doctorError = '';

  createForm = {
    patientId: '',
    firstName: '',
    lastName: '',
    doctorId: '',
    dialysisType: 'HEMODIALYSIS',
    sessionsPerWeek: 3,
    sessionDurationMinutes: 240,
    startDate: '',
    endDate: '',
    daysOfWeek: 'Monday, Wednesday, Friday',
    bloodFlowRate: null as number | null,
    dialysateFlowRate: null as number | null,
    ultrafiltrationGoal: null as number | null,
    dialysisCenterId: '',
    roomNumber: '',
    machineId: ''
  };

  editForm = {
    firstName: '',
    lastName: '',
    doctorId: '',
    dialysisType: 'HEMODIALYSIS',
    sessionsPerWeek: 3,
    sessionDurationMinutes: 240,
    startDate: '',
    endDate: '',
    daysOfWeek: '',
    bloodFlowRate: null as number | null,
    dialysateFlowRate: null as number | null,
    ultrafiltrationGoal: null as number | null,
    dialysisCenterId: '',
    roomNumber: '',
    machineId: '',
    status: 'PLANNED'
  };

  editState: Record<number, string> = {};

  constructor(
    private procedureApi: ProcedureApiService,
    private clinicalApi: ClinicalApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadPatients();
    this.loadDoctors();
    this.loadPlans();
  }

  get filteredPlans(): DialysisPlan[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.plans.filter((plan) => {
      const isArchived = plan.status === 'ARCHIVED';
      if (!this.showArchived && isArchived) {
        return false;
      }

      const matchesStatus = this.statusFilter === 'ALL' || plan.status === this.statusFilter;
      const fullName = `${plan.firstName ?? ''} ${plan.lastName ?? ''}`.trim().toLowerCase();
      const matchesSearch =
        !term ||
        fullName.includes(term);
      return matchesStatus && matchesSearch;
    });
  }

  get editingPlan(): DialysisPlan | undefined {
    return this.plans.find((plan) => plan.id === this.editingPlanId);
  }

  loadPlans(): void {
    this.loading = true;
    this.errorMessage = '';

    this.procedureApi.getDialysisPlans().subscribe({
      next: (plans) => {
        this.plans = plans ?? [];
        this.editState = this.plans.reduce<Record<number, string>>((acc, plan) => {
          acc[plan.id] = plan.status;
          return acc;
        }, {});
        this.loading = false;
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load dialysis plans.');
        this.refreshView();
      }
    });
  }

  createPlan(): void {
    const patientId = this.createForm.patientId.trim();
    const firstName = this.createForm.firstName.trim();
    const lastName = this.createForm.lastName.trim();
    const doctorId = this.createForm.doctorId.trim();
    const dialysisType = this.createForm.dialysisType.trim();
    const sessionsPerWeek = Number(this.createForm.sessionsPerWeek);
    const sessionDurationMinutes = Number(this.createForm.sessionDurationMinutes);
    const startDate = this.createForm.startDate;
    const endDate = this.createForm.endDate || null;
    const daysOfWeek = this.createForm.daysOfWeek.trim();
    const bloodFlowRate = this.createForm.bloodFlowRate;
    const dialysateFlowRate = this.createForm.dialysateFlowRate;
    const ultrafiltrationGoal = this.createForm.ultrafiltrationGoal;
    const dialysisCenterId = this.createForm.dialysisCenterId.trim() || null;
    const roomNumber = this.createForm.roomNumber.trim() || null;
    const machineId = this.createForm.machineId.trim() || null;
    const status = 'PLANNED';

    if (
      !patientId ||
      !firstName
      || !lastName
      || !doctorId
      || !dialysisType
      || !sessionsPerWeek
      || !sessionDurationMinutes
      || !startDate
      || !daysOfWeek
    ) {
      this.errorMessage = 'Please select a patient and complete the required planning fields.';
      this.refreshView();
      return;
    }

    if (sessionsPerWeek < 1 || sessionsPerWeek > 7) {
      this.errorMessage = 'Sessions per week must be between 1 and 7.';
      this.refreshView();
      return;
    }

    if (sessionDurationMinutes < 30 || sessionDurationMinutes > 720) {
      this.errorMessage = 'Session duration must be between 30 and 720 minutes.';
      this.refreshView();
      return;
    }

    if (endDate && endDate < startDate) {
      this.errorMessage = 'End date cannot be before start date.';
      this.refreshView();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.createDialysisPlan({
      patientId,
      firstName,
      lastName,
      doctorId,
      dialysisType,
      sessionsPerWeek,
      sessionDurationMinutes,
      startDate,
      endDate,
      daysOfWeek,
      bloodFlowRate,
      dialysateFlowRate,
      ultrafiltrationGoal,
      dialysisCenterId,
      roomNumber,
      machineId,
      status
    }).subscribe({
      next: () => {
        this.createForm = {
          patientId: '',
          firstName: '',
          lastName: '',
          doctorId: '',
          dialysisType: 'HEMODIALYSIS',
          sessionsPerWeek: 3,
          sessionDurationMinutes: 240,
          startDate: '',
          endDate: '',
          daysOfWeek: 'Monday, Wednesday, Friday',
          bloodFlowRate: null,
          dialysateFlowRate: null,
          ultrafiltrationGoal: null,
          dialysisCenterId: '',
          roomNumber: '',
          machineId: ''
        };
        this.selectedPatient = null;
        this.patientError = '';
        this.selectedDoctor = null;
        this.doctorError = '';
        this.successMessage = 'Dialysis plan created successfully.';
        this.saving = false;
        this.refreshView();
        this.loadPlans();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = this.formatApiError(err, 'Failed to create dialysis plan.');
        this.refreshView();
      }
    });
  }

  openEditModal(plan: DialysisPlan): void {
    this.editingPlanId = plan.id;
    this.editForm = {
      firstName: plan.firstName ?? '',
      lastName: plan.lastName ?? '',
      doctorId: plan.doctorId ?? '',
      dialysisType: plan.dialysisType ?? 'HEMODIALYSIS',
      sessionsPerWeek: Number(plan.sessionsPerWeek ?? 3),
      sessionDurationMinutes: Number(plan.sessionDurationMinutes ?? 240),
      startDate: plan.startDate ?? '',
      endDate: plan.endDate ?? '',
      daysOfWeek: plan.daysOfWeek ?? '',
      bloodFlowRate: plan.bloodFlowRate ?? null,
      dialysateFlowRate: plan.dialysateFlowRate ?? null,
      ultrafiltrationGoal: plan.ultrafiltrationGoal ?? null,
      dialysisCenterId: plan.dialysisCenterId ?? '',
      roomNumber: plan.roomNumber ?? '',
      machineId: plan.machineId ?? '',
      status: plan.status ?? 'PLANNED'
    };
    this.selectedDoctor = this.availableDoctors.find(
      (doctor) => String(doctor.keycloakId ?? doctor.id ?? '').trim() === this.editForm.doctorId
    ) ?? null;
    this.showEditModal = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.refreshView();
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.savingEdit = false;
    this.editingPlanId = null;
    this.refreshView();
  }

  savePlanChanges(): void {
    if (this.editingPlanId == null) {
      return;
    }

    const firstName = this.editForm.firstName.trim();
    const lastName = this.editForm.lastName.trim();
    const doctorId = this.editForm.doctorId.trim();
    const dialysisType = this.editForm.dialysisType.trim();
    const sessionsPerWeek = Number(this.editForm.sessionsPerWeek);
    const sessionDurationMinutes = Number(this.editForm.sessionDurationMinutes);
    const startDate = this.editForm.startDate;
    const endDate = this.editForm.endDate || null;
    const daysOfWeek = this.editForm.daysOfWeek.trim();
    const bloodFlowRate = this.editForm.bloodFlowRate;
    const dialysateFlowRate = this.editForm.dialysateFlowRate;
    const ultrafiltrationGoal = this.editForm.ultrafiltrationGoal;
    const dialysisCenterId = this.editForm.dialysisCenterId.trim() || null;
    const roomNumber = this.editForm.roomNumber.trim() || null;
    const machineId = this.editForm.machineId.trim() || null;
    const status = this.editForm.status.trim().toUpperCase();

    if (
      !firstName ||
      !lastName ||
      !doctorId ||
      !dialysisType ||
      !sessionsPerWeek ||
      !sessionDurationMinutes ||
      !startDate ||
      !daysOfWeek ||
      !status
    ) {
      this.errorMessage = 'Please complete the required fields before saving the dialysis plan.';
      this.refreshView();
      return;
    }

    if (sessionsPerWeek < 1 || sessionsPerWeek > 7) {
      this.errorMessage = 'Sessions per week must be between 1 and 7.';
      this.refreshView();
      return;
    }

    if (sessionDurationMinutes < 30 || sessionDurationMinutes > 720) {
      this.errorMessage = 'Session duration must be between 30 and 720 minutes.';
      this.refreshView();
      return;
    }

    if (endDate && endDate < startDate) {
      this.errorMessage = 'End date cannot be before start date.';
      this.refreshView();
      return;
    }

    this.savingEdit = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateDialysisPlan(this.editingPlanId, {
      firstName,
      lastName,
      doctorId,
      dialysisType,
      sessionsPerWeek,
      sessionDurationMinutes,
      startDate,
      endDate,
      daysOfWeek,
      bloodFlowRate,
      dialysateFlowRate,
      ultrafiltrationGoal,
      dialysisCenterId,
      roomNumber,
      machineId,
      status
    }).subscribe({
      next: () => {
        this.savingEdit = false;
        this.showEditModal = false;
        this.editingPlanId = null;
        this.successMessage = 'Dialysis plan updated successfully.';
        this.refreshView();
        this.loadPlans();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.savingEdit = false;
        this.errorMessage = this.formatApiError(err, 'Failed to update dialysis plan.');
        this.refreshView();
      }
    });
  }

  archivePlan(plan: DialysisPlan): void {
    if (plan.status === 'ARCHIVED') {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateDialysisPlan(plan.id, this.buildPlanPayload(plan, 'ARCHIVED')).subscribe({
      next: () => {
        this.successMessage = 'Dialysis plan archived successfully.';
        this.refreshView();
        this.loadPlans();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to archive dialysis plan.');
        this.refreshView();
      }
    });
  }

  private buildPlanPayload(plan: DialysisPlan, statusOverride?: string) {
    return {
      firstName: plan.firstName ?? '',
      lastName: plan.lastName ?? '',
      doctorId: plan.doctorId ?? '',
      dialysisType: plan.dialysisType ?? '',
      sessionsPerWeek: Number(plan.sessionsPerWeek ?? 0),
      sessionDurationMinutes: Number(plan.sessionDurationMinutes ?? 0),
      startDate: plan.startDate ?? '',
      endDate: plan.endDate ?? null,
      daysOfWeek: plan.daysOfWeek ?? '',
      bloodFlowRate: plan.bloodFlowRate ?? null,
      dialysateFlowRate: plan.dialysateFlowRate ?? null,
      ultrafiltrationGoal: plan.ultrafiltrationGoal ?? null,
      dialysisCenterId: plan.dialysisCenterId ?? null,
      roomNumber: plan.roomNumber ?? null,
      machineId: plan.machineId ?? null,
      status: statusOverride ?? plan.status ?? 'PLANNED'
    };
  }

  downloadPlanPdf(plan: DialysisPlan): void {
    this.downloadingPdfId = plan.id;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.downloadDialysisPlanSummaryPdf(plan.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `dialysis-plan-${plan.id}-summary.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.downloadingPdfId = null;
        this.successMessage = 'Dialysis plan PDF exported successfully.';
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.downloadingPdfId = null;
        this.errorMessage = this.formatApiError(err, 'Failed to export dialysis plan PDF.');
        this.refreshView();
      }
    });
  }

  private refreshView(): void {
    this.cdr.markForCheck();
  }

  formatDoctorLabel(doctor: DoctorSearchResult | null | undefined): string {
    if (!doctor) {
      return '';
    }
    const fullName = `${doctor.firstName ?? ''} ${doctor.lastName ?? ''}`.trim();
    return fullName || doctor.username || doctor.email || String(doctor.id ?? doctor.keycloakId ?? '');
  }

  formatPatientLabel(patient: PatientLookupItem | null | undefined): string {
    if (!patient) {
      return '';
    }
    const fullName = `${patient.firstName ?? ''} ${patient.lastName ?? ''}`.trim();
    return `${fullName} | #${patient.id}`;
  }

  onPatientSelectionChange(value: string): void {
    const patientId = Number(value);
    const patient = this.availablePatients.find((item) => item.id === patientId) ?? null;
    if (!patient) {
      this.clearSelectedPatient();
      return;
    }

    this.selectedPatient = patient;
    this.patientError = '';
    this.createForm.patientId = String(patient.id);
    this.createForm.firstName = patient.firstName ?? '';
    this.createForm.lastName = patient.lastName ?? '';
    this.refreshView();
  }

  onDoctorSelectionChange(value: string): void {
    const selectedId = value.trim();
    const doctor = this.availableDoctors.find(
      (doctor) => String(doctor.keycloakId ?? doctor.id ?? '').trim() === selectedId
    ) ?? null;
    if (this.showEditModal) {
      this.editForm.doctorId = selectedId;
      this.selectedDoctor = doctor;
    } else {
      this.createForm.doctorId = selectedId;
      this.selectedDoctor = doctor;
    }
    this.refreshView();
  }

  availableStatusesFor(plan: DialysisPlan): string[] {
    return this.allowedStatusTransitions[plan.status] ?? [plan.status];
  }

  canArchivePlan(plan: DialysisPlan): boolean {
    return this.availableStatusesFor(plan).includes('ARCHIVED') && plan.status !== 'ARCHIVED';
  }

  private loadPatients(): void {
    this.patientLoading = true;
    this.patientError = '';

    this.clinicalApi.listPatients().subscribe({
      next: (patients) => {
        this.availablePatients = (patients ?? [])
          .map((item) => this.normalizePatient(item))
          .filter((item) => item.id > 0)
          .sort((a, b) => this.formatPatientLabel(a).localeCompare(this.formatPatientLabel(b)));
        this.patientLoading = false;
        this.refreshView();
      },
      error: () => {
        this.availablePatients = [];
        this.patientLoading = false;
        this.patientError = 'Failed to load patients.';
        this.refreshView();
      }
    });
  }

  private loadDoctors(): void {
    this.doctorLoading = true;
    this.doctorError = '';

    this.clinicalApi.listDoctors(50).subscribe({
      next: (doctors) => {
        this.availableDoctors = doctors ?? [];
        this.doctorLoading = false;
        this.refreshView();
      },
      error: () => {
        this.availableDoctors = [];
        this.doctorLoading = false;
        this.doctorError = 'Failed to load doctors.';
        this.refreshView();
      }
    });
  }

  private clearSelectedPatient(): void {
    this.selectedPatient = null;
    this.patientError = '';
    this.createForm.patientId = '';
    this.createForm.firstName = '';
    this.createForm.lastName = '';
    this.refreshView();
  }

  private normalizePatient(item: any): PatientLookupItem {
    return {
      id: Number(item?.id ?? 0),
      firstName: String(item?.firstName ?? '').trim(),
      lastName: String(item?.lastName ?? '').trim(),
      dateOfBirth: item?.dateOfBirth ?? null,
      sex: item?.sex ?? null,
      bloodType: item?.bloodType ?? null,
      allergies: item?.allergies ?? null
    };
  }

  private formatApiError(
    err: { error?: { message?: string }; message?: string },
    fallback: string
  ): string {
    const message = err?.error?.message || err?.message || '';
    if (message.includes('503') || message.includes('Service Unavailable')) {
      return 'Procedure service is temporarily unavailable. Verify procedure-service and the API Gateway, then retry.';
    }
    return message || fallback;
  }
}
