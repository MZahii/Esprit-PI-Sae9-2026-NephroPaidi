import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { AppointmentRequestItem, AppointmentsApiService } from '../../../core/services/appointments-api.service';
import {
  CareTask,
  DialysisOutcome,
  DialysisPlan,
  DialysisSession,
  ProcedureApiService,
  SurgicalCase
} from '../../../core/services/procedure-api.service';
import {
  GuardianPatientProfile,
  GuardianPatientsService
} from '../../../features/administrative/api/guardian-patients.service';
import {
  ConsultationDossierConsultationItem,
  ConsultationDossierTimelineItem,
  ConsultationMedicalDossier
} from '../../../features/clinical/models/clinical.models';

type StatusTone = 'success' | 'warning' | 'danger' | 'neutral';

type TimelineEvent = {
  when: Date | null;
  title: string;
  detail: string;
  tone: StatusTone;
};

type ChildOption = {
  key: string;
  label: string;
  patientId: string;
};

type TrackingMetric = {
  label: string;
  value: string;
  tone: StatusTone;
};

type PatientSortMode = 'name-asc' | 'name-desc' | 'youngest-first' | 'oldest-first';
type WardSection = 'profiles' | 'dossier' | 'journey' | 'schedule';

@Component({
  selector: 'app-guardian-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './guardian-tracking.html',
  styleUrl: './guardian-tracking.scss'
})
export class GuardianTrackingComponent implements OnInit {
  loading = false;
  errorMessage = '';
  linkedPatients: GuardianPatientProfile[] = [];
  guardianDossier: ConsultationMedicalDossier | null = null;
  dossierLoading = false;
  dossierError = '';

  plans: DialysisPlan[] = [];
  sessions: DialysisSession[] = [];
  outcomes: DialysisOutcome[] = [];
  surgicalCases: SurgicalCase[] = [];
  careTasks: CareTask[] = [];

  childOptions: ChildOption[] = [];
  selectedChildKey = '';
  patientSearchTerm = '';
  patientSortMode: PatientSortMode = 'name-asc';
  activeWardSection: WardSection = 'profiles';
  appointmentRequests: AppointmentRequestItem[] = [];

  constructor(
    private procedureApi: ProcedureApiService,
    private guardianPatients: GuardianPatientsService,
    private clinicalApi: ClinicalApiService,
    private appointmentsApi: AppointmentsApiService
  ) {}

  ngOnInit(): void {
    this.loadTrackingData();
  }

  get selectedChild(): ChildOption | undefined {
    return this.childOptions.find((child) => child.key === this.selectedChildKey);
  }

  get selectedLinkedPatient(): GuardianPatientProfile | undefined {
    if (!this.selectedChild) {
      return undefined;
    }
    return this.linkedPatients.find((patient) => this.makeChildKey(String(patient.id), patient.firstName, patient.lastName) === this.selectedChild!.key);
  }

  get selectedChildNumericPatientId(): number | null {
    return this.selectedLinkedPatient?.id ?? null;
  }

  get hasChildData(): boolean {
    return this.childOptions.length > 0;
  }

  get selectedChildLabel(): string {
    return this.selectedChild?.label || 'Child profile';
  }

  get selectedChildReference(): string {
    return this.selectedLinkedPatient ? `PAT-${this.selectedLinkedPatient.id}` : (this.selectedChild?.patientId || 'Not available');
  }

  get childrenCount(): number {
    return this.childOptions.length;
  }

  get hasMultipleChildren(): boolean {
    return this.childOptions.length > 1;
  }

  get selectedDialysisPlans(): DialysisPlan[] {
    if (!this.selectedChild) return [];
    return this.plans.filter((plan) => this.matchesChild(plan.patientId, plan.firstName, plan.lastName));
  }

  get selectedPlanIds(): number[] {
    return this.selectedDialysisPlans.map((plan) => plan.id);
  }

  get selectedSessions(): DialysisSession[] {
    const ids = new Set(this.selectedPlanIds);
    return this.sessions.filter((session) => ids.has(session.planId));
  }

  get selectedSessionIds(): number[] {
    return this.selectedSessions.map((session) => session.id);
  }

  get selectedOutcomes(): DialysisOutcome[] {
    const ids = new Set(this.selectedSessionIds);
    return this.outcomes.filter((outcome) => ids.has(outcome.sessionId));
  }

  get selectedSurgicalCases(): SurgicalCase[] {
    if (!this.selectedChild) return [];
    return this.surgicalCases.filter((sCase) => this.matchesChild(sCase.patientId, sCase.firstName, sCase.lastName));
  }

  get selectedSurgicalCaseIds(): number[] {
    return this.selectedSurgicalCases.map((sCase) => sCase.id);
  }

  get selectedCareTasks(): CareTask[] {
    const ids = new Set(this.selectedSurgicalCaseIds);
    return this.careTasks.filter((task) => ids.has(task.surgicalCaseId));
  }

  get activeDialysisPlan(): DialysisPlan | undefined {
    return this.selectedDialysisPlans.find((plan) => plan.status !== 'ARCHIVED') ?? this.selectedDialysisPlans[0];
  }

  get nextSession(): DialysisSession | undefined {
    const now = new Date();
    return this.selectedSessions
      .map((session) => ({ session, date: this.parseDate(session.sessionDate) }))
      .filter((item) => item.date && item.date >= now)
      .sort((a, b) => (a.date!.getTime() - b.date!.getTime()))
      .map((item) => item.session)[0];
  }

  get latestOutcome(): DialysisOutcome | undefined {
    return [...this.selectedOutcomes].sort((a, b) => b.id - a.id)[0];
  }

  get latestSurgicalCase(): SurgicalCase | undefined {
    return [...this.selectedSurgicalCases]
      .sort((a, b) => this.dateWeight(b.scheduledDate, b.scheduledStartTime) - this.dateWeight(a.scheduledDate, a.scheduledStartTime))[0];
  }

  get globalStatus(): string {
    if (this.latestSurgicalCase) {
      return this.latestSurgicalCase.status || 'UNKNOWN';
    }
    if (this.activeDialysisPlan) {
      return this.activeDialysisPlan.status || 'UNKNOWN';
    }
    return 'NO_ACTIVE_CARE';
  }

  get globalStatusTone(): StatusTone {
    const status = this.globalStatus;
    if (status === 'READY_FOR_INTERVENTION' || status === 'POSTOP_STABLE' || status === 'DONE' || status === 'COMPLETED') return 'success';
    if (status === 'IN_PROGRESS' || status === 'OPEN' || status === 'PLANNED') return 'warning';
    if (status === 'BLOCKED_PREOP' || status === 'POSTOP_UNSTABLE' || status === 'CANCELLED') return 'danger';
    return 'neutral';
  }

  get nextStepLabel(): string {
    switch (this.globalStatus) {
      case 'OPEN':
        return 'Complete Pre-Op checklist.';
      case 'READY_FOR_INTERVENTION':
        return 'Intervention can be started by medical team.';
      case 'IN_PROGRESS':
        return 'Surgery in progress. Wait for Post-Op observation.';
      case 'POSTOP_STABLE':
        return 'Continue routine recovery follow-up.';
      case 'BLOCKED_PREOP':
        return 'Case locked due to Pre-Op complications.';
      case 'POSTOP_UNSTABLE':
        return 'Case locked, intensive monitoring in progress.';
      case 'DONE':
        return 'Case closed. Continue long-term follow-up.';
      case 'PLANNED':
        return 'Dialysis care is planned. Review the next scheduled session.';
      case 'COMPLETED':
        return 'Active dialysis cycle is completed. Follow future medical guidance.';
      default:
        return 'No active medical workflow yet.';
    }
  }

  get overviewTitle(): string {
    switch (this.globalStatusTone) {
      case 'danger':
        return 'Clinical attention required';
      case 'warning':
        return 'Follow-up in progress';
      case 'success':
        return 'Care pathway is stable';
      default:
        return 'No active procedure right now';
    }
  }

  get overviewDescription(): string {
    if (this.globalStatus === 'NO_ACTIVE_CARE') {
      return 'No dialysis or surgical workflow has been recorded yet for the selected child.';
    }
    if (this.globalStatus === 'POSTOP_UNSTABLE' || this.globalStatus === 'BLOCKED_PREOP') {
      return 'The medical team currently needs close monitoring or corrective action before the pathway can continue.';
    }
    if (this.nextSession) {
      return `Next dialysis session planned for ${this.formatDateTime(this.nextSession.sessionDate)}.`;
    }
    if (this.latestSurgicalCase) {
      return `Latest intervention workflow is ${this.statusLabel(this.latestSurgicalCase.status)}.`;
    }
    return this.nextStepLabel;
  }

  get metrics(): TrackingMetric[] {
    return [
      {
        label: 'Linked children',
        value: String(this.childrenCount),
        tone: 'neutral'
      },
      {
        label: 'Dialysis plans',
        value: String(this.selectedDialysisPlans.length),
        tone: this.activeDialysisPlan ? 'success' : 'neutral'
      },
      {
        label: 'Upcoming sessions',
        value: this.nextSession ? '1 scheduled' : 'None',
        tone: this.nextSession ? 'warning' : 'neutral'
      },
      {
        label: 'Open care tasks',
        value: String(this.openCareTasksCount),
        tone: this.openCareTasksCount > 0 ? 'danger' : 'success'
      }
    ];
  }

  get latestOutcomeSummary(): string {
    if (!this.latestOutcome) {
      return 'No outcome recorded yet.';
    }
    if (this.latestOutcome.summary) {
      return this.latestOutcome.summary;
    }
    return this.latestOutcome.validated ? 'Latest outcome validated.' : 'Latest outcome pending validation.';
  }

  get careAlerts(): string[] {
    const alerts: string[] = [];

    if (this.globalStatus === 'BLOCKED_PREOP') {
      alerts.push('Pre-op validation is blocked. Medical follow-up is needed before intervention can proceed.');
    }
    if (this.globalStatus === 'POSTOP_UNSTABLE') {
      alerts.push('Post-operative monitoring is unstable. Keep direct contact with the clinical team.');
    }
    if (this.openCareTasksCount > 0) {
      alerts.push(`${this.openCareTasksCount} care task(s) remain open for clinical follow-up.`);
    }
    if (this.activeDialysisPlan && !this.nextSession && this.activeDialysisPlan.status !== 'COMPLETED' && this.activeDialysisPlan.status !== 'ARCHIVED') {
      alerts.push('No upcoming dialysis session is currently visible for the active plan.');
    }
    if (this.latestOutcome && !this.latestOutcome.validated) {
      alerts.push('The latest dialysis outcome is still pending validation.');
    }

    return alerts;
  }

  get recommendedActions(): string[] {
    const actions: string[] = [];

    if (this.nextSession) {
      actions.push(`Prepare for the next dialysis session on ${this.formatDateTime(this.nextSession.sessionDate)}.`);
    }
    if (this.latestSurgicalCase?.status === 'READY_FOR_INTERVENTION') {
      actions.push('Confirm the intervention schedule with the care team if anything changed.');
    }
    if (this.latestSurgicalCase?.status === 'DONE') {
      actions.push('Continue long-term recovery follow-up and review future consultations.');
    }
    if (actions.length === 0) {
      actions.push('Use messages or appointments to stay aligned with the medical team.');
    }

    return actions;
  }

  get timelinePreview(): TimelineEvent[] {
    return this.timeline.slice(0, 8);
  }

  get hiddenTimelineCount(): number {
    return Math.max(this.timeline.length - this.timelinePreview.length, 0);
  }

  get dossierTimeline(): ConsultationDossierTimelineItem[] {
    return [...(this.guardianDossier?.timeline ?? [])]
      .sort((left, right) => this.sortByDateDesc(left.occurredAt, right.occurredAt));
  }

  get dossierTimelinePreview(): ConsultationDossierTimelineItem[] {
    return this.dossierTimeline.slice(0, 10);
  }

  get dossierHiddenCount(): number {
    return Math.max(this.dossierTimeline.length - this.dossierTimelinePreview.length, 0);
  }

  get consultationReports(): ConsultationDossierConsultationItem[] {
    return [...(this.guardianDossier?.consultations ?? [])]
      .sort((left, right) => this.sortByDateDesc(left.consultationDate, right.consultationDate));
  }

  get dossierGeneratedLabel(): string {
    return this.guardianDossier?.generatedAt ? this.formatDateTime(this.guardianDossier.generatedAt) : '-';
  }

  get filteredLinkedPatients(): GuardianPatientProfile[] {
    const term = this.patientSearchTerm.trim().toLowerCase();
    const items = [...this.linkedPatients].filter((patient) => {
      if (!term) return true;
      const searchable = [
        this.fullPatientName(patient),
        patient.sex ?? '',
        patient.bloodType ?? '',
        patient.dateOfBirth ?? '',
        patient.chronicConditions ?? ''
      ].join(' ').toLowerCase();
      return searchable.includes(term);
    });

    items.sort((left, right) => this.comparePatients(left, right));
    return items;
  }

  get openCareTasksCount(): number {
    return this.selectedCareTasks.filter((task) => !task.done).length;
  }

  get selectedAppointmentRequests(): AppointmentRequestItem[] {
    const patientId = this.selectedChildNumericPatientId;
    if (!patientId) return [];
    return [...this.appointmentRequests]
      .filter((item) => Number(item.patientId) === Number(patientId))
      .sort((left, right) => this.sortByDateDesc(left.scheduledDate || left.requestedDate, right.scheduledDate || right.requestedDate));
  }

  get upcomingAppointmentRequests(): AppointmentRequestItem[] {
    const now = Date.now();
    return this.selectedAppointmentRequests.filter((item) => {
      const date = this.parseDate(item.scheduledDate || item.requestedDate);
      return !!date && date.getTime() >= now;
    });
  }

  get recentConsultationReports(): ConsultationDossierConsultationItem[] {
    return this.consultationReports.slice(0, 6);
  }

  get timeline(): TimelineEvent[] {
    const events: TimelineEvent[] = [];

    for (const plan of this.selectedDialysisPlans) {
      events.push({
        when: this.parseDate(plan.startDate),
        title: 'Dialysis Plan Created',
        detail: `${plan.dialysisType} | ${plan.sessionsPerWeek}/week | ${plan.status}`,
        tone: 'neutral'
      });
    }

    for (const session of this.selectedSessions) {
      events.push({
        when: this.parseDate(session.sessionDate),
        title: 'Dialysis Session',
        detail: `Session #${session.id}`,
        tone: 'neutral'
      });
    }

    for (const outcome of this.selectedOutcomes) {
      const session = this.sessions.find((s) => s.id === outcome.sessionId);
      events.push({
        when: session ? this.parseDate(session.sessionDate) : null,
        title: 'Dialysis Outcome',
        detail: `${outcome.validated ? 'Validated' : 'Not validated'}${outcome.summary ? ` | ${outcome.summary}` : ''}`,
        tone: outcome.validated ? 'success' : 'warning'
      });
    }

    for (const sCase of this.selectedSurgicalCases) {
      events.push({
        when: this.parseDateTime(sCase.scheduledDate, sCase.scheduledStartTime),
        title: 'Surgical Case',
        detail: `${sCase.surgeryType || '-'} | ${sCase.status}`,
        tone: this.statusToneFromSurgicalStatus(sCase.status)
      });
    }

    if (this.selectedCareTasks.length > 0) {
      events.push({
        when: null,
        title: 'Care Tasks',
        detail: `${this.openCareTasksCount} open task(s) for clinical follow-up`,
        tone: this.openCareTasksCount > 0 ? 'danger' : 'success'
      });
    }

    return events.sort((a, b) => {
      if (a.when && b.when) return b.when.getTime() - a.when.getTime();
      if (a.when && !b.when) return -1;
      if (!a.when && b.when) return 1;
      return 0;
    });
  }

  loadTrackingData(retryCount = 0): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      plans: this.procedureApi.getDialysisPlans().pipe(catchError(() => of([]))),
      sessions: this.procedureApi.getDialysisSessions().pipe(catchError(() => of([]))),
      outcomes: this.procedureApi.getDialysisOutcomes().pipe(catchError(() => of([]))),
      surgicalCases: this.procedureApi.getSurgicalCases().pipe(catchError(() => of([]))),
      careTasks: this.procedureApi.getCareTasks().pipe(catchError(() => of([]))),
      linkedPatients: this.guardianPatients.getGuardianPatients().pipe(catchError(() => of([]))),
      appointmentRequests: this.appointmentsApi.getMyRequests().pipe(catchError(() => of([])))
    }).subscribe({
      next: (result) => {
        this.linkedPatients = result.linkedPatients ?? [];
        this.plans = result.plans ?? [];
        this.sessions = result.sessions ?? [];
        this.outcomes = result.outcomes ?? [];
        this.surgicalCases = result.surgicalCases ?? [];
        this.careTasks = result.careTasks ?? [];
        this.appointmentRequests = result.appointmentRequests ?? [];
        this.buildChildOptions();
        if (this.linkedPatients.length === 0 && retryCount < 3) {
          this.loading = false;
          setTimeout(() => this.loadTrackingData(retryCount + 1), 900);
          return;
        }
        this.loadGuardianDossier();
        this.loading = false;
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err);
      }
    });
  }

  onChildSelectionChange(): void {
    this.loadGuardianDossier();
  }

  setWardSection(section: WardSection): void {
    this.activeWardSection = section;
  }

  statusLabel(status: string | null | undefined): string {
    if (!status) return 'Unknown';
    const map: Record<string, string> = {
      OPEN: 'Open',
      READY_FOR_INTERVENTION: 'Ready For Intervention',
      BLOCKED_PREOP: 'Blocked Pre-Op',
      IN_PROGRESS: 'In Progress',
      POSTOP_STABLE: 'Post-Op Stable',
      POSTOP_UNSTABLE: 'Post-Op Unstable',
      DONE: 'Completed',
      CANCELLED: 'Cancelled',
      PLANNED: 'Planned',
      COMPLETED: 'Completed',
      ARCHIVED: 'Archived',
      NO_ACTIVE_CARE: 'No Active Care'
    };
    return map[status] ?? status;
  }

  formatDate(value: string | null | undefined): string {
    const date = this.parseDate(value);
    return date ? date.toLocaleDateString() : '-';
  }

  formatDateTime(value: string | null | undefined): string {
    const date = this.parseDate(value);
    return date ? `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : '-';
  }

  dossierKindLabel(kind: string | null | undefined): string {
    if (!kind) return 'Dossier Entry';
    const labels: Record<string, string> = {
      CONSULTATION: 'Consultation',
      LAB_REQUEST: 'Lab Request',
      PRESCRIPTION: 'Prescription',
      DISCHARGE: 'Discharge',
      LAB: 'Lab Result'
    };
    return labels[kind] ?? kind.replace(/_/g, ' ');
  }

  reportSummary(item: ConsultationDossierConsultationItem): string {
    const fragments = [
      item.diagnosis?.trim(),
      item.treatmentPlan?.trim(),
      item.notes?.trim()
    ].filter((value): value is string => !!value);

    if (fragments.length > 0) {
      return fragments[0];
    }

    if (item.labRequests?.length) {
      return `${item.labRequests.length} lab request(s) recorded.`;
    }

    if (item.prescriptions?.length) {
      return `${item.prescriptions.length} prescription item(s) recorded.`;
    }

    return 'Consultation outcome recorded in the child dossier.';
  }

  appointmentStatusLabel(status: string | null | undefined): string {
    const normalized = String(status || '').trim().toUpperCase();
    if (!normalized) return 'Pending';
    const labels: Record<string, string> = {
      REQUESTED: 'Requested',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      CANCELLED: 'Cancelled'
    };
    return labels[normalized] ?? normalized.replace(/_/g, ' ');
  }

  fullPatientName(patient: GuardianPatientProfile): string {
    return `${patient.firstName ?? ''} ${patient.lastName ?? ''}`.trim();
  }

  patientAge(dateOfBirth?: string | null): number | null {
    if (!dateOfBirth) return null;
    const dob = new Date(dateOfBirth);
    if (Number.isNaN(dob.getTime())) return null;

    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const monthDelta = today.getMonth() - dob.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age >= 0 ? age : null;
  }

  private buildChildOptions(): void {
    const map = new Map<string, ChildOption>();

    for (const patient of this.linkedPatients) {
      const key = this.makeChildKey(String(patient.id), patient.firstName, patient.lastName);
      map.set(key, {
        key,
        label: `${patient.firstName || '-'} ${patient.lastName || ''}`.trim(),
        patientId: String(patient.id)
      });
    }

    this.childOptions = [...map.values()].sort((a, b) => a.label.localeCompare(b.label));
    if (this.childOptions.length === 0) {
      this.selectedChildKey = '';
      return;
    }

    const stillValid = this.childOptions.some((child) => child.key === this.selectedChildKey);
    if (!this.selectedChildKey || !stillValid) {
      this.selectedChildKey = this.childOptions[0].key;
    }
  }

  private loadGuardianDossier(): void {
    const patientId = this.selectedChildNumericPatientId;
    if (!patientId) {
      this.guardianDossier = null;
      this.dossierError = '';
      this.dossierLoading = false;
      return;
    }

    this.dossierLoading = true;
    this.dossierError = '';

    this.clinicalApi.getGuardianMedicalDossier(patientId).subscribe({
      next: (dossier) => {
        this.guardianDossier = dossier;
        this.dossierLoading = false;
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.guardianDossier = null;
        this.dossierLoading = false;
        this.dossierError = this.formatDossierError(err);
      }
    });
  }

  private matchesChild(patientId: string | null | undefined, firstName: string | null | undefined, lastName: string | null | undefined): boolean {
    if (!this.selectedChild) return false;
    const directKey = this.makeChildKey(patientId, firstName, lastName);
    if (directKey === this.selectedChild.key) {
      return true;
    }

    const selectedPatient = this.selectedLinkedPatient;
    if (!selectedPatient) {
      return false;
    }

    const selectedFullName = `${selectedPatient.firstName ?? ''} ${selectedPatient.lastName ?? ''}`.trim().toLowerCase();
    const candidateFullName = `${firstName ?? ''} ${lastName ?? ''}`.trim().toLowerCase();

    return selectedFullName.length > 0 && selectedFullName === candidateFullName;
  }

  private makeChildKey(patientId: string | null | undefined, firstName: string | null | undefined, lastName: string | null | undefined): string {
    const id = (patientId ?? '').trim().toLowerCase();
    if (id) return `id:${id}`;
    return `name:${(firstName ?? '').trim().toLowerCase()}|${(lastName ?? '').trim().toLowerCase()}`;
  }

  private parseDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  private sortByDateDesc(left: string | null | undefined, right: string | null | undefined): number {
    const leftDate = this.parseDate(left);
    const rightDate = this.parseDate(right);
    if (leftDate && rightDate) return rightDate.getTime() - leftDate.getTime();
    if (leftDate && !rightDate) return -1;
    if (!leftDate && rightDate) return 1;
    return 0;
  }

  private parseDateTime(date: string | null | undefined, time: string | null | undefined): Date | null {
    if (!date) return null;
    const safeTime = (time ?? '00:00').slice(0, 8);
    return this.parseDate(`${date}T${safeTime}`);
  }

  private statusToneFromSurgicalStatus(status: string | null | undefined): StatusTone {
    if (!status) return 'neutral';
    if (status === 'POSTOP_STABLE' || status === 'DONE' || status === 'READY_FOR_INTERVENTION') return 'success';
    if (status === 'BLOCKED_PREOP' || status === 'POSTOP_UNSTABLE' || status === 'CANCELLED') return 'danger';
    if (status === 'OPEN' || status === 'IN_PROGRESS') return 'warning';
    return 'neutral';
  }

  private dateWeight(date: string | null | undefined, time: string | null | undefined): number {
    const parsed = this.parseDateTime(date, time);
    return parsed ? parsed.getTime() : 0;
  }

  private formatApiError(err: { error?: { message?: string }; message?: string }): string {
    const message = err?.error?.message || err?.message || '';
    if (message.includes('403')) {
      return 'Guardian tracking is not accessible with the current account permissions.';
    }
    if (message.includes('503') || message.includes('Service Unavailable')) {
      return 'Tracking data is temporarily unavailable. Please retry after the care services reconnect.';
    }
    return message || 'Failed to load guardian tracking data.';
  }

  private formatDossierError(err: { error?: { message?: string }; message?: string }): string {
    const message = err?.error?.message || err?.message || '';
    if (message.includes('403')) {
      return 'This child dossier is not available for the current guardian account.';
    }
    if (message.includes('404')) {
      return 'No medical dossier is available yet for the selected child.';
    }
    return message || 'Failed to load the child medical dossier.';
  }

  private comparePatients(left: GuardianPatientProfile, right: GuardianPatientProfile): number {
    switch (this.patientSortMode) {
      case 'name-desc':
        return this.fullPatientName(right).localeCompare(this.fullPatientName(left));
      case 'youngest-first':
        return this.safeTimestamp(right.dateOfBirth) - this.safeTimestamp(left.dateOfBirth);
      case 'oldest-first':
        return this.safeTimestamp(left.dateOfBirth) - this.safeTimestamp(right.dateOfBirth);
      case 'name-asc':
      default:
        return this.fullPatientName(left).localeCompare(this.fullPatientName(right));
    }
  }

  private safeTimestamp(value?: string | null): number {
    if (!value) return 0;
    const parsed = new Date(value).getTime();
    return Number.isNaN(parsed) ? 0 : parsed;
  }
}
