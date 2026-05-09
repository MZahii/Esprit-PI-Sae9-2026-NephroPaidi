import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';

@Component({
  selector: 'app-doctor-today-appointments',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './doctor-today-appointments.page.html',
  styleUrl: './doctor-today-appointments.page.scss'
})
export class DoctorTodayAppointmentsPage implements OnInit {
  appointments: any[] = [];

  loading = false;
  error = '';

  calendarMonth = new Date();
  calendarDays: Array<{ date: Date | null; inMonth: boolean; isToday: boolean; count: number }> = [];
  selectedDate = new Date();
  calendarMonthLabel = '';
  selectedDateLabel = '';
  dayAppointments: any[] = [];
  private selectedDateKey = '';

  constructor(
    private api: ClinicalApiService,
    private authStorage: AuthStorageService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refreshCalendarLabels();
    this.refreshSelectedDay();
    this.loadAppointments();
  }

  loadAppointments(): void {
    const doctorId = this.authStorage.getUser()?.keycloakId;
    if (!doctorId) {
      this.appointments = [];
      this.buildCalendarDays();
      return;
    }

    const range = this.getMonthRange(this.calendarMonth);
    this.loading = true;
    this.error = '';

    this.api.listAppointments({
      doctorId,
      from: range.start,
      to: range.end
    }).subscribe({
      next: (items) => {
        this.appointments = items || [];
        this.loading = false;
        this.buildCalendarDays();
        this.syncSelectedDate();
        this.refreshSelectedDay();
      },
      error: () => {
        this.appointments = [];
        this.loading = false;
        this.error = 'Failed to load appointments.';
        this.buildCalendarDays();
        this.refreshSelectedDay();
      }
    });
  }

  startConsultation(appointment: any): void {
    if (!appointment?.id) return;
    this.api.startAppointmentConsultation(appointment.id).subscribe({
      next: (res) => {
        const consultationId = res?.consultationId;
        if (consultationId) {
          this.router.navigate(['/backoffice/consultations', consultationId, 'workspace']);
        }
      },
      error: () => {
        this.error = 'Unable to start consultation for this appointment.';
      }
    });
  }

  prevMonth(): void {
    const current = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    current.setMonth(current.getMonth() - 1);
    this.calendarMonth = current;
    this.refreshCalendarLabels();
    this.loadAppointments();
  }

  nextMonth(): void {
    const current = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    current.setMonth(current.getMonth() + 1);
    this.calendarMonth = current;
    this.refreshCalendarLabels();
    this.loadAppointments();
  }

  selectCalendarDay(day: { date: Date | null }): void {
    if (!day?.date) return;
    this.selectedDate = day.date;
    this.refreshSelectedDay();
  }

  statusBadge(status?: string): string {
    if (status === 'CONFIRMED') return 'bg-soft-primary text-primary';
    if (status === 'CANCELLED' || status === 'NO_SHOW') return 'bg-soft-danger text-danger';
    return 'bg-soft-warning text-warning';
  }

  private getMonthRange(date: Date): { start: string; end: string } {
    const start = new Date(date.getFullYear(), date.getMonth(), 1, 0, 0, 0);
    const end = new Date(date.getFullYear(), date.getMonth() + 1, 0, 23, 59, 59);
    return {
      start: this.formatDateTime(start),
      end: this.formatDateTime(end)
    };
  }

  private buildCalendarDays(): void {
    const year = this.calendarMonth.getFullYear();
    const month = this.calendarMonth.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const leadingBlanks = firstDay.getDay();
    const totalDays = lastDay.getDate();
    const todayKey = this.dateKey(new Date());

    const counts = new Map<string, number>();
    (this.appointments || []).forEach(item => {
      const when = this.parseDate(item?.scheduledAt);
      if (!when) return;
      const key = this.dateKey(when);
      counts.set(key, (counts.get(key) ?? 0) + 1);
    });

    const days: Array<{ date: Date | null; inMonth: boolean; isToday: boolean; count: number }> = [];

    for (let i = 0; i < leadingBlanks; i++) {
      days.push({ date: null, inMonth: false, isToday: false, count: 0 });
    }

    for (let day = 1; day <= totalDays; day++) {
      const date = new Date(year, month, day);
      const key = this.dateKey(date);
      days.push({
        date,
        inMonth: true,
        isToday: key === todayKey,
        count: counts.get(key) ?? 0
      });
    }

    const remaining = 7 - (days.length % 7);
    if (remaining < 7) {
      for (let i = 0; i < remaining; i++) {
        days.push({ date: null, inMonth: false, isToday: false, count: 0 });
      }
    }

    this.calendarDays = days;
  }

  private syncSelectedDate(): void {
    const currentMonthKey = `${this.calendarMonth.getFullYear()}-${this.calendarMonth.getMonth()}`;
    const selectedMonthKey = `${this.selectedDate.getFullYear()}-${this.selectedDate.getMonth()}`;
    if (currentMonthKey !== selectedMonthKey) {
      this.selectedDate = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    }
  }

  isSelected(day: { date: Date | null }): boolean {
    if (!day?.date) return false;
    return this.dateKey(day.date) === this.selectedDateKey;
  }

  private refreshCalendarLabels(): void {
    this.calendarMonthLabel = this.calendarMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  }

  private refreshSelectedDay(): void {
    this.selectedDateKey = this.dateKey(this.selectedDate);
    this.selectedDateLabel = this.selectedDate.toLocaleDateString(undefined, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
    this.dayAppointments = (this.appointments || []).filter(item => {
      const when = this.parseDate(item?.scheduledAt);
      return when ? this.dateKey(when) === this.selectedDateKey : false;
    });
  }

  private dateKey(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private parseDate(value: any): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private formatDateTime(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }
}
