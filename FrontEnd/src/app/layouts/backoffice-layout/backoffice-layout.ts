import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {
  NavigationEnd,
  NavigationStart,
  Params,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { firstValueFrom, Subscription } from 'rxjs';
import { AuthStorageService } from '../../core/auth/auth-storage.service';
import { getValidToken, logout } from '../../core/auth/keycloak.service';
import { TemplateAssetsService } from '../../core/services/template-assets.service';
import { InterfacePreferencesService } from '../../core/services/interface-preferences.service';
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

interface DockedFilterGroup {
  node: HTMLElement;
  placeholder: Comment;
  parentPanel?: HTMLElement;
  compactMenu?: DockedFilterCompactMenu;
}

interface DockedFilterCompactMenu {
  wrapper: HTMLElement;
  panel: HTMLElement;
  button: HTMLButtonElement;
  movedControls: Array<{
    node: HTMLElement;
    parent: Node;
    nextSibling: ChildNode | null;
  }>;
}

interface DockedStatGroup {
  node: HTMLElement;
  placeholder: Comment;
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
  notificationsEnabled = true;

  private notificationsTimer?: ReturnType<typeof setInterval>;
  private filterDockTimer?: ReturnType<typeof setTimeout>;
  private filterDockObserver?: MutationObserver;
  private dockedFilterGroups: DockedFilterGroup[] = [];
  private dockedStatGroups: DockedStatGroup[] = [];
  private lastNotificationId?: number;
  private readonly notificationsPollMs = 15000;
  private lastManualMenuToggleAt = 0;
  private readonly manualMenuToggleGraceMs = 900;
  private destroyed = false;
  hasPageStats = false;
  statsPanelOpen = false;

  openMenus: Record<string, boolean> = {
    accounts: false,
    staff: false,
    patients: false,
    clinic: false,
    pharmacy: false,
    procedures: false,
    ops: false,
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
    private interfacePreferences: InterfacePreferencesService,
    private router: Router,
    private http: HttpClient,
    private elementRef: ElementRef<HTMLElement>,
    private cdr: ChangeDetectorRef
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

  get isLabAgent(): boolean {
    return this.role === 'LAB_AGENT';
  }

  get canUseInternalStaffMessaging(): boolean {
    return this.isAdmin
      || this.isHr
      || this.isDoctor
      || this.isNurse
      || this.isReceptionist
      || this.isPharmacist
      || this.isLabAgent
      || this.isSurgeon;
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
    if (this.isPharmacist) return 'bg-soft-info text-info';
    if (this.isSurgeon) return 'bg-soft-success text-success';
    if (this.isLabAgent) return 'bg-soft-info text-info';
    return 'bg-soft-secondary text-muted';
  }

  get homeRoute(): string {
    return this.isPharmacist ? '/backoffice/pharmacy/dashboard' : '/backoffice/dashboard';
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

    if (this.isLabAgent) {
      return 'Lab Agent Workspace';
    }

    if (this.isPharmacist) {
      return 'Pharmacy Workspace';
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

    if (this.isLabAgent) {
      return 'Receive lab orders, upload result files, and monitor AI-assisted clinical analysis.';
    }

    if (this.isPharmacist) {
      return 'Manage prescriptions, stock movements, suppliers, and dispensing operations.';
    }

    return 'Manage your backoffice workspace.';
  }

  private buildNavigationItems(): BackofficeNavItem[] {
    const items: BackofficeNavItem[] = [
      {
        key: 'dashboard',
        label: 'Dashboard',
        icon: 'feather-airplay',
        route: this.isPharmacist ? '/backoffice/pharmacy/dashboard' : '/backoffice/dashboard',
        exact: true
      }
    ];

    if (this.isAdmin) {
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
            ...(this.isAdmin
              ? [
                {
                  label: 'Hospital Structure',
                  route: '/backoffice/hospital-structure',
                  implemented: true
                } as BackofficeNavChild,
                {
                  label: 'Office Assignments',
                  route: '/backoffice/office-assignments',
                  implemented: true
                } as BackofficeNavChild
              ]
              : []),
            ...(this.isHr
              ? [
                {
                  label: 'Equipment Inventory',
                  route: '/backoffice/equipment-inventory',
                  implemented: true
                } as BackofficeNavChild
              ]
              : []),
            ...(this.isHr
              ? [
                {
                  label: 'Equipment Placement',
                  route: '/backoffice/equipment-placement',
                  implemented: true
                } as BackofficeNavChild,
                {
                  label: 'Staff Placements',
                  route: '/backoffice/staff-placements',
                  implemented: true
                } as BackofficeNavChild
              ]
              : [])
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

    if (this.canUseInternalStaffMessaging) {
      items.push({
        key: 'internalStaffMessaging',
        label: 'Internal Staff Messaging',
        icon: 'feather-message-circle',
        route: '/backoffice/internal-staff-messaging',
        exact: true
      });
    }

    if (this.isPharmacist || this.isNurse) {
      items.push({
        key: 'pharmacy',
        label: 'Pharmacy',
        icon: 'feather-package',
        children: [
          {
            label: 'Prescriptions',
            route: '/backoffice/pharmacy/prescriptions',
            implemented: true
          },
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
            label: 'Dispensations',
            route: '/backoffice/pharmacy/dispensations',
            implemented: true
          },
          {
            label: 'Movements',
            route: '/backoffice/pharmacy/movements',
            implemented: true
          },
          {
            label: 'Alerts',
            route: '/backoffice/pharmacy/alerts',
            implemented: true
          },
          ...(this.isPharmacist || this.isAdmin
            ? [{
                label: 'Equipment',
                route: '/backoffice/pharmacy/equipment',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Dialysis Stock',
                route: '/backoffice/pharmacy/dialysis-stock',
                implemented: true
              } as BackofficeNavChild]
            : []),
          ...(this.isPharmacist || this.isAdmin
            ? [{
                label: 'Suppliers',
                route: '/backoffice/pharmacy/suppliers',
                implemented: true
              } as BackofficeNavChild]
            : []),
        ]
      });
    }

    if (this.isNurse) {
      items.push({
        key: 'ops',
        label: 'OPS Workflow',
        icon: 'feather-clipboard',
        children: [
          {
            label: 'Hospitalizations',
            route: '/backoffice/nurse/hospitalizations',
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
            label: 'Appointments',
            route: '/backoffice/appointments',
            implemented: true
          },
          {
            label: 'Requests',
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
          }
        ]
      });
    }

    if (this.isLabAgent) {
      items.push({
        key: 'labWorkflow',
        label: 'Lab Workflow',
        icon: 'feather-droplet',
        children: [
          {
            label: 'Lab Requests Inbox',
            route: '/backoffice/lab-inbox',
            implemented: true
          }
        ]
      });
    }

    if (this.isSurgeon || this.isReceptionist) {
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
          ...(this.isSurgeon
            ? [
              {
                label: 'Surgical Advanced',
                route: '/backoffice/procedures/surgical-advanced',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Dialysis Management',
                route: '/backoffice/procedures/dialysis',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Dialysis Sessions',
                route: '/backoffice/procedures/dialysis-sessions',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Dialysis Outcomes',
                route: '/backoffice/procedures/dialysis-outcomes',
                implemented: true
              } as BackofficeNavChild,
              {
                label: 'Dialysis Prescriptions',
                route: '/backoffice/procedures/dialysis-prescriptions',
                implemented: true
              } as BackofficeNavChild
            ]
            : [])
        ]
      });
    }

    return items;
  }

  async ngOnInit(): Promise<void> {
    this.templateAssetsService.clearAll();

    document.body.classList.remove('public-body', 'frontoffice-body');
    document.body.classList.add('backoffice-body', 'admin-redesign');
    this.updateRouteBodyClasses();

    await this.templateAssetsService.loadGroup(
      'backoffice',
      BACKOFFICE_STYLES,
      BACKOFFICE_SCRIPTS
    );
    this.applyPreferences(this.authStorage.getPreferences());
    await this.loadAccountPreferences();

    this.navSub = this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.restoreDockedFilters();
        this.restoreDockedStats();
        return;
      }

      if (event instanceof NavigationEnd) {
        this.updateRouteBodyClasses();
        this.userMenuOpen = false;
        this.notificationsOpen = false;
        if (Date.now() - this.lastManualMenuToggleAt > this.manualMenuToggleGraceMs) {
          this.syncOpenMenusWithRoute();
        }
        setTimeout(() => {
          this.refreshFeatherIcons();
          this.interfacePreferences.setLanguage(this.authStorage.getPreferences().preferredLanguage);
        }, 120);
        this.scheduleFilterDocking(0);
      }
    });

    this.syncOpenMenusWithRoute();

    if (this.notificationsEnabled) {
      await this.loadNotifications(true);
      this.notificationsTimer = setInterval(() => {
        this.loadNotifications();
      }, this.notificationsPollMs);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.refreshFeatherIcons();
      this.scheduleFilterDocking(0);
    }, 300);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.navSub?.unsubscribe();
    this.restoreDockedFilters();
    this.restoreDockedStats();
    this.filterDockObserver?.disconnect();
    if (this.filterDockTimer) {
      clearTimeout(this.filterDockTimer);
    }
    if (this.notificationsTimer) {
      clearInterval(this.notificationsTimer);
    }
    this.templateAssetsService.unloadGroup('backoffice');
    this.interfacePreferences.stop();
    document.body.classList.remove('backoffice-body', 'admin-redesign', 'backoffice-dashboard-route', 'backoffice-has-page-stats');
  }

  private scheduleFilterDocking(delay = 0): void {
    if (this.filterDockTimer) {
      clearTimeout(this.filterDockTimer);
    }

    this.filterDockTimer = setTimeout(() => {
      this.dockPageFilters();
      this.dockPageStats();
      this.watchForLateFilters();
    }, delay);
  }

  private dockPageFilters(): void {
    const host = this.elementRef.nativeElement;
    const dock = host.querySelector<HTMLElement>('.header-filter-dock');
    if (!dock) return;

    this.pruneDisconnectedDockedFilters();

    const candidates = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.nxl-content .filters, .nxl-content .filters-grid'
      )
    ).filter((node) => this.isDockableFilterGroup(node, dock));

    candidates.forEach((node) => {
      const parent = node.parentElement;
      if (!parent) return;

      const placeholder = document.createComment('backoffice-filter-dock-placeholder');
      parent.insertBefore(placeholder, node);

      const parentPanel = node.closest<HTMLElement>('.filters-panel, .staff-filter-panel');
      parentPanel?.classList.add('topbar-filter-panel-docked');

      node.classList.add('topbar-filter-group');
      dock.appendChild(node);
      this.dockedFilterGroups.push({
        node,
        placeholder,
        parentPanel: parentPanel ?? undefined
      });
    });

    this.compactDockedFilterSelects(dock);
  }

  private pruneDisconnectedDockedFilters(): void {
    this.dockedFilterGroups = this.dockedFilterGroups.filter((group) => {
      if (group.node.isConnected && group.placeholder.isConnected) {
        return true;
      }

      group.compactMenu?.wrapper.remove();
      group.node.remove();
      group.placeholder.remove();
      group.parentPanel?.classList.remove('topbar-filter-panel-docked');
      return false;
    });
  }

  private isDockableFilterGroup(node: HTMLElement, dock: HTMLElement): boolean {
    if (dock.contains(node)) return false;
    if (this.dockedFilterGroups.some((group) => group.node === node)) return false;
    if (!node.isConnected) return false;
    if (!node.querySelector('input, select')) return false;
    if (!node.querySelector('input[placeholder], select')) return false;
    if (node.closest('form, .modal, .dropdown-menu, .notifications-dropdown, .user-menu-dropdown')) return false;

    const styles = window.getComputedStyle(node);
    return styles.display !== 'none' && styles.visibility !== 'hidden';
  }

  private compactDockedFilterSelects(dock: HTMLElement): void {
    if (this.dockedFilterGroups.some((group) => !!group.compactMenu)) {
      return;
    }

    const selects = Array.from(
      dock.querySelectorAll<HTMLElement>('.topbar-filter-group select.form-control, .topbar-filter-group select.form-select')
    );
    if (selects.length <= 1) {
      return;
    }

    const movedControls = selects.map((select) => ({
      node: select,
      parent: select.parentNode as Node,
      nextSibling: select.nextSibling
    }));

    const wrapper = document.createElement('div');
    wrapper.className = 'topbar-filter-menu';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'topbar-filter-menu-trigger';
    button.setAttribute('aria-haspopup', 'true');
    button.setAttribute('aria-expanded', 'false');

    const hasSearch = !!dock.querySelector('.topbar-filter-group input');
    button.innerHTML = `
      <span>${hasSearch ? 'Filters & Sort' : 'Sort'}</span>
      <i class="feather-chevron-down" aria-hidden="true"></i>
    `;

    const panel = document.createElement('div');
    panel.className = 'topbar-filter-menu-panel';

    selects.forEach((select) => {
      panel.appendChild(select);
    });

    button.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();

      const nextOpen = !wrapper.classList.contains('open');
      this.closeTopbarFilterMenus();
      wrapper.classList.toggle('open', nextOpen);
      button.setAttribute('aria-expanded', String(nextOpen));
      this.refreshFeatherIcons();
    });

    wrapper.appendChild(button);
    wrapper.appendChild(panel);
    dock.appendChild(wrapper);

    setTimeout(() => this.refreshFeatherIcons(), 0);

    const owner = this.dockedFilterGroups[0];
    if (owner) {
      owner.compactMenu = { wrapper, panel, button, movedControls };
    }
  }

  private closeTopbarFilterMenus(): void {
    this.dockedFilterGroups.forEach((group) => {
      group.compactMenu?.wrapper.classList.remove('open');
      group.compactMenu?.button.setAttribute('aria-expanded', 'false');
    });
  }

  private restoreDockedFilters(): void {
    this.dockedFilterGroups.forEach(({ node, placeholder, parentPanel, compactMenu }) => {
      if (compactMenu) {
        compactMenu.movedControls.forEach((control) => {
          if (control.parent.isConnected) {
            control.parent.insertBefore(
              control.node,
              control.nextSibling?.parentNode === control.parent ? control.nextSibling : null
            );
          }
        });
        compactMenu.wrapper.remove();
      }

      node.classList.remove('topbar-filter-group');
      parentPanel?.classList.remove('topbar-filter-panel-docked');

      if (placeholder.parentNode && node.isConnected) {
        placeholder.parentNode.insertBefore(node, placeholder);
      }

      placeholder.remove();
    });

    this.dockedFilterGroups = [];
  }

  private watchForLateFilters(): void {
    this.filterDockObserver?.disconnect();

    const content = document.querySelector('.nxl-content');
    if (!content) return;

    this.filterDockObserver = new MutationObserver(() => {
      this.scheduleFilterDocking(0);
    });

    this.filterDockObserver.observe(content, {
      childList: true,
      subtree: true
    });
  }

  private dockPageStats(): void {
    const host = this.elementRef.nativeElement;
    const statsBody = host.querySelector<HTMLElement>('.statistics-modal-body');
    if (!statsBody) return;

    if (this.isDashboardRoute()) {
      this.restoreDockedStats();
      this.hasPageStats = false;
      return;
    }

    this.pruneDisconnectedDockedStats();

    const candidates = this.findStatisticGroups(statsBody);
    candidates.forEach((node) => {
      const parent = node.parentElement;
      if (!parent) return;

      const placeholder = document.createComment('backoffice-stat-dock-placeholder');
      parent.insertBefore(placeholder, node);
      node.classList.add('statistics-modal-group');
      statsBody.appendChild(node);
      this.dockedStatGroups.push({ node, placeholder });
    });

    this.hasPageStats = this.dockedStatGroups.length > 0;
    document.body.classList.toggle('backoffice-has-page-stats', this.hasPageStats);
    if (!this.hasPageStats) {
      this.statsPanelOpen = false;
    }
    this.refreshLayoutState();
  }

  private findStatisticGroups(statsBody: HTMLElement): HTMLElement[] {
    const baseGroups = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.nxl-content .stats-grid, .nxl-content .kpi-grid, .nxl-content .resource-overview-grid'
      )
    );

    const roleRows = Array.from(
      document.querySelectorAll<HTMLElement>('.nxl-content .row')
    ).filter((node) => !!node.querySelector('.role-card'));

    return [...baseGroups, ...roleRows].filter((node, index, all) => {
      if (all.indexOf(node) !== index) return false;
      if (statsBody.contains(node)) return false;
      if (this.dockedStatGroups.some((group) => group.node === node)) return false;
      if (!node.isConnected) return false;
      if (!node.querySelector('.stats-card, .kpi-card, .metric-card, .role-card, .resource-overview-card')) return false;
      if (node.closest('.dashboard-page')) return false;

      const styles = window.getComputedStyle(node);
      return styles.display !== 'none' && styles.visibility !== 'hidden';
    });
  }

  private pruneDisconnectedDockedStats(): void {
    this.dockedStatGroups = this.dockedStatGroups.filter((group) => {
      if (group.node.isConnected && group.placeholder.isConnected) {
        return true;
      }

      group.node.remove();
      group.placeholder.remove();
      return false;
    });
  }

  private restoreDockedStats(): void {
    this.dockedStatGroups.forEach(({ node, placeholder }) => {
      node.classList.remove('statistics-modal-group');

      if (placeholder.parentNode && node.isConnected) {
        placeholder.parentNode.insertBefore(node, placeholder);
      }

      placeholder.remove();
    });

    this.dockedStatGroups = [];
    this.hasPageStats = false;
    this.statsPanelOpen = false;
    document.body.classList.remove('backoffice-has-page-stats');
    this.refreshLayoutState();
  }

  private refreshLayoutState(): void {
    if (!this.destroyed) {
      this.cdr.detectChanges();
    }
  }

  private isDashboardRoute(): boolean {
    const path = this.router.url.split('?')[0].split('#')[0];
    return path.endsWith('/dashboard') || path.includes('/dashboard/');
  }

  private updateRouteBodyClasses(): void {
    document.body.classList.toggle('backoffice-dashboard-route', this.isDashboardRoute());
  }

  openStatsPanel(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.statsPanelOpen = true;
    this.refreshLayoutState();
  }

  closeStatsPanel(): void {
    this.statsPanelOpen = false;
    this.refreshLayoutState();
  }

  toggleMenu(event: MouseEvent, key: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.lastManualMenuToggleAt = Date.now();

    const item = this.navigationItems.find((navItem) => navItem.key === key);
    const isActiveRouteMenu = item ? this.isItemRouteActive(item) : false;
    const willOpen = isActiveRouteMenu || !this.openMenus[key];

    Object.keys(this.openMenus).forEach(menuKey => {
      this.openMenus[menuKey] = false;
    });

    this.openMenus[key] = willOpen;

    setTimeout(() => {
      this.openMenus[key] = willOpen;
      this.refreshFeatherIcons();
    }, 0);
  }

  isMenuOpen(key: string): boolean {
    return !!this.openMenus[key];
  }

  isItemRouteActive(item: BackofficeNavItem): boolean {
    if (!item.children?.length) {
      return false;
    }

    return item.children.some((child) => this.isChildRouteActive(child));
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
    if (!target?.closest('.topbar-filter-menu')) {
      this.closeTopbarFilterMenus();
    }
    if (this.statsPanelOpen && !target?.closest('.statistics-modal') && !target?.closest('.statistics-trigger')) {
      this.closeStatsPanel();
    }
  }

  async toggleNotifications(event: MouseEvent): Promise<void> {
    if (!this.notificationsEnabled) {
      return;
    }
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
    if (!this.notificationsEnabled) {
      this.notifications = [];
      this.unreadCount = 0;
      return;
    }
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

  private syncOpenMenusWithRoute(): void {
    Object.keys(this.openMenus).forEach(menuKey => {
      this.openMenus[menuKey] = false;
    });

    const activeParent = this.navigationItems.find((item) => this.isItemRouteActive(item));
    if (activeParent) {
      this.openMenus[activeParent.key] = true;
    }
  }

  isChildRouteActive(child: BackofficeNavChild): boolean {
    if (!child.route) {
      return false;
    }

    const tree = this.router.createUrlTree([child.route], {
      queryParams: child.queryParams as Params | undefined
    });

    return this.router.isActive(tree, {
      paths: 'subset',
      queryParams: child.queryParams ? 'subset' : 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored'
    });
  }

  async onLogout(): Promise<void> {
    this.userMenuOpen = false;
    await logout();
  }

  private async loadAccountPreferences(): Promise<void> {
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      const response = await firstValueFrom(this.http.get<any>(`${environment.apiBaseUrl}/api/users/me/settings`, { headers }));
      const preferences = {
        theme: String(response?.theme ?? 'light'),
        preferredLanguage: String(response?.preferredLanguage ?? 'en'),
        notificationsEnabled: response?.notificationsEnabled !== false
      };
      this.authStorage.setPreferences(preferences);
      this.applyPreferences(preferences);
    } catch {
      // Keep locally cached preferences when settings endpoint is unavailable.
    }
  }

  private applyPreferences(preferences: { theme: string; preferredLanguage: string; notificationsEnabled: boolean }): void {
    const theme = preferences.theme || 'light';
    const language = preferences.preferredLanguage || 'en';
    const effectiveTheme = theme === 'system'
      ? (window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : theme;
    this.notificationsEnabled = preferences.notificationsEnabled !== false;
    document.documentElement.setAttribute('lang', language);
    document.documentElement.setAttribute('data-theme-preference', theme);
    document.body.classList.toggle('np-theme-dark', effectiveTheme === 'dark');
    this.interfacePreferences.setLanguage(language);
  }
}
