import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type PlacementRole = 'DOCTOR' | 'LAB_AGENT' | 'PHARMACIST' | 'RECEPTIONIST';

interface UserRow {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  enabled: boolean;
}

interface AssignmentRow {
  id: number;
  userId: number;
  role: PlacementRole;
  workspaceId: number;
  workspaceCode: string;
  workspaceName: string;
  workspaceType: string;
  floorLabel: string;
}

interface WorkspaceOption {
  workspaceId: number;
  workspaceCode: string;
  workspaceName: string;
  workspaceType: string;
  floorLabel: string;
  assignedCount: number;
}

interface WorkspaceGroup {
  workspaceId: number;
  workspaceCode: string;
  workspaceName: string;
  workspaceType: string;
  floorLabel: string;
  assigned: AssignmentRow[];
}

@Component({
  selector: 'app-staff-placements',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './staff-placements.html',
  styleUrl: './staff-placements.scss'
})
export class StaffPlacementsComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  activeTab: PlacementRole = 'DOCTOR';

  users: UserRow[] = [];
  assignments: Record<PlacementRole, AssignmentRow[]> = {
    DOCTOR: [],
    LAB_AGENT: [],
    PHARMACIST: [],
    RECEPTIONIST: []
  };
  workspaceOptions: Record<PlacementRole, WorkspaceOption[]> = {
    DOCTOR: [],
    LAB_AGENT: [],
    PHARMACIST: [],
    RECEPTIONIST: []
  };

  createForm: Record<PlacementRole, { userId: number | null; workspaceId: number | null }> = {
    DOCTOR: { userId: null, workspaceId: null },
    LAB_AGENT: { userId: null, workspaceId: null },
    PHARMACIST: { userId: null, workspaceId: null },
    RECEPTIONIST: { userId: null, workspaceId: null }
  };

  moveState: Record<number, { enabled: boolean; targetWorkspaceId: number | null }> = {};

  constructor(private http: HttpClient) {}

  async ngOnInit(): Promise<void> {
    await this.loadAll();
  }

  get totalAssignments(): number {
    return this.assignments.DOCTOR.length + this.assignments.LAB_AGENT.length + this.assignments.PHARMACIST.length + this.assignments.RECEPTIONIST.length;
  }

  get tabAssignments(): AssignmentRow[] {
    return this.assignments[this.activeTab];
  }

  get tabWorkspaceOptions(): WorkspaceOption[] {
    return this.workspaceOptions[this.activeTab];
  }

  get tabWorkspaceGroups(): WorkspaceGroup[] {
    return this.groupAssignmentsByWorkspace(this.activeTab);
  }

  get tabAvailableUsers(): UserRow[] {
    return this.availableUsersByRole(this.activeTab);
  }

  setTab(tab: PlacementRole): void {
    this.activeTab = tab;
    this.errorMessage = '';
    this.successMessage = '';
  }

  async loadAll(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      await Promise.all([
        this.loadUsers(),
        this.loadRoleData('DOCTOR'),
        this.loadRoleData('LAB_AGENT'),
        this.loadRoleData('PHARMACIST'),
        this.loadRoleData('RECEPTIONIST')
      ]);
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load staff placements.';
    } finally {
      this.loading = false;
    }
  }

  async createAssignment(role: PlacementRole): Promise<void> {
    const form = this.createForm[role];
    if (!form.userId || !form.workspaceId) {
      this.errorMessage = 'Please select both workspace and user.';
      return;
    }
    const selectedUser = this.users.find((user) => user.id === form.userId);
    if (!selectedUser || !selectedUser.enabled) {
      this.errorMessage = 'Selected user is not active/enabled for placement.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.post(`${environment.apiBaseUrl}/api/staff-assignments`, {
        userId: form.userId,
        role,
        workspaceId: form.workspaceId
      }, { headers }));

      this.successMessage = `${this.roleLabel(role)} placement created.`;
      this.createForm[role] = { userId: null, workspaceId: null };
      this.moveState = {};
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to create placement.';
    } finally {
      this.saving = false;
    }
  }

  startMove(assignmentId: number): void {
    this.moveState[assignmentId] = { enabled: true, targetWorkspaceId: null };
  }

  cancelMove(assignmentId: number): void {
    this.moveState[assignmentId] = { enabled: false, targetWorkspaceId: null };
  }

  async confirmMove(assignment: AssignmentRow): Promise<void> {
    const state = this.moveState[assignment.id];
    if (!state?.targetWorkspaceId) {
      this.errorMessage = 'Select target workspace first.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/staff-assignments/${assignment.id}/move`, {
        workspaceId: state.targetWorkspaceId
      }, { headers }));

      this.successMessage = 'Placement moved.';
      this.cancelMove(assignment.id);
      this.moveState = {};
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to move placement.';
    } finally {
      this.saving = false;
    }
  }

  async deleteAssignment(assignment: AssignmentRow): Promise<void> {
    if (!confirm(`Delete placement for ${this.userLabel(assignment.userId)}?`)) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.delete(`${environment.apiBaseUrl}/api/staff-assignments/${assignment.id}`, { headers }));
      this.successMessage = 'Placement deleted.';
      this.moveState = {};
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to delete placement.';
    } finally {
      this.saving = false;
    }
  }

  userLabel(userId: number): string {
    const user = this.users.find((item) => item.id === userId);
    if (!user) {
      return `User #${userId}`;
    }
    const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return fullName || user.username;
  }

  userEmail(userId: number): string {
    return this.users.find((item) => item.id === userId)?.email ?? '-';
  }

  roleLabel(role: PlacementRole): string {
    return role.replaceAll('_', ' ');
  }

  workspaceLabel(option: WorkspaceOption): string {
    return `${option.floorLabel} / ${option.workspaceName} (${option.workspaceCode})`;
  }

  private async loadRoleData(role: PlacementRole): Promise<void> {
    const headers = await this.authHeaders();
    const [assignments, workspaceOptions] = await Promise.all([
      firstValueFrom(this.http.get<AssignmentRow[]>(`${environment.apiBaseUrl}/api/staff-assignments`, {
        headers,
        params: { role }
      })),
      firstValueFrom(this.http.get<WorkspaceOption[]>(`${environment.apiBaseUrl}/api/staff-assignments/workspaces`, {
        headers,
        params: { role }
      }))
    ]);

    this.assignments[role] = assignments ?? [];
    this.workspaceOptions[role] = workspaceOptions ?? [];
  }

  private async loadUsers(): Promise<void> {
    const headers = await this.authHeaders();
    const allUsers = await firstValueFrom(this.http.get<UserRow[]>(`${environment.apiBaseUrl}/api/users`, { headers }));
    this.users = (allUsers ?? []).filter((user) => ['DOCTOR', 'LAB_AGENT', 'PHARMACIST', 'RECEPTIONIST'].includes(user.role) && user.enabled);
  }

  private availableUsersByRole(role: PlacementRole): UserRow[] {
    const usersByRole = this.users.filter((user) => user.role === role);

    if (role === 'DOCTOR') {
      const selectedWorkspaceId = this.createForm.DOCTOR.workspaceId;
      if (!selectedWorkspaceId) {
        return usersByRole.sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
      }

      const assignedToSelectedWorkspace = new Set(
        this.assignments.DOCTOR
          .filter((assignment) => assignment.workspaceId === selectedWorkspaceId)
          .map((assignment) => assignment.userId)
      );

      return usersByRole
        .filter((user) => !assignedToSelectedWorkspace.has(user.id))
        .sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
    }

    const assignedIds = new Set(this.assignments[role].map((assignment) => assignment.userId));
    return usersByRole
      .filter((user) => !assignedIds.has(user.id))
      .sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
  }

  private groupAssignmentsByWorkspace(role: PlacementRole): WorkspaceGroup[] {
    const groups = new Map<number, WorkspaceGroup>();

    for (const option of this.workspaceOptions[role]) {
      groups.set(option.workspaceId, {
        workspaceId: option.workspaceId,
        workspaceCode: option.workspaceCode,
        workspaceName: option.workspaceName,
        workspaceType: option.workspaceType,
        floorLabel: option.floorLabel,
        assigned: []
      });
    }

    for (const assignment of this.assignments[role]) {
      const group = groups.get(assignment.workspaceId);
      if (!group) {
        groups.set(assignment.workspaceId, {
          workspaceId: assignment.workspaceId,
          workspaceCode: assignment.workspaceCode,
          workspaceName: assignment.workspaceName,
          workspaceType: assignment.workspaceType,
          floorLabel: assignment.floorLabel,
          assigned: [assignment]
        });
      } else {
        group.assigned.push(assignment);
      }
    }

    return Array.from(groups.values())
      .map((group) => ({
        ...group,
        assigned: [...group.assigned].sort((a, b) => this.userLabel(a.userId).localeCompare(this.userLabel(b.userId)))
      }))
      .sort((a, b) => `${a.floorLabel}-${a.workspaceName}`.localeCompare(`${b.floorLabel}-${b.workspaceName}`));
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' });
  }
}
