import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { ProcedureApiService, SurgicalCase, SurgeryRequest } from '../../../core/services/procedure-api.service';
import { ClinicalApiService, DoctorSearchResult } from '../../../core/services/clinical-api.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

interface PatientLookupItem {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  sex?: 'MALE' | 'FEMALE' | string | null;
  bloodType?: string | null;
  allergies?: string | null;
}

interface SurgicalProcedureCatalogItem {
  type: string;
  procedures: string[];
}

@Component({
  selector: 'app-procedure-surgical',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './procedure-surgical.html',
  styleUrl: './procedure-surgical.scss'
})
export class ProcedureSurgicalComponent implements OnInit, OnDestroy {
  private readonly allowedStatusTransitions: Record<string, string[]> = {
    OPEN: ['OPEN', 'READY_FOR_INTERVENTION', 'BLOCKED_PREOP', 'CANCELLED'],
    READY_FOR_INTERVENTION: ['READY_FOR_INTERVENTION', 'IN_PROGRESS', 'CANCELLED'],
    IN_PROGRESS: ['IN_PROGRESS', 'POSTOP_STABLE', 'POSTOP_UNSTABLE', 'DONE', 'CANCELLED'],
    POSTOP_STABLE: ['POSTOP_STABLE', 'DONE', 'ARCHIVED'],
    POSTOP_UNSTABLE: ['POSTOP_UNSTABLE', 'ARCHIVED'],
    BLOCKED_PREOP: ['BLOCKED_PREOP', 'ARCHIVED'],
    DONE: ['DONE', 'ARCHIVED'],
    CANCELLED: ['CANCELLED', 'ARCHIVED'],
    ARCHIVED: ['ARCHIVED']
  };

  loading = false;
  saving = false;
  sendingWhatsAppTest = false;
  downloadingPdfId: number | null = null;
  createAttempted = false;
  errorMessage = '';
  successMessage = '';

  cases: SurgicalCase[] = [];
  searchTerm = '';
  caseStatusFilter = 'ALL';
  showArchived = false;
  readonly caseStatuses = [
    'OPEN',
    'READY_FOR_INTERVENTION',
    'BLOCKED_PREOP',
    'IN_PROGRESS',
    'POSTOP_STABLE',
    'POSTOP_UNSTABLE',
    'DONE',
    'CANCELLED',
    'ARCHIVED'
  ];
  readonly editableCaseStatuses = [
    'OPEN',
    'READY_FOR_INTERVENTION',
    'BLOCKED_PREOP',
    'IN_PROGRESS',
    'POSTOP_STABLE',
    'POSTOP_UNSTABLE',
    'DONE',
    'CANCELLED',
    'ARCHIVED'
  ];
  readonly offerStatuses = ['PENDING', 'ACCEPTED', 'REJECTED'];
  readonly surgeryCategories = ['MINOR_SURGERY', 'MAJOR_SURGERY'];
  readonly urgencyLevels = ['SCHEDULED', 'URGENT'];
  readonly genders = ['MALE', 'FEMALE'];
  readonly surgeryCatalog: SurgicalProcedureCatalogItem[] = [
    {
      type: 'Kidney Transplant Surgery',
      procedures: [
        'Living Donor Kidney Transplant',
        'Deceased Donor Kidney Transplant',
        'Transplant Revision Surgery',
        'Transplant Graft Exploration'
      ]
    },
    {
      type: 'Dialysis Access Surgery',
      procedures: [
        'Peritoneal Dialysis Catheter Placement',
        'Peritoneal Dialysis Catheter Revision',
        'Hemodialysis Catheter Insertion',
        'Hemodialysis Catheter Exchange',
        'Hemodialysis Catheter Removal'
      ]
    },
    {
      type: 'Pediatric Urologic Surgery',
      procedures: [
        'Pyeloplasty',
        'Ureteral Reimplantation',
        'Nephrectomy',
        'Ureteroscopy',
        'Cystoscopy'
      ]
    },
    {
      type: 'Renal Biopsy Procedure',
      procedures: [
        'Open Renal Biopsy',
        'Laparoscopic Renal Biopsy',
        'Biopsy Site Hemostasis'
      ]
    },
    {
      type: 'Vascular Access Procedure',
      procedures: [
        'Arteriovenous Fistula Creation',
        'Arteriovenous Fistula Revision',
        'Central Line Placement',
        'Central Line Revision'
      ]
    }
  ];
  readonly otherProcedureOption = 'Other';
  readonly caseStatusLabels: Record<string, string> = {
    OPEN: 'Open',
    READY_FOR_INTERVENTION: 'Ready for Intervention',
    BLOCKED_PREOP: 'Blocked (Pre-Op)',
    IN_PROGRESS: 'In Progress',
    POSTOP_STABLE: 'Post-Op Stable',
    POSTOP_UNSTABLE: 'Post-Op Unstable',
    DONE: 'Done',
    CANCELLED: 'Cancelled',
    ARCHIVED: 'Archived'
  };
  readonly offerStatusLabels: Record<string, string> = {
    PENDING: 'Pending',
    ACCEPTED: 'Accepted',
    REJECTED: 'Rejected'
  };
  readonly surgeryCategoryLabels: Record<string, string> = {
    MINOR_SURGERY: 'Minor Surgery',
    MAJOR_SURGERY: 'Major Surgery'
  };
  readonly urgencyLabels: Record<string, string> = {
    SCHEDULED: 'Scheduled',
    URGENT: 'Urgent'
  };
  readonly genderLabels: Record<string, string> = {
    MALE: 'Male',
    FEMALE: 'Female'
  };
  readonly workflowSteps = [
    { code: 'OPEN', label: 'Registration' },
    { code: 'READY_FOR_INTERVENTION', label: 'Ready' },
    { code: 'IN_PROGRESS', label: 'In Progress' },
    { code: 'POSTOP_STABLE', label: 'Post-Op Stable' },
    { code: 'DONE', label: 'Completed' }
  ];
  selectedPatient: PatientLookupItem | null = null;
  availablePatients: PatientLookupItem[] = [];
  surgeryRequests: SurgeryRequest[] = [];
  requestStatusFilter = 'ALL';
  readonly surgeryRequestStatuses = ['PENDING', 'PLANNED', 'REJECTED', 'CANCELLED'];
  editRequestStatus: Record<number, string> = {};
  selectedWorkspaceCaseId = '';
  workspaceSection: 'overview' | 'patient' | 'referral' | 'planning' = 'overview';
  patientLoading = false;
  patientError = '';
  availableSurgeons: DoctorSearchResult[] = [];
  selectedSurgeon: DoctorSearchResult | null = null;
  surgeonLoading = false;
  surgeonError = '';
  role = '';

  createForm = {
    patientId: '',
    consultationId: '',
    surgeryRequestId: null as number | null,
    firstName: '',
    lastName: '',
    age: 0,
    gender: 'MALE',
    medicalRecordNumber: '',
    surgeryType: '',
    procedureName: '',
    customProcedureName: '',
    surgeryCategory: 'MAJOR_SURGERY',
    urgencyLevel: 'SCHEDULED',
    surgeonId: '',
    assistantSurgeonId: '',
    anesthesiologistId: '',
    nurseTeam: '',
    scheduledDate: '',
    scheduledStartTime: '',
    estimatedDurationMinutes: 180,
    operatingRoom: '',
    status: 'OPEN'
  };

  editStatus: Record<number, string> = {};
  editOfferStatus: Record<number, string> = {};
  private readonly destroy$ = new Subject<void>();

  constructor(
    private authStorage: AuthStorageService,
    private procedureApi: ProcedureApiService,
    private clinicalApi: ClinicalApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.role = this.authStorage.getRole() ?? '';
    this.loadPatients();
    this.loadSurgeons();
    this.loadSurgeryRequests();
    this.loadCases();
  }

  get isReceptionist(): boolean {
    return this.role === 'RECEPTIONIST';
  }

  get isSurgeon(): boolean {
    return this.role === 'SURGEON';
  }

  get canPlanCases(): boolean {
    return this.isReceptionist || this.isSurgeon;
  }

  get canManageWorkflow(): boolean {
    return this.isSurgeon;
  }

  get pageTitle(): string {
    return this.isReceptionist ? 'Surgical Planning Desk' : 'Surgical Management';
  }

  get intakeTitle(): string {
    return 'Plan Surgical Case';
  }

  get intakeSummaryTitle(): string {
    return 'Planning workspace';
  }

  get intakeSummaryText(): string {
    return 'Use this panel to transform a validated surgery request into a scheduled surgical case.';
  }

  get workspaceTitle(): string {
    return this.isReceptionist ? 'Planned Case Review' : 'Surgeon Workspace';
  }

  get workspaceSubtitle(): string {
    return this.isReceptionist
      ? 'Review the patient snapshot and the final scheduling data after a case has been created.'
      : 'Read-only patient and referral context for the selected surgical case.';
  }

  get boardTitle(): string {
    return this.isSurgeon ? 'My Surgical Cases' : 'Scheduled Surgical Cases';
  }

  get boardSubtitle(): string {
    return this.isSurgeon
      ? 'Only cases assigned to you are shown here for intra-op and follow-up actions.'
      : 'Track created cases, review their schedule, and confirm what has already been handed off to the surgical team.';
  }

  get currentWorkflowSteps(): Array<{ code: string; label: string; detail: string }> {
    if (this.isReceptionist) {
      return [
        { code: 'REQUEST', label: 'Review Request', detail: 'Validate referral, urgency and consultation context.' },
        { code: 'ASSIGN', label: 'Assign Surgeon', detail: 'Choose the responsible surgeon and supporting team.' },
        { code: 'SCHEDULE', label: 'Schedule Slot', detail: 'Set date, time, room and estimated duration.' },
        { code: 'CREATE', label: 'Create Case', detail: 'Generate the surgical case for surgeon handoff.' }
      ];
    }

    return [
      { code: 'OPEN', label: 'Operation Review', detail: 'Check planning, referral and patient file.' },
      { code: 'READY_FOR_INTERVENTION', label: 'Pre-Op Ready', detail: 'Proceed once pre-op clearance is complete.' },
      { code: 'IN_PROGRESS', label: 'Intervention', detail: 'Track the intra-operative execution state.' },
      { code: 'POSTOP_STABLE', label: 'Recovery', detail: 'Follow the patient after the operation.' }
    ];
  }

  get planningQueueCount(): number {
    return this.surgeryRequests.filter((item) => item.status === 'PENDING').length;
  }

  get plannedRequestsCount(): number {
    return this.surgeryRequests.filter((item) => item.status === 'PLANNED').length;
  }

  get myScheduledCases(): SurgicalCase[] {
    return [...this.roleScopedCases]
      .filter((item) => item.status !== 'ARCHIVED' && item.status !== 'CANCELLED')
      .sort((a, b) => this.caseDateTimeValue(a) - this.caseDateTimeValue(b));
  }

  get myTodayCases(): SurgicalCase[] {
    const today = this.todayIsoDate();
    return this.myScheduledCases.filter((item) => item.scheduledDate === today);
  }

  get myUpcomingCases(): SurgicalCase[] {
    const today = this.todayIsoDate();
    return this.myScheduledCases.filter((item) => !!item.scheduledDate && item.scheduledDate > today);
  }

  get needsAttentionCases(): SurgicalCase[] {
    return this.myScheduledCases.filter((item) =>
      item.urgencyLevel === 'URGENT'
      || item.status === 'BLOCKED_PREOP'
      || item.status === 'POSTOP_UNSTABLE'
      || !String(item.operatingRoom ?? '').trim()
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredCases(): SurgicalCase[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.roleScopedCases.filter((surgicalCase) => {
      const isArchived = surgicalCase.status === 'ARCHIVED';
      if (!this.showArchived && isArchived) {
        return false;
      }

      const fullName = `${surgicalCase.firstName ?? ''} ${surgicalCase.lastName ?? ''}`.trim().toLowerCase();
      const surgeryType = (surgicalCase.surgeryType ?? '').toLowerCase();
      const procedureName = (surgicalCase.procedureName ?? '').toLowerCase();
      const medicalRecordNumber = (surgicalCase.medicalRecordNumber ?? '').toLowerCase();
      const operatingRoom = (surgicalCase.operatingRoom ?? '').toLowerCase();
      const matchesSearch =
        !term ||
        fullName.includes(term) ||
        surgeryType.includes(term) ||
        procedureName.includes(term) ||
        medicalRecordNumber.includes(term) ||
        operatingRoom.includes(term);
      const matchesCaseStatus =
        this.caseStatusFilter === 'ALL' || surgicalCase.status === this.caseStatusFilter;
      return matchesSearch && matchesCaseStatus;
    });
  }

  get totalCases(): number {
    return this.roleScopedCases.length;
  }

  get activeCasesCount(): number {
    return this.roleScopedCases.filter((item) => item.status !== 'ARCHIVED' && item.status !== 'DONE' && item.status !== 'CANCELLED').length;
  }

  get blockedCasesCount(): number {
    return this.roleScopedCases.filter((item) => item.status === 'BLOCKED_PREOP' || item.status === 'POSTOP_UNSTABLE').length;
  }

  get archivedCasesCount(): number {
    return this.roleScopedCases.filter((item) => item.status === 'ARCHIVED').length;
  }

  get plannedCasesCount(): number {
    return this.roleScopedCases.filter((item) => item.status === 'OPEN' || item.status === 'READY_FOR_INTERVENTION').length;
  }

  get todayCasesCount(): number {
    const today = this.todayIsoDate();
    return this.roleScopedCases.filter((item) => item.scheduledDate === today && item.status !== 'CANCELLED' && item.status !== 'ARCHIVED').length;
  }

  get upcomingCasesCount(): number {
    const today = this.todayIsoDate();
    return this.roleScopedCases.filter((item) => !!item.scheduledDate && item.scheduledDate > today && item.status !== 'CANCELLED' && item.status !== 'ARCHIVED').length;
  }

  get filteredSurgeryRequests(): SurgeryRequest[] {
    return this.surgeryRequests.filter((item) =>
      this.requestStatusFilter === 'ALL' || item.status === this.requestStatusFilter
    );
  }

  get selectedPlanningRequest(): SurgeryRequest | undefined {
    const requestId = this.createForm.surgeryRequestId;
    if (!requestId) {
      return undefined;
    }
    return this.surgeryRequests.find((item) => item.id === requestId);
  }

  get selectedWorkspaceCase(): SurgicalCase | undefined {
    const id = Number(this.selectedWorkspaceCaseId);
    return this.roleScopedCases.find((item) => item.id === id) ?? this.cases.find((item) => item.id === id);
  }

  get selectedWorkspacePatient(): PatientLookupItem | undefined {
    const surgicalCase = this.selectedWorkspaceCase;
    if (!surgicalCase) {
      return undefined;
    }
    return this.availablePatients.find((item) => String(item.id) === String(surgicalCase.patientId));
  }

  get selectedWorkspaceRequest(): SurgeryRequest | undefined {
    const surgicalCase = this.selectedWorkspaceCase;
    if (!surgicalCase?.surgeryRequestId) {
      return undefined;
    }
    return this.surgeryRequests.find((item) => item.id === surgicalCase.surgeryRequestId);
  }

  get selectedWorkspacePatientName(): string {
    const surgicalCase = this.selectedWorkspaceCase;
    if (!surgicalCase) {
      return '-';
    }
    return `${surgicalCase.firstName || ''} ${surgicalCase.lastName || ''}`.trim() || '-';
  }

  get selectedWorkspacePatientAge(): string {
    const age = Number(this.selectedWorkspaceCase?.age ?? NaN);
    return Number.isFinite(age) && age >= 0 ? `${age} years` : '-';
  }

  get selectedWorkspaceDob(): string {
    const dateOfBirth = this.selectedWorkspacePatient?.dateOfBirth;
    if (!dateOfBirth) {
      return '-';
    }
    const parsed = new Date(dateOfBirth);
    return Number.isNaN(parsed.getTime()) ? dateOfBirth : parsed.toLocaleDateString();
  }

  get selectedWorkspaceStatusSummary(): string {
    const surgicalCase = this.selectedWorkspaceCase;
    if (!surgicalCase) {
      return '-';
    }
    return this.workflowStepLabel(surgicalCase);
  }

  get selectedWorkspaceOperationSummary(): Array<{ label: string; value: string }> {
    const surgicalCase = this.selectedWorkspaceCase;
    if (!surgicalCase) {
      return [];
    }

    return [
      { label: 'Procedure', value: surgicalCase.procedureName || '-' },
      { label: 'Surgery type', value: surgicalCase.surgeryType || '-' },
      { label: 'Schedule', value: this.formatDateTime(surgicalCase.scheduledDate, surgicalCase.scheduledStartTime) },
      { label: 'Operating room', value: surgicalCase.operatingRoom || '-' },
      { label: 'Lead surgeon', value: surgicalCase.surgeonId || '-' },
      { label: 'Status', value: this.labelFrom(this.caseStatusLabels, surgicalCase.status) }
    ];
  }

  get selectedWorkspaceAttentionItems(): string[] {
    const surgicalCase = this.selectedWorkspaceCase;
    const items: string[] = [];
    if (!surgicalCase) {
      return items;
    }

    if (surgicalCase.urgencyLevel === 'URGENT') {
      items.push('Urgent case requiring priority scheduling and pre-op validation.');
    }
    if (surgicalCase.status === 'BLOCKED_PREOP') {
      items.push('Pre-op clearance is blocked. Do not proceed until blocking issues are resolved.');
    }
    if (surgicalCase.status === 'POSTOP_UNSTABLE') {
      items.push('Post-op instability flagged. Escalated monitoring is required.');
    }
    if (!String(surgicalCase.operatingRoom ?? '').trim()) {
      items.push('Operating room has not been assigned yet.');
    }
    if (!String(surgicalCase.nurseTeam ?? '').trim()) {
      items.push('Nurse team has not been documented yet.');
    }

    return items;
  }

  get selectedWorkspacePlanningChecklist(): Array<{ label: string; done: boolean; value: string }> {
    const surgicalCase = this.selectedWorkspaceCase;
    const request = this.selectedWorkspaceRequest;
    if (!surgicalCase) {
      return [];
    }

    return [
      {
        label: 'Referral linked',
        done: !!request,
        value: request ? `Request #${request.id}` : 'No linked request'
      },
      {
        label: 'Operating room',
        done: !!String(surgicalCase.operatingRoom ?? '').trim(),
        value: surgicalCase.operatingRoom || 'Missing'
      },
      {
        label: 'Lead surgeon',
        done: !!String(surgicalCase.surgeonId ?? '').trim(),
        value: surgicalCase.surgeonId || 'Missing'
      },
      {
        label: 'Schedule fixed',
        done: !!String(surgicalCase.scheduledDate ?? '').trim() && !!String(surgicalCase.scheduledStartTime ?? '').trim(),
        value: this.formatDateTime(surgicalCase.scheduledDate, surgicalCase.scheduledStartTime)
      },
      {
        label: 'Nurse team',
        done: !!String(surgicalCase.nurseTeam ?? '').trim(),
        value: surgicalCase.nurseTeam || 'Missing'
      }
    ];
  }

  get generatedPatientReference(): string {
    if (this.selectedPatient?.id) {
      return `PAT-${this.selectedPatient.id}`;
    }
    const firstName = this.createForm.firstName.trim().toUpperCase().slice(0, 2) || 'PT';
    const lastName = this.createForm.lastName.trim().toUpperCase().slice(0, 2) || 'XX';
    return `${firstName}${lastName}-${new Date().getFullYear()}`;
  }

  get isCreateFormValid(): boolean {
    return Object.values(this.createFormErrors()).every((value) => !value);
  }

  get availableProcedureNames(): string[] {
    const selectedType = this.createForm.surgeryType.trim();
    const item = this.surgeryCatalog.find((entry) => entry.type === selectedType);
    return item ? [...item.procedures, this.otherProcedureOption] : [];
  }

  get requiresCustomProcedureName(): boolean {
    return this.createForm.procedureName === this.otherProcedureOption;
  }

  loadCases(): void {
    this.loading = true;
    this.errorMessage = '';

    this.procedureApi.getSurgicalCases().subscribe({
      next: (cases) => {
        this.cases = cases ?? [];
        this.editStatus = {};
        this.editOfferStatus = {};

        for (const surgicalCase of this.cases) {
          this.editStatus[surgicalCase.id] = surgicalCase.status;
          this.editOfferStatus[surgicalCase.id] = surgicalCase.offerStatus;
        }

        this.syncSelectedWorkspaceCase();
        this.loading = false;
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err, 'Failed to load surgical cases.');
        this.refreshView();
      }
    });
  }

  loadSurgeryRequests(): void {
    this.procedureApi.getSurgeryRequests().subscribe({
      next: (items) => {
        this.surgeryRequests = items ?? [];
        this.editRequestStatus = {};
        for (const request of this.surgeryRequests) {
          this.editRequestStatus[request.id] = request.status;
        }
        this.refreshView();
      },
      error: () => {
        this.surgeryRequests = [];
        this.refreshView();
      }
    });
  }

  createCase(): void {
    this.createAttempted = true;
    const formErrors = this.createFormErrors();
    const firstError = Object.values(formErrors).find(Boolean);
    if (firstError) {
      this.errorMessage = firstError;
      this.successMessage = '';
      this.refreshView();
      return;
    }

    const firstName = this.createForm.firstName.trim();
    const lastName = this.createForm.lastName.trim();
    const age = Number(this.createForm.age);
    const gender = this.createForm.gender.trim();
    const medicalRecordNumber = this.generatedPatientReference;
    const surgeryType = this.createForm.surgeryType.trim();
    const procedureName = this.requiresCustomProcedureName
      ? this.createForm.customProcedureName.trim()
      : this.createForm.procedureName.trim();
    const surgeryCategory = this.createForm.surgeryCategory.trim();
    const urgencyLevel = this.createForm.urgencyLevel.trim();
    const surgeonId = this.createForm.surgeonId.trim();
    const assistantSurgeonId = this.createForm.assistantSurgeonId.trim() || null;
    const anesthesiologistId = this.createForm.anesthesiologistId.trim() || null;
    const nurseTeam = this.createForm.nurseTeam.trim() || null;
    const scheduledDate = this.createForm.scheduledDate;
    const scheduledStartTime = this.createForm.scheduledStartTime;
    const estimatedDurationMinutes = Number(this.createForm.estimatedDurationMinutes);
    const operatingRoom = this.createForm.operatingRoom.trim() || null;
    const status = 'OPEN';

    const patientId = this.createForm.patientId.trim();
    const consultationId = this.createForm.consultationId.trim() || null;
    const surgeryRequestId = this.createForm.surgeryRequestId;

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.createSurgicalCase({
      patientId,
      consultationId,
      surgeryRequestId,
      firstName,
      lastName,
      age,
      gender,
      medicalRecordNumber,
      surgeryType,
      procedureName,
      surgeryCategory,
      urgencyLevel,
      surgeonId,
      assistantSurgeonId,
      anesthesiologistId,
      nurseTeam,
      scheduledDate,
      scheduledStartTime,
      estimatedDurationMinutes,
      operatingRoom,
      status
    }).subscribe({
      next: () => {
        this.createForm = {
          patientId: '',
          consultationId: '',
          surgeryRequestId: null,
          firstName: '',
          lastName: '',
          age: 0,
          gender: 'MALE',
          medicalRecordNumber: '',
          surgeryType: '',
          procedureName: '',
          customProcedureName: '',
          surgeryCategory: 'MAJOR_SURGERY',
          urgencyLevel: 'SCHEDULED',
          surgeonId: '',
          assistantSurgeonId: '',
          anesthesiologistId: '',
          nurseTeam: '',
          scheduledDate: '',
          scheduledStartTime: '',
          estimatedDurationMinutes: 180,
          operatingRoom: '',
          status: 'OPEN'
        };
        this.selectedPatient = null;
        this.selectedSurgeon = null;
        this.patientError = '';
        this.surgeonError = '';
        this.createAttempted = false;
        this.successMessage = this.isReceptionist
          ? 'Surgical case created and linked to the planning request.'
          : 'Surgical case created successfully. WhatsApp alert triggered for the surgical coordination scenario.';
        this.saving = false;
        this.refreshView();
        this.loadSurgeryRequests();
        this.loadCases();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.saving = false;
        this.errorMessage = this.formatApiError(err, 'Failed to create surgical case.');
        this.refreshView();
      }
    });
  }

  sendWhatsAppTest(): void {
    if (this.sendingWhatsAppTest) {
      return;
    }

    this.sendingWhatsAppTest = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.sendWhatsAppTestMessage().subscribe({
      next: () => {
        this.sendingWhatsAppTest = false;
        this.successMessage = 'WhatsApp test message sent successfully.';
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.sendingWhatsAppTest = false;
        this.errorMessage = this.formatApiError(err, 'Failed to send WhatsApp test message.');
        this.refreshView();
      }
    });
  }

  updateStatus(surgicalCase: SurgicalCase): void {
    const status = (this.editStatus[surgicalCase.id] ?? '').trim().toUpperCase();

    if (!status) {
      this.errorMessage = 'Case status is required.';
      this.refreshView();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateSurgicalCase(surgicalCase.id, { status }).subscribe({
      next: () => {
        this.successMessage = 'Surgical case status updated.';
        this.refreshView();
        this.loadCases();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to update surgical case.');
        this.refreshView();
      }
    });
  }

  updateOffer(surgicalCase: SurgicalCase): void {
    const offerStatus = (this.editOfferStatus[surgicalCase.id] ?? '').trim().toUpperCase();

    if (!offerStatus) {
      this.errorMessage = 'Offer status is required.';
      this.refreshView();
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateTransplantOffer(surgicalCase.id, { offerStatus }).subscribe({
      next: () => {
        this.successMessage = 'Transplant offer updated.';
        this.refreshView();
        this.loadCases();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to update transplant offer.');
        this.refreshView();
      }
    });
  }

  updateSurgeryRequestStatus(request: SurgeryRequest): void {
    const status = (this.editRequestStatus[request.id] ?? '').trim().toUpperCase();
    if (!status) {
      this.errorMessage = 'Surgery request status is required.';
      this.refreshView();
      return;
    }

    this.procedureApi.updateSurgeryRequestStatus(request.id, { status }).subscribe({
      next: () => {
        this.successMessage = 'Surgery request status updated.';
        this.refreshView();
        this.loadSurgeryRequests();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to update surgery request.');
        this.refreshView();
      }
    });
  }

  startPlanningRequest(request: SurgeryRequest): void {
    const patient = this.availablePatients.find((item) => String(item.id) === String(request.patientId)) ?? null;
    this.selectedPatient = patient;
    this.patientError = '';

    this.createForm.patientId = request.patientId;
    this.createForm.consultationId = request.consultationId;
    this.createForm.surgeryRequestId = request.id;
    this.createForm.firstName = request.patientFirstName;
    this.createForm.lastName = request.patientLastName;
    this.createForm.medicalRecordNumber = this.generatedPatientReference;
    this.createForm.surgeryType = '';
    this.createForm.procedureName = this.otherProcedureOption;
    this.createForm.customProcedureName = request.reason;
    this.createForm.surgeryCategory = 'MAJOR_SURGERY';
    this.createForm.urgencyLevel = request.urgencyLevel || 'SCHEDULED';
    this.createForm.surgeonId = '';
    this.createForm.assistantSurgeonId = '';
    this.createForm.anesthesiologistId = '';
    this.createForm.nurseTeam = '';
    this.createForm.scheduledDate = '';
    this.createForm.scheduledStartTime = '';
    this.createForm.estimatedDurationMinutes = 180;
    this.createForm.operatingRoom = '';
    this.selectedSurgeon = null;

    if (patient) {
      const age = this.patientAge(patient.dateOfBirth);
      this.createForm.age = age ?? 0;
      this.createForm.gender = this.mapPatientGender(patient.sex);
    } else {
      this.createForm.age = 0;
      this.createForm.gender = 'MALE';
    }

    this.createAttempted = false;
    this.errorMessage = '';
    this.successMessage = `Planning surgery request #${request.id}. Complete the scheduling fields, then create the surgical case.`;
    this.refreshView();
  }

  clearPlanningRequest(): void {
    this.clearSelectedPatient();
    this.createForm.consultationId = '';
    this.createForm.surgeryRequestId = null;
    this.createForm.procedureName = '';
    this.createForm.customProcedureName = '';
    this.createForm.medicalRecordNumber = '';
    this.createForm.urgencyLevel = 'SCHEDULED';
    this.createForm.surgeonId = '';
    this.createForm.assistantSurgeonId = '';
    this.createForm.anesthesiologistId = '';
    this.createForm.nurseTeam = '';
    this.createForm.scheduledDate = '';
    this.createForm.scheduledStartTime = '';
    this.createForm.estimatedDurationMinutes = 180;
    this.createForm.operatingRoom = '';
    this.selectedSurgeon = null;
    this.successMessage = '';
    this.refreshView();
  }

  openWorkspace(surgicalCase: SurgicalCase): void {
    this.selectedWorkspaceCaseId = String(surgicalCase.id);
    this.workspaceSection = 'overview';
    this.errorMessage = '';
    this.successMessage = `Workspace opened for ${surgicalCase.firstName} ${surgicalCase.lastName}.`;
    this.refreshView();
  }

  setWorkspaceSection(section: 'overview' | 'patient' | 'referral' | 'planning'): void {
    this.workspaceSection = section;
    this.refreshView();
  }

  archiveCase(surgicalCase: SurgicalCase): void {
    if (surgicalCase.status === 'ARCHIVED') {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.updateSurgicalCase(surgicalCase.id, { status: 'ARCHIVED' }).subscribe({
      next: () => {
        this.successMessage = 'Surgical case archived successfully.';
        this.refreshView();
        this.loadCases();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.errorMessage = this.formatApiError(err, 'Failed to archive surgical case.');
        this.refreshView();
      }
    });
  }

  labelFrom(labels: Record<string, string>, value: string | null | undefined): string {
    if (!value) {
      return '-';
    }
    return labels[value] ?? value;
  }

  caseDecisionLabel(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.status) {
      case 'OPEN':
        return 'Apply Intake Decision';
      case 'READY_FOR_INTERVENTION':
        return 'Confirm Workflow Step';
      case 'IN_PROGRESS':
        return 'Record Procedure Outcome';
      case 'POSTOP_STABLE':
      case 'POSTOP_UNSTABLE':
        return 'Apply Recovery Decision';
      case 'DONE':
        return 'Finalize Case Status';
      case 'CANCELLED':
        return 'Confirm Cancellation';
      case 'ARCHIVED':
        return 'Archived';
      default:
        return 'Apply Status Decision';
    }
  }

  offerDecisionLabel(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.offerStatus) {
      case 'PENDING':
        return 'Apply Offer Decision';
      case 'ACCEPTED':
        return 'Confirm Offer Acceptance';
      case 'REJECTED':
        return 'Confirm Offer Rejection';
      default:
        return 'Apply Offer Decision';
    }
  }

  caseDecisionHint(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.status) {
      case 'OPEN':
        return 'Move the file from intake to a pre-op outcome.';
      case 'READY_FOR_INTERVENTION':
        return 'Confirm readiness before surgery begins.';
      case 'IN_PROGRESS':
        return 'Register the immediate post-procedure outcome.';
      case 'POSTOP_STABLE':
        return 'Close the case when recovery documentation is complete.';
      case 'POSTOP_UNSTABLE':
        return 'Keep the case under escalation or controlled closure.';
      case 'DONE':
        return 'Only archive once the surgical file is complete.';
      case 'CANCELLED':
        return 'Keep the cancellation aligned with the final record.';
      default:
        return 'Apply the next workflow decision for this case.';
    }
  }

  offerDecisionHint(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.offerStatus) {
      case 'PENDING':
        return 'Review and record the transplant offer outcome.';
      case 'ACCEPTED':
        return 'Offer already accepted and should stay aligned with the file.';
      case 'REJECTED':
        return 'Offer already rejected and should stay aligned with the file.';
      default:
        return 'Apply the offer decision for this case.';
    }
  }

  downloadCasePdf(surgicalCase: SurgicalCase): void {
    this.downloadingPdfId = surgicalCase.id;
    this.errorMessage = '';
    this.successMessage = '';

    this.procedureApi.downloadSurgicalCaseSummaryPdf(surgicalCase.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `surgical-case-${surgicalCase.id}-summary.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.downloadingPdfId = null;
        this.successMessage = 'Surgical case PDF exported successfully.';
        this.refreshView();
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.downloadingPdfId = null;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to export surgical case PDF.';
        this.refreshView();
      }
    });
  }

  fieldError(field: keyof typeof this.createForm): string | null {
    if (!this.createAttempted) {
      return null;
    }
    return this.createFormErrors()[field];
  }

  onPatientSelectionChange(value: string): void {
    const patientId = Number(value);
    const patient = this.availablePatients.find((item) => item.id === patientId) ?? null;
    if (!patient) {
      this.clearSelectedPatient();
      return;
    }

    const age = this.patientAge(patient.dateOfBirth);
    this.selectedPatient = patient;
    this.patientError = '';

    this.createForm.patientId = String(patient.id);
    this.createForm.firstName = patient.firstName ?? '';
    this.createForm.lastName = patient.lastName ?? '';
    this.createForm.age = age ?? 0;
    this.createForm.gender = this.mapPatientGender(patient.sex);

    this.refreshView();
  }

  onSurgeryTypeChange(value: string): void {
    this.createForm.surgeryType = value;
    this.createForm.procedureName = '';
    this.createForm.customProcedureName = '';
    this.refreshView();
  }

  onSurgeonSelectionChange(value: string): void {
    const selectedId = value.trim();
    this.createForm.surgeonId = selectedId;
    this.selectedSurgeon = this.availableSurgeons.find(
      (surgeon) => String(surgeon.keycloakId ?? surgeon.id ?? '').trim() === selectedId
    ) ?? null;
    this.refreshView();
  }

  onProcedureNameChange(value: string): void {
    this.createForm.procedureName = value;
    if (value !== this.otherProcedureOption) {
      this.createForm.customProcedureName = '';
    }
    this.refreshView();
  }

  clearSelectedPatient(): void {
    this.selectedPatient = null;
    this.patientError = '';
    this.createForm.patientId = '';
    this.createForm.firstName = '';
    this.createForm.lastName = '';
    this.createForm.age = 0;
    this.createForm.gender = 'MALE';
    this.refreshView();
  }

  isFieldInvalid(field: keyof typeof this.createForm): boolean {
    return !!this.fieldError(field);
  }

  teamSummary(surgicalCase: SurgicalCase): string {
    const roles = [
      surgicalCase.surgeonId ? `Lead ${surgicalCase.surgeonId}` : null,
      surgicalCase.assistantSurgeonId ? `Assistant ${surgicalCase.assistantSurgeonId}` : null,
      surgicalCase.anesthesiologistId ? `Anesthesia ${surgicalCase.anesthesiologistId}` : null
    ].filter(Boolean);

    return roles.length ? roles.join(' | ') : 'Team not assigned';
  }

  workflowStepLabel(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.status) {
      case 'OPEN':
        return 'Waiting for pre-op validation';
      case 'READY_FOR_INTERVENTION':
        return 'Ready to enter operating workflow';
      case 'BLOCKED_PREOP':
        return 'Blocked until pre-op issues are resolved';
      case 'IN_PROGRESS':
        return 'Procedure currently underway';
      case 'POSTOP_STABLE':
        return 'Stable recovery monitoring';
      case 'POSTOP_UNSTABLE':
        return 'Critical follow-up required';
      case 'DONE':
        return 'Procedure successfully closed';
      case 'CANCELLED':
        return 'Case cancelled';
      case 'ARCHIVED':
        return 'Archived for history';
      default:
        return 'Workflow status not available';
    }
  }

  statusToneClass(status: string | null | undefined): string {
    switch (status) {
      case 'DONE':
      case 'READY_FOR_INTERVENTION':
      case 'POSTOP_STABLE':
        return 'bg-soft-success text-success';
      case 'BLOCKED_PREOP':
      case 'POSTOP_UNSTABLE':
      case 'CANCELLED':
        return 'bg-soft-danger text-danger';
      case 'IN_PROGRESS':
      case 'OPEN':
        return 'bg-soft-warning text-warning';
      case 'ARCHIVED':
        return 'bg-soft-secondary text-muted';
      default:
        return 'bg-soft-secondary text-muted';
    }
  }

  surgeryRequestStatusClass(status: string | null | undefined): string {
    if (status === 'PLANNED') {
      return 'bg-soft-success text-success';
    }
    if (status === 'REJECTED' || status === 'CANCELLED') {
      return 'bg-soft-danger text-danger';
    }
    return 'bg-soft-warning text-warning';
  }

  isPlanningRequestSelected(request: SurgeryRequest): boolean {
    return this.createForm.surgeryRequestId === request.id;
  }

  nextActionLabel(surgicalCase: SurgicalCase): string {
    switch (surgicalCase.status) {
      case 'OPEN':
        return 'Complete pre-op assessment';
      case 'READY_FOR_INTERVENTION':
        return 'Confirm theatre availability';
      case 'IN_PROGRESS':
        return 'Await post-op observation';
      case 'POSTOP_STABLE':
        return 'Continue recovery follow-up';
      case 'POSTOP_UNSTABLE':
        return 'Escalate and monitor closely';
      case 'BLOCKED_PREOP':
        return 'Resolve blocking pre-op findings';
      case 'DONE':
        return 'Archive when documentation is complete';
      case 'CANCELLED':
        return 'Document cancellation reason';
      default:
        return 'Review case';
    }
  }

  formatDateTime(date: string | null | undefined, time: string | null | undefined): string {
    const datePart = (date ?? '').trim();
    const timePart = (time ?? '').trim();
    if (!datePart && !timePart) {
      return '-';
    }

    const safeTime = timePart.length >= 5 ? timePart.slice(0, 5) : timePart;
    return `${datePart || '-'} ${safeTime || ''}`.trim();
  }

  isLockedCase(surgicalCase: SurgicalCase): boolean {
    return surgicalCase.status === 'BLOCKED_PREOP' || surgicalCase.status === 'POSTOP_UNSTABLE';
  }

  availableStatusesFor(surgicalCase: SurgicalCase): string[] {
    return this.allowedStatusTransitions[surgicalCase.status] ?? [surgicalCase.status];
  }

  canArchiveCase(surgicalCase: SurgicalCase): boolean {
    return this.availableStatusesFor(surgicalCase).includes('ARCHIVED') && surgicalCase.status !== 'ARCHIVED';
  }

  workspaceReadinessLabel(surgicalCase: SurgicalCase | undefined): string {
    if (!surgicalCase) {
      return '-';
    }

    switch (surgicalCase.status) {
      case 'READY_FOR_INTERVENTION':
        return 'Ready for intervention';
      case 'OPEN':
        return 'Awaiting final pre-op review';
      case 'BLOCKED_PREOP':
        return 'Pre-op blocked';
      case 'IN_PROGRESS':
        return 'Intervention in progress';
      default:
        return this.labelFrom(this.caseStatusLabels, surgicalCase.status);
    }
  }

  private refreshView(): void {
    this.cdr.markForCheck();
  }

  private syncSelectedWorkspaceCase(): void {
    if (this.roleScopedCases.length === 0) {
      this.selectedWorkspaceCaseId = '';
      return;
    }

    const selectedId = Number(this.selectedWorkspaceCaseId);
    const exists = this.roleScopedCases.some((item) => item.id === selectedId);
    if (!this.selectedWorkspaceCaseId || Number.isNaN(selectedId) || !exists) {
      const nextCase = this.roleScopedCases.find((item) => item.status !== 'ARCHIVED' && item.status !== 'CANCELLED') ?? this.roleScopedCases[0];
      this.selectedWorkspaceCaseId = String(nextCase.id);
    }
  }

  get roleScopedCases(): SurgicalCase[] {
    if (!this.isSurgeon) {
      return this.cases;
    }

    const actorId = this.authStorage.getUser()?.keycloakId;
    if (!actorId) {
      return [];
    }

    return this.cases.filter((item) => String(item.surgeonId ?? '').trim() === String(actorId).trim());
  }

  private caseDateTimeValue(surgicalCase: SurgicalCase): number {
    const datePart = String(surgicalCase.scheduledDate ?? '').trim();
    const timePart = String(surgicalCase.scheduledStartTime ?? '').trim() || '00:00';
    const parsed = new Date(`${datePart}T${timePart}`);
    return Number.isNaN(parsed.getTime()) ? Number.MAX_SAFE_INTEGER : parsed.getTime();
  }

  private todayIsoDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private loadPatients(): void {
    this.patientLoading = true;
    this.patientError = '';

    this.clinicalApi.listPatients()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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

  private loadSurgeons(): void {
    this.surgeonLoading = true;
    this.surgeonError = '';

    this.clinicalApi.listSurgeons(50)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (surgeons) => {
          this.availableSurgeons = surgeons ?? [];
          this.surgeonLoading = false;
          this.refreshView();
        },
        error: () => {
          this.availableSurgeons = [];
          this.selectedSurgeon = null;
          this.surgeonLoading = false;
          this.surgeonError = 'Failed to load surgeons.';
          this.refreshView();
        }
      });
  }

  private createFormErrors(): Record<keyof typeof this.createForm, string | null> {
    const patientId = this.createForm.patientId.trim();
    const firstName = this.createForm.firstName.trim();
    const lastName = this.createForm.lastName.trim();
    const age = Number(this.createForm.age);
    const surgeryType = this.createForm.surgeryType.trim();
    const procedureName = this.requiresCustomProcedureName
      ? this.createForm.customProcedureName.trim()
      : this.createForm.procedureName.trim();
    const surgeonId = this.createForm.surgeonId.trim();
    const scheduledDate = this.createForm.scheduledDate;
    const scheduledStartTime = this.createForm.scheduledStartTime;
    const estimatedDurationMinutes = Number(this.createForm.estimatedDurationMinutes);
    const operatingRoom = this.createForm.operatingRoom.trim();

    return {
      patientId: !patientId ? 'Please select an existing pediatric patient before creating the case.' : null,
      consultationId: null,
      surgeryRequestId: this.isReceptionist && !this.createForm.surgeryRequestId
        ? 'Select a surgery request from the planning queue before creating the case.'
        : null,
      firstName: firstName.length < 2 ? 'First name must contain at least 2 characters.' : null,
      lastName: lastName.length < 2 ? 'Last name must contain at least 2 characters.' : null,
      age: !Number.isFinite(age) || age < 0 || age > 17 ? 'Age must be between 0 and 17 years for pediatric care.' : null,
      gender: !this.createForm.gender ? 'Gender is required.' : null,
      medicalRecordNumber: null,
      surgeryType: !surgeryType ? 'Please select a surgery type.' : null,
      procedureName: procedureName.length < 3 ? 'Procedure name must contain at least 3 characters.' : null,
      customProcedureName: null,
      surgeryCategory: !this.createForm.surgeryCategory ? 'Procedure category is required.' : null,
      urgencyLevel: !this.createForm.urgencyLevel ? 'Urgency level is required.' : null,
      surgeonId: surgeonId.length < 3 ? 'Lead surgeon reference is required.' : null,
      assistantSurgeonId: null,
      anesthesiologistId: null,
      nurseTeam: null,
      scheduledDate: this.isScheduledDateInvalid(scheduledDate) ? 'Scheduled date cannot be in the past.' : null,
      scheduledStartTime: !scheduledStartTime ? 'Start time is required.' : null,
      estimatedDurationMinutes: !Number.isFinite(estimatedDurationMinutes) || estimatedDurationMinutes < 15 || estimatedDurationMinutes > 720
        ? 'Estimated duration must be between 15 and 720 minutes.'
        : null,
      operatingRoom: operatingRoom.length > 0 && operatingRoom.length < 2 ? 'Operating room reference is too short.' : null,
      status: null
    };
  }

  private isScheduledDateInvalid(value: string): boolean {
    if (!value) {
      return true;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const scheduled = new Date(value);
    scheduled.setHours(0, 0, 0, 0);
    return Number.isNaN(scheduled.getTime()) || scheduled < today;
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

  private patientAge(dateOfBirth: string | null | undefined): number | null {
    if (!dateOfBirth) {
      return null;
    }

    const birthDate = new Date(dateOfBirth);
    if (Number.isNaN(birthDate.getTime())) {
      return null;
    }

    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDelta = today.getMonth() - birthDate.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && today.getDate() < birthDate.getDate())) {
      age -= 1;
    }
    return age;
  }

  private mapPatientGender(sex: string | null | undefined): 'MALE' | 'FEMALE' {
    return String(sex ?? '').toUpperCase() === 'FEMALE' ? 'FEMALE' : 'MALE';
  }

  formatPatientLabel(patient: PatientLookupItem | null | undefined): string {
    if (!patient) {
      return '';
    }
    const fullName = `${patient.firstName ?? ''} ${patient.lastName ?? ''}`.trim();
    return `${fullName} | #${patient.id}`;
  }

  formatStaffLabel(staff: DoctorSearchResult | null | undefined): string {
    if (!staff) {
      return '';
    }
    const fullName = `${staff.firstName ?? ''} ${staff.lastName ?? ''}`.trim();
    return fullName || staff.username || staff.email || String(staff.id ?? staff.keycloakId ?? '');
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
