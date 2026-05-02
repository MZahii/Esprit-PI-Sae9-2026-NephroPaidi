import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { Medication, Batch, ReorderAlert } from '../../../../core/models/pharmacy.models';
import { hasAnyRole } from '../../../../core/auth/keycloak.service';

@Component({
  selector: 'app-medications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medications.component.html'
})
export class MedicationsComponent implements OnInit {
  private svc = inject(PharmacyService);
  private readonly canManagePharmacy = hasAnyRole(['PHARMACIST', 'ADMIN']);

  medications = signal<Medication[]>([]);
  reorderAlerts = signal<ReorderAlert[]>([]);
  loading = signal(false);
  showReorderPanel = false;

  showModal = false;
  editMode = false;
  currentMed: Partial<Medication> = {};

  showBatchModal = false;
  selectedMed: Medication | null = null;
  batches = signal<Batch[]>([]);
  newBatch: Partial<Batch> = {};
  batchError = signal('');

  nameError   = signal('');
  saving      = signal(false);
  addingBatch = signal(false);
  sendingAlert = signal(false);
  alertEmail   = '';
  showAlertModal = false;
  toast   = signal('');
  toastOk = signal(true);

  today = new Date().toISOString().split('T')[0];

  ngOnInit() {
    this.load();
    if (this.canManagePharmacy) {
      this.loadReorderAlerts();
    }
  }

  load() {
    this.loading.set(true);
    this.svc.getMedications().subscribe({
      next: d => { this.medications.set(d); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  loadReorderAlerts() {
    if (!this.canManagePharmacy) {
      this.reorderAlerts.set([]);
      return;
    }
    this.svc.getReorderNeeded().subscribe({
      next: d => this.reorderAlerts.set(d),
      error: () => {}
    });
  }

  validateName(name: string | undefined): string {
    if (!name || !name.trim()) return 'Medication name is required.';
    if (!/^[a-zA-ZÀ-ÿ\s]+$/.test(name.trim())) return 'Name must contain letters only (no numbers or special characters).';
    return '';
  }

  onNameChange(value: string) {
    this.currentMed.name = value;
    this.nameError.set(this.validateName(value));
  }

  openAdd() {
    this.editMode = false;
    this.currentMed = { name: '', form: 'tablet', pediatricDosage: '', minimumStock: null };
    this.nameError.set('');
    this.showModal = true;
  }

  openEdit(med: Medication) {
    this.editMode = true;
    this.currentMed = { ...med };
    this.nameError.set('');
    this.showModal = true;
  }

  save() {
    const err = this.validateName(this.currentMed.name);
    if (err) { this.nameError.set(err); return; }

    const obs = this.editMode
      ? this.svc.updateMedication(this.currentMed.medicationId!, this.currentMed as Medication)
      : this.svc.createMedication(this.currentMed as Medication);

    this.saving.set(true);
    obs.subscribe({
      next: () => { this.saving.set(false); this.showModal = false; this.load(); this.loadReorderAlerts(); this.notify(`Medication ${this.editMode ? 'updated' : 'created'}`, true); },
      error: () => { this.saving.set(false); this.notify('Operation failed', false); }
    });
  }

  delete(med: Medication) {
    if (!confirm(`Delete "${med.name}"?`)) return;
    this.svc.deleteMedication(med.medicationId!).subscribe({
      next: () => { this.load(); this.notify('Deleted', true); },
      error: () => this.notify('Delete failed', false)
    });
  }

  openBatches(med: Medication) {
    this.selectedMed = med;
    this.resetBatchForm();
    this.showBatchModal = true;
    this.loadBatches();
  }

  resetBatchForm() {
    this.newBatch = { batchNumber: '', quantity: 1, manufactureDate: '', expirationDate: '' };
    this.batchError.set('');
  }

  loadBatches() {
    this.svc.getBatches(this.selectedMed!.medicationId!).subscribe({
      next: d => this.batches.set(d)
    });
  }

  validateBatch(): string {
    if (!this.newBatch.batchNumber?.trim()) return 'Batch number is required.';
    if (!this.newBatch.quantity || this.newBatch.quantity < 1) return 'Quantity must be at least 1.';
    if (!this.newBatch.expirationDate) return 'Expiration date is required.';
    if (this.newBatch.expirationDate <= this.today) return 'Expiration date must be in the future.';
    if (this.newBatch.manufactureDate && this.newBatch.manufactureDate >= this.newBatch.expirationDate) {
      return 'Manufacture date must be before expiration date.';
    }
    return '';
  }

  addBatch() {
    const err = this.validateBatch();
    if (err) { this.batchError.set(err); return; }
    this.batchError.set('');
    this.addingBatch.set(true);
    this.svc.addBatch(this.selectedMed!.medicationId!, this.newBatch as Batch).subscribe({
      next: () => { this.addingBatch.set(false); this.resetBatchForm(); this.loadBatches(); this.notify('Batch added', true); },
      error: () => { this.addingBatch.set(false); this.notify('Failed to add batch', false); }
    });
  }

  daysUntilExpiry(date: string): number {
    return Math.ceil((new Date(date).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
  }

  expiryBadgeClass(b: Batch): string {
    if (b.expired) return 'bg-danger';
    const days = this.daysUntilExpiry(b.expirationDate);
    if (days <= 30) return 'bg-warning text-dark';
    return 'bg-success';
  }

  expiryLabel(b: Batch): string {
    if (b.expired) return 'Expired';
    const days = this.daysUntilExpiry(b.expirationDate);
    if (days <= 30) return `Expires in ${days}d`;
    return 'Valid';
  }

  openAlertModal() {
    this.alertEmail = '';
    this.showAlertModal = true;
  }

  sendAlert() {
    if (!this.alertEmail || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.alertEmail)) return;
    this.sendingAlert.set(true);
    this.svc.sendLowStockAlert(this.alertEmail).subscribe({
      next: () => { this.sendingAlert.set(false); this.showAlertModal = false; this.notify('Low-stock alert email sent!', true); },
      error: (e) => {
        this.sendingAlert.set(false);
        if (e?.status === 204) { this.showAlertModal = false; this.notify('All stocks are above minimum — no alert needed.', true); }
        else this.notify('Failed to send alert email.', false);
      }
    });
  }

  notify(msg: string, ok: boolean) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3000);
  }
}