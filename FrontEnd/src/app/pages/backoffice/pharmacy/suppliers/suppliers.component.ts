import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { Medication, Supplier, SupplierStats, SupplyOrder } from '../../../../core/models/pharmacy.models';

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './suppliers.component.html'
})
export class SuppliersComponent implements OnInit {
  private svc = inject(PharmacyService);

  suppliers    = signal<Supplier[]>([]);
  orders       = signal<SupplyOrder[]>([]);
  selectedSupplier: Supplier | null = null;
  supplierStats: SupplierStats | null = null;
  loading      = signal(false);
  togglingId   = signal<number | null>(null);

  // Search & filter — must be signals so computed() reacts to changes
  searchQuery  = signal('');
  statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  filteredSuppliers = computed(() => {
    let list = this.suppliers();
    if (this.statusFilter() === 'ACTIVE')   list = list.filter(s => s.isActive !== false);
    if (this.statusFilter() === 'INACTIVE') list = list.filter(s => s.isActive === false);
    const q = this.searchQuery().trim().toLowerCase();
    if (q) {
      list = list.filter(s =>
        s.name.toLowerCase().includes(q) ||
        (s.contactInfo || '').toLowerCase().includes(q));
    }
    return list;
  });

  // Supplier modal
  showSupplierModal = false;
  editMode = false;
  currentSupplier: Partial<Supplier> = {};

  // Order modal
  showOrderModal = false;
  newOrder: Partial<SupplyOrder> = {};
  medications = signal<Medication[]>([]);
  medicationMap = computed(() => {
    const map: Record<number, string> = {};
    this.medications().forEach(m => { if (m.medicationId != null) map[m.medicationId] = m.name; });
    return map;
  });

  // Order detail modal (view notes & full info)
  showOrderDetail = false;
  detailOrder: SupplyOrder | null = null;

  // Validation
  supplierNameError = signal('');
  orderQtyError     = signal('');
  orderDateError    = signal('');

  // Loading states
  saving       = signal(false);
  placingOrder = signal(false);

  // Toast
  toast   = signal('');
  toastOk = signal(true);

  ngOnInit() { this.load(); this.loadMedications(); }

  // ─── Computed helpers ─────────────────────────────────────────────────────

  isOverdue(o: SupplyOrder): boolean {
    if (o.status !== 'PENDING' || !o.expectedDeliveryDate) return false;
    return new Date(o.expectedDeliveryDate) < new Date(new Date().toDateString());
  }

  overdueCount = computed(() => this.orders().filter(o => this.isOverdue(o)).length);

  pendingCount = computed(() => this.orders().filter(o => o.status === 'PENDING').length);

  // ─── Load ────────────────────────────────────────────────────────────────

  load() {
    this.loading.set(true);
    this.svc.getSuppliers().subscribe({ next: d => { this.suppliers.set(d); this.loading.set(false); } });
  }

  loadMedications() {
    this.svc.getMedications().subscribe({ next: d => this.medications.set(d) });
  }

  // ─── Supplier CRUD ───────────────────────────────────────────────────────

  validateSupplierName(name: string | undefined): string {
    if (!name || !name.trim()) return 'Supplier name is required.';
    if (!/^[a-zA-ZÀ-ÿ\s\-&.]+$/.test(name.trim())) return 'Name must contain letters only.';
    return '';
  }

  onSupplierNameChange(value: string) {
    this.currentSupplier.name = value;
    this.supplierNameError.set(this.validateSupplierName(value));
  }

  openAdd() {
    this.editMode = false;
    this.currentSupplier = { name: '', contactInfo: '' };
    this.supplierNameError.set('');
    this.showSupplierModal = true;
  }

  openEdit(s: Supplier) {
    this.editMode = true;
    this.currentSupplier = { ...s };
    this.supplierNameError.set('');
    this.showSupplierModal = true;
  }

  save() {
    const nameErr = this.validateSupplierName(this.currentSupplier.name);
    if (nameErr) { this.supplierNameError.set(nameErr); return; }
    const obs = this.editMode
      ? this.svc.updateSupplier(this.currentSupplier.supplierId!, this.currentSupplier as Supplier)
      : this.svc.createSupplier(this.currentSupplier as Supplier);
    this.saving.set(true);
    obs.subscribe({
      next: () => { this.saving.set(false); this.showSupplierModal = false; this.load(); this.notify(`Supplier ${this.editMode ? 'updated' : 'created'}`, true); },
      error: () => { this.saving.set(false); this.notify('Operation failed', false); }
    });
  }

  delete(s: Supplier) {
    if (!confirm(`Delete "${s.name}"?`)) return;
    this.svc.deleteSupplier(s.supplierId!).subscribe({
      next: () => {
        if (this.selectedSupplier?.supplierId === s.supplierId) {
          this.selectedSupplier = null;
          this.supplierStats = null;
          this.orders.set([]);
        }
        this.load();
        this.notify('Supplier deleted', true);
      }
    });
  }

  toggleStatus(s: Supplier) {
    this.togglingId.set(s.supplierId!);
    this.svc.toggleSupplierStatus(s.supplierId!).subscribe({
      next: updated => {
        this.togglingId.set(null);
        this.suppliers.update(list => list.map(x => x.supplierId === updated.supplierId ? updated : x));
        if (this.selectedSupplier?.supplierId === updated.supplierId) this.selectedSupplier = updated;
        this.notify(`Supplier ${updated.isActive ? 'activated' : 'deactivated'}`, true);
      },
      error: () => { this.togglingId.set(null); this.notify('Toggle failed', false); }
    });
  }

  // ─── Select supplier & load stats ────────────────────────────────────────

  selectSupplier(s: Supplier) {
    this.selectedSupplier = s;
    this.supplierStats = null;
    this.svc.getOrdersForSupplier(s.supplierId!).subscribe({ next: d => this.orders.set(d) });
    this.svc.getSupplierStats(s.supplierId!).subscribe({ next: d => this.supplierStats = d });
  }

  // ─── Orders ──────────────────────────────────────────────────────────────

  validateOrderQty(qty: number | undefined): string {
    if (qty == null || isNaN(qty)) return 'Quantity is required.';
    if (!Number.isInteger(qty)) return 'Quantity must be a whole number.';
    if (qty < 1) return 'Quantity must be at least 1.';
    return '';
  }

  validateExpectedDate(date: string | undefined): string {
    if (!date) return '';
    const d = new Date(date);
    const today = new Date(); today.setHours(0,0,0,0);
    return d < today ? 'Expected delivery date cannot be in the past.' : '';
  }

  onOrderQtyChange(value: number) {
    this.newOrder.orderedQuantity = value;
    this.orderQtyError.set(this.validateOrderQty(value));
  }

  onOrderDateChange(value: string) {
    this.newOrder.expectedDeliveryDate = value;
    this.orderDateError.set(this.validateExpectedDate(value));
  }

  openOrder() {
    this.newOrder = { medicationId: undefined as any, orderedQuantity: 1, notes: '' };
    this.orderQtyError.set('');
    this.orderDateError.set('');
    if (this.medications().length === 0) this.loadMedications();
    this.showOrderModal = true;
  }

  placeOrder() {
    const qtyErr = this.validateOrderQty(this.newOrder.orderedQuantity);
    if (qtyErr) { this.orderQtyError.set(qtyErr); return; }
    const dateErr = this.validateExpectedDate(this.newOrder.expectedDeliveryDate);
    if (dateErr) { this.orderDateError.set(dateErr); return; }
    if (!this.newOrder.medicationId) return;
    this.placingOrder.set(true);
    this.svc.placeOrder(this.selectedSupplier!.supplierId!, this.newOrder as SupplyOrder).subscribe({
      next: () => { this.placingOrder.set(false); this.showOrderModal = false; this.selectSupplier(this.selectedSupplier!); this.notify('Order placed successfully', true); },
      error: (e) => { this.placingOrder.set(false); this.notify(e?.error?.message || 'Order failed', false); }
    });
  }

  deliver(orderId: number) {
    this.svc.markDelivered(orderId).subscribe({
      next: () => { this.selectSupplier(this.selectedSupplier!); this.notify('Order delivered — stock updated', true); }
    });
  }

  cancel(orderId: number) {
    this.svc.cancelOrder(orderId).subscribe({
      next: () => { this.selectSupplier(this.selectedSupplier!); this.notify('Order cancelled', true); }
    });
  }

  openDetail(o: SupplyOrder) {
    this.detailOrder = o;
    this.showOrderDetail = true;
  }

  // ─── Toast ───────────────────────────────────────────────────────────────

  notify(msg: string, ok: boolean) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3200);
  }
}
