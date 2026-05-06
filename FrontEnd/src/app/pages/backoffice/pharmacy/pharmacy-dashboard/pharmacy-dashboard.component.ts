import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { PharmacyRealtimeService } from '../../../../core/services/pharmacy-realtime.service';
import {
  Medication, EquipmentItem, DialysisItem,
  PharmacyPrescription, StockMovement, ReorderAlert, DispensationLog
} from '../../../../core/models/pharmacy.models';

@Component({
  selector: 'app-pharmacy-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pharmacy-dashboard.component.html'
})
export class PharmacyDashboardComponent implements OnInit, OnDestroy {
  private svc = inject(PharmacyService);
  private realtime = inject(PharmacyRealtimeService);
  private sub = new Subscription();

  realtimeConnected = signal(false);

  medications      = signal<Medication[]>([]);
  equipmentItems   = signal<EquipmentItem[]>([]);
  dialysisItems    = signal<DialysisItem[]>([]);
  prescriptions    = signal<PharmacyPrescription[]>([]);
  movements        = signal<StockMovement[]>([]);
  reorderAlerts    = signal<ReorderAlert[]>([]);
  todayDispensations = signal<DispensationLog[]>([]);

  loadingMeds      = signal(true);
  loadingEquip     = signal(true);
  loadingDial      = signal(true);
  loadingPrescr    = signal(true);
  loadingMovements = signal(true);
  loadingAlerts    = signal(true);
  loadingDispens   = signal(true);

  today = new Date().toISOString().split('T')[0];

  ngOnInit() {
    this.loadAll();
    this.sub.add(
      this.realtime.all$().subscribe({
        next: event => {
          this.realtimeConnected.set(true);
          if (event.type === 'PRESCRIPTION_CREATED' || event.type === 'PRESCRIPTION_UPDATED') {
            this.svc.getPrescriptions().subscribe({ next: d => this.prescriptions.set(d) });
          }
          if (event.type === 'MOVEMENT_RECORDED') {
            this.svc.getStockMovements().subscribe({ next: d => this.movements.set(d) });
            this.svc.getEquipmentItems().subscribe({ next: d => this.equipmentItems.set(d) });
            this.svc.getDialysisItems().subscribe({ next: d => this.dialysisItems.set(d) });
          }
          if (event.type === 'ALERT_UPDATE') {
            this.svc.getReorderNeeded().subscribe({ next: d => this.reorderAlerts.set(d) });
          }
        },
        error: () => this.realtimeConnected.set(false)
      })
    );
  }

  ngOnDestroy() { this.sub.unsubscribe(); }

  loadAll() {
    this.svc.getMedications().subscribe({ next: d => { this.medications.set(d); this.loadingMeds.set(false); }, error: () => this.loadingMeds.set(false) });
    this.svc.getEquipmentItems().subscribe({ next: d => { this.equipmentItems.set(d); this.loadingEquip.set(false); }, error: () => this.loadingEquip.set(false) });
    this.svc.getDialysisItems().subscribe({ next: d => { this.dialysisItems.set(d); this.loadingDial.set(false); }, error: () => this.loadingDial.set(false) });
    this.svc.getPrescriptions().subscribe({ next: d => { this.prescriptions.set(d); this.loadingPrescr.set(false); }, error: () => this.loadingPrescr.set(false) });
    this.svc.getStockMovements().subscribe({ next: d => { this.movements.set(d); this.loadingMovements.set(false); }, error: () => this.loadingMovements.set(false) });
    this.svc.getReorderNeeded().subscribe({ next: d => { this.reorderAlerts.set(d); this.loadingAlerts.set(false); }, error: () => this.loadingAlerts.set(false) });
    this.svc.getDispensationHistory(this.today).subscribe({ next: d => { this.todayDispensations.set(d); this.loadingDispens.set(false); }, error: () => this.loadingDispens.set(false) });
  }

  get pendingPrescriptions(): PharmacyPrescription[] {
    return this.prescriptions().filter(p => p.status === 'PENDING');
  }

  get recentMovements(): StockMovement[] {
    return this.movements().slice(0, 5);
  }

  get totalAlerts(): number {
    return this.reorderAlerts().length
      + this.equipmentItems().filter(i => i.lowStock).length
      + this.dialysisItems().filter(i => i.lowStock).length;
  }

  get lowEquipment(): EquipmentItem[] {
    return this.equipmentItems().filter(i => i.lowStock);
  }

  get lowDialysis(): DialysisItem[] {
    return this.dialysisItems().filter(i => i.lowStock);
  }

  get isLoading(): boolean {
    return this.loadingMeds() || this.loadingEquip() || this.loadingDial() || this.loadingPrescr();
  }

  stockPillClass(item: EquipmentItem | DialysisItem): string {
    if ((item.currentStock ?? 0) === 0) return 'phx-pill-danger';
    if (item.lowStock) return 'phx-pill-warning';
    return 'phx-pill-success';
  }
}
