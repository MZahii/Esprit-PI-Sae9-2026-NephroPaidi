import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type EquipmentStatus =
  | 'AVAILABLE'
  | 'UNDER_MAINTENANCE'
  | 'OUT_OF_SERVICE'
  | 'ARCHIVED';

interface PlacementWorkspaceSummary {
  workspaceId: number;
  workspaceCode: string;
  workspaceName: string;
  workspaceType: string;
  placedCount: number;
}

interface PlacementFloor {
  floorId: number;
  floorLabel: string;
  eligibleWorkspaceCount: number;
  totalPlacedEquipment: number;
  workspaces: PlacementWorkspaceSummary[];
}

interface EquipmentItem {
  id: number;
  equipmentCode: string;
  name: string;
  category: string;
  subtype: string;
  status: EquipmentStatus;
}

interface WorkspaceDetails {
  workspaceId: number;
  workspaceName: string;
  workspaceCode: string;
  workspaceType: string;
  floorLabel: string;
  allowedCategories: string[];
  placedEquipment: EquipmentItem[];
  availableCompatibleEquipment: EquipmentItem[];
}

interface EquipmentGroup {
  key: string;
  category: string;
  subtype: string;
  count: number;
  available: number;
  underMaintenance: number;
  outOfService: number;
  archived: number;
  items: EquipmentItem[];
}

const MOVE_COMPATIBILITY: Record<string, string[]> = {
  CONSULTATION_BED: ['DOCTOR_OFFICE'],
  HOSPITALIZATION_BED: ['HOSPITALIZATION_ROOM'],
  DIALYSIS_BED: ['DIALYSIS_ROOM'],
  DIALYSIS_MACHINE: ['DIALYSIS_ROOM'],
  SURGERY_TABLE_OR_BED: ['SURGERY_ROOM'],
  LABORATORY_EQUIPMENT: ['LABORATORY'],
  ANALYSIS_EQUIPMENT: ['LABORATORY'],
  IMAGING_EQUIPMENT: ['LABORATORY']
};

@Component({
  selector: 'app-equipment-placement',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './equipment-placement.html',
  styleUrl: './equipment-placement.scss'
})
export class EquipmentPlacementComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  floors: PlacementFloor[] = [];
  selectedFloorId: number | null = null;
  selectedWorkspaceId: number | null = null;
  details: WorkspaceDetails | null = null;

  groupedPlacedEquipment: EquipmentGroup[] = [];
  selectedGroup: EquipmentGroup | null = null;

  equipmentToPlaceId: number | null = null;
  movingEquipmentId: number | null = null;
  selectedMoveTargetWorkspaceId: number | null = null;

  readonly statuses: EquipmentStatus[] = ['AVAILABLE', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'ARCHIVED'];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadFloors();
  }

  get selectedFloor(): PlacementFloor | null {
    return this.floors.find((f) => f.floorId === this.selectedFloorId) ?? null;
  }

  get selectedWorkspaceSummary(): PlacementWorkspaceSummary | null {
    if (!this.selectedWorkspaceId) {
      return null;
    }

    for (const floor of this.floors) {
      const found = floor.workspaces.find((workspace) => workspace.workspaceId === this.selectedWorkspaceId);
      if (found) {
        return found;
      }
    }

    return null;
  }

  get totalEligibleRooms(): number {
    return this.floors.reduce((acc, floor) => acc + floor.eligibleWorkspaceCount, 0);
  }

  async loadFloors(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      const rows = await firstValueFrom(this.http.get<PlacementFloor[]>(`${environment.apiBaseUrl}/api/hospital-resources/placement/floors`, { headers }));
      this.floors = (rows ?? []).sort((a, b) => {
        const av = a.floorLabel === 'GF' ? 0 : Number(a.floorLabel);
        const bv = b.floorLabel === 'GF' ? 0 : Number(b.floorLabel);
        return av - bv;
      });

      if (!this.selectedFloorId && this.floors.length > 0) {
        this.selectedFloorId = this.floors[0].floorId;
      }

      if (this.selectedWorkspaceId) {
        const exists = this.floors.some((floor) => floor.workspaces.some((workspace) => workspace.workspaceId === this.selectedWorkspaceId));
        if (!exists) {
          this.selectedWorkspaceId = null;
          this.details = null;
          this.groupedPlacedEquipment = [];
          this.selectedGroup = null;
        }
      }
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load placement floors.';
    } finally {
      this.loading = false;
    }
  }

  async selectFloor(floorId: number): Promise<void> {
    this.selectedFloorId = floorId;
    this.selectedWorkspaceId = null;
    this.details = null;
    this.groupedPlacedEquipment = [];
    this.selectedGroup = null;
    this.equipmentToPlaceId = null;
    this.movingEquipmentId = null;
    this.selectedMoveTargetWorkspaceId = null;
  }

  async openWorkspace(workspaceId: number): Promise<void> {
    this.selectedWorkspaceId = workspaceId;
    this.equipmentToPlaceId = null;
    this.movingEquipmentId = null;
    this.selectedMoveTargetWorkspaceId = null;
    this.selectedGroup = null;
    await this.loadWorkspaceDetails();
  }

  async loadWorkspaceDetails(): Promise<void> {
    if (!this.selectedWorkspaceId) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      const loaded = await firstValueFrom(
        this.http.get<WorkspaceDetails>(`${environment.apiBaseUrl}/api/hospital-resources/placement/workspaces/${this.selectedWorkspaceId}`, { headers })
      );

      this.details = {
        ...loaded,
        placedEquipment: [...loaded.placedEquipment].sort((a, b) => a.name.localeCompare(b.name)),
        availableCompatibleEquipment: [...loaded.availableCompatibleEquipment].sort((a, b) => a.name.localeCompare(b.name))
      };

      this.groupedPlacedEquipment = this.buildGrouped(this.details.placedEquipment);
      if (this.selectedGroup) {
        this.selectedGroup = this.groupedPlacedEquipment.find((group) => group.key === this.selectedGroup?.key) ?? null;
      }
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load workspace details.';
    } finally {
      this.loading = false;
    }
  }

  openGroupDetails(group: EquipmentGroup): void {
    this.selectedGroup = group;
  }

  closeGroupDetails(): void {
    this.selectedGroup = null;
  }

  async placeEquipment(): Promise<void> {
    if (!this.selectedWorkspaceId || !this.equipmentToPlaceId) {
      return;
    }

    this.saving = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.http.post(
          `${environment.apiBaseUrl}/api/hospital-resources/placement/workspaces/${this.selectedWorkspaceId}/place`,
          { equipmentId: this.equipmentToPlaceId },
          { headers }
        )
      );
      this.successMessage = 'Equipment placed successfully.';
      this.equipmentToPlaceId = null;
      await this.refreshPlacementState();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to place equipment.';
    } finally {
      this.saving = false;
    }
  }

  async removeEquipment(equipmentId: number): Promise<void> {
    if (!confirm('Remove this equipment from the current room?')) {
      return;
    }

    this.saving = true;

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/hospital-resources/placement/equipment/${equipmentId}/remove`, {}, { headers }));
      this.successMessage = 'Equipment removed from room.';
      await this.refreshPlacementState();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to remove equipment.';
    } finally {
      this.saving = false;
    }
  }

  startMove(equipmentId: number): void {
    this.movingEquipmentId = equipmentId;
    this.selectedMoveTargetWorkspaceId = null;
    this.errorMessage = '';
  }

  cancelMove(): void {
    this.movingEquipmentId = null;
    this.selectedMoveTargetWorkspaceId = null;
  }

  async confirmMove(equipment: EquipmentItem): Promise<void> {
    if (!this.selectedMoveTargetWorkspaceId) {
      this.errorMessage = 'Select target workspace first.';
      return;
    }
    const allowedTargetIds = this.moveTargetOptionsFor(equipment).map((option) => option.workspaceId);
    if (!allowedTargetIds.includes(this.selectedMoveTargetWorkspaceId)) {
      this.errorMessage = 'Selected target workspace is not compatible with this equipment type.';
      return;
    }

    this.saving = true;

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(
        this.http.patch(
          `${environment.apiBaseUrl}/api/hospital-resources/placement/equipment/${equipment.id}/move`,
          { targetWorkspaceId: this.selectedMoveTargetWorkspaceId },
          { headers }
        )
      );
      this.successMessage = 'Equipment moved.';
      this.movingEquipmentId = null;
      this.selectedMoveTargetWorkspaceId = null;
      await this.refreshPlacementState();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to move equipment.';
    } finally {
      this.saving = false;
    }
  }

  async updateStatus(equipmentId: number, status: EquipmentStatus): Promise<void> {
    this.saving = true;

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/hospital-resources/placement/equipment/${equipmentId}/status`, { status }, { headers }));
      await this.refreshPlacementState();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to update status.';
    } finally {
      this.saving = false;
    }
  }

  label(value: string): string {
    return value.replaceAll('_', ' ');
  }

  groupLabel(group: EquipmentGroup): string {
    return `${this.label(group.category)} / ${this.label(group.subtype)}`;
  }

  trackByGroup(_: number, group: EquipmentGroup): string {
    return group.key;
  }

  trackByItem(_: number, item: EquipmentItem): number {
    return item.id;
  }

  moveTargetOptionsFor(equipment: EquipmentItem): Array<{ label: string; workspaceId: number }> {
    const allowedWorkspaceTypes = new Set(MOVE_COMPATIBILITY[equipment.category] ?? []);
    if (allowedWorkspaceTypes.size === 0) {
      return [];
    }

    const options: Array<{ label: string; workspaceId: number }> = [];
    for (const floor of this.floors) {
      for (const workspace of floor.workspaces) {
        if (workspace.workspaceId === this.selectedWorkspaceId) {
          continue;
        }
        if (!allowedWorkspaceTypes.has(workspace.workspaceType)) {
          continue;
        }
        options.push({
          label: `${floor.floorLabel} / ${workspace.workspaceName}`,
          workspaceId: workspace.workspaceId
        });
      }
    }

    return options.sort((a, b) => a.label.localeCompare(b.label));
  }

  private buildGrouped(rows: EquipmentItem[]): EquipmentGroup[] {
    const map = new Map<string, EquipmentGroup>();

    for (const row of rows) {
      const key = `${row.category}::${row.subtype}`;
      const existing = map.get(key);

      if (!existing) {
        map.set(key, {
          key,
          category: row.category,
          subtype: row.subtype,
          count: 1,
          available: row.status === 'AVAILABLE' ? 1 : 0,
          underMaintenance: row.status === 'UNDER_MAINTENANCE' ? 1 : 0,
          outOfService: row.status === 'OUT_OF_SERVICE' ? 1 : 0,
          archived: row.status === 'ARCHIVED' ? 1 : 0,
          items: [row]
        });
      } else {
        existing.count += 1;
        if (row.status === 'AVAILABLE') {
          existing.available += 1;
        }
        if (row.status === 'UNDER_MAINTENANCE') {
          existing.underMaintenance += 1;
        }
        if (row.status === 'OUT_OF_SERVICE') {
          existing.outOfService += 1;
        }
        if (row.status === 'ARCHIVED') {
          existing.archived += 1;
        }
        existing.items.push(row);
      }
    }

    return Array.from(map.values())
      .map((group) => ({
        ...group,
        items: [...group.items].sort((a, b) => a.name.localeCompare(b.name))
      }))
      .sort((a, b) => this.groupLabel(a).localeCompare(this.groupLabel(b)));
  }

  private async refreshPlacementState(): Promise<void> {
    await this.loadFloors();
    await this.loadWorkspaceDetails();
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' });
  }
}
