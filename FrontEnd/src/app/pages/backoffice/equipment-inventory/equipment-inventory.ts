import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { getValidToken } from '../../../core/auth/keycloak.service';
import { environment } from '../../../../environments/environment';

type EquipmentCategory =
  | 'CONSULTATION_BED'
  | 'HOSPITALIZATION_BED'
  | 'DIALYSIS_BED'
  | 'DIALYSIS_MACHINE'
  | 'SURGERY_TABLE_OR_BED'
  | 'LABORATORY_EQUIPMENT'
  | 'IMAGING_EQUIPMENT'
  | 'ANALYSIS_EQUIPMENT';

type EquipmentStatus = 'AVAILABLE' | 'UNDER_MAINTENANCE' | 'OUT_OF_SERVICE' | 'ARCHIVED';

interface EquipmentItem {
  id: number;
  equipmentCode: string;
  name: string;
  category: EquipmentCategory;
  subtype: string;
  status: EquipmentStatus;
  description?: string;
  archived: boolean;
  archivedAt?: string;
  archivedReason?: string;
  currentWorkspaceId?: number;
  currentWorkspaceName?: string;
  currentFloorLabel?: string;
  createdAt: string;
  updatedAt: string;
}

interface EquipmentGroup {
  key: string;
  category: EquipmentCategory;
  subtype: string;
  count: number;
  available: number;
  underMaintenance: number;
  outOfService: number;
  archived: number;
  items: EquipmentItem[];
}

interface CategoryOption {
  category: EquipmentCategory;
  subtypeOptions: { value: string; label: string }[];
}

interface SummaryResponse {
  total: number;
  available: number;
  underMaintenance: number;
  outOfService: number;
  archived: number;
  byCategory: { category: string; count: number }[];
}

@Component({
  selector: 'app-equipment-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './equipment-inventory.html',
  styleUrl: './equipment-inventory.scss'
})
export class EquipmentInventoryComponent implements OnInit {
  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  items: EquipmentItem[] = [];
  grouped: EquipmentGroup[] = [];
  summary: SummaryResponse = { total: 0, available: 0, underMaintenance: 0, outOfService: 0, archived: 0, byCategory: [] };

  categoryOptions: CategoryOption[] = [];

  filters: {
    query: string;
    category: EquipmentCategory | 'ALL';
    subtype: string;
    status: EquipmentStatus | 'ALL';
    archived: 'ACTIVE' | 'ARCHIVED' | 'ALL';
  } = {
      query: '',
      category: 'ALL',
      subtype: '',
      status: 'ALL',
      archived: 'ACTIVE'
    };

  showCreateForm = false;
  selectedGroup: EquipmentGroup | null = null;

  form: {
    category: EquipmentCategory | '';
    subtype: string;
    customSubtype: string;
    quantity: number;
    description: string;
  } = {
      category: '',
      subtype: '',
      customSubtype: '',
      quantity: 1,
      description: ''
    };

  readonly statuses: EquipmentStatus[] = ['AVAILABLE', 'UNDER_MAINTENANCE', 'OUT_OF_SERVICE', 'ARCHIVED'];
  readonly complexSubtypeCategories: EquipmentCategory[] = ['LABORATORY_EQUIPMENT', 'ANALYSIS_EQUIPMENT', 'IMAGING_EQUIPMENT'];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAll();
  }

  get requiresSubtype(): boolean {
    if (!this.form.category) {
      return false;
    }
    return this.requiresSubtypeForCategory(this.form.category);
  }

  get subtypeOptions(): { value: string; label: string }[] {
    if (!this.form.category || !this.requiresSubtypeForCategory(this.form.category)) {
      return [];
    }
    return this.categoryOptions.find((option) => option.category === this.form.category)?.subtypeOptions ?? [];
  }

  get filterSubtypeOptions(): { value: string; label: string }[] {
    if (this.filters.category === 'ALL' || !this.requiresSubtypeForCategory(this.filters.category)) {
      return [];
    }
    return this.categoryOptions.find((option) => option.category === this.filters.category)?.subtypeOptions ?? [];
  }

  get usedCategoryCount(): number {
    return this.summary.byCategory.filter((entry) => entry.count > 0).length;
  }

  onCategoryChange(): void {
    this.form.subtype = '';
    this.form.customSubtype = '';
  }

  onFilterCategoryChange(): void {
    this.filters.subtype = '';
  }

  async loadAll(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';

    try {
      const headers = await this.authHeaders();
      const [summary, options] = await Promise.all([
        firstValueFrom(this.http.get<SummaryResponse>(`${environment.apiBaseUrl}/api/hospital-resources/equipment/summary`, { headers })),
        firstValueFrom(this.http.get<CategoryOption[]>(`${environment.apiBaseUrl}/api/hospital-resources/equipment/options`, { headers }))
      ]);

      this.summary = summary;
      this.categoryOptions = options;
      await this.search();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to load equipment data.';
    } finally {
      this.loading = false;
    }
  }

  async search(): Promise<void> {
    const headers = await this.authHeaders();
    const params: Record<string, string> = {};

    if (this.filters.archived === 'ACTIVE') {
      params['archived'] = 'false';
    }
    if (this.filters.archived === 'ARCHIVED') {
      params['archived'] = 'true';
    }

    if (this.filters.query.trim()) {
      params['query'] = this.filters.query.trim();
    }
    if (this.filters.category !== 'ALL') {
      params['category'] = this.filters.category;
    }
    if (this.filters.subtype.trim()) {
      params['subtype'] = this.filters.subtype.trim();
    }
    if (this.filters.status !== 'ALL') {
      params['status'] = this.filters.status;
    }

    const rows = await firstValueFrom(this.http.get<EquipmentItem[]>(`${environment.apiBaseUrl}/api/hospital-resources/equipment`, { headers, params }));
    this.items = rows ?? [];
    this.grouped = this.buildGrouped(this.items);

    if (this.selectedGroup) {
      this.selectedGroup = this.grouped.find((group) => group.key === this.selectedGroup?.key) ?? null;
    }
  }

  openCreate(): void {
    this.showCreateForm = true;
    this.form = {
      category: '',
      subtype: '',
      customSubtype: '',
      quantity: 1,
      description: ''
    };
    this.errorMessage = '';
  }

  closeCreate(): void {
    this.showCreateForm = false;
  }

  openGroupDetails(group: EquipmentGroup): void {
    this.selectedGroup = group;
  }

  closeGroupDetails(): void {
    this.selectedGroup = null;
  }

  async createBatch(): Promise<void> {
    if (!this.form.category) {
      this.errorMessage = 'Category is required.';
      return;
    }

    if (this.requiresSubtype && !this.form.subtype.trim()) {
      this.errorMessage = 'Subtype is required for this category.';
      return;
    }

    if (this.form.subtype === 'OTHER' && !this.form.customSubtype.trim()) {
      this.errorMessage = 'Custom subtype is required when subtype is OTHER.';
      return;
    }

    if (!this.form.quantity || this.form.quantity < 1) {
      this.errorMessage = 'Quantity must be greater than 0.';
      return;
    }
    if (!Number.isInteger(this.form.quantity)) {
      this.errorMessage = 'Quantity must be a whole number.';
      return;
    }
    if (this.form.quantity > 500) {
      this.errorMessage = 'Quantity cannot exceed 500 in one batch.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';

    try {
      const headers = await this.authHeaders();
      const payload = {
        category: this.form.category,
        subtype: this.requiresSubtype ? this.form.subtype : null,
        customSubtype: this.form.subtype === 'OTHER' ? this.form.customSubtype.trim() : null,
        quantity: this.form.quantity,
        description: this.form.description.trim() || null
      };

      const created = await firstValueFrom(
        this.http.post<EquipmentItem[]>(`${environment.apiBaseUrl}/api/hospital-resources/equipment`, payload, { headers })
      );

      const createdCount = created?.length ?? 0;
      this.successMessage = `${createdCount} equipment item(s) created successfully.`;
      this.showCreateForm = false;
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to create equipment.';
    } finally {
      this.saving = false;
    }
  }

  async updateStatus(item: EquipmentItem, status: EquipmentStatus): Promise<void> {
    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.patch(`${environment.apiBaseUrl}/api/hospital-resources/equipment/${item.id}/status`, { status }, { headers }));
      this.successMessage = `Status updated for ${item.equipmentCode}.`;
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to update status.';
    }
  }

  async archive(item: EquipmentItem): Promise<void> {
    const reason = prompt(`Archive ${item.equipmentCode}. Reason (optional):`) ?? '';
    if (!confirm(`Archive equipment ${item.equipmentCode}?`)) {
      return;
    }

    try {
      const headers = await this.authHeaders();
      await firstValueFrom(this.http.delete(`${environment.apiBaseUrl}/api/hospital-resources/equipment/${item.id}`, { headers, body: { reason } }));
      this.successMessage = `${item.equipmentCode} archived.`;
      await this.loadAll();
    } catch (error: any) {
      this.errorMessage = error?.error?.message || error?.message || 'Failed to archive equipment.';
    }
  }

  categoryLabel(value: string): string {
    return value.replaceAll('_', ' ');
  }

  groupLabel(group: EquipmentGroup): string {
    if (!this.requiresSubtypeForCategory(group.category)) {
      return this.categoryLabel(group.category);
    }
    return `${this.categoryLabel(group.category)} / ${this.categoryLabel(group.subtype)}`;
  }

  trackByGroup(_: number, group: EquipmentGroup): string {
    return group.key;
  }

  trackByItem(_: number, item: EquipmentItem): number {
    return item.id;
  }

  private buildGrouped(rows: EquipmentItem[]): EquipmentGroup[] {
    const map = new Map<string, EquipmentGroup>();

    for (const item of rows) {
      const key = `${item.category}::${item.subtype}`;
      const existing = map.get(key);

      if (!existing) {
        map.set(key, {
          key,
          category: item.category,
          subtype: item.subtype,
          count: 1,
          available: item.status === 'AVAILABLE' ? 1 : 0,
          underMaintenance: item.status === 'UNDER_MAINTENANCE' ? 1 : 0,
          outOfService: item.status === 'OUT_OF_SERVICE' ? 1 : 0,
          archived: item.archived || item.status === 'ARCHIVED' ? 1 : 0,
          items: [item]
        });
      } else {
        existing.count += 1;
        if (item.status === 'AVAILABLE') {
          existing.available += 1;
        }
        if (item.status === 'UNDER_MAINTENANCE') {
          existing.underMaintenance += 1;
        }
        if (item.status === 'OUT_OF_SERVICE') {
          existing.outOfService += 1;
        }
        if (item.archived || item.status === 'ARCHIVED') {
          existing.archived += 1;
        }
        existing.items.push(item);
      }
    }

    return Array.from(map.values())
      .map((group) => ({
        ...group,
        items: [...group.items].sort((a, b) => a.name.localeCompare(b.name))
      }))
      .sort((a, b) => this.groupLabel(a).localeCompare(this.groupLabel(b)));
  }

  private requiresSubtypeForCategory(category: EquipmentCategory): boolean {
    return this.complexSubtypeCategories.includes(category);
  }

  private async authHeaders(): Promise<HttpHeaders> {
    const token = await getValidToken();
    return new HttpHeaders({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' });
  }
}
