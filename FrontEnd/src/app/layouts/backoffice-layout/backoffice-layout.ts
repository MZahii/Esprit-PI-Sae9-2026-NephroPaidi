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
import { InternalStaffMessagingComponent } from '../../pages/backoffice/internal-staff-messaging/internal-staff-messaging';
import { CreateHr } from '../../pages/backoffice/create-hr/create-hr';
import { CreateStaff } from '../../pages/backoffice/create-staff/create-staff';
import { CreateContract } from '../../pages/backoffice/create-contract/create-contract';
import { StaffUserDetails } from '../../pages/backoffice/staff-user-details/staff-user-details';

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
  queryParams?: Record<string, string>;
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

interface DockedActionGroup {
  node: HTMLElement;
  placeholder: Comment;
}

interface FancySelectBinding {
  select: HTMLSelectElement;
  wrapper: HTMLElement;
  button: HTMLButtonElement;
  list: HTMLElement;
  observer: MutationObserver;
  changeHandler: () => void;
}

interface FancyDateBinding {
  input: HTMLInputElement;
  wrapper: HTMLElement;
  button: HTMLButtonElement;
  panel: HTMLElement;
  observer: MutationObserver;
  changeHandler: () => void;
  visibleMonth: Date;
}

@Component({
  selector: 'app-backoffice-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, InternalStaffMessagingComponent, CreateHr, CreateStaff, CreateContract, StaffUserDetails],
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
  messagesOpen = false;
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
  private dockedActionGroups: DockedActionGroup[] = [];
  private fancySelectBindings: FancySelectBinding[] = [];
  private fancyDateBindings: FancyDateBinding[] = [];
  private suppressNativeControlClickUntil = 0;
  private lastNotificationId?: number;
  private readonly notificationsPollMs = 15000;
  private lastManualMenuToggleAt = 0;
  private readonly manualMenuToggleGraceMs = 900;
  private destroyed = false;
  hasPageStats = false;
  hasPageActions = false;
  statsPanelOpen = false;
  createHrPanelOpen = false;
  createContractPanelOpen = false;
  staffDetailsPanelOpen = false;
  staffDetailsUserId: number | null = null;

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

  get showCreateHrTool(): boolean {
    const path = this.router.url.split('?')[0];
    return (this.isAdmin && path === '/backoffice/hr-list')
      || (this.isHr && path === '/backoffice/staff');
  }

  get createAccountToolLabel(): string {
    return this.isHr ? 'Create Staff Account' : 'Create HR Account';
  }

  get isCreateStaffTool(): boolean {
    return this.isHr && this.router.url.split('?')[0] === '/backoffice/staff';
  }

  get showPageToolsShell(): boolean {
    return this.hasPageStats || this.hasPageActions || this.showCreateHrTool || this.isActionDockRoute();
  }

  private isActionDockRoute(): boolean {
    const path = this.router.url.split('?')[0];
    return path === '/backoffice/contracts' || path === '/backoffice/office-assignments';
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
        key: 'hrList',
        label: 'HR List',
        icon: 'feather-users',
        route: '/backoffice/hr-list',
        exact: true
      });
      items.push({
        key: 'hrContracts',
        label: 'HR Contracts List',
        icon: 'feather-file-text',
        route: '/backoffice/contracts',
        queryParams: { scope: 'HR' },
        exact: true
      });
    }

    if (this.isHr) {
      items.push(
        {
          key: 'staffAccounts',
          label: 'Staff Accounts List',
          icon: 'feather-users',
          route: '/backoffice/staff',
          exact: true
        }
      );
    }

    if (this.isAdmin || this.isHr) {
      if (this.isAdmin) {
        items.push({
          key: 'staffDetails',
          label: 'Staff & Roles Details',
          icon: 'feather-briefcase',
          route: '/backoffice/staff-details',
          exact: true
        });
      }

      items.push({
        key: 'staffContracts',
        label: 'StaffContracts List',
        icon: 'feather-file-text',
        route: '/backoffice/contracts',
        exact: true
      });

    }

    if (this.isAdmin || this.isHr) {
      if (this.isAdmin) {
        items.push(
          {
            key: 'hospitalStructure',
            label: 'Hospital Structure',
            icon: 'feather-grid',
            route: '/backoffice/hospital-structure',
            exact: true
          },
          {
            key: 'officeAssignments',
            label: 'Office Assignments',
            icon: 'feather-map-pin',
            route: '/backoffice/office-assignments',
            exact: true
          }
        );
      }

      if (this.isHr) {
        items.push(
          {
            key: 'equipmentInventory',
            label: 'Equipment Inventory',
            icon: 'feather-package',
            route: '/backoffice/equipment-inventory',
            exact: true
          },
          {
            key: 'equipmentPlacement',
            label: 'Equipment Placement',
            icon: 'feather-map-pin',
            route: '/backoffice/equipment-placement',
            exact: true
          },
          {
            key: 'staffPlacements',
            label: 'Staff Placements',
            icon: 'feather-briefcase',
            route: '/backoffice/staff-placements',
            exact: true
          }
        );
      }
    }

    if (this.isAdmin || this.isReceptionist) {
      items.push({
        key: 'patientsList',
        label: 'Patients List',
        icon: 'feather-heart',
        route: '/backoffice/patients',
        exact: true
      });

      if (this.isReceptionist) {
        items.push(
          {
            key: 'createGuardianPatient',
            label: 'Create Guardian + Patient Profile',
            icon: 'feather-user-plus',
            route: '/backoffice/create-guardian-patient',
            exact: true
          },
          {
            key: 'existingGuardianPatient',
            label: 'Existing Guardian + New Patient',
            icon: 'feather-user-check',
            route: '/backoffice/existing-guardian-patient',
            exact: true
          }
        );
      }

      items.push({
        key: 'guardiansLinked',
        label: 'Guardians & Linked Profiles',
        icon: 'feather-link',
        route: '/backoffice/guardians-linked',
        exact: true
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
          },
          {
            label: 'My Patients',
            route: '/backoffice/doctor/patients',
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
        this.destroyFancySelects();
        this.destroyFancyDates();
        this.restoreDockedFilters();
        this.restoreDockedStats();
        this.restoreDockedActions();
        return;
      }

      if (event instanceof NavigationEnd) {
        this.updateRouteBodyClasses();
        this.userMenuOpen = false;
        this.notificationsOpen = false;
        this.messagesOpen = false;
        this.createHrPanelOpen = false;
        this.createContractPanelOpen = false;
        this.staffDetailsPanelOpen = false;
        this.staffDetailsUserId = null;
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
    this.restoreDockedActions();
    this.destroyFancySelects();
    this.destroyFancyDates();
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
      this.dockPageActions();
      this.dockPageStats();
      this.enhanceBackofficeSelects();
      this.enhanceBackofficeDates();
      this.enhanceStatisticVisuals();
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
    const hasSearch = !!dock.querySelector('.topbar-filter-group input');
    if (selects.length === 0 || (selects.length === 1 && !hasSearch)) {
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

    button.setAttribute('data-tooltip', hasSearch ? 'Filters & Sort' : 'Sort');
    button.setAttribute('aria-label', hasSearch ? 'Filters & Sort' : 'Sort');
    button.innerHTML = `
      <img class="topbar-filter-icon" src="assets/backoffice/images/auth/filtre.png" alt="" aria-hidden="true">
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

  private enhanceBackofficeSelects(): void {
    this.pruneFancySelects();

    const selects = Array.from(
      document.querySelectorAll<HTMLSelectElement>(
        'body.admin-redesign.backoffice-body select.form-control, body.admin-redesign.backoffice-body select.form-select'
      )
    ).filter((select) => this.canEnhanceSelect(select));

    selects.forEach((select) => this.createFancySelect(select));
  }

  private canEnhanceSelect(select: HTMLSelectElement): boolean {
    if (!select.isConnected) return false;
    if (select.classList.contains('fancy-select-native')) return false;
    if (select.closest('.fancy-select')) return false;
    if (this.fancySelectBindings.some((binding) => binding.select === select)) return false;

    const styles = window.getComputedStyle(select);
    return styles.display !== 'none' && styles.visibility !== 'hidden';
  }

  private createFancySelect(select: HTMLSelectElement): void {
    const wrapper = document.createElement('div');
    wrapper.className = 'fancy-select';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'fancy-select-trigger';
    button.setAttribute('aria-haspopup', 'listbox');
    button.setAttribute('aria-expanded', 'false');

    const list = document.createElement('div');
    list.className = 'fancy-select-list';
    list.setAttribute('role', 'listbox');

    wrapper.appendChild(button);
    wrapper.appendChild(list);
    select.insertAdjacentElement('afterend', wrapper);
    select.classList.add('fancy-select-native');
    select.setAttribute('tabindex', '-1');

    const changeHandler = () => this.refreshFancySelect(select);
    select.addEventListener('change', changeHandler);

    const observer = new MutationObserver(() => this.refreshFancySelect(select));
    observer.observe(select, {
      attributes: true,
      childList: true,
      subtree: true,
      attributeFilter: ['disabled', 'label', 'selected', 'value']
    });

    button.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();

      if (select.disabled) return;

      const willOpen = !wrapper.classList.contains('open');
      this.closeFancySelects(wrapper);
      wrapper.classList.toggle('open', willOpen);
      button.setAttribute('aria-expanded', String(willOpen));
    });

    button.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        wrapper.classList.remove('open');
        button.setAttribute('aria-expanded', 'false');
      }
      if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        if (!select.disabled) {
          this.closeFancySelects(wrapper);
          wrapper.classList.add('open');
          button.setAttribute('aria-expanded', 'true');
        }
      }
    });

    this.fancySelectBindings.push({ select, wrapper, button, list, observer, changeHandler });
    this.refreshFancySelect(select);
  }

  private refreshFancySelect(select: HTMLSelectElement): void {
    const binding = this.fancySelectBindings.find((item) => item.select === select);
    if (!binding) return;

    const selected = select.selectedOptions.item(0);
    binding.button.textContent = selected?.textContent?.trim() || 'Select';
    binding.button.disabled = select.disabled;
    binding.wrapper.classList.toggle('disabled', select.disabled);
    binding.list.innerHTML = '';

    Array.from(select.options).forEach((option, index) => {
      const item = document.createElement('button');
      item.type = 'button';
      item.className = 'fancy-select-option';
      item.textContent = option.textContent?.trim() || option.value;
      item.disabled = option.disabled;
      item.setAttribute('role', 'option');
      item.setAttribute('aria-selected', String(index === select.selectedIndex));
      item.classList.toggle('active', index === select.selectedIndex);

      item.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        if (option.disabled || select.disabled) return;

        select.selectedIndex = index;
        select.dispatchEvent(new Event('change', { bubbles: true }));
        select.dispatchEvent(new Event('input', { bubbles: true }));
        binding.wrapper.classList.remove('open');
        binding.button.setAttribute('aria-expanded', 'false');
        this.refreshFancySelect(select);
      });

      binding.list.appendChild(item);
    });
  }

  private openFancySelect(select: HTMLSelectElement): void {
    const binding = this.fancySelectBindings.find((item) => item.select === select);
    if (!binding || select.disabled) return;

    this.closeFancySelects(binding.wrapper);
    binding.wrapper.classList.add('open');
    binding.button.setAttribute('aria-expanded', 'true');
  }

  private closeFancySelects(except?: HTMLElement): void {
    this.fancySelectBindings.forEach((binding) => {
      if (binding.wrapper === except) return;
      binding.wrapper.classList.remove('open');
      binding.button.setAttribute('aria-expanded', 'false');
    });
  }

  private pruneFancySelects(): void {
    this.fancySelectBindings = this.fancySelectBindings.filter((binding) => {
      if (binding.select.isConnected && binding.wrapper.isConnected) {
        return true;
      }

      binding.observer.disconnect();
      binding.select.removeEventListener('change', binding.changeHandler);
      binding.wrapper.remove();
      binding.select.classList.remove('fancy-select-native');
      return false;
    });
  }

  private destroyFancySelects(): void {
    this.fancySelectBindings.forEach((binding) => {
      binding.observer.disconnect();
      binding.select.removeEventListener('change', binding.changeHandler);
      binding.select.classList.remove('fancy-select-native');
      binding.select.removeAttribute('tabindex');
      binding.wrapper.remove();
    });
    this.fancySelectBindings = [];
  }

  private enhanceBackofficeDates(): void {
    this.pruneFancyDates();

    const inputs = Array.from(
      document.querySelectorAll<HTMLInputElement>(
        'body.admin-redesign.backoffice-body input[type="date"].form-control'
      )
    ).filter((input) => this.canEnhanceDate(input));

    inputs.forEach((input) => this.createFancyDate(input));
  }

  private canEnhanceDate(input: HTMLInputElement): boolean {
    if (!input.isConnected) return false;
    if (input.classList.contains('fancy-date-native')) return false;
    if (input.closest('.fancy-date')) return false;
    if (this.fancyDateBindings.some((binding) => binding.input === input)) return false;

    const styles = window.getComputedStyle(input);
    return styles.display !== 'none' && styles.visibility !== 'hidden';
  }

  private createFancyDate(input: HTMLInputElement): void {
    const selectedDate = this.parseDateValue(input.value);
    const wrapper = document.createElement('div');
    wrapper.className = 'fancy-date';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'fancy-date-trigger';
    button.setAttribute('aria-haspopup', 'dialog');
    button.setAttribute('aria-expanded', 'false');

    const panel = document.createElement('div');
    panel.className = 'fancy-date-panel';

    wrapper.appendChild(button);
    wrapper.appendChild(panel);
    input.insertAdjacentElement('afterend', wrapper);
    input.classList.add('fancy-date-native');
    input.setAttribute('tabindex', '-1');

    const changeHandler = () => this.refreshFancyDate(input);
    input.addEventListener('change', changeHandler);
    input.addEventListener('input', changeHandler);

    const observer = new MutationObserver(() => this.refreshFancyDate(input));
    observer.observe(input, {
      attributes: true,
      attributeFilter: ['disabled', 'min', 'max', 'placeholder', 'value']
    });

    const binding: FancyDateBinding = {
      input,
      wrapper,
      button,
      panel,
      observer,
      changeHandler,
      visibleMonth: selectedDate ?? this.resolveInitialFancyDateMonth(input)
    };

    button.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();

      if (input.disabled) return;

      const willOpen = !wrapper.classList.contains('open');
      this.closeFancyDates(wrapper);
      wrapper.classList.toggle('open', willOpen);
      button.setAttribute('aria-expanded', String(willOpen));
      if (willOpen) {
        binding.visibleMonth = this.parseDateValue(input.value) ?? this.resolveInitialFancyDateMonth(input);
        this.renderFancyDate(binding);
        this.placeFancyDatePanel(binding);
      }
    });

    button.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') {
        wrapper.classList.remove('open');
        button.setAttribute('aria-expanded', 'false');
      }
      if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        if (!input.disabled) {
          this.closeFancyDates(wrapper);
          wrapper.classList.add('open');
          button.setAttribute('aria-expanded', 'true');
          this.renderFancyDate(binding);
          this.placeFancyDatePanel(binding);
        }
      }
    });

    this.fancyDateBindings.push(binding);
    this.refreshFancyDate(input);
  }

  private refreshFancyDate(input: HTMLInputElement): void {
    const binding = this.fancyDateBindings.find((item) => item.input === input);
    if (!binding) return;

    binding.button.textContent = input.value ? this.formatDateForDisplay(input.value) : (input.getAttribute('placeholder') || 'Select date');
    binding.button.disabled = input.disabled;
    binding.wrapper.classList.toggle('disabled', input.disabled);
    this.renderFancyDate(binding);
  }

  private openFancyDate(input: HTMLInputElement): void {
    const binding = this.fancyDateBindings.find((item) => item.input === input);
    if (!binding || input.disabled) return;

    this.closeFancyDates(binding.wrapper);
    binding.visibleMonth = this.parseDateValue(input.value) ?? this.resolveInitialFancyDateMonth(input);
    binding.wrapper.classList.add('open');
    binding.button.setAttribute('aria-expanded', 'true');
    this.renderFancyDate(binding);
    this.placeFancyDatePanel(binding);
  }

  private renderFancyDate(binding: FancyDateBinding): void {
    const { input, panel } = binding;
    panel.innerHTML = '';

    const selected = this.parseDateValue(input.value);
    if (!selected) {
      binding.visibleMonth = this.clampFancyDateVisibleMonth(input, binding.visibleMonth);
    }
    const visible = new Date(binding.visibleMonth.getFullYear(), binding.visibleMonth.getMonth(), 1);
    const weekdays = ['lu', 'ma', 'me', 'je', 've', 'sa', 'di'];

    const head = document.createElement('div');
    head.className = 'fancy-date-head';

    const monthLabel = document.createElement('button');
    monthLabel.type = 'button';
    monthLabel.className = 'fancy-date-month';
    monthLabel.textContent = visible.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });

    const nav = document.createElement('div');
    nav.className = 'fancy-date-nav';

    const prev = document.createElement('button');
    prev.type = 'button';
    prev.className = 'fancy-date-nav-btn';
    prev.textContent = '<';
    prev.setAttribute('aria-label', 'Previous month');
    prev.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      binding.visibleMonth = new Date(visible.getFullYear(), visible.getMonth() - 1, 1);
      this.renderFancyDate(binding);
    });

    const next = document.createElement('button');
    next.type = 'button';
    next.className = 'fancy-date-nav-btn';
    next.textContent = '>';
    next.setAttribute('aria-label', 'Next month');
    next.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      binding.visibleMonth = new Date(visible.getFullYear(), visible.getMonth() + 1, 1);
      this.renderFancyDate(binding);
    });

    nav.appendChild(prev);
    nav.appendChild(next);
    head.appendChild(monthLabel);
    head.appendChild(nav);
    panel.appendChild(head);

    const grid = document.createElement('div');
    grid.className = 'fancy-date-grid';

    weekdays.forEach((day) => {
      const label = document.createElement('div');
      label.className = 'fancy-date-weekday';
      label.textContent = day;
      grid.appendChild(label);
    });

    const firstDay = new Date(visible);
    const mondayOffset = (firstDay.getDay() + 6) % 7;
    const cursor = new Date(visible.getFullYear(), visible.getMonth(), 1 - mondayOffset);

    for (let index = 0; index < 42; index += 1) {
      const current = new Date(cursor.getFullYear(), cursor.getMonth(), cursor.getDate() + index);
      const dayButton = document.createElement('button');
      dayButton.type = 'button';
      dayButton.className = 'fancy-date-day';
      dayButton.textContent = String(current.getDate());
      dayButton.classList.toggle('outside', current.getMonth() !== visible.getMonth());
      dayButton.classList.toggle('today', this.isSameCalendarDate(current, new Date()));
      dayButton.classList.toggle('active', !!selected && this.isSameCalendarDate(current, selected));
      dayButton.disabled = this.isDateOutOfBounds(input, current);

      dayButton.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        if (dayButton.disabled) return;
        input.value = this.toDateInputValue(current);
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
        binding.wrapper.classList.remove('open');
        binding.button.setAttribute('aria-expanded', 'false');
        this.refreshFancyDate(input);
      });

      grid.appendChild(dayButton);
    }

    panel.appendChild(grid);

    const footer = document.createElement('div');
    footer.className = 'fancy-date-footer';

    const clear = document.createElement('button');
    clear.type = 'button';
    clear.className = 'fancy-date-link';
    clear.textContent = 'Effacer';
    clear.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      input.value = '';
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
      binding.wrapper.classList.remove('open');
      binding.button.setAttribute('aria-expanded', 'false');
      this.refreshFancyDate(input);
    });

    const today = document.createElement('button');
    today.type = 'button';
    today.className = 'fancy-date-link';
    today.textContent = "Aujourd'hui";
    today.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      const now = new Date();
      if (this.isDateOutOfBounds(input, now)) return;
      input.value = this.toDateInputValue(now);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
      binding.wrapper.classList.remove('open');
      binding.button.setAttribute('aria-expanded', 'false');
      this.refreshFancyDate(input);
    });

    footer.appendChild(clear);
    footer.appendChild(today);
    panel.appendChild(footer);

    if (binding.wrapper.classList.contains('open')) {
      requestAnimationFrame(() => this.placeFancyDatePanel(binding));
    }
  }

  private placeFancyDatePanel(binding: FancyDateBinding): void {
    if (!binding.wrapper.classList.contains('open')) return;

    binding.wrapper.classList.remove('drop-up');
    binding.wrapper.classList.remove('align-right');
    const triggerRect = binding.button.getBoundingClientRect();
    const panelWidth = Math.min(360, Math.max(310, triggerRect.width));
    const panelHeight = binding.panel.offsetHeight || 340;
    const spaceBelow = window.innerHeight - triggerRect.bottom;
    const spaceAbove = triggerRect.top;
    const shouldOpenUp = spaceBelow < panelHeight + 18 && spaceAbove > spaceBelow;
    const shouldAlignRight = triggerRect.left + panelWidth > window.innerWidth - 16;

    binding.wrapper.classList.toggle('drop-up', shouldOpenUp);
    binding.wrapper.classList.toggle('align-right', shouldAlignRight);
    binding.panel.style.width = `${panelWidth}px`;
    binding.panel.style.removeProperty('--fancy-date-left');
    binding.panel.style.removeProperty('--fancy-date-top');
  }

  private closeFancyDates(except?: HTMLElement): void {
    this.fancyDateBindings.forEach((binding) => {
      if (binding.wrapper === except) return;
      binding.wrapper.classList.remove('open');
      binding.button.setAttribute('aria-expanded', 'false');
    });
  }

  private pruneFancyDates(): void {
    this.fancyDateBindings = this.fancyDateBindings.filter((binding) => {
      if (binding.input.isConnected && binding.wrapper.isConnected) {
        return true;
      }

      binding.observer.disconnect();
      binding.input.removeEventListener('change', binding.changeHandler);
      binding.input.removeEventListener('input', binding.changeHandler);
      binding.wrapper.remove();
      binding.input.classList.remove('fancy-date-native');
      binding.input.removeAttribute('tabindex');
      return false;
    });
  }

  private destroyFancyDates(): void {
    this.fancyDateBindings.forEach((binding) => {
      binding.observer.disconnect();
      binding.input.removeEventListener('change', binding.changeHandler);
      binding.input.removeEventListener('input', binding.changeHandler);
      binding.input.classList.remove('fancy-date-native');
      binding.input.removeAttribute('tabindex');
      binding.wrapper.remove();
    });
    this.fancyDateBindings = [];
  }

  private parseDateValue(value: string): Date | null {
    if (!value) return null;
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    if (!match) return null;
    const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private resolveInitialFancyDateMonth(input: HTMLInputElement): Date {
    const selected = this.parseDateValue(input.value);
    if (selected) return selected;

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const min = this.getEffectiveDateMin(input);
    const max = this.getEffectiveDateMax(input);

    if (max && today.getTime() > max.getTime()) {
      return max;
    }

    if (min && today.getTime() < min.getTime()) {
      return min;
    }

    return today;
  }

  private clampFancyDateVisibleMonth(input: HTMLInputElement, date: Date): Date {
    const min = this.getEffectiveDateMin(input);
    const max = this.getEffectiveDateMax(input);
    const visibleMonth = new Date(date.getFullYear(), date.getMonth(), 1);

    if (max) {
      const maxMonth = new Date(max.getFullYear(), max.getMonth(), 1);
      if (visibleMonth.getTime() > maxMonth.getTime()) return max;
    }

    if (min) {
      const minMonth = new Date(min.getFullYear(), min.getMonth(), 1);
      if (visibleMonth.getTime() < minMonth.getTime()) return min;
    }

    return date;
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDateForDisplay(value: string): string {
    const date = this.parseDateValue(value);
    if (!date) return value;
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  private isSameCalendarDate(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate();
  }

  private isDateOutOfBounds(input: HTMLInputElement, date: Date): boolean {
    const min = this.getEffectiveDateMin(input);
    const max = this.getEffectiveDateMax(input);
    const value = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
    if (min && value < min.getTime()) return true;
    if (max && value > max.getTime()) return true;
    return false;
  }

  private getEffectiveDateMin(input: HTMLInputElement): Date | null {
    return this.parseDateValue(input.min);
  }

  private getEffectiveDateMax(input: HTMLInputElement): Date | null {
    const explicitMax = this.parseDateValue(input.max);
    if (explicitMax) return explicitMax;
    return this.isBirthDateInput(input) ? this.getAdultBirthDateCutoff() : null;
  }

  private isBirthDateInput(input: HTMLInputElement): boolean {
    const key = `${input.name} ${input.id} ${input.getAttribute('ng-reflect-name') ?? ''}`.toLowerCase();
    if (key.includes('birth')) return true;

    const labelText = input.closest('div')?.querySelector('label')?.textContent?.toLowerCase() ?? '';
    return labelText.includes('birth') || labelText.includes('date of birth');
  }

  private getAdultBirthDateCutoff(): Date {
    const cutoff = new Date();
    cutoff.setFullYear(cutoff.getFullYear() - 18);
    return new Date(cutoff.getFullYear(), cutoff.getMonth(), cutoff.getDate());
  }

  private dockPageActions(): void {
    const host = this.elementRef.nativeElement;
    const dock = host.querySelector<HTMLElement>('.page-statistics-tools');
    if (!dock) return;

    this.pruneDisconnectedDockedActions();

    const candidates = Array.from(
      document.querySelectorAll<HTMLElement>('.nxl-content [data-page-action-dock]')
    ).filter((node) => this.isDockableActionGroup(node, dock));

    candidates.forEach((node) => {
      const parent = node.parentElement;
      if (!parent) return;

      const placeholder = document.createComment('backoffice-action-dock-placeholder');
      parent.insertBefore(placeholder, node);
      node.classList.add('page-action-docked');
      dock.insertBefore(node, dock.firstChild);
      this.dockedActionGroups.push({ node, placeholder });
    });

    this.hasPageActions = this.dockedActionGroups.length > 0;
    this.refreshLayoutState();
  }

  private isDockableActionGroup(node: HTMLElement, dock: HTMLElement): boolean {
    if (dock.contains(node)) return false;
    if (this.dockedActionGroups.some((group) => group.node === node)) return false;
    if (!node.isConnected) return false;
    if (!node.querySelector('button, a')) return false;
    if (node.closest('.modal, .dropdown-menu, .notifications-dropdown, .user-menu-dropdown, .messages-dropdown')) return false;

    const styles = window.getComputedStyle(node);
    return styles.display !== 'none' && styles.visibility !== 'hidden';
  }

  private pruneDisconnectedDockedActions(): void {
    this.dockedActionGroups = this.dockedActionGroups.filter((group) => {
      if (group.node.isConnected && group.placeholder.isConnected) {
        return true;
      }

      group.node.remove();
      group.placeholder.remove();
      return false;
    });
    this.hasPageActions = this.dockedActionGroups.length > 0;
  }

  private restoreDockedActions(): void {
    this.dockedActionGroups.forEach(({ node, placeholder }) => {
      node.classList.remove('page-action-docked');

      if (placeholder.parentNode && node.isConnected) {
        placeholder.parentNode.insertBefore(node, placeholder);
      }

      placeholder.remove();
    });

    this.dockedActionGroups = [];
    this.hasPageActions = false;
    this.refreshLayoutState();
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
    this.enhanceStatisticVisuals(statsBody);
    this.refreshLayoutState();
  }

  private enhanceStatisticVisuals(root: ParentNode = document): void {
    const bodies = root instanceof HTMLElement && root.classList.contains('statistics-modal-body')
      ? [root]
      : Array.from(root.querySelectorAll<HTMLElement>('.statistics-modal-body'));

    bodies.forEach((body) => {
      body.classList.add('visual-stat-stage');

      const groups = Array.from(
        body.querySelectorAll<HTMLElement>('.stats-grid, .kpi-grid, .resource-overview-grid, .statistics-modal-group')
      );
      groups.forEach((group) => group.classList.add('visual-stat-grid'));

      const cards = Array.from(
        body.querySelectorAll<HTMLElement>('.stats-card, .kpi-card, .metric-card, .role-card, .resource-overview-card')
      );
      cards.forEach((card, index) => this.enhanceStatisticCard(card, index));
    });
  }

  keepStatsVisuals(): void {
    this.queueStatisticVisualRefresh();
  }

  private queueStatisticVisualRefresh(): void {
    window.requestAnimationFrame(() => this.refreshStatisticVisualsNow());
    [60, 180].forEach((delay) => {
      window.setTimeout(() => this.refreshStatisticVisualsNow(), delay);
    });
  }

  private refreshStatisticVisualsNow(): void {
    if (this.destroyed) return;

    const statsBody = this.elementRef.nativeElement.querySelector<HTMLElement>('.statistics-modal-body');
    if (!statsBody) return;

    this.enhanceStatisticVisuals(statsBody);
  }

  private enhanceStatisticCard(card: HTMLElement, index: number): void {
    const label = this.extractStatisticLabel(card);
    const metric = this.extractStatisticMetric(card);
    const accents = ['#07869a', '#2563eb', '#14b8a6', '#22c55e', '#8b5cf6', '#f97316'];
    const accent = accents[index % accents.length];

    card.classList.add('visual-stat-card');
    card.classList.toggle('visual-stat-spider', index % 3 === 1);
    card.classList.toggle('visual-stat-speed', index % 3 === 2);
    card.style.setProperty('--stat-p', `${metric.percent}`);
    card.style.setProperty('--stat-accent', accent);
    card.style.setProperty('--stat-delay', `${Math.min(index * 80, 520)}ms`);

    if (!card.getAttribute('data-stat-tooltip')) {
      card.setAttribute(
        'data-stat-tooltip',
        `${label}: live visual statistic based on the current page data. It updates when the page data changes.`
      );
    }

    let orbit = card.querySelector<HTMLElement>(':scope > .visual-stat-orbit');
    if (!orbit) {
      orbit = document.createElement('div');
      orbit.className = 'visual-stat-orbit';
      orbit.setAttribute('aria-hidden', 'true');
      orbit.innerHTML = `
        <svg class="visual-stat-web" viewBox="0 0 120 120" focusable="false" aria-hidden="true">
          <circle class="web-ring web-ring-outer" cx="60" cy="60" r="48"></circle>
          <circle class="web-ring web-ring-inner" cx="60" cy="60" r="30"></circle>
          <path class="web-axis" d="M60 12V108M12 60H108M26 26L94 94M94 26L26 94"></path>
          <polygon class="web-shape" points="60,16 96,42 88,84 49,100 22,57"></polygon>
        </svg>
        <div class="visual-stat-core"></div>
      `;
      card.insertBefore(orbit, card.firstChild);
    }

    const core = orbit.querySelector<HTMLElement>('.visual-stat-core');
    if (core) {
      core.textContent = metric.display;
    }
  }

  private extractStatisticLabel(card: HTMLElement): string {
    const labelNode = card.querySelector<HTMLElement>(
      '.stats-label, .metric-label, .resource-overview-card span, .role-card h6, .kpi-card span, h6, h5'
    );
    const label = labelNode?.textContent?.trim();
    if (label) return label;

    const firstLine = card.innerText.split('\n').map((line) => line.trim()).find(Boolean);
    return firstLine || 'Statistic';
  }

  private extractStatisticMetric(card: HTMLElement): { display: string; percent: number } {
    const valueNode = card.querySelector<HTMLElement>(
      '.circle-value, .stats-value, .metric-value, .role-total, .resource-overview-card strong, .kpi-card strong'
    );
    const source = `${valueNode?.textContent ?? ''} ${card.innerText}`;
    const percentMatch = source.match(/(-?\d+(?:[.,]\d+)?)\s*%/);
    if (percentMatch) {
      const percent = this.clampStatisticPercent(Number(percentMatch[1].replace(',', '.')));
      return { display: `${Math.round(percent)}%`, percent };
    }

    const numberMatch = source.match(/-?\d+(?:[.,]\d+)?/);
    if (numberMatch) {
      const rawValue = Number(numberMatch[0].replace(',', '.'));
      const percent = this.clampStatisticPercent(rawValue <= 1 ? rawValue * 100 : (rawValue * 9) % 101);
      return { display: Number.isInteger(rawValue) ? `${rawValue}` : `${rawValue.toFixed(1)}`, percent };
    }

    return { display: 'Live', percent: 64 };
  }

  private clampStatisticPercent(value: number): number {
    if (!Number.isFinite(value)) return 64;
    return Math.max(12, Math.min(100, Math.round(value)));
  }

  private findStatisticGroups(statsBody: HTMLElement): HTMLElement[] {
    const baseGroups = Array.from(
      document.querySelectorAll<HTMLElement>(
        '.nxl-content .stats-grid, .nxl-content .kpi-grid, .nxl-content .resource-overview-grid, .nxl-content .hr-summary-grid, .nxl-content .role-stats-grid'
      )
    );

    const roleRows = Array.from(
      document.querySelectorAll<HTMLElement>('.nxl-content .row')
    ).filter((node) => {
      const hasDirectRoleCards = !!node.querySelector(':scope > [class*="col"] .role-card');
      const containsPageContent = !!node.querySelector('.card-body, .filters-panel, .table-responsive, table');
      return hasDirectRoleCards && !containsPageContent;
    });

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
    this.createHrPanelOpen = false;
    this.createContractPanelOpen = false;
    this.staffDetailsPanelOpen = false;
    this.statsPanelOpen = true;
    this.refreshLayoutState();
    this.queueStatisticVisualRefresh();
  }

  closeStatsPanel(): void {
    this.statsPanelOpen = false;
    this.refreshLayoutState();
  }

  openCreateHrPanel(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.statsPanelOpen = false;
    this.createContractPanelOpen = false;
    this.staffDetailsPanelOpen = false;
    this.createHrPanelOpen = true;
    this.refreshLayoutState();
    this.scheduleFilterDocking(0);
  }

  closeCreateHrPanel(): void {
    this.createHrPanelOpen = false;
    this.refreshLayoutState();
  }

  @HostListener('window:open-create-contract-panel', ['$event'])
  openCreateContractPanel(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.statsPanelOpen = false;
    this.createHrPanelOpen = false;
    this.staffDetailsPanelOpen = false;
    this.createContractPanelOpen = true;
    this.refreshLayoutState();
    this.scheduleFilterDocking(0);
  }

  closeCreateContractPanel(): void {
    this.createContractPanelOpen = false;
    this.refreshLayoutState();
  }

  @HostListener('window:open-staff-details-panel', ['$event'])
  openStaffDetailsPanel(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    const detail = (event as CustomEvent<{ userId?: number }> | undefined)?.detail;
    const userId = Number(detail?.userId);
    if (!Number.isFinite(userId) || userId <= 0) {
      return;
    }
    this.statsPanelOpen = false;
    this.createHrPanelOpen = false;
    this.createContractPanelOpen = false;
    this.staffDetailsUserId = userId;
    this.staffDetailsPanelOpen = true;
    this.refreshLayoutState();
    this.scheduleFilterDocking(0);
  }

  closeStaffDetailsPanel(): void {
    this.staffDetailsPanelOpen = false;
    this.staffDetailsUserId = null;
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
    this.messagesOpen = false;
    this.userMenuOpen = !this.userMenuOpen;

    setTimeout(() => {
      this.refreshFeatherIcons();
    }, 0);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (Date.now() < this.suppressNativeControlClickUntil) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }

    const target = event.target as HTMLElement | null;
    if (!target?.closest('.notifications-menu')) {
      this.notificationsOpen = false;
    }
    if (!target?.closest('.messages-menu')) {
      this.messagesOpen = false;
    }
    if (!target?.closest('.user-menu')) {
      this.userMenuOpen = false;
    }
    if (!target?.closest('.topbar-filter-menu')) {
      this.closeTopbarFilterMenus();
    }
    if (!target?.closest('.fancy-select')) {
      this.closeFancySelects();
    }
    if (!target?.closest('.fancy-date')) {
      this.closeFancyDates();
    }
    if (this.statsPanelOpen && !target?.closest('.statistics-modal') && !target?.closest('.statistics-trigger')) {
      this.closeStatsPanel();
    }
    if (this.createHrPanelOpen && !target?.closest('.create-hr-modal') && !target?.closest('.create-hr-trigger')) {
      this.closeCreateHrPanel();
    }
    if (this.createContractPanelOpen && !target?.closest('.create-contract-modal') && !target?.closest('.create-contract-action')) {
      this.closeCreateContractPanel();
    }
    if (this.staffDetailsPanelOpen && !target?.closest('.staff-details-modal') && !target?.closest('.staff-details-action')) {
      this.closeStaffDetailsPanel();
    }
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMouseDown(event: MouseEvent): void {
    if (!document.body.classList.contains('admin-redesign')) return;

    const target = event.target as HTMLElement | null;
    if (!target) return;

    const nativeDate = target.closest<HTMLInputElement>('input[type="date"].form-control');
    if (nativeDate && this.canEnhanceDate(nativeDate)) {
      event.preventDefault();
      event.stopPropagation();
      this.suppressNativeControlClickUntil = Date.now() + 160;
      this.createFancyDate(nativeDate);
      this.openFancyDate(nativeDate);
      return;
    }

    const nativeSelect = target.closest<HTMLSelectElement>('select.form-control, select.form-select');
    if (nativeSelect && this.canEnhanceSelect(nativeSelect)) {
      event.preventDefault();
      event.stopPropagation();
      this.suppressNativeControlClickUntil = Date.now() + 160;
      this.createFancySelect(nativeSelect);
      this.openFancySelect(nativeSelect);
    }
  }

  async toggleNotifications(event: MouseEvent): Promise<void> {
    if (!this.notificationsEnabled) {
      return;
    }
    event.stopPropagation();
    this.userMenuOpen = false;
    this.messagesOpen = false;
    this.notificationsOpen = !this.notificationsOpen;

    if (this.notificationsOpen) {
      await this.loadNotifications();
      if (this.unreadCount > 0) {
        await this.markAllVisibleAsRead();
      }
    }
  }

  toggleMessages(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationsOpen = false;
    this.userMenuOpen = false;
    this.messagesOpen = !this.messagesOpen;
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
    this.messagesOpen = false;
    this.createHrPanelOpen = false;
    this.createContractPanelOpen = false;
    this.staffDetailsPanelOpen = false;
    this.staffDetailsUserId = null;
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
