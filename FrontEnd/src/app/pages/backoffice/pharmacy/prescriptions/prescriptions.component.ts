import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { PharmacyRealtimeService } from '../../../../core/services/pharmacy-realtime.service';
import { Medication, PharmacyPrescription, DispenseRequest } from '../../../../core/models/pharmacy.models';
import { AuthStorageService } from '../../../../core/auth/auth-storage.service';
import { AiService, DoseVerificationRequest, DoseVerificationResponse } from '../../../../core/services/ai.service';
import { ClinicalApiService } from '../../../../core/services/clinical-api.service';

interface DispenseItem { med: any; batchId: number | null; quantity: number; }
interface BatchOption  { batchId: number; label: string; maxQty: number; medicationName: string; }

@Component({
  selector: 'app-prescriptions',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './prescriptions.component.html'
})
export class PrescriptionsComponent implements OnInit, OnDestroy {
  private svc = inject(PharmacyService);
  private auth = inject(AuthStorageService);
  private realtime = inject(PharmacyRealtimeService);
  private aiSvc = inject(AiService);
  private clinicalSvc = inject(ClinicalApiService);
  private sub = new Subscription();

  // Pediatric dose verification
  detailTab: 'info' | 'ai' = 'info';
  doseVerifyForm: Partial<DoseVerificationRequest> = {};
  doseVerifyResult: DoseVerificationResponse | null = null;
  doseVerifying = signal(false);
  doseVerifyError = signal('');
  recommendedDoseCatalogLabel = signal('');
  recommendedDoseSource = signal<'catalog' | 'none'>('none');
  ageSource    = signal<'prescription' | 'metrics' | 'none'>('none');
  weightSource = signal<'prescription' | 'metrics' | 'none'>('none');

  prescriptions = signal<PharmacyPrescription[]>([]);
  filtered = signal<PharmacyPrescription[]>([]);
  loading = signal(false);

  filterStatus: string = 'ALL';
  statusOptions = ['ALL', 'PENDING', 'PROCESSING', 'DISPENSED', 'CANCELLED'];

  selectedPrescription: PharmacyPrescription | null = null;
  showDetail = false;

  // Dispense modal
  showDispenseModal = false;
  dispenseItems: DispenseItem[] = [];
  batchOptions: BatchOption[] = [];
  dispenseStockLoading = signal(false);
  dispenseSubmitting = signal(false);
  dispenseError = '';

  toast = signal('');
  toastOk = signal(true);
  saving = signal(false);

  ngOnInit() {
    this.load();
    this.sub.add(
      this.realtime.prescriptions$().subscribe(() => this.load())
    );
  }

  ngOnDestroy() { this.sub.unsubscribe(); }

  load() {
    this.loading.set(true);
    this.svc.getPrescriptions().subscribe({
      next: d => { this.prescriptions.set(d); this.applyFilter(); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  setFilter(status: string) {
    this.filterStatus = status;
    this.applyFilter();
  }

  applyFilter() {
    if (this.filterStatus === 'ALL') {
      this.filtered.set(this.prescriptions());
    } else {
      this.filtered.set(this.prescriptions().filter(p => p.status === this.filterStatus));
    }
  }

  openDetail(p: PharmacyPrescription) {
    this.selectedPrescription = p;
    this.detailTab = 'info';
    this.doseVerifyResult = null;
    this.doseVerifyError.set('');
    this.showDetail = true;
  }

  verifyPrescription(allergyConfirmed: boolean) {
    if (!this.selectedPrescription?.id) return;
    this.saving.set(true);
    const username = this.auth.getUser()?.username || 'pharmacist';
    this.svc.verifyPrescription(this.selectedPrescription.id, username, allergyConfirmed).subscribe({
      next: (updated) => {
        this.selectedPrescription = updated;
        this.load();
        this.showToast('Prescription verified — moved to Processing');
        this.saving.set(false);
      },
      error: () => { this.showToast('Verification failed', false); this.saving.set(false); }
    });
  }

  updateStatus(status: string) {
    if (!this.selectedPrescription?.id) return;
    this.saving.set(true);
    const username = this.auth.getUser()?.username || 'pharmacist';
    this.svc.updatePrescriptionStatus(this.selectedPrescription.id, status, username).subscribe({
      next: (updated) => {
        this.selectedPrescription = updated;
        this.load();
        this.showToast(`Status updated to ${status}`);
        this.saving.set(false);
      },
      error: () => { this.showToast('Update failed', false); this.saving.set(false); }
    });
  }

  parseMedications(json: string | undefined): any[] {
    if (!json) return [];
    try { return JSON.parse(json); } catch { return []; }
  }

  countByStatus(status: string): number {
    return this.prescriptions().filter(p => p.status === status).length;
  }

  statusPillClass(status: string | undefined): string {
    switch (status) {
      case 'PENDING':    return 'phx-pill-pending';
      case 'PROCESSING': return 'phx-pill-processing';
      case 'DISPENSED':  return 'phx-pill-dispensed';
      case 'CANCELLED':  return 'phx-pill-cancelled';
      default:           return 'phx-pill-pending';
    }
  }

  statusIcon(status: string | undefined): string {
    switch (status) {
      case 'PENDING':    return 'bi-hourglass-split';
      case 'PROCESSING': return 'bi-gear-wide-connected';
      case 'DISPENSED':  return 'bi-check-circle-fill';
      case 'CANCELLED':  return 'bi-x-circle-fill';
      default:           return 'bi-circle';
    }
  }

  urgencyPillClass(urgency: string | undefined): string {
    switch (urgency) {
      case 'STAT':   return 'phx-pill-stat';
      case 'URGENT': return 'phx-pill-urgent';
      default:       return 'phx-pill-routine';
    }
  }

  urgencyIcon(urgency: string | undefined): string {
    switch (urgency) {
      case 'STAT':   return 'bi-lightning-fill';
      case 'URGENT': return 'bi-exclamation-triangle-fill';
      default:       return 'bi-clock';
    }
  }

  batchOptionsForMed(medName: string): BatchOption[] {
    if (!medName) return this.batchOptions;
    const key = medName.trim().toLowerCase();
    const filtered = this.batchOptions.filter(b => b.medicationName.toLowerCase().includes(key) || key.includes(b.medicationName.toLowerCase()));
    return filtered.length > 0 ? filtered : this.batchOptions;
  }

  openDispenseModal() {
    const meds = this.parseMedications(this.selectedPrescription?.medicationsJson);
    if (meds.length === 0) { this.showToast('No medications in this prescription', false); return; }
    this.dispenseItems = meds.map(med => ({ med, batchId: null, quantity: 1 }));
    this.dispenseError = '';
    this.batchOptions = [];
    this.dispenseStockLoading.set(true);
    this.showDispenseModal = true;

    forkJoin([this.svc.getMedications(), this.svc.getAllStock()]).subscribe({
      next: ([medications, stocks]) => {
        const medNameMap: Record<number, string> = {};
        medications.forEach(m => { if (m.medicationId) medNameMap[m.medicationId] = m.name; });
        const stockQtyMap: Record<number, number> = {};
        stocks.forEach(s => { stockQtyMap[s.batchId] = s.quantityAvailable; });

        const medsWithId = medications.filter(m => m.medicationId);
        if (medsWithId.length === 0) { this.dispenseStockLoading.set(false); return; }

        forkJoin(medsWithId.map(m => this.svc.getBatches(m.medicationId!))).subscribe({
          next: (batchArrays) => {
            const options: BatchOption[] = [];
            batchArrays.forEach((batches, i) => {
              const medName = medNameMap[medsWithId[i].medicationId!] || `Med #${medsWithId[i].medicationId}`;
              batches.forEach(b => {
                if (b.batchId == null || b.expired) return;
                const qty = stockQtyMap[b.batchId] ?? 0;
                if (qty > 0) options.push({ batchId: b.batchId, label: `${b.batchNumber} (${qty} avail.)`, maxQty: qty, medicationName: medName });
              });
            });
            this.batchOptions = options;
            this.dispenseStockLoading.set(false);
          },
          error: () => this.dispenseStockLoading.set(false)
        });
      },
      error: () => this.dispenseStockLoading.set(false)
    });
  }

  submitDispense() {
    if (!this.selectedPrescription?.id) return;
    if (this.dispenseItems.some(i => !i.batchId || i.quantity < 1)) {
      this.dispenseError = 'Select a batch and enter a quantity for each medication.'; return;
    }
    const username = this.auth.getUser()?.username || 'pharmacist';
    this.dispenseSubmitting.set(true);
    this.dispenseError = '';

    const calls = this.dispenseItems.map(item =>
      this.svc.dispense({
        batchId: item.batchId!,
        quantity: item.quantity,
        patientId:  this.selectedPrescription!.patientId,
        patientName: this.selectedPrescription!.patientName,
        prescriptionId: this.selectedPrescription!.id,
        dispensedBy: username
      } as DispenseRequest)
    );

    forkJoin(calls).subscribe({
      next: () => {
        this.svc.updatePrescriptionStatus(this.selectedPrescription!.id!, 'DISPENSED', username).subscribe({
          next: (updated) => {
            this.selectedPrescription = updated;
            this.showDispenseModal = false;
            this.load();
            this.showToast('All medications dispensed — stock updated');
            this.dispenseSubmitting.set(false);
          },
          error: () => {
            this.dispenseError = 'Stock deducted but could not update prescription status. Refresh the page.';
            this.dispenseSubmitting.set(false);
          }
        });
      },
      error: (e: any) => {
        this.dispenseError = e?.error?.message || 'Failed to dispense — check stock availability.';
        this.dispenseSubmitting.set(false);
      }
    });
  }

  openDoseVerify() {
    const p       = this.selectedPrescription;
    const meds    = this.parseMedications(p?.medicationsJson);
    const med     = meds[0] ?? {};
    const medName = (med.name || med.medicationName || '').trim().toLowerCase();

    this.doseVerifyForm = {
      age_years:           p?.patientAge    ?? undefined,
      weight_kg:           p?.patientWeight ?? undefined,
      prescribed_dose_mg:  this.parseMg(med.dosage ?? med.prescribedDoseMg),
      recommended_dose_mg: undefined,
      frequency_per_day:   this.parseFreq(med.frequency ?? med.frequencyPerDay),
    };
    this.doseVerifyResult = null;
    this.doseVerifyError.set('');
    this.recommendedDoseCatalogLabel.set('');
    this.recommendedDoseSource.set('none');
    this.ageSource.set(p?.patientAge    != null ? 'prescription' : 'none');
    this.weightSource.set(p?.patientWeight != null ? 'prescription' : 'none');
    this.detailTab = 'ai';
    this.doseVerifying.set(true);

    const needsMetrics = (!this.doseVerifyForm.age_years || !this.doseVerifyForm.weight_kg) && !!p?.consultationId;

    if (needsMetrics) {
      forkJoin({
        catalog: this.svc.getMedications(),
        metrics: this.clinicalSvc.getConsultationMetrics(p!.consultationId!)
      }).subscribe({
        next: ({ catalog, metrics }) => {
          this.applyMedicationCatalog(catalog, medName);
          if (metrics?.ageYears) { this.doseVerifyForm.age_years  = metrics.ageYears;  if (this.ageSource()    === 'none') this.ageSource.set('metrics'); }
          if (metrics?.weightKg) { this.doseVerifyForm.weight_kg  = metrics.weightKg;  if (this.weightSource() === 'none') this.weightSource.set('metrics'); }
          this.doseVerifying.set(false);
          this.autoSubmitIfReady();
        },
        error: () => this.doseVerifying.set(false)
      });
    } else {
      this.svc.getMedications().subscribe({
        next: catalog => {
          this.applyMedicationCatalog(catalog, medName);
          this.doseVerifying.set(false);
          this.autoSubmitIfReady();
        },
        error: () => this.doseVerifying.set(false)
      });
    }
  }

  private applyMedicationCatalog(medications: Medication[], medName: string) {
    if (!medName) { this.recommendedDoseSource.set('none'); return; }
    const match = medications.find(m => {
      const name = (m.name || '').trim().toLowerCase();
      return name.includes(medName) || medName.includes(name);
    });
    if (!match) { this.recommendedDoseSource.set('none'); return; }

    const raw = match.pediatricDosage || match.standardDosage || '';
    const parsed = this.parseMg(raw);
    if (parsed != null) {
      this.doseVerifyForm.recommended_dose_mg = parsed;
      this.recommendedDoseCatalogLabel.set(raw);
      this.recommendedDoseSource.set('catalog');
    } else {
      this.recommendedDoseCatalogLabel.set(raw);
      this.recommendedDoseSource.set('none');
    }
  }

  private autoSubmitIfReady() {
    const f = this.doseVerifyForm;
    if (f.age_years && f.weight_kg && f.prescribed_dose_mg && f.recommended_dose_mg && f.frequency_per_day) {
      this.submitDoseVerify();
    }
  }

  private parseMg(val: any): number | undefined {
    if (val == null) return undefined;
    if (typeof val === 'number') return val;
    const n = parseFloat(String(val).replace(/[^\d.]/g, ''));
    return isNaN(n) ? undefined : n;
  }

  private parseFreq(val: any): number | undefined {
    if (val == null) return undefined;
    if (typeof val === 'number') return val;
    const map: Record<string, number> = { qd:1, od:1, bid:2, tid:3, qid:4, q6h:4, q8h:3, q12h:2 };
    const lower = String(val).toLowerCase().trim();
    if (map[lower]) return map[lower];
    const n = parseInt(lower.replace(/[^\d]/g, ''), 10);
    return isNaN(n) ? undefined : n;
  }

  submitDoseVerify() {
    const f = this.doseVerifyForm;
    if (!f.age_years || !f.weight_kg || !f.prescribed_dose_mg || !f.recommended_dose_mg || !f.frequency_per_day) {
      this.doseVerifyError.set('All fields are required.'); return;
    }
    this.doseVerifyError.set('');
    this.doseVerifying.set(true);
    this.aiSvc.verifyPediatricDose(f as DoseVerificationRequest).subscribe({
      next: r => { this.doseVerifyResult = r; this.doseVerifying.set(false); },
      error: e => { this.doseVerifyError.set(e?.error?.detail || 'Verification failed'); this.doseVerifying.set(false); }
    });
  }

  doseResultClass(prediction: string): string {
    switch (prediction) {
      case 'SAFE':      return 'text-success';
      case 'OVERDOSE':  return 'text-danger';
      case 'UNDERDOSE': return 'text-warning';
      default:          return '';
    }
  }

  doseResultIcon(prediction: string): string {
    switch (prediction) {
      case 'SAFE':      return 'bi-check-circle-fill';
      case 'OVERDOSE':  return 'bi-exclamation-triangle-fill';
      case 'UNDERDOSE': return 'bi-arrow-down-circle-fill';
      default:          return 'bi-circle';
    }
  }

  private showToast(msg: string, ok = true) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3000);
  }
}