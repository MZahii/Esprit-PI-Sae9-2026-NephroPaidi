import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { Stock, DispenseRequest, Batch, Medication, SmartDispenseRequest, SmartDispenseResponse, TransferStockRequest, TransferStockResponse } from '../../../../core/models/pharmacy.models';
import { hasAnyRole } from '../../../../core/auth/keycloak.service';

@Component({
  selector: 'app-stock',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './stock.component.html'
})
export class StockComponent implements OnInit {
  private svc = inject(PharmacyService);
  private readonly canManagePharmacy = hasAnyRole(['PHARMACIST', 'ADMIN']);

  stocks = signal<Stock[]>([]);
  lowStock = signal<Stock[]>([]);
  outOfStock = signal<Stock[]>([]);
  expiredBatches = signal<Batch[]>([]);
  activeTab = signal<'all' | 'low' | 'out' | 'expired'>('all');
  loading = signal(false);

  batchMedicationMap: Record<number, string> = {};
  batchNumberMap:     Record<number, string> = {};

  showDispense = false;
  dispenseForm: Partial<DispenseRequest> = {};

  // Init modal state
  showInit = false;
  initMedications = signal<Medication[]>([]);
  initSelectedMedId: number | null = null;
  initBatches = signal<Batch[]>([]);
  initBatchesLoading = signal(false);
  initSelectedBatch: Batch | null = null;
  initQty = 1;

  showAdjust = false;
  adjustBatchId = 0;
  adjustDelta = 0;
  adjustReason = '';

  // Smart Dispense
  showSmartDispense = false;
  smartDispenseForm: Partial<SmartDispenseRequest> = {};
  smartDispenseResult: SmartDispenseResponse | null = null;
  smartDispenseError = signal('');
  smartMedications = signal<Medication[]>([]);

  // Transfer
  showTransfer = false;
  transferForm: Partial<TransferStockRequest> = {};
  transferResult: TransferStockResponse | null = null;
  transferError = signal('');

  alertOutDismissed = false;
  alertLowDismissed = false;

  dispenseError = signal('');
  initError = signal('');
  adjustError = signal('');

  // Button loading states
  dispensing       = signal(false);
  initializingStock = signal(false);
  adjusting        = signal(false);
  smartDispensing  = signal(false);
  transferring     = signal(false);

  toast = signal('');
  toastOk = signal(true);

  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.loading.set(true);
    this.alertOutDismissed = false;
    this.alertLowDismissed = false;
    this.svc.getAllStock().subscribe({ next: d => { this.stocks.set(d); this.loading.set(false); } });
    this.svc.getLowStock(10).subscribe({ next: d => this.lowStock.set(d) });
    this.svc.getOutOfStock().subscribe({ next: d => this.outOfStock.set(d) });
    if (this.canManagePharmacy) {
      this.svc.getExpiredBatches().subscribe({ next: d => this.expiredBatches.set(d) });
    } else {
      this.expiredBatches.set([]);
    }
    this.loadBatchMedicationMap();
  }

  loadBatchMedicationMap() {
    this.svc.getMedications().subscribe({ next: medications => {
      if (medications.length === 0) return;
      const calls = medications.map(m => this.svc.getBatches(m.medicationId!));
      forkJoin(calls).subscribe({ next: batchArrays => {
        const medMap:    Record<number, string> = {};
        const numMap:    Record<number, string> = {};
        batchArrays.forEach((batches, i) => {
          batches.forEach(b => {
            if (b.batchId != null) {
              medMap[b.batchId] = medications[i].name;
              numMap[b.batchId] = b.batchNumber;
            }
          });
        });
        this.batchMedicationMap = medMap;
        this.batchNumberMap     = numMap;
      }});
    }});
  }

  /** Label for a stock entry in a dropdown */
  stockLabel(s: Stock): string {
    const med = this.batchMedicationMap[s.batchId] || `Batch #${s.batchId}`;
    const num = this.batchNumberMap[s.batchId]     ? ` — ${this.batchNumberMap[s.batchId]}` : '';
    return `${med}${num} (${s.quantityAvailable} available)`;
  }

  get sourceAvailableQty(): number {
    const src = this.stocks().find(s => s.batchId === this.transferForm.sourceBatchId);
    return src ? src.quantityAvailable : 0;
  }

  openInit() {
    this.initSelectedMedId = null;
    this.initSelectedBatch = null;
    this.initBatches.set([]);
    this.initQty = 1;
    this.initError.set('');
    this.showInit = true;
    this.svc.getMedications().subscribe({ next: d => this.initMedications.set(d) });
  }

  onInitMedChange() {
    this.initSelectedBatch = null;
    this.initBatches.set([]);
    if (!this.initSelectedMedId) return;
    this.initBatchesLoading.set(true);
    this.svc.getBatches(this.initSelectedMedId).subscribe({
      next: d => { this.initBatches.set(d); this.initBatchesLoading.set(false); },
      error: () => this.initBatchesLoading.set(false)
    });
  }

  get currentList(): Stock[] {
    if (this.activeTab() === 'low') return this.lowStock();
    if (this.activeTab() === 'out') return this.outOfStock();
    return this.stocks();
  }

  stockLevel(s: Stock): 'ok' | 'low' | 'out' {
    if (s.quantityAvailable === 0) return 'out';
    if (s.quantityAvailable <= 10) return 'low';
    return 'ok';
  }

  dispense() {
    if (!this.dispenseForm.quantity || this.dispenseForm.quantity < 1) {
      this.dispenseError.set('Quantity must be at least 1.'); return;
    }
    if (!Number.isInteger(this.dispenseForm.quantity)) {
      this.dispenseError.set('Quantity must be a whole number.'); return;
    }
    this.dispenseError.set('');
    this.dispensing.set(true);
    this.svc.dispense(this.dispenseForm as DispenseRequest).subscribe({
      next: () => { this.dispensing.set(false); this.showDispense = false; this.loadAll(); this.notify('Dispensed successfully', true); },
      error: (e) => { this.dispensing.set(false); this.notify(e?.error?.message || 'Dispense failed', false); }
    });
  }

  initStock() {
    if (!this.initSelectedBatch || !this.initQty) { this.initError.set('Please select a batch and enter a quantity.'); return; }
    if (this.initQty < 1) { this.initError.set('Quantity must be at least 1.'); return; }
    if (!Number.isInteger(this.initQty)) { this.initError.set('Quantity must be a whole number.'); return; }
    if (this.initQty > this.initSelectedBatch.quantity) {
      this.initError.set(`Quantity cannot exceed batch size (${this.initSelectedBatch.quantity}).`); return;
    }
    this.initError.set('');
    this.initializingStock.set(true);
    this.svc.initializeStock(this.initSelectedBatch.batchId!, this.initQty).subscribe({
      next: () => { this.initializingStock.set(false); this.showInit = false; this.loadAll(); this.notify('Stock initialized', true); },
      error: (e) => { this.initializingStock.set(false); this.notify(e?.error?.message || 'Already initialized or error', false); }
    });
  }

  adjust() {
    if (this.adjustDelta === 0) { this.adjustError.set('Delta cannot be zero.'); return; }
    if (!this.adjustReason || !this.adjustReason.trim()) { this.adjustError.set('Reason is required.'); return; }
    this.adjustError.set('');
    this.adjusting.set(true);
    this.svc.adjustStock(this.adjustBatchId, this.adjustDelta, this.adjustReason).subscribe({
      next: () => { this.adjusting.set(false); this.showAdjust = false; this.loadAll(); this.notify('Stock adjusted', true); },
      error: () => { this.adjusting.set(false); this.notify('Adjustment failed', false); }
    });
  }

  openSmartDispense() {
    this.smartDispenseForm = { quantity: 1 };
    this.smartDispenseResult = null;
    this.smartDispenseError.set('');
    this.showSmartDispense = true;
    this.svc.getMedications().subscribe({ next: d => this.smartMedications.set(d) });
  }

  submitSmartDispense() {
    if (!this.smartDispenseForm.medicationId) { this.smartDispenseError.set('Please select a medication.'); return; }
    if (!this.smartDispenseForm.quantity || this.smartDispenseForm.quantity < 1) { this.smartDispenseError.set('Quantity must be at least 1.'); return; }
    this.smartDispenseError.set('');
    this.smartDispensing.set(true);
    this.svc.smartDispense(this.smartDispenseForm as SmartDispenseRequest).subscribe({
      next: r => { this.smartDispensing.set(false); this.smartDispenseResult = r; this.loadAll(); this.notify('Smart dispense successful (FEFO)', true); },
      error: e => { this.smartDispensing.set(false); this.smartDispenseError.set(e?.error?.message || 'Smart dispense failed'); }
    });
  }

  openTransfer(sourceBatchId?: number) {
    this.transferForm = { sourceBatchId: sourceBatchId ?? undefined, quantity: 1, reason: '' };
    this.transferResult = null;
    this.transferError.set('');
    this.showTransfer = true;
  }

  submitTransfer() {
    if (!this.transferForm.sourceBatchId) { this.transferError.set('Source batch ID is required.'); return; }
    if (!this.transferForm.targetBatchId) { this.transferError.set('Target batch ID is required.'); return; }
    if (this.transferForm.sourceBatchId === this.transferForm.targetBatchId) { this.transferError.set('Source and target must be different batches.'); return; }
    if (!this.transferForm.quantity || this.transferForm.quantity < 1) { this.transferError.set('Quantity must be at least 1.'); return; }
    if (!this.transferForm.reason?.trim()) { this.transferError.set('Reason is required.'); return; }
    this.transferError.set('');
    this.transferring.set(true);
    this.svc.transferStock(this.transferForm as TransferStockRequest).subscribe({
      next: r => { this.transferring.set(false); this.transferResult = r; this.loadAll(); this.notify('Stock transferred successfully', true); },
      error: e => { this.transferring.set(false); this.transferError.set(e?.error?.message || 'Transfer failed'); }
    });
  }

  notify(msg: string, ok: boolean) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3500);
  }
}