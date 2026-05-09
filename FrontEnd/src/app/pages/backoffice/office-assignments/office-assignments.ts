import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom, Observable, timeout } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { CountUpDirective } from '../../../shared/directives/count-up.directive';

type AssignmentRole = 'ADMIN' | 'HR';

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
  role: AssignmentRole;
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
  selector: 'app-office-assignments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CountUpDirective],
  templateUrl: './office-assignments.html',
  styleUrl: './office-assignments.scss'
})
export class OfficeAssignmentsComponent implements OnInit {
  private static readonly REQUEST_TIMEOUT_MS = 15000;

  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  activeTab: AssignmentRole = 'ADMIN';

  users: UserRow[] = [];
  assignments: Record<AssignmentRole, AssignmentRow[]> = {
    ADMIN: [],
    HR: []
  };
  workspaceOptions: Record<AssignmentRole, WorkspaceOption[]> = {
    ADMIN: [],
    HR: []
  };

  createForm: Record<AssignmentRole, { userId: number | null; workspaceId: number | null }> = {
    ADMIN: { userId: null, workspaceId: null },
    HR: { userId: null, workspaceId: null }
  };

  moveState: Record<number, { enabled: boolean; targetWorkspaceId: number | null }> = {};
  moveOptionsByAssignmentId: Record<number, WorkspaceOption[]> = {};
  workspaceGroupsByRole: Record<AssignmentRole, WorkspaceGroup[]> = { ADMIN: [], HR: [] };
  availableUsersByRoleCache: Record<AssignmentRole, UserRow[]> = { ADMIN: [], HR: [] };

  async ngOnInit(): Promise<void> {
    try {
      await this.loadAll();
    } catch {
      // loadAll already sets user-facing error state.
    }
  }

  get totalAssignments(): number {
    return this.assignments.ADMIN.length + this.assignments.HR.length;
  }

  get totalOfficeOptions(): number {
    return this.workspaceOptions.ADMIN.length + this.workspaceOptions.HR.length;
  }

  get occupiedOfficeOptions(): number {
    const occupied = new Set([
      ...this.assignments.ADMIN.map((assignment) => assignment.workspaceId),
      ...this.assignments.HR.map((assignment) => assignment.workspaceId)
    ]);
    return occupied.size;
  }

  get availableOfficeOptions(): number {
    return Math.max(0, this.totalOfficeOptions - this.occupiedOfficeOptions);
  }

  get adminWithoutOffice(): number {
    return this.availableUsersByRoleCache.ADMIN.length;
  }

  get hrWithoutOffice(): number {
    return this.availableUsersByRoleCache.HR.length;
  }

  get tabAssignments(): AssignmentRow[] {
    return this.assignments[this.activeTab];
  }

  get tabWorkspaceOptions(): WorkspaceOption[] {
    return this.workspaceOptions[this.activeTab];
  }

  get tabAvailableUsers(): UserRow[] {
    return this.availableUsersByRoleCache[this.activeTab];
  }

  get tabWorkspaceGroups(): WorkspaceGroup[] {
    return this.workspaceGroupsByRole[this.activeTab];
  }

  setTab(tab: AssignmentRole): void {
    this.activeTab = tab;
    this.moveState = {};
    this.moveOptionsByAssignmentId = {};
    this.errorMessage = '';
    this.successMessage = '';
  }

  async loadAll(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      await Promise.all([
        this.loadUsers(),
        this.loadRoleData('ADMIN'),
        this.loadRoleData('HR')
      ]);
      this.recomputeDerivedCaches();
    } catch (error: any) {
      this.errorMessage = this.extractBackendError(error, 'Failed to load office assignments.');
      throw error;
    } finally {
      this.loading = false;
    }
  }

  async createAssignment(role: AssignmentRole): Promise<void> {
    const form = this.createForm[role];
    if (!form.userId || !form.workspaceId) {
      this.errorMessage = 'Please select both user and workspace.';
      return;
    }
    const selectedUser = this.users.find((user) => user.id === form.userId);
    if (!selectedUser || !selectedUser.enabled) {
      this.errorMessage = 'Selected user is not active/enabled for assignment.';
      return;
    }

    if (this.saving) return;
    this.startSave();

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.withTimeout(this.http.post(`${environment.apiBaseUrl}/api/staff-assignments`, {
        userId: form.userId,
        role,
        workspaceId: form.workspaceId
      }, { headers })));

      this.successMessage = `${role} assignment created.`;
      this.createForm[role] = { userId: null, workspaceId: null };
      this.moveState = {};
      this.moveOptionsByAssignmentId = {};
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = this.extractBackendError(error, 'Failed to create assignment.');
    } finally {
      this.endSave();
    }
  }

  startMove(assignmentId: number): void {
    const assignment = this.tabAssignments.find((item) => item.id === assignmentId);
    if (!assignment) {
      return;
    }
    this.moveState[assignmentId] = { enabled: true, targetWorkspaceId: null };
    this.moveOptionsByAssignmentId[assignmentId] = this.workspaceOptions[assignment.role]
      .filter((option) => this.canMoveToWorkspace(assignment, option));
  }

  cancelMove(assignmentId: number): void {
    this.moveState[assignmentId] = { enabled: false, targetWorkspaceId: null };
    delete this.moveOptionsByAssignmentId[assignmentId];
  }

  async confirmMove(assignment: AssignmentRow): Promise<void> {
    const state = this.moveState[assignment.id];
    if (!state?.targetWorkspaceId) {
      this.errorMessage = 'Select target workspace first.';
      return;
    }

    if (this.saving) return;
    this.startSave();

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.withTimeout(this.http.patch(`${environment.apiBaseUrl}/api/staff-assignments/${assignment.id}/move`, {
        workspaceId: state.targetWorkspaceId
      }, { headers })));

      assignment.workspaceId = state.targetWorkspaceId;
      const selectedWorkspace = this.tabWorkspaceOptions.find((ws) => ws.workspaceId === state.targetWorkspaceId);
      if (selectedWorkspace) {
        assignment.workspaceCode = selectedWorkspace.workspaceCode;
        assignment.workspaceName = selectedWorkspace.workspaceName;
        assignment.workspaceType = selectedWorkspace.workspaceType;
        assignment.floorLabel = selectedWorkspace.floorLabel;
      }

      this.successMessage = 'Assignment moved.';
      this.cancelMove(assignment.id);
      this.moveState = {};
      this.moveOptionsByAssignmentId = {};
      await this.refreshCurrentTabData();
    } catch (error: any) {
      this.errorMessage = this.extractBackendError(error, 'Failed to move assignment.');
    } finally {
      this.endSave();
    }
  }

  async deleteAssignment(assignment: AssignmentRow): Promise<void> {
    if (!confirm(`Delete assignment for ${this.userLabel(assignment.userId)}?`)) {
      return;
    }

    if (this.saving) return;
    this.startSave();

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.withTimeout(this.http.delete(`${environment.apiBaseUrl}/api/staff-assignments/${assignment.id}`, { headers })));
      this.assignments[assignment.role] = this.assignments[assignment.role].filter((row) => row.id !== assignment.id);
      this.successMessage = 'Assignment deleted.';
      this.errorMessage = '';
      this.moveState = {};
      this.moveOptionsByAssignmentId = {};
      await this.refreshCurrentTabData();
    } catch (error: any) {
      this.errorMessage = this.extractBackendError(error, 'Failed to delete assignment.');
    } finally {
      this.endSave();
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

  workspaceLabel(option: WorkspaceOption): string {
    return `${option.floorLabel} / ${option.workspaceName} (${option.workspaceCode})`;
  }

  workspaceTypeLabel(workspaceType: string): string {
    return (workspaceType ?? '').split('_').join(' ');
  }

  canMoveToWorkspace(assignment: AssignmentRow, option: WorkspaceOption): boolean {
    if (assignment.workspaceId === option.workspaceId) {
      return false;
    }
    if (assignment.role === 'ADMIN' && option.assignedCount > 0) {
      return false;
    }
    return true;
  }

  moveOptionsFor(assignment: AssignmentRow): WorkspaceOption[] {
    return this.moveOptionsByAssignmentId[assignment.id] ?? [];
  }

  canCreateInWorkspace(role: AssignmentRole, option: WorkspaceOption): boolean {
    if (role === 'ADMIN' && option.assignedCount > 0) {
      return false;
    }
    return true;
  }

  private async loadRoleData(role: AssignmentRole): Promise<void> {
    const headers = await this.authHeaders();
    const [assignments, workspaceOptions] = await Promise.all([
      firstValueFrom(this.withTimeout(this.http.get<AssignmentRow[]>(`${environment.apiBaseUrl}/api/staff-assignments`, {
        headers,
        params: { role }
      }))),
      firstValueFrom(this.withTimeout(this.http.get<WorkspaceOption[]>(`${environment.apiBaseUrl}/api/staff-assignments/workspaces`, {
        headers,
        params: { role }
      })))
    ]);

    this.assignments[role] = assignments ?? [];
    this.workspaceOptions[role] = workspaceOptions ?? [];
    this.recomputeRoleDerived(role);
  }

  private async refreshCurrentTabData(): Promise<void> {
    try {
      await this.loadRoleData(this.activeTab);
      this.moveState = {};
      this.moveOptionsByAssignmentId = {};
    } catch (error: any) {
      this.errorMessage = this.extractBackendError(error, 'Data refresh failed. Please reload the page.');
    }
  }

  private async loadUsers(): Promise<void> {
    const headers = await this.authHeaders();
    const allUsers = await firstValueFrom(this.withTimeout(this.http.get<UserRow[]>(`${environment.apiBaseUrl}/api/users`, { headers })));
    this.users = (allUsers ?? []).filter((user) => ['ADMIN', 'HR'].includes(user.role));
    this.recomputeDerivedCaches();
  }

  private availableUsersByRole(role: AssignmentRole): UserRow[] {
    const assignedIds = new Set(this.assignments[role].map((assignment) => assignment.userId));
    return this.users
      .filter((user) => user.role === role && user.enabled)
      .filter((user) => !assignedIds.has(user.id))
      .sort((a, b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`));
  }

  private groupAssignmentsByWorkspace(role: AssignmentRole): WorkspaceGroup[] {
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

  private recomputeDerivedCaches(): void {
    this.recomputeRoleDerived('ADMIN');
    this.recomputeRoleDerived('HR');
  }

  private recomputeRoleDerived(role: AssignmentRole): void {
    this.workspaceGroupsByRole[role] = this.groupAssignmentsByWorkspace(role);
    this.availableUsersByRoleCache[role] = this.availableUsersByRole(role);
  }

  private withTimeout<T>(source$: Observable<T>): Observable<T> {
    return source$.pipe(timeout(OfficeAssignmentsComponent.REQUEST_TIMEOUT_MS));
  }

  private startSave(): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  private endSave(): void {
    this.saving = false;
  }

  private extractBackendError(error: unknown, fallbackMessage: string): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error;
      if (body && typeof body === 'object') {
        if ('message' in body && typeof body.message === 'string' && body.message.trim()) {
          return body.message;
        }
      }
      return error.message || fallbackMessage;
    }

    if (error instanceof Error && error.message.trim()) {
      return error.message;
    }

    return fallbackMessage;
  }

  trackByWorkspaceId(_index: number, group: WorkspaceGroup): number {
    return group.workspaceId;
  }

  trackByAssignmentId(_index: number, assignment: AssignmentRow): number {
    return assignment.id;
  }

  trackByWorkspaceOptionId(_index: number, option: WorkspaceOption): number {
    return option.workspaceId;
  }

  constructor(private http: HttpClient) {}
}




