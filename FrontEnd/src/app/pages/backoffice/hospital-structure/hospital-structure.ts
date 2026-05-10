import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom, Observable, timeout } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';
import { CountUpDirective } from '../../../shared/directives/count-up.directive';

type FloorInsertPosition = 'TOP' | 'BOTTOM' | 'BETWEEN';

type WorkspaceType =
  | 'ADMIN_OFFICE'
  | 'HR_OFFICE'
  | 'DOCTOR_OFFICE'
  | 'OFFICE'
  | 'HOSPITALIZATION_ROOM'
  | 'DIALYSIS_ROOM'
  | 'SURGERY_ROOM'
  | 'LABORATORY'
  | 'PHARMACY'
  | 'RECEPTION';

interface WorkspaceTypeOption {
  value: WorkspaceType;
  label: string;
  codePrefix: string;
}

interface WorkspaceItem {
  id: number;
  floorId: number;
  floorLabel: string;
  floorOrder: number;
  workspaceType: WorkspaceType;
  workspaceTypeLabel: string;
  sequenceNumber: number;
  workspaceCode: string;
  workspaceName: string;
  createdAt: string;
  updatedAt: string;
}

interface FloorItem {
  id: number;
  floorOrder: number;
  floorLabel: string;
  description?: string;
  totalWorkspaces: number;
  createdAt: string;
  updatedAt: string;
  workspaces: WorkspaceItem[];
}

interface StructureResponse {
  initialized: boolean;
  totalFloors: number;
  totalWorkspaces: number;
  floors: FloorItem[];
}

interface WorkspaceGroupView {
  workspaceType: WorkspaceType;
  label: string;
  count: number;
  items: WorkspaceItem[];
}

interface SummaryStats {
  totalFloors: number;
  totalWorkspaces: number;
  totalWorkspaceTypesUsed: number;
  emptyFloors: number;
  floorsWithWorkspaces: number;
  mostUsedWorkspaceType: string;
  lastUpdatedAt: string;
}

@Component({
  selector: 'app-hospital-structure',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CountUpDirective],
  templateUrl: './hospital-structure.html',
  styleUrl: './hospital-structure.scss'
})
export class HospitalStructureComponent implements OnInit {
  private static readonly REQUEST_TIMEOUT_MS = 15000;

  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  structure: StructureResponse | null = null;
  workspaceTypes: WorkspaceTypeOption[] = [];

  initializeTotalFloors = 3;

  addFloorPosition: FloorInsertPosition = 'TOP';
  addFloorAfterId: number | null = null;

  expandedFloorIds = new Set<number>();

  workspaceFormByFloor: Record<number, { workspaceType: WorkspaceType | ''; quantity: number }> = {};

  groupedWorkspacesByFloor: Record<number, WorkspaceGroupView[]> = {};
  summaryStats: SummaryStats = {
    totalFloors: 0,
    totalWorkspaces: 0,
    totalWorkspaceTypesUsed: 0,
    emptyFloors: 0,
    floorsWithWorkspaces: 0,
    mostUsedWorkspaceType: '-',
    lastUpdatedAt: '-'
  };

  detailModal: { floor: FloorItem; group: WorkspaceGroupView } | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAll().catch(() => {
      // loadAll already sets user-facing error state.
    });
  }

  get canInsertBetween(): boolean {
    return (this.structure?.floors.length ?? 0) > 1;
  }

  get betweenCandidates(): FloorItem[] {
    const floors = this.structure?.floors ?? [];
    return floors.slice(0, Math.max(0, floors.length - 1));
  }

  toggleFloor(floorId: number): void {
    if (this.expandedFloorIds.has(floorId)) {
      this.expandedFloorIds.delete(floorId);
      return;
    }
    this.expandedFloorIds.add(floorId);
  }

  isExpanded(floorId: number): boolean {
    return this.expandedFloorIds.has(floorId);
  }

  quickGroupPreview(floorId: number): WorkspaceGroupView[] {
    return (this.groupedWorkspacesByFloor[floorId] ?? []).slice(0, 4);
  }

  get busiestFloorLabel(): string {
    const floor = [...(this.structure?.floors ?? [])]
      .sort((a, b) => b.totalWorkspaces - a.totalWorkspaces)[0];
    return floor ? `${floor.floorLabel} (${floor.totalWorkspaces})` : '-';
  }

  get workspaceTypeDistribution(): Array<{ label: string; count: number }> {
    const counters = new Map<string, number>();
    for (const floor of this.structure?.floors ?? []) {
      for (const workspace of floor.workspaces) {
        counters.set(workspace.workspaceTypeLabel, (counters.get(workspace.workspaceTypeLabel) ?? 0) + 1);
      }
    }
    return Array.from(counters.entries())
      .map(([label, count]) => ({ label, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);
  }

  async loadAll(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      const [structure, workspaceTypes] = await Promise.all([
        this.fetchStructure(),
        this.fetchWorkspaceTypes()
      ]);
      this.structure = structure;
      this.workspaceTypes = workspaceTypes;
      this.ensureFormDefaults();
      this.recomputeDerivedState();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to load hospital structure.');
      throw error;
    } finally {
      this.loading = false;
    }
  }

  async initializeFloors(): Promise<void> {
    if (this.saving) return;
    if (!Number.isFinite(this.initializeTotalFloors) || this.initializeTotalFloors < 1) {
      this.errorMessage = 'Please enter a valid floor count (minimum 1).';
      return;
    }
    if (this.initializeTotalFloors > 120) {
      this.errorMessage = 'Floor count cannot exceed 120.';
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.withTimeout(this.http.post<StructureResponse>(
          `${environment.apiBaseUrl}/api/hospital-structure/floors/initialize`,
          { totalFloors: this.initializeTotalFloors },
          { headers }
        ))
      );

      this.successMessage = 'Floors initialized successfully.';
      await this.refreshStructureOnly();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to initialize floors.');
    } finally {
      this.endSave();
    }
  }

  async addFloor(): Promise<void> {
    if (this.saving) return;
    if (this.addFloorPosition === 'BETWEEN' && !this.addFloorAfterId) {
      this.errorMessage = 'Please select the floor after which you want to insert.';
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.withTimeout(this.http.post(
          `${environment.apiBaseUrl}/api/hospital-structure/floors`,
          {
            position: this.addFloorPosition,
            afterFloorId: this.addFloorPosition === 'BETWEEN' ? this.addFloorAfterId : null
          },
          { headers }
        ))
      );

      this.successMessage = 'Floor added and renumbered successfully.';
      await this.refreshStructureOnly();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to add floor.');
    } finally {
      this.endSave();
    }
  }

  async deleteFloor(floor: FloorItem): Promise<void> {
    if (this.saving) return;
    if (!confirm(`Delete floor ${floor.floorLabel} and all its workspaces?`)) {
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.withTimeout(this.http.delete(`${environment.apiBaseUrl}/api/hospital-structure/floors/${floor.id}`, { headers })));
      this.successMessage = `Floor ${floor.floorLabel} deleted.`;
      this.expandedFloorIds.delete(floor.id);
      await this.refreshStructureOnly();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to delete floor.');
    } finally {
      this.endSave();
    }
  }

  async addWorkspaces(floor: FloorItem): Promise<void> {
    const form = this.workspaceFormByFloor[floor.id];
    if (!form || !form.workspaceType) {
      this.errorMessage = `Select a workspace type for floor ${floor.floorLabel}.`;
      return;
    }
    if (!Number.isFinite(form.quantity) || form.quantity < 1) {
      this.errorMessage = 'Quantity must be at least 1.';
      return;
    }
    if (form.quantity > 200) {
      this.errorMessage = 'Quantity cannot exceed 200 per request.';
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.withTimeout(this.http.post(
          `${environment.apiBaseUrl}/api/hospital-structure/workspaces`,
          {
            floorId: floor.id,
            workspaceType: form.workspaceType,
            quantity: form.quantity
          },
          { headers }
        ))
      );

      this.workspaceFormByFloor[floor.id] = { workspaceType: '', quantity: 1 };
      this.successMessage = `Workspace group added to floor ${floor.floorLabel}.`;
      await this.refreshStructureOnly();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to add workspace group.');
    } finally {
      this.endSave();
    }
  }

  async deleteGroup(floor: FloorItem, group: WorkspaceGroupView): Promise<void> {
    if (this.saving) return;
    if (!confirm(`Delete all ${group.label} from floor ${floor.floorLabel}?`)) {
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.withTimeout(this.http.delete(
          `${environment.apiBaseUrl}/api/hospital-structure/workspaces/groups`,
          {
            headers,
            params: { floorId: String(floor.id), workspaceType: group.workspaceType }
          }
        ))
      );

      this.successMessage = `${group.label} removed from floor ${floor.floorLabel}.`;
      await this.refreshStructureOnly();
    } catch (error: unknown) {
      this.handleError(error, 'Failed to delete workspace group.');
    } finally {
      this.endSave();
    }
  }

  openGroupDetails(floor: FloorItem, group: WorkspaceGroupView): void {
    this.detailModal = { floor, group };
  }

  closeGroupDetails(): void {
    this.detailModal = null;
  }

  async deleteWorkspaceItem(item: WorkspaceItem): Promise<void> {
    if (this.saving) return;
    if (!confirm(`Delete ${item.workspaceName}?`)) {
      return;
    }

    this.startSave();
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.withTimeout(this.http.delete(`${environment.apiBaseUrl}/api/hospital-structure/workspaces/${item.id}`, { headers })));
      this.successMessage = `${item.workspaceName} deleted.`;
      await this.refreshStructureOnly();
      if (this.detailModal) {
        const floor = this.structure?.floors.find((f) => f.id === this.detailModal!.floor.id);
        const group = (this.groupedWorkspacesByFloor[floor?.id ?? -1] ?? []).find((g) => g.workspaceType === this.detailModal!.group.workspaceType);
        if (!floor || !group) {
          this.detailModal = null;
        } else {
          this.detailModal = { floor, group };
        }
      }
    } catch (error: unknown) {
      this.handleError(error, 'Failed to delete workspace item.');
    } finally {
      this.endSave();
    }
  }

  private recomputeDerivedState(): void {
    const floors = this.structure?.floors ?? [];
    const typeCounters = new Map<string, number>();
    const grouped: Record<number, WorkspaceGroupView[]> = {};

    let emptyFloors = 0;
    let floorsWithWorkspaces = 0;
    let latestDate: Date | null = null;

    for (const floor of floors) {
      if (!floor.workspaces.length) {
        emptyFloors += 1;
      } else {
        floorsWithWorkspaces += 1;
      }

      const floorUpdated = new Date(floor.updatedAt);
      if (!Number.isNaN(floorUpdated.getTime()) && (!latestDate || floorUpdated > latestDate)) {
        latestDate = floorUpdated;
      }

      const byType = new Map<WorkspaceType, WorkspaceItem[]>();
      for (const workspace of floor.workspaces) {
        const items = byType.get(workspace.workspaceType) ?? [];
        items.push(workspace);
        byType.set(workspace.workspaceType, items);

        const counterKey = workspace.workspaceTypeLabel;
        typeCounters.set(counterKey, (typeCounters.get(counterKey) ?? 0) + 1);

        const wsUpdated = new Date(workspace.updatedAt);
        if (!Number.isNaN(wsUpdated.getTime()) && (!latestDate || wsUpdated > latestDate)) {
          latestDate = wsUpdated;
        }
      }

      grouped[floor.id] = Array.from(byType.entries())
        .map(([workspaceType, items]) => {
          const sortedItems = [...items].sort((a, b) => a.workspaceName.localeCompare(b.workspaceName));
          return {
            workspaceType,
            label: sortedItems[0]?.workspaceTypeLabel ?? workspaceType,
            count: sortedItems.length,
            items: sortedItems
          };
        })
        .sort((a, b) => a.label.localeCompare(b.label));

    }

    this.groupedWorkspacesByFloor = grouped;

    let mostUsedWorkspaceType = '-';
    let maxCount = 0;
    for (const [label, count] of typeCounters.entries()) {
      if (count > maxCount) {
        maxCount = count;
        mostUsedWorkspaceType = `${label} (${count})`;
      }
    }

    this.summaryStats = {
      totalFloors: floors.length,
      totalWorkspaces: floors.reduce((sum, f) => sum + f.workspaces.length, 0),
      totalWorkspaceTypesUsed: typeCounters.size,
      emptyFloors,
      floorsWithWorkspaces,
      mostUsedWorkspaceType,
      lastUpdatedAt: latestDate ? latestDate.toLocaleString() : '-'
    };
  }

  private async refreshStructureOnly(): Promise<void> {
    try {
      this.structure = await this.fetchStructure();
      this.ensureFormDefaults();
      this.recomputeDerivedState();
    } catch (error: unknown) {
      this.handleError(error, 'Data refresh failed. Please reload the page.');
    }
  }

  private ensureFormDefaults(): void {
    for (const floor of this.structure?.floors ?? []) {
      if (!this.workspaceFormByFloor[floor.id]) {
        this.workspaceFormByFloor[floor.id] = { workspaceType: '', quantity: 1 };
      }
    }
  }

  private async fetchStructure(): Promise<StructureResponse> {
    const headers = await this.authHeaders();
    return firstValueFrom(this.withTimeout(this.http.get<StructureResponse>(`${environment.apiBaseUrl}/api/hospital-structure`, { headers })));
  }

  private async fetchWorkspaceTypes(): Promise<WorkspaceTypeOption[]> {
    const headers = await this.authHeaders();
    const response = await firstValueFrom(
      this.withTimeout(this.http.get<WorkspaceTypeOption[]>(`${environment.apiBaseUrl}/api/hospital-structure/workspace-types`, { headers }))
    );
    return Array.isArray(response) ? response : [];
  }

  private withTimeout<T>(source$: Observable<T>): Observable<T> {
    return source$.pipe(timeout(HospitalStructureComponent.REQUEST_TIMEOUT_MS));
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  private startSave(): void {
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  private endSave(): void {
    this.saving = false;
  }

  private handleError(error: unknown, fallbackMessage: string): void {
    if (error instanceof HttpErrorResponse) {
      this.errorMessage = this.extractBackendError(error) || fallbackMessage;
      return;
    }
    this.errorMessage = error instanceof Error ? error.message : fallbackMessage;
  }

  private extractBackendError(error: HttpErrorResponse): string {
    const body = error.error;
    if (body && typeof body === 'object') {
      if ('message' in body && typeof body.message === 'string') {
        return body.message;
      }
      if ('validationErrors' in body && body.validationErrors && typeof body.validationErrors === 'object') {
        return Object.values(body.validationErrors as Record<string, string>).join(' | ');
      }
    }
    return error.message;
  }
}




