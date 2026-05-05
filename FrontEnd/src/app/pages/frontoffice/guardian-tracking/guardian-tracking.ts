import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
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

  plans: DialysisPlan[] = [];
  sessions: DialysisSession[] = [];
  outcomes: DialysisOutcome[] = [];
  surgicalCases: SurgicalCase[] = [];
  careTasks: CareTask[] = [];

  childOptions: ChildOption[] = [];
  selectedChildKey = '';

  constructor(
    private procedureApi: ProcedureApiService,
    private guardianPatients: GuardianPatientsService
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

  get openCareTasksCount(): number {
    return this.selectedCareTasks.filter((task) => !task.done).length;
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

  loadTrackingData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      plans: this.procedureApi.getDialysisPlans(),
      sessions: this.procedureApi.getDialysisSessions(),
      outcomes: this.procedureApi.getDialysisOutcomes(),
      surgicalCases: this.procedureApi.getSurgicalCases(),
      careTasks: this.procedureApi.getCareTasks(),
      linkedPatients: this.guardianPatients.getGuardianPatients()
    }).subscribe({
      next: (result) => {
        this.linkedPatients = result.linkedPatients ?? [];
        this.plans = result.plans ?? [];
        this.sessions = result.sessions ?? [];
        this.outcomes = result.outcomes ?? [];
        this.surgicalCases = result.surgicalCases ?? [];
        this.careTasks = result.careTasks ?? [];
        this.buildChildOptions();
        this.loading = false;
      },
      error: (err: { error?: { message?: string }; message?: string }) => {
        this.loading = false;
        this.errorMessage = this.formatApiError(err);
      }
    });
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
}
