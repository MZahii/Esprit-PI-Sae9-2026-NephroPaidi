import {
  AfterViewInit,
  Component,
  HostListener,
  OnDestroy,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { filter, firstValueFrom, Subscription } from 'rxjs';
import { AuthStorageService } from '../../core/auth/auth-storage.service';
import { getValidToken, logout } from '../../core/auth/keycloak.service';
import { TemplateAssetsService } from '../../core/services/template-assets.service';
import { environment } from '../../../environments/environment';

declare const window: any;

const BACKOFFICE_STYLES: string[] = [
  'assets/backoffice/css/bootstrap.min.css',
  'assets/backoffice/vendors/css/daterangepicker.min.css',
  'assets/backoffice/css/theme.min.css'
];

const BACKOFFICE_SCRIPTS: string[] = [
  'assets/backoffice/vendors/js/vendors.min.js',
  'assets/backoffice/vendors/js/daterangepicker.min.js',
  'assets/backoffice/js/common-init.min.js',
  'assets/backoffice/js/theme-customizer-init.min.js'
];

interface BackofficeNavChild {
  label: string;
  route?: string;
  queryParams?: Record<string, string>;
  implemented: boolean;
  note?: string;
}

interface BackofficeNavItem {
  key: string;
  label: string;
  icon: string;
  route?: string;
  exact?: boolean;
  children?: BackofficeNavChild[];
}

interface HeaderNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  targetUserId?: number;
  read: boolean;
  createdAt: string;
}

@Component({
  selector: 'app-backoffice-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './backoffice-layout.html',
  styleUrls: ['./backoffice-layout.scss']
})
export class BackofficeLayoutComponent implements OnInit, AfterViewInit, OnDestroy {
  user: any;
  role = '';
  navigationItems: BackofficeNavItem[] = [];

  private navSub?: Subscription;

  userMenuOpen = false;
  notificationsOpen = false;
  loadingNotifications = false;
  notifications: HeaderNotification[] = [];
  unreadCount = 0;
  hasNewNotificationPulse = false;

  private notificationsTimer?: ReturnType<typeof setInterval>;
  private lastNotificationId?: number;
  private readonly notificationsPollMs = 15000;

  openMenus: Record<string, boolean> = {
    accounts: false,
    staff: false,
    patients: false,
    clinic: false,
    pharmacy: false,
    procedures: false,
    communication: false,
    appointments: false,
    doctorClinical: false
  };

  // updated with your real files
  logoPath = 'assets/backoffice/images/logo/Logo_fin.png';
  avatarPath = 'assets/backoffice/images/avatar/profil.png';

  constructor(
    private authStorage: AuthStorageService,
    private templateAssetsService: TemplateAssetsService,
    private router: Router,
    private http: HttpClient
  ) {
    this.user = this.authStorage.getUser();
    this.role = this.authStorage.getRole() ?? '';
    this.navigationItems = this.buildNavigationItems();
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get isHr(): boolean {
    return this.role === 'HR';
  }

  get isReceptionist(): boolean {
    return this.role === 'RECEPTIONIST';
  }

  get isNurse(): boolean {
    return this.role === 'NURSE';
  }

  get isDoctor(): boolean {
    return this.role === 'DOCTOR';
  }

  get isSurgeon(): boolean {
    return this.role === 'SURGEON';
  }

  get isPharmacist(): boolean {
    return this.role === 'PHARMACIST';
  }

  get canViewMyContract(): boolean {
    return !this.isAdmin;
  }

  get userId(): number | null {
    const rawId = this.user?.userId;
    if (rawId === undefined || rawId === null || rawId === '') return null;
    const parsed = Number(rawId);
    return Number.isNaN(parsed) ? null : parsed;
  }

  get displayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }

    return this.user?.username ?? 'User';
  }

  get displayEmail(): string {
    return this.user?.email ?? '';
  }

  get roleBadgeClass(): string {
    if (this.isAdmin) return 'bg-soft-primary text-primary';
    if (this.isHr) return 'bg-soft-warning text-warning';
    if (this.isSurgeon) return 'bg-soft-success text-success';
    return 'bg-soft-secondary text-muted';
  }

  get headerTitle(): string {
    if (this.isAdmin) {
      return 'Admin Control Center';
    }

    if (this.isHr) {
      return 'HR Operations Dashboard';
    }

    if (this.isSurgeon) {
      return 'Procedure Service Workspace';
    }

    return 'Backoffice Dashboard';
  }

  get headerSubtitle(): string {
    if (this.isAdmin) {
      return 'Manage HR access and supervise staff, patients, and clinic resources.';
    }

    if (this.isHr) {
      return 'Manage staff, guardians, patient profiles, and clinic resources.';
    }

    if (this.isSurgeon) {
      return 'Manage surgical and dialysis workflows from your procedure-service module.';
    }

    return 'Manage your backoffice workspace.';
  }

  private buildNavigationItems(): BackofficeNavItem[] {
    const dashboardRoute = (this.isAdmin || this.isHr)
      ? '/backoffice/user-admin'
      : '/backoffice/dashboard';

    const items: BackofficeNavItem[] = [
      {
        key: 'dashboard',
        label: 'Dashboard',
        icon: 'feather-airplay',
        route: dashboardRoute,
        exact: true
      }
    ];

    if (this.isAdmin) {
      items.push({
        key: 'logs',
        label: 'Audit Logs',
        icon: 'feather-activity',
        route: '/backoffice/logs',
        exact: true
      });

      items.push({
        key: 'clinicalLogs',
        label: 'Clinical Audit Logs',
        icon: 'feather-file-text',
        route: '/backoffice/clinical-audit-logs',
        exact: true
      });

      items.push({
        key: 'accounts',
        label: 'Accounts',
        icon: 'feather-users',
        children: [
          {
            label: 'Create HR Account',
            route: '/backoffice/create-hr',
            implemented: true
          },
          {
            label: 'HR List',
            route: '/backoffice/hr-list',
            implemented: true
          },
          {
            label: 'HR Contracts List',
            route: '/backoffice/contracts',
            queryParams: { scope: 'HR' },
            implemented: true
          }
        ]
      });
    }

    if (this.isHr) {
      items.push({
        key: 'accounts',
        label: 'Accounts',
        icon: 'feather-users',
        children: [
          {
            label: 'Create Internal User',
            route: '/backoffice/create-internal-user',
            implemented: true
          },
          {
            label: 'Create Staff Account',
            route: '/backoffice/create-staff',
            implemented: true
          },
          {
            label: 'Staff Accounts List',
            route: '/backoffice/staff',
            implemented: true
          }
        ]
      });
    }

    if (this.isAdmin || this.isHr) {
      items.push(
        {
          key: 'staff',
          label: 'Staff',
          icon: 'feather-briefcase',
          children: [
            ...(this.isAdmin
              ? [{
                label: 'Staff & Roles Details',
                route: '/backoffice/staff-details',
                implemented: true
              } as BackofficeNavChild]
              : []),
            {
              label: 'StaffContracts List',
              route: '/backoffice/contracts',
              implemented: true
            },
            ...(this.isHr
              ? [
                {
                  label: 'Create Contract',
                  route: '/backoffice/contracts/create',
                  implemented: true
                } as BackofficeNavChild
              ]
              : [])
          ]
        },
      );
    }

    if (this.isAdmin || this.isHr) {
      items.push(
        {
          key: 'clinic',
          label: 'Clinic Resources',
          icon: 'feather-grid',
          children: [
            {
              label: 'Resources List',
              implemented: false,
              note: 'Coming soon'
            },
            ...(this.isHr
              ? [
                {
                  label: 'Create Clinic Resource',
                  implemented: false,
                  note: 'Coming soon'
                } as BackofficeNavChild
              ]
              : []),
            {
              label: 'Rooms / Beds / Dialysis Machines',
              implemented: false,
              note: 'Coming soon'
            }
          ]
        }
      );
    }

    if (this.isAdmin || this.isReceptionist) {
      items.push({
        key: 'patients',
        label: 'Patients',
        icon: 'feather-heart',
        children: [
          {
            label: 'Patients List',
            route: '/backoffice/patients',
            implemented: true
          },
          ...(this.isReceptionist
            ? [
              {
                label: 'Create Guardian + Patient Profile',
                route: '/backoffice/create-guardian-patient',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Existing Guardian + New Patient',
                route: '/backoffice/existing-guardian-patient',
                implemented: true
              } as BackofficeNavChild
            ]
            : []),
          {
            label: 'Guardians & Linked Profiles',
            route: '/backoffice/guardians-linked',
            implemented: true
          }
        ]
      });
    }

    if (this.isReceptionist || this.isNurse || this.isDoctor) {
      items.push({
        key: 'communication',
        label: 'Communication',
        icon: 'feather-message-square',
        children: [
          {
            label: 'Inbox',
            route: '/backoffice/communication/inbox',
            implemented: true
          },
          {
            label: 'Templates',
            route: '/backoffice/communication/templates',
            implemented: true
          },
          {
            label: 'Analytics',
            route: '/backoffice/communication/analytics',
            implemented: true
          }
        ]
      });
    }

    if (this.isPharmacist || this.isAdmin || this.isNurse) {
      items.push({
        key: 'pharmacy',
        label: 'Pharmacy',
        icon: 'feather-package',
        children: [
          {
            label: 'Medications',
            route: '/backoffice/pharmacy/medications',
            implemented: true
          },
          {
            label: 'Stock',
            route: '/backoffice/pharmacy/stock',
            implemented: true
          },
          {
            label: 'Suppliers',
            route: '/backoffice/pharmacy/suppliers',
            implemented: true
          },
          {
            label: 'Dispensations',
            route: '/backoffice/pharmacy/dispensations',
            implemented: true
          }
        ]
      });
    }

    if (this.isReceptionist) {
      items.push({
        key: 'appointments',
        label: 'Appointments',
        icon: 'feather-calendar',
        children: [
          {
            label: 'Appointments Board',
            route: '/backoffice/appointments',
            implemented: true
          },
          {
            label: 'Clinical Appointments',
            route: '/backoffice/appointments-clinical',
            implemented: true
          },
          {
            label: 'Appointment Requests',
            route: '/backoffice/appointments/requests',
            implemented: true
          }
        ]
      });
    }

    if (this.isDoctor) {
      items.push({
        key: 'doctorClinical',
        label: 'Clinical Workspace',
        icon: 'feather-activity',
        children: [
          {
            label: 'Today Appointments',
            route: '/backoffice/doctor/today',
            implemented: true
          },
          {
            label: 'Consultations',
            route: '/backoffice/consultations',
            implemented: true
          },
          {
            label: 'Lab Requests',
            route: '/backoffice/consultations/lab-requests',
            implemented: true
          }
        ]
      });
    }

    if (this.isSurgeon) {
      items.push({
        key: 'procedures',
        label: 'Procedure Service',
        icon: 'feather-activity',
        children: [
          {
            label: 'Surgical Management',
            route: '/backoffice/procedures/surgical',
            implemented: true
          },
          {
            label: 'Surgical Advanced',
            route: '/backoffice/procedures/surgical-advanced',
            implemented: true
          },
          {
            label: 'Dialysis Management',
            route: '/backoffice/procedures/dialysis',
            implemented: true
          },
          {
            label: 'Dialysis Sessions',
            route: '/backoffice/procedures/dialysis-sessions',
            implemented: true
          },
          {
            label: 'Dialysis Outcomes',
            route: '/backoffice/procedures/dialysis-outcomes',
            implemented: true
          },
          {
            label: 'Dialysis Prescriptions',
            route: '/backoffice/procedures/dialysis-prescriptions',
            implemented: true
          }
        ]
      });
    }

    return items;
  }

  async ngOnInit(): Promise<void> {
    this.templateAssetsService.clearAll();

    document.body.classList.remove('public-body', 'frontoffice-body');
    document.body.classList.add('backoffice-body');

    await this.templateAssetsService.loadGroup(
      'backoffice',
      BACKOFFICE_STYLES,
      BACKOFFICE_SCRIPTS
    );

    this.navSub = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        this.userMenuOpen = false;
        this.notificationsOpen = false;
        setTimeout(() => {
          this.refreshFeatherIcons();
        }, 120);
      });

    await this.loadNotifications(true);
    this.notificationsTimer = setInterval(() => {
      this.loadNotifications();
    }, this.notificationsPollMs);
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.refreshFeatherIcons();
    }, 300);
  }

  ngOnDestroy(): void {
    this.navSub?.unsubscribe();
    if (this.notificationsTimer) {
      clearInterval(this.notificationsTimer);
    }
    this.templateAssetsService.unloadGroup('backoffice');
    document.body.classList.remove('backoffice-body');
  }

  toggleMenu(event: MouseEvent, key: string): void {
    event.preventDefault();
    event.stopPropagation();

    const willOpen = !this.openMenus[key];

    Object.keys(this.openMenus).forEach(menuKey => {
      this.openMenus[menuKey] = false;
    });

    this.openMenus[key] = willOpen;

    setTimeout(() => {
      this.refreshFeatherIcons();
    }, 0);
  }

  isMenuOpen(key: string): boolean {
    return !!this.openMenus[key];
  }

  toggleUserMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = false;
    this.userMenuOpen = !this.userMenuOpen;

    setTimeout(() => {
      this.refreshFeatherIcons();
    }, 0);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target?.closest('.notifications-menu')) {
      this.notificationsOpen = false;
    }
    if (!target?.closest('.user-menu')) {
      this.userMenuOpen = false;
    }
  }

  async toggleNotifications(event: MouseEvent): Promise<void> {
    event.stopPropagation();
    this.userMenuOpen = false;
    this.notificationsOpen = !this.notificationsOpen;

    if (this.notificationsOpen) {
      await this.loadNotifications();
      if (this.unreadCount > 0) {
        await this.markAllVisibleAsRead();
      }
    }
  }

  async markAsRead(notificationId: number): Promise<void> {
    if (!notificationId) return;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      await firstValueFrom(this.http.patch(
        `${environment.apiBaseUrl}/api/observability/notifications/${notificationId}/read`,
        {},
        { headers }
      ));
      const target = this.notifications.find((n) => n.id === notificationId);
      if (target) {
        target.read = true;
      }
      this.unreadCount = this.notifications.filter((n) => !n.read).length;
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  }

  async markAllVisibleAsRead(): Promise<void> {
    const unread = this.notifications.filter((n) => !n.read);
    if (unread.length === 0) return;
    await Promise.all(unread.map((n) => this.markAsRead(n.id)));
  }

  private async loadNotifications(initial = false): Promise<void> {
    if (this.loadingNotifications) return;
    this.loadingNotifications = true;
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const query = this.userId != null ? `?userId=${this.userId}` : '';
      const response = await firstValueFrom(this.http.get<HeaderNotification[] | unknown>(
        `${environment.apiBaseUrl}/api/observability/notifications${query}`,
        { headers }
      ));
      const fetched = Array.isArray(response) ? response : [];
      this.notifications = fetched.slice(0, 8);
      this.unreadCount = this.notifications.filter((n) => !n.read).length;

      const newestId = this.notifications[0]?.id;
      if (!initial && newestId != null && this.lastNotificationId != null && newestId !== this.lastNotificationId) {
        this.hasNewNotificationPulse = true;
        setTimeout(() => {
          this.hasNewNotificationPulse = false;
        }, 5000);
      }
      if (newestId != null) {
        this.lastNotificationId = newestId;
      }
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      this.loadingNotifications = false;
    }
  }

  private refreshFeatherIcons(): void {
    try {
      if (window?.feather) {
        window.feather.replace();
      }
    } catch (error) {
      console.log('Feather init skipped:', error);
    }
  }

  async onLogout(): Promise<void> {
    this.userMenuOpen = false;
    await logout();
  }
}
