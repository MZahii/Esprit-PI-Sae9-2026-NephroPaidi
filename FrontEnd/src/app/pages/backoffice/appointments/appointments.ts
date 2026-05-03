import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClinicalApiService } from '../../../core/services/clinical-api.service';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { forkJoin, of, Subject, throwError } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, takeUntil } from 'rxjs/operators';

type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'NO_SHOW';

interface DoctorOption {
  id: string;
  displayName: string;
  email?: string;
  role?: string;
  available: boolean;
  conflictReason?: string;
  raw?: any;
}

@Component({
  selector: 'app-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './appointments.html',
  styleUrl: './appointments.scss'
})
export class Appointments implements OnInit, OnDestroy {
  loading = false;
  errorMessage = '';

  appointments: any[] = [];

  filters = {
    status: 'ALL' as AppointmentStatus | 'ALL',
    doctorId: '',
    patientId: ''
  };

  showCreateModal = false;
  showEditModal = false;
  showCancelModal = false;
  startingAppointmentId: string | null = null;
  appointmentCountdown: { [key: string]: { timeLeft: number; canStart: boolean } } = {};

  createForm = {
    patientId: 0,
    doctorId: '',
    scheduledAt: '',
    durationMinutes: 30,
    reason: ''
  };

  minScheduledAt = '';
  scheduledAtError = '';
  durationError = '';

  editForm = {
    id: '',
    patientId: 0,
    doctorId: '',
    scheduledAt: '',
    durationMinutes: 30,
    reason: '',
    status: 'SCHEDULED' as AppointmentStatus
  };

  cancelForm = {
    id: '',
    reason: ''
  };

  patients: any[] = [];
  patientSearch = '';
  selectedPatient: any = null;
  patientLoading = false;
  patientError = '';
  patientSuggestions: any[] = [];
  patientNoMatches = false;
  patientFilterSearch = '';
  selectedPatientFilter: any = null;
  patientFilterSuggestions: any[] = [];
  patientFilterLoading = false;
  patientFilterError = '';
  patientFilterNoMatches = false;

  doctors: any[] = [];
  doctorSearch = '';
  selectedDoctor: DoctorOption | null = null;
  doctorLoading = false;
  doctorError = '';
  doctorOptions: DoctorOption[] = [];
  doctorNoMatches = false;
  forceSchedule = false;

  private readonly patientSearch$ = new Subject<string>();
  private readonly patientFilterSearch$ = new Subject<string>();
  private readonly doctorSearch$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  calendarMonth = new Date();
  calendarDays: Array<{ date: Date | null; inMonth: boolean; isToday: boolean; count: number }> = [];
  selectedDate = new Date();

  constructor(
    private api: ClinicalApiService,
    private auth: AuthStorageService
  ) {}

  /** Receptionists schedule only; they must not start consultations or change/cancel appointment times here. */
  get isReceptionist(): boolean {
    return this.auth.hasAnyRole(['RECEPTIONIST']);
  }

  ngOnInit(): void {
    this.initPatientSearch();
    this.initPatientFilterSearch();
    this.initDoctorSearch();

    // Defer first data load to next task to avoid NG0100 on initial render.
    setTimeout(() => {
      this.loadAppointments();
      this.ensureDoctorsLoaded();
    }, 0);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMessage = '';

    const range = this.getMonthRange(this.calendarMonth);
    const status = this.filters.status === 'ALL' ? undefined : this.filters.status;
    const doctorId = this.filters.doctorId || undefined;
    const patientId = this.filters.patientId || undefined;

    this.api.listAppointments({
      doctorId,
      patientId,
      status,
      from: range.start,
      to: range.end
    }).subscribe({
      next: (items) => {
        this.appointments = items || [];
        this.buildCalendarDays();
        this.syncSelectedDate();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = this.formatHttpError(err, 'Failed to load appointments');
      }
    });
  }

  clearFilters(): void {
    this.filters = { status: 'ALL', doctorId: '', patientId: '' };
    this.patientFilterSearch = '';
    this.selectedPatientFilter = null;
    this.patientFilterSuggestions = [];
    this.patientFilterNoMatches = false;
    this.loadAppointments();
  }

  openCreateModal(): void {
    this.createForm = {
      patientId: 0,
      doctorId: '',
      scheduledAt: '',
      durationMinutes: 30,
      reason: ''
    };
    this.patientSearch = '';
    this.selectedPatient = null;
    this.patientSuggestions = [];
    this.patientNoMatches = false;
    this.doctorSearch = '';
    this.selectedDoctor = null;
    this.doctorOptions = [];
    this.doctorNoMatches = false;
    this.forceSchedule = false;
    this.scheduledAtError = '';
    this.durationError = '';
    this.doctorError = '';
    this.patientError = '';
    this.minScheduledAt = this.toLocalDateTimeMin(new Date());
    this.showCreateModal = true;
    this.ensureDoctorsLoaded();
  }

  openEditModal(item: any): void {
    if (this.isReceptionist) {
      return;
    }
    this.editForm = {
      id: item.id,
      patientId: Number(item.patientId),
      doctorId: item.doctorId,
      scheduledAt: this.toLocalInput(item.scheduledAt),
      durationMinutes: item.durationMinutes ?? 30,
      reason: item.reason ?? '',
      status: item.status ?? 'SCHEDULED'
    };
    this.selectedPatient = this.patients.find(p => Number(p.id) === Number(item.patientId)) || null;
    const doctor = this.doctors.find(d => d.keycloakId === item.doctorId) || null;
    this.selectedDoctor = doctor
      ? {
        id: doctor.keycloakId,
        displayName: this.getDoctorLabel(doctor.keycloakId),
        email: doctor.email,
        available: true,
        raw: doctor
      }
      : null;
    this.patientSearch = this.selectedPatient ? this.getPatientLabel(this.selectedPatient.id) : '';
    this.doctorSearch = this.selectedDoctor ? this.selectedDoctor.displayName : '';
    this.showEditModal = true;
  }

  openCancelModal(item: any): void {
    if (this.isReceptionist) {
      return;
    }
    this.cancelForm = { id: item.id, reason: '' };
    this.showCancelModal = true;
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.showCancelModal = false;
  }

  submitCreate(): void {
    if (!this.canCreate) return;

    this.api.createAppointment({
      patientId: this.createForm.patientId,
      doctorId: this.createForm.doctorId,
      scheduledAt: this.normalizeDateTime(this.createForm.scheduledAt),
      durationMinutes: this.createForm.durationMinutes,
      reason: this.createForm.reason || undefined
    }).subscribe({
      next: () => {
        this.closeModals();
        this.loadAppointments();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to create appointment.';
      }
    });
  }

  submitEdit(): void {
    if (this.isReceptionist) {
      return;
    }
    if (!this.editForm.id) return;
    this.api.updateAppointment(this.editForm.id, {
      patientId: this.editForm.patientId || undefined,
      doctorId: this.editForm.doctorId || undefined,
      scheduledAt: this.editForm.scheduledAt ? this.normalizeDateTime(this.editForm.scheduledAt) : undefined,
      durationMinutes: this.editForm.durationMinutes,
      reason: this.editForm.reason || undefined,
      status: this.editForm.status
    }).subscribe({
      next: () => {
        this.closeModals();
        this.loadAppointments();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to update appointment.';
      }
    });
  }

  submitCancel(): void {
    if (this.isReceptionist) {
      return;
    }
    if (!this.cancelForm.id) return;
    this.api.cancelAppointment(this.cancelForm.id, this.cancelForm.reason || undefined).subscribe({
      next: () => {
        this.closeModals();
        this.loadAppointments();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Failed to cancel appointment.';
      }
    });
  }

  startConsultation(appointmentId: string): void {
    if (this.isReceptionist) {
      return;
    }
    this.startingAppointmentId = appointmentId;

    this.api.startConsultation(appointmentId).subscribe({
      next: (response) => {
        this.startingAppointmentId = null;
        this.loadAppointments();
        this.errorMessage = '';
      },
      error: (err) => {
        this.startingAppointmentId = null;
        const message = err?.error?.message || 'Failed to start consultation. Appointment window is 5 min before to 30 min after scheduled time.';
        this.errorMessage = message;
        setTimeout(() => this.errorMessage = '', 5000);
      }
    });
  }

  canStartAppointment(appointment: any): boolean {
    if (!appointment || appointment.status !== 'SCHEDULED') return false;

    const now = new Date();
    const appointmentTime = new Date(appointment.scheduledAt);
    const today = new Date();

    // Check if appointment is today
    if (appointmentTime.toDateString() !== today.toDateString()) return false;

    // OPTION 3 HYBRID: 20-30 minute window with 5-minute buffer
    // Button available: 5 minutes BEFORE appointment
    // Hard cancel at: 30 minutes AFTER appointment
    const diffMinutes = (now.getTime() - appointmentTime.getTime()) / 60000;
    
    // Can start from 5 min before (-5) up to 30 min after (+30)
    return diffMinutes >= -5 && diffMinutes <= 30;
  }

  getAppointmentCountdown(appointment: any): string {
    if (!appointment) return '';

    const now = new Date();
    const appointmentTime = new Date(appointment.scheduledAt);
    const diffMinutes = (appointmentTime.getTime() - now.getTime()) / 60000;

    // 5 minutes before
    if (diffMinutes >= 5) {
      const minutes = Math.floor(diffMinutes);
      const seconds = Math.floor((diffMinutes % 1) * 60);
      return `starts in ${minutes}m ${seconds}s`;
    }
    
    // 5 minutes before to appointment time (prep window)
    if (diffMinutes >= 0) {
      const minutes = Math.floor(diffMinutes);
      const seconds = Math.floor((diffMinutes % 1) * 60);
      return `⏱️ Starts in ${minutes}m ${seconds}s (Prep ready!)`;
    }

    const diffMinutesAfter = Math.abs(diffMinutes);
    
    // Up to 30 minutes after (window open)
    if (diffMinutesAfter <= 30) {
      const minutes = Math.floor(diffMinutesAfter);
      const seconds = Math.floor((diffMinutesAfter % 1) * 60);
      const minutesUntilClose = Math.floor(30 - diffMinutesAfter);
      return `⏳ ${minutes}m ${seconds}s late | Closes in ${minutesUntilClose}m`;
    }

    // After 30 minutes (window closed)
    return `⛔ Window closed`;
  }

  ensureDoctorsLoaded(): void {
    if (this.doctors.length || this.doctorLoading) return;
    this.doctorLoading = true;
    this.api.listDoctors(50).subscribe({
      next: (users) => {
        this.doctors = users ?? [];
        this.doctorLoading = false;
      },
      error: () => {
        this.doctorLoading = false;
      }
    });
  }

  onPatientSearchInput(value: string): void {
    this.patientSearch = value;
    if (this.selectedPatient) {
      this.selectedPatient = null;
      this.createForm.patientId = 0;
    }
    this.patientSearch$.next(value);
  }

  onPatientFilterInput(value: string): void {
    this.patientFilterSearch = value;
    if (this.selectedPatientFilter) {
      this.selectedPatientFilter = null;
      this.filters.patientId = '';
    }
    this.patientFilterSearch$.next(value);
  }

  onDoctorSearchInput(value: string): void {
    this.doctorSearch = value;
    this.forceSchedule = false;
    if (this.selectedDoctor) {
      this.selectedDoctor = null;
      this.createForm.doctorId = '';
    }
    this.doctorSearch$.next(value);
  }

  selectPatient(p: any): void {
    this.selectedPatient = p;
    this.createForm.patientId = Number(p.id);
    this.patientSearch = this.getPatientLabel(p.id);
    this.patientSuggestions = [];
    this.patientNoMatches = false;
    if (p?.id && !this.patients.find(item => String(item.id) === String(p.id))) {
      this.patients = [...this.patients, p];
    }
  }

  selectPatientFilter(p: any): void {
    this.selectedPatientFilter = p;
    this.filters.patientId = String(p.id);
    this.patientFilterSearch = this.getPatientLabel(p.id);
    this.patientFilterSuggestions = [];
    this.patientFilterNoMatches = false;
    if (p?.id && !this.patients.find(item => String(item.id) === String(p.id))) {
      this.patients = [...this.patients, p];
    }
    this.loadAppointments();
  }

  clearPatientFilter(): void {
    this.selectedPatientFilter = null;
    this.filters.patientId = '';
    this.patientFilterSearch = '';
    this.patientFilterSuggestions = [];
    this.patientFilterNoMatches = false;
    this.loadAppointments();
  }

  selectDoctor(option: DoctorOption): void {
    this.selectedDoctor = option;
    this.createForm.doctorId = option.id;
    this.doctorSearch = option.displayName;
    this.doctorOptions = [];
    this.doctorNoMatches = false;
    this.forceSchedule = false;
  }

  onScheduledAtChange(value: string): void {
    this.createForm.scheduledAt = value;
    this.refreshDoctorSuggestions();
  }

  onDurationChange(value: number): void {
    this.createForm.durationMinutes = Number(value);
    this.refreshDoctorSuggestions();
  }

  clearSelectedPatient(): void {
    this.selectedPatient = null;
    this.createForm.patientId = 0;
    this.patientSearch = '';
    this.patientSuggestions = [];
    this.patientNoMatches = false;
  }

  clearSelectedDoctor(): void {
    this.selectedDoctor = null;
    this.createForm.doctorId = '';
    this.doctorSearch = '';
    this.doctorOptions = [];
    this.doctorNoMatches = false;
    this.forceSchedule = false;
  }

  get isScheduledAtValid(): boolean {
    return !!this.createForm.scheduledAt && !this.isPastDatetime(this.createForm.scheduledAt);
  }

  get isDurationValid(): boolean {
    const duration = Number(this.createForm.durationMinutes);
    return Number.isFinite(duration) && duration >= 10 && duration <= 180;
  }

  get isScheduleReady(): boolean {
    return this.isScheduledAtValid && this.isDurationValid;
  }

  get canCreate(): boolean {
    if (!this.createForm.patientId || !this.createForm.doctorId) return false;
    if (!this.isScheduleReady) return false;
    if (this.selectedDoctor && !this.selectedDoctor.available && !this.forceSchedule) return false;
    return true;
  }

  get doctorConflictMessage(): string {
    if (!this.selectedDoctor || this.selectedDoctor.available) return '';
    return this.selectedDoctor.conflictReason || 'This doctor has a conflicting appointment.';
  }

  private initPatientSearch(): void {
    this.patientSearch$
      .pipe(
        map(value => value.trim()),
        debounceTime(350),
        distinctUntilChanged(),
        switchMap(query => {
          if (query.length < 2) {
            return of({ query, items: [], error: false, skipped: true, err: null });
          }
          this.patientLoading = true;
          this.patientError = '';
          return this.api.searchPatients(query).pipe(
            map(items => ({ query, items: items ?? [], error: false, skipped: false, err: null })),
            catchError((err) => of({ query, items: [], error: true, skipped: false, err }))
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(({ query, items, error, skipped, err }) => {
        if (skipped) {
          this.patientLoading = false;
          this.patientError = '';
          this.patientSuggestions = [];
          this.patientNoMatches = false;
          return;
        }

        this.patientLoading = false;

        if (error) {
          this.patientError = this.formatHttpError(err, 'Patient search failed');
          this.patientSuggestions = [];
          this.patientNoMatches = false;
          return;
        }

        this.patientSuggestions = (items || []).slice(0, 3);
        this.patientNoMatches = query.length >= 2 && this.patientSuggestions.length === 0;
      });
  }

  private initPatientFilterSearch(): void {
    this.patientFilterSearch$
      .pipe(
        map(value => value.trim()),
        debounceTime(350),
        distinctUntilChanged(),
        switchMap(query => {
          if (query.length < 2) {
            return of({ query, items: [], error: false, skipped: true, err: null });
          }
          this.patientFilterLoading = true;
          this.patientFilterError = '';
          return this.api.searchPatients(query).pipe(
            map(items => ({ query, items: items ?? [], error: false, skipped: false, err: null })),
            catchError((err) => of({ query, items: [], error: true, skipped: false, err }))
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(({ query, items, error, skipped, err }) => {
        if (skipped) {
          this.patientFilterLoading = false;
          this.patientFilterError = '';
          this.patientFilterSuggestions = [];
          this.patientFilterNoMatches = false;
          return;
        }

        this.patientFilterLoading = false;

        if (error) {
          this.patientFilterError = this.formatHttpError(err, 'Patient search failed');
          this.patientFilterSuggestions = [];
          this.patientFilterNoMatches = false;
          return;
        }

        this.patientFilterSuggestions = (items || []).slice(0, 3);
        this.patientFilterNoMatches = query.length >= 2 && this.patientFilterSuggestions.length === 0;
      });
  }

  private initDoctorSearch(): void {
    this.doctorSearch$
      .pipe(
        map(value => value.trim()),
        debounceTime(350),
        switchMap(query => this.loadDoctorSuggestions(query)),
        takeUntil(this.destroy$)
      )
      .subscribe(({ query, options, error, skipped, err }) => {
        this.doctorLoading = false;

        if (skipped) {
          this.doctorError = '';
          this.doctorOptions = [];
          this.doctorNoMatches = false;
          return;
        }

        if (error) {
          this.doctorError = this.formatHttpError(err, 'Doctor search failed');
          this.doctorOptions = [];
          this.doctorNoMatches = false;
          return;
        }

        this.doctorOptions = options;
        const trimmed = query.trim();
        const hasQuery = trimmed.length >= 2;
        const isDefault = trimmed.length === 0;
        this.doctorNoMatches = this.isScheduleReady && (hasQuery || isDefault) && options.length === 0;
        if (!this.isScheduleReady) {
          this.doctorNoMatches = false;
        }
        if (!query && this.doctorNoMatches) {
          this.doctorError = '';
        }
      });
  }

  private refreshDoctorSuggestions(): void {
    this.validateScheduledAt();
    this.validateDuration();

    if (!this.isScheduleReady) {
      this.resetDoctorSelection();
      return;
    }

    if (this.selectedDoctor) {
      this.selectedDoctor = null;
      this.createForm.doctorId = '';
    }

    this.forceSchedule = false;
    this.doctorOptions = [];
    this.doctorNoMatches = false;
    this.doctorSearch$.next(this.doctorSearch || '');
  }

  private resetDoctorSelection(): void {
    this.doctorOptions = [];
    this.doctorNoMatches = false;
    this.doctorSearch = '';
    this.selectedDoctor = null;
    this.createForm.doctorId = '';
    this.forceSchedule = false;
    this.doctorLoading = false;
    this.doctorError = '';
  }

  private loadDoctorSuggestions(query: string) {
    if (!this.isScheduleReady) {
      return of({ query, options: [] as DoctorOption[], error: false, skipped: true, err: null });
    }

    const trimmed = query.trim();

    if (!trimmed) {
      this.doctorLoading = true;
      this.doctorError = '';
      return this.api.listDoctors(10).pipe(
        switchMap(doctors => this.buildDoctorOptions(doctors ?? [], trimmed)),
        map(options => {
          const finalOptions = options.filter(option => option.available).slice(0, 3);
          return { query: trimmed, options: finalOptions, error: false, skipped: false, err: null };
        }),
        catchError((err) => of({ query: trimmed, options: [] as DoctorOption[], error: true, skipped: false, err }))
      );
    }

    if (trimmed.length < 2) {
      return of({ query: trimmed, options: [] as DoctorOption[], error: false, skipped: true, err: null });
    }

    this.doctorLoading = true;
    this.doctorError = '';

    return this.fetchDoctors(trimmed).pipe(
      switchMap(doctors => this.buildDoctorOptions(doctors, trimmed)),
      map(options => ({ query: trimmed, options: options.slice(0, 3), error: false, skipped: false, err: null })),
      catchError((err) => of({ query: trimmed, options: [] as DoctorOption[], error: true, skipped: false, err }))
    );
  }

  private fetchDoctors(query: string) {
    return this.api.searchDoctors(query, 3).pipe(
      map(users => users ?? []),
      catchError((err) => throwError(() => err))
    );
  }

  private buildDoctorOptions(doctors: any[], query: string) {
    if (!doctors.length) return of([] as DoctorOption[]);

    const start = this.parseLocalDateTime(this.createForm.scheduledAt);
    const duration = Number(this.createForm.durationMinutes);
    if (!start || !Number.isFinite(duration)) {
      return of([] as DoctorOption[]);
    }

    const end = new Date(start.getTime() + duration * 60000);
    const candidates = query.trim() ? doctors.slice(0, 3) : doctors;

    return forkJoin(
      candidates.map(doctor => this.buildDoctorOption(doctor, start, end))
    );
  }

  private buildDoctorOption(doctor: any, start: Date, end: Date) {
    const id = doctor?.keycloakId ?? doctor?.id ?? '';
    return this.checkDoctorAvailability(id, start, end).pipe(
      map((availability) => ({
        id,
        displayName: this.getDoctorDisplayName(doctor),
        email: doctor?.email ?? '',
        role: doctor?.role ?? 'DOCTOR',
        available: availability.available,
        conflictReason: availability.conflictReason,
        raw: doctor
      }))
    );
  }

  private checkDoctorAvailability(doctorId: string, start: Date, end: Date) {
    if (!doctorId) {
      return of({
        available: false,
        conflictReason: 'Doctor identifier missing.'
      });
    }

    const from = this.formatDateTime(start);
    const to = this.formatDateTime(end);

    return this.api.checkDoctorAvailability(doctorId, from, to).pipe(
      map(result => ({
        available: result?.available ?? true,
        conflictReason: result?.conflictReason
      })),
      catchError(() => of({
        available: false,
        conflictReason: 'Availability check unavailable. You may force schedule.'
      }))
    );
  }

  private validateScheduledAt(): void {
    if (!this.createForm.scheduledAt) {
      this.scheduledAtError = '';
      return;
    }
    this.scheduledAtError = this.isPastDatetime(this.createForm.scheduledAt)
      ? 'Scheduled time must be in the future.'
      : '';
  }

  private validateDuration(): void {
    const duration = Number(this.createForm.durationMinutes);
    if (!duration) {
      this.durationError = 'Duration is required.';
      return;
    }
    if (duration < 10 || duration > 180) {
      this.durationError = 'Duration must be between 10 and 180 minutes.';
      return;
    }
    this.durationError = '';
  }

  private getDoctorDisplayName(doctor: any): string {
    const name = `${doctor?.firstName ?? ''} ${doctor?.lastName ?? ''}`.trim();
    return name || doctor?.username || doctor?.keycloakId || doctor?.id || 'Doctor';
  }

  private formatHttpError(err: any, fallback: string): string {
    if (!err) return fallback;
    const status = err?.status;
    const message = err?.error?.message || err?.message;
    if (status && message) return `${fallback} (${status}): ${message}`;
    if (status) return `${fallback} (${status})`;
    if (message) return `${fallback}: ${message}`;
    return fallback;
  }

  getPatientLabel(id: number | string): string {
    const match = this.patients.find(p => String(p.id) === String(id));
    if (!match) return String(id);
    return `${match.firstName ?? ''} ${match.lastName ?? ''}`.trim() || String(id);
  }

  getDoctorLabel(id: string): string {
    const match = this.doctors.find(d => d.keycloakId === id);
    if (!match) return id;
    const name = `${match.firstName ?? ''} ${match.lastName ?? ''}`.trim();
    return name || match.username || id;
  }

  statusClass(status: AppointmentStatus): string {
    if (status === 'CONFIRMED') return 'bg-soft-primary text-primary';
    if (status === 'CANCELLED' || status === 'NO_SHOW') return 'bg-soft-danger text-danger';
    return 'bg-soft-warning text-warning';
  }

  prevMonth(): void {
    const current = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    current.setMonth(current.getMonth() - 1);
    this.calendarMonth = current;
    this.loadAppointments();
  }

  nextMonth(): void {
    const current = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    current.setMonth(current.getMonth() + 1);
    this.calendarMonth = current;
    this.loadAppointments();
  }

  selectCalendarDay(day: { date: Date | null }): void {
    if (!day?.date) return;
    this.selectedDate = day.date;
  }

  get calendarMonthLabel(): string {
    return this.calendarMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  }

  get selectedDateLabel(): string {
    return this.selectedDate.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  get dayAppointments(): any[] {
    if (!this.selectedDate) return [];
    const key = this.dateKey(this.selectedDate);
    return (this.appointments || []).filter(item => this.dateKey(new Date(item.scheduledAt)) === key);
  }

  private isPastDatetime(value: string): boolean {
    if (!value) return false;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return false;
    const min = this.toLocalDateTimeMin(new Date());
    const minDate = new Date(min);
    if (Number.isNaN(minDate.getTime())) return date.getTime() < Date.now();
    return date.getTime() < minDate.getTime();
  }

  private toLocalDateTimeMin(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private parseLocalDateTime(value: string): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private formatDateTime(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private normalizeDateTime(value: string): string {
    if (!value) return value;
    return value.length === 16 ? `${value}:00` : value;
  }

  private toLocalInput(value: string): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
      if (!item?.scheduledAt) return;
      const key = this.dateKey(new Date(item.scheduledAt));
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

  dateKey(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private syncSelectedDate(): void {
    const currentMonthKey = `${this.calendarMonth.getFullYear()}-${this.calendarMonth.getMonth()}`;
    const selectedMonthKey = `${this.selectedDate.getFullYear()}-${this.selectedDate.getMonth()}`;
    if (currentMonthKey !== selectedMonthKey) {
      this.selectedDate = new Date(this.calendarMonth.getFullYear(), this.calendarMonth.getMonth(), 1);
    }
  }
}
