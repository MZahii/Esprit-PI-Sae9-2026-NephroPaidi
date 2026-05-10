import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { ReorderAlert, EquipmentItem, DialysisItem } from '../../../../core/models/pharmacy.models';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alerts.component.html'
})
export class AlertsComponent implements OnInit {
  private svc = inject(PharmacyService);

  medicationAlerts = signal<ReorderAlert[]>([]);
  equipmentAlerts = signal<EquipmentItem[]>([]);
  dialysisAlerts = signal<DialysisItem[]>([]);
  loading = signal(false);

  toast = signal('');
  toastOk = signal(true);
  sendingEmail = signal(false);
  alertEmail = '';
  showEmailModal = false;

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    let done = 0;
    const check = () => { if (++done === 3) this.loading.set(false); };

    this.svc.getReorderNeeded().subscribe({ next: d => { this.medicationAlerts.set(d); check(); }, error: check });
    this.svc.getLowEquipmentStock().subscribe({ next: d => { this.equipmentAlerts.set(d); check(); }, error: check });
    this.svc.getLowDialysisStock().subscribe({ next: d => { this.dialysisAlerts.set(d); check(); }, error: check });
  }

  get totalAlerts(): number {
    return this.medicationAlerts().length + this.equipmentAlerts().length + this.dialysisAlerts().length;
  }

  sendEmail() {
    if (!this.alertEmail.trim()) return;
    this.sendingEmail.set(true);
    this.svc.sendLowStockAlert(this.alertEmail).subscribe({
      next: () => { this.showEmailModal = false; this.showToast('Alert email sent'); this.sendingEmail.set(false); },
      error: () => { this.showToast('Failed to send email', false); this.sendingEmail.set(false); }
    });
  }

  private showToast(msg: string, ok = true) {
    this.toast.set(msg); this.toastOk.set(ok);
    setTimeout(() => this.toast.set(''), 3000);
  }
}