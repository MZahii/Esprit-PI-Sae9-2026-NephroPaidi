import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { EquipmentItem, RecordMovementRequest } from '../../../../core/models/pharmacy.models';
import { ClinicalApiService, DoctorSearchResult } from '../../../../core/services/clinical-api.service';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment.component.html'
})
export class EquipmentComponent implements OnInit {
  private svc    = inject(PharmacyService);
  private apiSvc = inject(ClinicalApiService);

  items = signal<EquipmentItem[]>([]);
  loading = signal(false);

  showModal = false;
  editMode = false;
  current: Partial<EquipmentItem> = {};

  showMovementModal = false;
  movementTarget: EquipmentItem | null = null;
  movement: Partial<RecordMovementRequest> = {};

  readonly movementRoles = ['DOCTOR', 'NURSE', 'PHARMACIST', 'ADMIN'];
  movementRole = '';
  movementStaff = signal<DoctorSearchResult[]>([]);
  movementStaffLoading = signal(false);

  toast = signal('');
  toastOk = signal(true);
  saving = signal(false);

  CATEGORIES = ['SYRINGE', 'PPE', 'CONSUMABLE', 'BANDAGE', 'GLOVES', 'OTHER'];

  get lowStockCount() { return this.items().filter(i => i.lowStock && (i.currentStock ?? 0) > 0).length; }
  get outOfStockCount() { return this.items().filter(i => (i.currentStock ?? 0) === 0).length; }

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.svc.getEquipmentItems().subscribe({
      next: d => { this.items.set(d); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openCreate() {
    this.editMode = false;
    this.current = { category: 'CONSUMABLE', unit: 'piece', minimumStock: 10, currentStock: 0 };
    this.showModal = true;
  }

  openEdit(item: EquipmentItem) {
    this.editMode = true;
    this.current = { ...item };
    this.showModal = true;
  }

  save() {
    if (!this.current.name?.trim()) { this.showToast('Name is required', false); return; }
    this.saving.set(true);
    const obs = this.editMode && this.current.itemId
      ? this.svc.updateEquipmentItem(this.current.itemId, this.current as EquipmentItem)
      : this.svc.createEquipmentItem(this.current as EquipmentItem);
    obs.subscribe({
      next: () => { this.showModal = false; this.load(); this.showToast('Saved successfully'); this.saving.set(false); },
      error: () => { this.showToast('Save failed', false); this.saving.set(false); }
    });
  }

  delete(item: EquipmentItem) {
    if (!confirm(`Delete "${item.name}"?`)) return;
    this.svc.deleteEquipmentItem(item.itemId!).subscribe({
      next: () => { this.load(); this.showToast('Deleted'); },
      error: () => this.showToast('Delete failed', false)
    });
  }

  openMovement(item: EquipmentItem) {
    this.movementTarget = item;
    this.movement = { stockType: 'EQUIPMENT', itemId: item.itemId };
    this.movementRole = '';
    this.movementStaff.set([]);
    this.showMovementModal = true;
  }

  onMovementRoleChange() {
    this.movement.requestedBy = '';
    this.movement.requestedByRole = this.movementRole;
    this.movementStaff.set([]);
    if (!this.movementRole) return;
    this.movementStaffLoading.set(true);
    this.apiSvc.listStaffByRoles([this.movementRole], 100).subscribe({
      next: (staff) => { this.movementStaff.set(staff); this.movementStaffLoading.set(false); },
      error: ()     => { this.movementStaffLoading.set(false); }
    });
  }

  staffDisplayName(s: DoctorSearchResult): string {
    return [s.firstName, s.lastName].filter(Boolean).join(' ').trim() || s.username || s.email || '';
  }

  recordMovement() {
    if (!this.movement.quantityTaken || this.movement.quantityTaken <= 0) {
      this.showToast('Quantity must be greater than 0', false); return;
    }
    this.saving.set(true);
    this.svc.recordStockMovement(this.movement as RecordMovementRequest).subscribe({
      next: () => { this.showMovementModal = false; this.load(); this.showToast('Stock movement recorded'); this.saving.set(false); },
      error: (e) => { this.showToast(e?.error?.message || 'Failed to record movement', false); this.saving.set(false); }
    });
  }

  private showToast(msg: string, ok = true) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3000);
  }
}