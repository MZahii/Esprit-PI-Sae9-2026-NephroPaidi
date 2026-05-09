import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import {
  CommunicationApiService,
  FollowUpMessage,
  MessageQueue,
  MessageStatus,
  MessageType,
  PatientDirectoryItem,
  StaffDirectoryItem
} from '../../../core/services/communication-api.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

type DatePreset = 'LAST_7_DAYS' | 'LAST_30_DAYS' | 'LAST_3_MONTHS' | 'CUSTOM';

interface StaffPerformanceRow {
  staffKey: string;
  staffName: string;
  messagesHandled: number;
  averageResponseHours: number | null;
  messagesClosed: number;
  messagesEscalated: number;
}

interface PieSlice {
  type: MessageType;
  count: number;
  percentage: number;
  color: string;
  path: string;
}

interface BarItem {
  staffName: string;
  avgHours: number;
  percent: number;
  colorClass: string;
}

@Component({
  selector: 'app-communication-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './communication-analytics.html',
  styleUrl: './communication-analytics.scss'
})
export class CommunicationAnalyticsComponent implements OnInit {
  loading = true;
  errorMessage = '';

  readonly presets: Array<{ value: DatePreset; label: string }> = [
    { value: 'LAST_7_DAYS', label: 'Last 7 days' },
    { value: 'LAST_30_DAYS', label: 'Last 30 days' },
    { value: 'LAST_3_MONTHS', label: 'Last 3 months' },
    { value: 'CUSTOM', label: 'Custom range' }
  ];

  selectedPreset: DatePreset = 'LAST_30_DAYS';
  customFrom = '';
  customTo = '';

  rangeStart!: Date;
  rangeEnd!: Date;

  activeTypeFilter: MessageType | 'ALL' = 'ALL';

  allMessages: FollowUpMessage[] = [];
  messagesInRange: FollowUpMessage[] = [];
  messagesForTable: FollowUpMessage[] = [];

  patientsById: Record<number, string> = {};
  staffById: Record<string, string> = {};

  averageResponseHours = 0;
  averageResponseLabel = '-';
  averageResponseCardClass = 'border-start border-4 border-success';

  highSlaPercent = 0;
  normalSlaPercent = 0;

  totalMessages = 0;
  createdCount = 0;
  respondedCount = 0;
  closedCount = 0;

  escalationRatePercent = 0;
  escalationTrendDirection: 'up' | 'down' | 'flat' = 'flat';
  escalationTrendDelta = 0;

  timelineLabels: string[] = [];
  createdSeries: number[] = [];
  respondedSeries: number[] = [];
  closedSeries: number[] = [];

  chartWidth = 760;
  chartHeight = 220;
  chartPadding = 24;
  maxLineValue = 1;
  createdLinePoints = '';
  respondedLinePoints = '';
  closedLinePoints = '';

  pieSlices: PieSlice[] = [];
  readonly pieColors = ['#2563eb', '#16a34a', '#6b7280', '#f59e0b', '#dc2626'];

  responseTimeBars: BarItem[] = [];
  staffRows: StaffPerformanceRow[] = [];

  readonly trackedTypes: MessageType[] = ['MEDICAL', 'ADMINISTRATIVE', 'LAB_RESULT', 'QUESTION', 'COMPLAINT'];

  constructor(
    private communicationApi: CommunicationApiService,
    private authStorage: AuthStorageService
  ) {}

  ngOnInit(): void {
    this.applyPreset('LAST_30_DAYS');
    this.loadData();
  }

  get queue(): MessageQueue {
    const role = this.authStorage.getRole();
    if (role === 'NURSE') return 'NURSE';
    if (role === 'DOCTOR') return 'DOCTOR';
    return 'RECEPTIONIST';
  }

  get dateRangeLabel(): string {
    return `${this.formatDateOnly(this.rangeStart)} → ${this.formatDateOnly(this.rangeEnd)}`;
  }

  get lineChartHeight(): number {
    return this.chartHeight + this.chartPadding * 2;
  }

  get isCustomRange(): boolean {
    return this.selectedPreset === 'CUSTOM';
  }

  get filteredTypeBadge(): string {
    return this.activeTypeFilter === 'ALL' ? 'All Types' : this.activeTypeFilter;
  }

  onPresetChange(): void {
    this.applyPreset(this.selectedPreset);
    this.recomputeAnalytics();
  }

  onCustomRangeChange(): void {
    if (!this.customFrom || !this.customTo) {
      return;
    }
    this.applyCustomRange();
    this.recomputeAnalytics();
  }

  clearTypeFilter(): void {
    this.activeTypeFilter = 'ALL';
    this.recomputeTableAndBars();
  }

  onSliceClick(type: MessageType): void {
    this.activeTypeFilter = this.activeTypeFilter === type ? 'ALL' : type;
    this.recomputeTableAndBars();
  }

  exportCsv(): void {
    const today = this.formatDateOnly(new Date());
    const summary = [
      `Summary`,
      `Total Messages: ${this.totalMessages}`,
      `Avg Response: ${this.averageResponseLabel}`,
      `SLA HIGH<=2h: ${this.highSlaPercent.toFixed(1)}%`,
      `SLA NORMAL<=24h: ${this.normalSlaPercent.toFixed(1)}%`
    ].join(',');

    const header = 'Date Sent,Patient Name,Message Type,Priority,Status,Assigned To,Response Time (hours)';

    const rows = this.messagesForTable.map((message) => {
      const responseHours = this.getResponseHours(message);
      return [
        this.escapeCsv(this.formatDateTime(message.createdAt)),
        this.escapeCsv(this.getPatientName(message.patientId)),
        message.messageType,
        message.priority,
        message.status,
        this.escapeCsv(this.getStaffName(message.assignedToUserKeycloakId)),
        responseHours === null ? '' : responseHours.toFixed(2)
      ].join(',');
    });

    const csv = [summary, '', header, ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `communication-analytics-${today}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  trackBarLabel(index: number): string {
    const total = this.responseTimeBars.length;
    if (total <= 8) {
      return this.responseTimeBars[index].staffName;
    }
    if (index === 0 || index === total - 1 || index % 2 === 0) {
      return this.responseTimeBars[index].staffName;
    }
    return '';
  }

  getAvgClass(hours: number | null): string {
    if (hours === null) return 'text-muted';
    if (hours < 2) return 'text-success fw-semibold';
    if (hours <= 6) return 'text-warning fw-semibold';
    return 'text-danger fw-semibold';
  }

  getSlaBarClass(percent: number): string {
    if (percent > 90) return 'bg-success';
    if (percent >= 70) return 'bg-warning';
    return 'bg-danger';
  }

  private loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      inbox: this.communicationApi.getInbox({ queue: this.queue }).pipe(catchError(() => of([] as FollowUpMessage[]))),
      patients: this.communicationApi.getPatientsDirectory().pipe(catchError(() => of([] as PatientDirectoryItem[]))),
      doctors: this.communicationApi.getDoctorsDirectory().pipe(catchError(() => of([] as StaffDirectoryItem[])))
    }).pipe(
      finalize(() => {
        this.loading = false;
      })
    ).subscribe({
      next: ({ inbox, patients, doctors }) => {
        this.allMessages = inbox;
        this.patientsById = patients.reduce((acc: Record<number, string>, patient) => {
          acc[patient.id] = `${patient.firstName} ${patient.lastName}`.trim();
          return acc;
        }, {});
        this.staffById = doctors.reduce((acc: Record<string, string>, doctor) => {
          acc[doctor.keycloakId] = doctor.username;
          return acc;
        }, {});

        this.recomputeAnalytics();
      },
      error: () => {
        this.errorMessage = 'Unable to load analytics data right now.';
      }
    });
  }

  private applyPreset(preset: DatePreset): void {
    const now = new Date();
    const end = this.endOfDay(now);

    if (preset === 'CUSTOM') {
      if (!this.customFrom || !this.customTo) {
        this.customTo = this.formatDateOnly(now);
        const start = new Date(now);
        start.setDate(start.getDate() - 29);
        this.customFrom = this.formatDateOnly(start);
      }
      this.applyCustomRange();
      return;
    }

    const start = new Date(now);
    if (preset === 'LAST_7_DAYS') {
      start.setDate(start.getDate() - 6);
    } else if (preset === 'LAST_30_DAYS') {
      start.setDate(start.getDate() - 29);
    } else {
      start.setMonth(start.getMonth() - 3);
      start.setDate(start.getDate() + 1);
    }

    this.rangeStart = this.startOfDay(start);
    this.rangeEnd = end;
  }

  private applyCustomRange(): void {
    const from = new Date(`${this.customFrom}T00:00:00`);
    const to = new Date(`${this.customTo}T23:59:59`);

    if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) {
      return;
    }

    this.rangeStart = this.startOfDay(from);
    this.rangeEnd = this.endOfDay(to);

    if (this.rangeStart > this.rangeEnd) {
      const temp = this.rangeStart;
      this.rangeStart = this.startOfDay(this.rangeEnd);
      this.rangeEnd = this.endOfDay(temp);
    }
  }

  private recomputeAnalytics(): void {
    this.messagesInRange = this.allMessages.filter((message) => {
      const created = new Date(message.createdAt);
      return created >= this.rangeStart && created <= this.rangeEnd;
    });

    this.recomputeMetricCards();
    this.recomputeTimelineChart();
    this.recomputePieChart();
    this.recomputeTableAndBars();
  }

  private recomputeMetricCards(): void {
    this.totalMessages = this.messagesInRange.length;
    this.createdCount = this.messagesInRange.length;
    this.respondedCount = this.messagesInRange.filter((message) => this.getFirstStaffReply(message) !== null).length;
    this.closedCount = this.messagesInRange.filter((message) => message.status === 'CLOSED').length;

    const responseSamples = this.messagesInRange
      .map((message) => this.getResponseHours(message))
      .filter((value): value is number => value !== null);

    this.averageResponseHours = responseSamples.length > 0
      ? responseSamples.reduce((sum, value) => sum + value, 0) / responseSamples.length
      : 0;
    this.averageResponseLabel = responseSamples.length > 0 ? this.formatHoursAndMinutes(this.averageResponseHours) : '-';

    this.averageResponseCardClass = this.averageResponseHours < 4
      ? 'border-start border-4 border-success'
      : this.averageResponseHours <= 12
        ? 'border-start border-4 border-warning'
        : 'border-start border-4 border-danger';

    const highMessages = this.messagesInRange.filter((message) => message.priority === 'HIGH');
    const highWithinSla = highMessages.filter((message) => {
      const response = this.getResponseHours(message);
      return response !== null && response <= 2;
    });
    this.highSlaPercent = highMessages.length > 0 ? (highWithinSla.length / highMessages.length) * 100 : 0;

    const normalMessages = this.messagesInRange.filter((message) => message.priority === 'NORMAL');
    const normalWithinSla = normalMessages.filter((message) => {
      const response = this.getResponseHours(message);
      return response !== null && response <= 24;
    });
    this.normalSlaPercent = normalMessages.length > 0 ? (normalWithinSla.length / normalMessages.length) * 100 : 0;

    const currentRate = this.computeEscalationRate(this.messagesInRange);
    const previousRange = this.getPreviousRangeMessages();
    const previousRate = this.computeEscalationRate(previousRange);
    this.escalationRatePercent = currentRate;
    this.escalationTrendDelta = currentRate - previousRate;
    this.escalationTrendDirection = this.escalationTrendDelta > 0.05 ? 'up' : this.escalationTrendDelta < -0.05 ? 'down' : 'flat';
  }

  private recomputeTimelineChart(): void {
    const labels: string[] = [];
    const created: number[] = [];
    const responded: number[] = [];
    const closed: number[] = [];

    const days = this.enumerateDays(this.rangeStart, this.rangeEnd);
    days.forEach((day) => {
      const key = this.formatDateOnly(day);
      labels.push(key);

      created.push(this.messagesInRange.filter((message) => this.formatDateOnly(new Date(message.createdAt)) === key).length);
      responded.push(this.messagesInRange.filter((message) => {
        const reply = this.getFirstStaffReply(message);
        if (!reply) return false;
        return this.formatDateOnly(new Date(reply.createdAt)) === key;
      }).length);
      closed.push(this.messagesInRange.filter((message) => {
        if (!message.closedAt) return false;
        return this.formatDateOnly(new Date(message.closedAt)) === key;
      }).length);
    });

    this.timelineLabels = labels;
    this.createdSeries = created;
    this.respondedSeries = responded;
    this.closedSeries = closed;

    this.maxLineValue = Math.max(1, ...created, ...responded, ...closed);
    this.createdLinePoints = this.buildLinePoints(created);
    this.respondedLinePoints = this.buildLinePoints(responded);
    this.closedLinePoints = this.buildLinePoints(closed);
  }

  private recomputePieChart(): void {
    const total = this.messagesInRange.length;
    const counts = this.trackedTypes.map((type) => ({
      type,
      count: this.messagesInRange.filter((message) => message.messageType === type).length
    }));

    let currentAngle = -90;
    const center = 110;
    const radius = 90;
    const slices: PieSlice[] = [];

    counts.forEach((item, index) => {
      const percentage = total > 0 ? (item.count / total) * 100 : 0;
      const sliceAngle = (percentage / 100) * 360;
      const endAngle = currentAngle + sliceAngle;

      slices.push({
        type: item.type,
        count: item.count,
        percentage,
        color: this.pieColors[index % this.pieColors.length],
        path: percentage <= 0
          ? ''
          : this.describeArc(center, center, radius, currentAngle, endAngle)
      });

      currentAngle = endAngle;
    });

    this.pieSlices = slices;
  }

  private recomputeTableAndBars(): void {
    this.messagesForTable = this.activeTypeFilter === 'ALL'
      ? [...this.messagesInRange]
      : this.messagesInRange.filter((message) => message.messageType === this.activeTypeFilter);

    const rows = this.computeStaffRows(this.messagesForTable);
    this.staffRows = rows
      .sort((a, b) => b.messagesHandled - a.messagesHandled)
      .slice(0, 10);

    const barRows = this.computeStaffRows(this.messagesInRange)
      .filter((row) => row.averageResponseHours !== null)
      .sort((a, b) => (a.averageResponseHours ?? 0) - (b.averageResponseHours ?? 0))
      .slice(0, 10);

    const maxValue = Math.max(1, ...barRows.map((row) => row.averageResponseHours ?? 0));
    this.responseTimeBars = barRows.map((row) => {
      const value = row.averageResponseHours ?? 0;
      return {
        staffName: row.staffName,
        avgHours: value,
        percent: (value / maxValue) * 100,
        colorClass: value < 2 ? 'bg-success' : value <= 6 ? 'bg-warning' : 'bg-danger'
      };
    });
  }

  private computeStaffRows(messages: FollowUpMessage[]): StaffPerformanceRow[] {
    const rows = new Map<string, {
      handled: Set<string>;
      responseHours: number[];
      closed: number;
      escalated: number;
    }>();

    const ensure = (staffKey: string) => {
      if (!rows.has(staffKey)) {
        rows.set(staffKey, {
          handled: new Set<string>(),
          responseHours: [],
          closed: 0,
          escalated: 0
        });
      }
      return rows.get(staffKey)!;
    };

    messages.forEach((message) => {
      const firstReply = this.getFirstStaffReply(message);
      const assigned = message.assignedToUserKeycloakId ?? null;
      const primaryStaffKey = firstReply?.senderKeycloakId ?? assigned;

      if (!primaryStaffKey) {
        return;
      }

      const row = ensure(primaryStaffKey);
      row.handled.add(message.id);

      const responseHours = this.getResponseHours(message);
      if (firstReply && responseHours !== null) {
        row.responseHours.push(responseHours);
      }

      if (message.status === 'CLOSED') {
        row.closed += 1;
      }

      if (this.isEscalated(message)) {
        row.escalated += 1;
      }
    });

    return Array.from(rows.entries()).map(([staffKey, entry]) => ({
      staffKey,
      staffName: this.getStaffName(staffKey),
      messagesHandled: entry.handled.size,
      averageResponseHours: entry.responseHours.length > 0
        ? entry.responseHours.reduce((sum, value) => sum + value, 0) / entry.responseHours.length
        : null,
      messagesClosed: entry.closed,
      messagesEscalated: entry.escalated
    }));
  }

  private getPreviousRangeMessages(): FollowUpMessage[] {
    const durationMs = this.rangeEnd.getTime() - this.rangeStart.getTime() + 1;
    const previousEnd = new Date(this.rangeStart.getTime() - 1);
    const previousStart = new Date(previousEnd.getTime() - durationMs + 1);

    return this.allMessages.filter((message) => {
      const created = new Date(message.createdAt);
      return created >= previousStart && created <= previousEnd;
    });
  }

  private computeEscalationRate(messages: FollowUpMessage[]): number {
    if (messages.length === 0) {
      return 0;
    }
    const escalatedCount = messages.filter((message) => this.isEscalated(message)).length;
    return (escalatedCount / messages.length) * 100;
  }

  private isEscalated(message: FollowUpMessage): boolean {
    if (message.status === 'ESCALATED') return true;
    if (message.assignedDoctorKeycloakId) return true;
    return message.replies.some((reply) => reply.replyText.toLowerCase().startsWith('escalation reason:'));
  }

  private getFirstStaffReply(message: FollowUpMessage) {
    return message.replies
      .filter((reply) => reply.senderRole !== 'GUARDIAN')
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())[0] ?? null;
  }

  private getResponseHours(message: FollowUpMessage): number | null {
    const reply = this.getFirstStaffReply(message);
    if (!reply) return null;
    const created = new Date(message.createdAt).getTime();
    const responded = new Date(reply.createdAt).getTime();
    const diff = responded - created;
    if (diff < 0) return null;
    return diff / 3600000;
  }

  private getPatientName(patientId: number): string {
    return this.patientsById[patientId] ?? `#${patientId}`;
  }

  private getStaffName(staffId: string | null | undefined): string {
    if (!staffId) return 'Unassigned';
    return this.staffById[staffId] ?? staffId;
  }

  private buildLinePoints(series: number[]): string {
    if (series.length === 0) {
      return '';
    }

    const width = this.chartWidth;
    const height = this.chartHeight;
    const xStep = series.length > 1 ? width / (series.length - 1) : width;

    return series.map((value, index) => {
      const x = this.chartPadding + (index * xStep);
      const y = this.chartPadding + (height - (value / this.maxLineValue) * height);
      return `${x},${y}`;
    }).join(' ');
  }

  private enumerateDays(start: Date, end: Date): Date[] {
    const days: Date[] = [];
    const cursor = this.startOfDay(start);
    const endDay = this.startOfDay(end);

    while (cursor <= endDay) {
      days.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }

    return days;
  }

  private describeArc(cx: number, cy: number, radius: number, startAngle: number, endAngle: number): string {
    const start = this.polarToCartesian(cx, cy, radius, endAngle);
    const end = this.polarToCartesian(cx, cy, radius, startAngle);
    const largeArcFlag = endAngle - startAngle <= 180 ? '0' : '1';

    return `M ${cx} ${cy} L ${start.x} ${start.y} A ${radius} ${radius} 0 ${largeArcFlag} 0 ${end.x} ${end.y} Z`;
  }

  private polarToCartesian(cx: number, cy: number, radius: number, angleInDegrees: number): { x: number; y: number } {
    const radians = (angleInDegrees - 90) * Math.PI / 180;
    return {
      x: cx + (radius * Math.cos(radians)),
      y: cy + (radius * Math.sin(radians))
    };
  }

  private formatHoursAndMinutes(hours: number): string {
    const totalMinutes = Math.round(hours * 60);
    const h = Math.floor(totalMinutes / 60);
    const m = totalMinutes % 60;
    return `${h}h ${m}m`;
  }

  private formatDateOnly(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDateTime(dateInput: string): string {
    const date = new Date(dateInput);
    return `${this.formatDateOnly(date)} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
  }

  private startOfDay(date: Date): Date {
    const copy = new Date(date);
    copy.setHours(0, 0, 0, 0);
    return copy;
  }

  private endOfDay(date: Date): Date {
    const copy = new Date(date);
    copy.setHours(23, 59, 59, 999);
    return copy;
  }

  private escapeCsv(value: string): string {
    const safe = (value ?? '').replaceAll('"', '""');
    return `"${safe}"`;
  }
}
