import {
  AfterViewInit,
  Component,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthStorageService } from '../../../core/auth/auth-storage.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom, forkJoin } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { getValidToken } from '../../../core/auth/keycloak.service';

declare global {
interface Window {
    feather?: any;
  }
}

interface HospitalStructureSummary {
  initialized: boolean;
  totalFloors: number;
  totalWorkspaces: number;
  floors: Array<{ id: number; floorLabel: string; workspaces: Array<{ workspaceTypeLabel: string }> }>;
}

interface EquipmentInventorySummary {
  total: number;
  available: number;
  underMaintenance: number;
  outOfService: number;
  archived: number;
}

interface PlacementFloorSummary {
  floorId: number;
  floorLabel: string;
  eligibleWorkspaceCount: number;
  totalPlacedEquipment: number;
  workspaces: Array<{ workspaceId: number; workspaceName: string; workspaceType: string; placedCount: number }>;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements AfterViewInit, OnInit {
  loadingStats = false;
  statsError = '';
  allUsers: any[] = [];
  allContracts: any[] = [];
  allPatients: any[] = [];
  allGuardians: any[] = [];
  actionAlerts = {
    contractsEndingIn7Days: 0,
    contractsEndingIn30Days: 0,
    pendingUsersTooLong: 0,
    profilesMissingRequiredData: 0
  };
  latestNotifications: Array<{ id: number; type: string; title: string; message: string; createdAt: string }> = [];
  clinicStructure: HospitalStructureSummary | null = null;
  equipmentSummary: EquipmentInventorySummary | null = null;
  placementFloors: PlacementFloorSummary[] = [];
  assignmentCounts: Record<string, number> = {
    ADMIN: 0,
    HR: 0,
    DOCTOR: 0,
    LAB_AGENT: 0,
    PHARMACIST: 0,
    RECEPTIONIST: 0
  };

  get expiringContractsPreview(): Array<{
    id: number;
    staffUserId: number;
    fullName: string;
    role: string;
    contractReference: string;
    endDate: string;
    daysLeft: number;
    status: string;
  }> {
    const today = new Date();
    const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const maxDate = new Date(startOfToday);
    maxDate.setDate(maxDate.getDate() + 30);

    return this.allContracts
      .filter((contract) => ['ACTIVE', 'SUSPENDED'].includes(String(contract.status ?? '')))
      .map((contract) => {
        const staffUserId = Number(contract.staffUserId);
        const user = this.allUsers.find((u) => Number(u.id) === staffUserId);
        const fullName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.username || `User #${staffUserId}`;
        const end = new Date(contract.endDate ?? '');
        const startOfEnd = new Date(end.getFullYear(), end.getMonth(), end.getDate());
        const daysLeft = Math.ceil((startOfEnd.getTime() - startOfToday.getTime()) / (1000 * 60 * 60 * 24));
        return {
          id: Number(contract.id),
          staffUserId,
          fullName,
          role: String(user?.role ?? '-'),
          contractReference: String(contract.contractReference ?? `#${contract.id ?? '-'}`),
          endDate: String(contract.endDate ?? ''),
          daysLeft,
          status: String(contract.status ?? '')
        };
      })
      .filter((item) => Number.isFinite(item.daysLeft) && item.daysLeft >= 0 && new Date(item.endDate).getTime() <= maxDate.getTime())
      .sort((a, b) => a.daysLeft - b.daysLeft)
      .slice(0, 8);
  }

  get canOpenExpiringAccounts(): boolean {
    return this.isAdmin || this.isHr;
  }

  getExpirySeverity(daysLeft: number): 'critical' | 'warning' | 'normal' {
    if (daysLeft <= 3) return 'critical';
    if (daysLeft <= 10) return 'warning';
    return 'normal';
  }

  getDaysLeftLabel(daysLeft: number): string {
    if (daysLeft <= 0) return 'Today';
    if (daysLeft === 1) return '1 day left';
    return `${daysLeft} days left`;
  }

  getExpiryProgress(daysLeft: number): number {
    const ratio = Math.max(0, Math.min(1, (30 - daysLeft) / 30));
    return Math.round(ratio * 100);
  }

  constructor(
    private authStorage: AuthStorageService,
    private http: HttpClient
  ) {}

  get user(): any | null {
    return this.authStorage.getUser();
  }

  get role(): string {
    return this.authStorage.getRole() ?? '';
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

  get displayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }
    return this.user?.username ?? 'User';
  }

  get displayEmail(): string {
    return this.user?.email ?? 'No email';
  }

  get roleBadgeClass(): string {
    return this.isAdmin ? 'bg-soft-primary text-primary' : 'bg-soft-warning text-warning';
  }

  get roleDescription(): string {
    if (this.isAdmin) return 'Global administration and platform supervision';
    if (this.isHr) return 'Human resources and operational management';
    if (this.isReceptionist) return 'Reception and patient onboarding operations';
    return 'Connected backoffice user';
  }

  get staffUsers(): any[] {
    return this.allUsers.filter((user) =>
      ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'].includes(user.role)
    );
  }

  get totalStaff(): number {
    return this.staffUsers.length;
  }

  get activeStaff(): number {
    return this.staffUsers.filter(u => u.accountStatus === 'ACTIVE').length;
  }

  get pendingStaff(): number {
    return this.staffUsers.filter(u => u.accountStatus === 'PENDING_CONTRACT').length;
  }

  get inactiveStaff(): number {
    return this.staffUsers.filter(u => u.accountStatus === 'INACTIVE').length;
  }

  get totalContracts(): number {
    return this.allContracts.length;
  }

  get activeContracts(): number {
    return this.allContracts.filter(c => c.status === 'ACTIVE').length;
  }

  get suspendedContracts(): number {
    return this.allContracts.filter(c => c.status === 'SUSPENDED').length;
  }

  get endedContracts(): number {
    return this.allContracts.filter(c => c.status === 'ENDED' || c.status === 'EXPIRED').length;
  }

  get contractCoverage(): number {
    if (!this.totalStaff) return 0;
    const withContract = new Set(this.allContracts.map(c => c.staffUserId)).size;
    return Math.round((withContract / this.totalStaff) * 100);
  }

  get activeStaffRate(): number {
    if (!this.totalStaff) return 0;
    return Math.round((this.activeStaff / this.totalStaff) * 100);
  }

  get activeContractsRate(): number {
    if (!this.totalContracts) return 0;
    return Math.round((this.activeContracts / this.totalContracts) * 100);
  }

  get roleBreakdown(): Array<{ role: string; count: number }> {
    const roles = ['DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];
    return roles.map(role => ({
      role,
      count: this.staffUsers.filter(u => u.role === role).length
    }));
  }

  get roleBreakdownWithPercent(): Array<{ role: string; count: number; percent: number }> {
    const total = this.totalStaff || 1;
    return this.roleBreakdown.map((item) => ({
      ...item,
      percent: Math.round((item.count / total) * 100)
    }));
  }

  get hrUsers(): any[] {
    return this.allUsers.filter((user) => user.role === 'HR');
  }

  get totalHr(): number {
    return this.hrUsers.length;
  }

  get activeHr(): number {
    return this.hrUsers.filter((u) => u.accountStatus === 'ACTIVE').length;
  }

  get pendingHr(): number {
    return this.hrUsers.filter((u) => u.accountStatus === 'PENDING_CONTRACT').length;
  }

  get inactiveHr(): number {
    return this.hrUsers.filter((u) => u.accountStatus === 'INACTIVE').length;
  }

  get enabledUsers(): number {
    return this.allUsers.filter((u) => !!u.enabled).length;
  }

  get disabledUsers(): number {
    return Math.max(0, this.allUsers.length - this.enabledUsers);
  }

  get enabledUsersRate(): number {
    if (!this.allUsers.length) return 0;
    return Math.round((this.enabledUsers / this.allUsers.length) * 100);
  }

  get staffWithContractCount(): number {
    const staffIds = new Set(this.staffUsers.map((user) => user.id));
    const contracted = new Set(
      this.allContracts
        .map((contract) => contract.staffUserId)
        .filter((staffUserId) => staffIds.has(staffUserId))
    );
    return contracted.size;
  }

  get staffWithoutContractCount(): number {
    return Math.max(0, this.totalStaff - this.staffWithContractCount);
  }

  get patientCoverageRate(): number {
    if (!this.totalGuardians) return 0;
    return Math.round((this.linkedGuardians / this.totalGuardians) * 100);
  }

  get patientSexBreakdown(): Array<{ label: string; value: number; percent: number; css: string }> {
    const total = this.totalPatients || 1;
    return [
      { label: 'Male', value: this.malePatients, percent: Math.round((this.malePatients / total) * 100), css: 'male' },
      { label: 'Female', value: this.femalePatients, percent: Math.round((this.femalePatients / total) * 100), css: 'female' }
    ];
  }

  get staffStatusBreakdown(): Array<{ label: string; value: number; percent: number; css: string }> {
    const total = this.totalStaff || 1;
    return [
      { label: 'Active', value: this.activeStaff, percent: Math.round((this.activeStaff / total) * 100), css: 'active' },
      { label: 'Pending', value: this.pendingStaff, percent: Math.round((this.pendingStaff / total) * 100), css: 'pending' },
      { label: 'Inactive', value: this.inactiveStaff, percent: Math.round((this.inactiveStaff / total) * 100), css: 'inactive' }
    ];
  }

  get contractStatusBreakdown(): Array<{ label: string; value: number; percent: number; css: string }> {
    const total = this.totalContracts || 1;
    return [
      { label: 'Active', value: this.activeContracts, percent: Math.round((this.activeContracts / total) * 100), css: 'active' },
      { label: 'Suspended', value: this.suspendedContracts, percent: Math.round((this.suspendedContracts / total) * 100), css: 'suspended' },
      { label: 'Ended/Expired', value: this.endedContracts, percent: Math.round((this.endedContracts / total) * 100), css: 'ended' }
    ];
  }

  

  get totalPatients(): number {
    return this.allPatients.length;
  }

  get totalGuardians(): number {
    return this.allGuardians.length;
  }

  get malePatients(): number {
    return this.allPatients.filter(p => p.sex === 'MALE').length;
  }

  get femalePatients(): number {
    return this.allPatients.filter(p => p.sex === 'FEMALE').length;
  }

  get linkedGuardians(): number {
    const guardianSet = new Set(this.allPatients.map(p => p.guardianUserId));
    return guardianSet.size;
  }

  get unlinkedGuardians(): number {
    return Math.max(0, this.totalGuardians - this.linkedGuardians);
  }

  get averageKidsPerGuardian(): number {
    if (!this.totalGuardians) return 0;
    return Number((this.totalPatients / this.totalGuardians).toFixed(2));
  }

  get todayPatients(): number {
    const today = new Date().toISOString().slice(0, 10);
    return this.allPatients.filter(p => (p.createdAt ?? '').slice(0, 10) === today).length;
  }

  get monthPatients(): number {
    const now = new Date();
    return this.allPatients.filter(p => {
      const created = new Date(p.createdAt ?? '');
      return !Number.isNaN(created.getTime())
        && created.getFullYear() === now.getFullYear()
        && created.getMonth() === now.getMonth();
    }).length;
  }

  get minorsPatients(): number {
    const now = new Date();
    return this.allPatients.filter((patient) => {
      const dob = new Date(patient.dateOfBirth ?? '');
      if (Number.isNaN(dob.getTime())) return false;
      let age = now.getFullYear() - dob.getFullYear();
      const monthDiff = now.getMonth() - dob.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < dob.getDate())) {
        age--;
      }
      return age < 18;
    }).length;
  }

  get adultsPatients(): number {
    return Math.max(0, this.totalPatients - this.minorsPatients);
  }

  get withAllergiesPatients(): number {
    return this.allPatients.filter((patient) => {
      const allergies = (patient.allergies ?? '').toString().trim();
      return allergies.length > 0 && allergies !== '-';
    }).length;
  }

  get bloodTypeFilledPatients(): number {
    return this.allPatients.filter((patient) => {
      const bloodType = (patient.bloodType ?? '').toString().trim();
      return bloodType.length > 0 && bloodType !== '-';
    }).length;
  }

  get bloodTypeMissingPatients(): number {
    return Math.max(0, this.totalPatients - this.bloodTypeFilledPatients);
  }

  get bloodTypeFilledRate(): number {
    if (!this.totalPatients) return 0;
    return Math.round((this.bloodTypeFilledPatients / this.totalPatients) * 100);
  }

  get guardianLinkingRate(): number {
    if (!this.totalGuardians) return 0;
    return Math.round((this.linkedGuardians / this.totalGuardians) * 100);
  }

  get patientAgeBreakdown(): Array<{ label: string; value: number; percent: number; css: string }> {
    const total = this.totalPatients || 1;
    return [
      { label: 'Minors (<18)', value: this.minorsPatients, percent: Math.round((this.minorsPatients / total) * 100), css: 'pending' },
      { label: 'Adults (18+)', value: this.adultsPatients, percent: Math.round((this.adultsPatients / total) * 100), css: 'inactive' }
    ];
  }

  get clinicWorkspaceTypesCount(): number {
    const counters = new Set<string>();
    for (const floor of this.clinicStructure?.floors ?? []) {
      for (const workspace of floor.workspaces ?? []) {
        if (workspace.workspaceTypeLabel) {
          counters.add(workspace.workspaceTypeLabel);
        }
      }
    }
    return counters.size;
  }

  get totalPlacedEquipment(): number {
    return this.placementFloors.reduce((sum, floor) => sum + Number(floor.totalPlacedEquipment ?? 0), 0);
  }

  get emptyEligibleWorkspaces(): number {
    return this.placementFloors.reduce((sum, floor) => {
      const floorEmpty = (floor.workspaces ?? []).filter((workspace) => Number(workspace.placedCount ?? 0) === 0).length;
      return sum + floorEmpty;
    }, 0);
  }

  get floorsWithEquipmentPlacements(): number {
    return this.placementFloors.filter((floor) => Number(floor.totalPlacedEquipment ?? 0) > 0).length;
  }

  async ngOnInit(): Promise<void> {
    await this.loadGlobalStats();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      try {
        if (window.feather) window.feather.replace();
      } catch {
        // noop
      }
    }, 100);
  }

  private async loadGlobalStats(): Promise<void> {
    if (!(this.isAdmin || this.isHr || this.isReceptionist)) return;
    this.loadingStats = true;
    this.statsError = '';
    try {
      const token = await getValidToken();
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      if (this.isAdmin || this.isHr) {
        const response = await firstValueFrom(forkJoin({
          users: this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/users`, { headers }),
          contracts: this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/contracts`, { headers })
        }));
        this.allUsers = Array.isArray(response?.users) ? response.users : [];
        this.allContracts = Array.isArray(response?.contracts) ? response.contracts : [];
      }

      if (this.isAdmin) {
        const [structure, equipmentSummary, placementFloors, adminAssignments, hrAssignments, doctorAssignments, labAssignments, pharmacistAssignments, receptionistAssignments] =
          await Promise.all([
            firstValueFrom(this.http.get<HospitalStructureSummary>(`${environment.apiBaseUrl}/api/hospital-structure`, { headers })),
            firstValueFrom(this.http.get<EquipmentInventorySummary>(`${environment.apiBaseUrl}/api/hospital-resources/equipment/summary`, { headers })),
            firstValueFrom(this.http.get<PlacementFloorSummary[]>(`${environment.apiBaseUrl}/api/hospital-resources/placement/floors`, { headers })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'ADMIN' } })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'HR' } })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'DOCTOR' } })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'LAB_AGENT' } })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'PHARMACIST' } })),
            firstValueFrom(this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/staff-assignments`, { headers, params: { role: 'RECEPTIONIST' } }))
          ]);

        this.clinicStructure = structure;
        this.equipmentSummary = equipmentSummary;
        this.placementFloors = Array.isArray(placementFloors) ? placementFloors : [];
        this.assignmentCounts = {
          ADMIN: Array.isArray(adminAssignments) ? adminAssignments.length : 0,
          HR: Array.isArray(hrAssignments) ? hrAssignments.length : 0,
          DOCTOR: Array.isArray(doctorAssignments) ? doctorAssignments.length : 0,
          LAB_AGENT: Array.isArray(labAssignments) ? labAssignments.length : 0,
          PHARMACIST: Array.isArray(pharmacistAssignments) ? pharmacistAssignments.length : 0,
          RECEPTIONIST: Array.isArray(receptionistAssignments) ? receptionistAssignments.length : 0
        };
      }

      if (this.isReceptionist || this.isAdmin) {
        const response = await firstValueFrom(forkJoin({
          patients: this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/patients`, { headers }),
          guardians: this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/users/guardians`, { headers })
        }));
        this.allPatients = Array.isArray(response?.patients) ? response.patients : [];
        this.allGuardians = Array.isArray(response?.guardians) ? response.guardians : [];
      }

      if (this.isAdmin || this.isHr || this.isReceptionist) {
        try {
          const alerts = await firstValueFrom(
            this.http.get<any>(`${environment.apiBaseUrl}/api/contracts/alerts/action-required?pendingDays=7`, { headers })
          );
          this.actionAlerts = {
            contractsEndingIn7Days: Number(alerts?.contractsEndingIn7Days ?? 0),
            contractsEndingIn30Days: Number(alerts?.contractsEndingIn30Days ?? 0),
            pendingUsersTooLong: Number(alerts?.pendingUsersTooLong ?? 0),
            profilesMissingRequiredData: Number(alerts?.profilesMissingRequiredData ?? 0)
          };
        } catch {
          // keep defaults when alerts endpoint is unavailable
        }

        try {
          const userId = Number(this.user?.userId);
          const notifications = await firstValueFrom(
            this.http.get<any[] | unknown>(`${environment.apiBaseUrl}/api/observability/notifications?userId=${userId}`, { headers })
          );
          this.latestNotifications = Array.isArray(notifications) ? notifications.slice(0, 6) : [];
        } catch {
          this.latestNotifications = [];
        }
      }
    } catch {
      this.statsError = 'Failed to load live statistics.';
    } finally {
      this.loadingStats = false;
    }
  }
}
