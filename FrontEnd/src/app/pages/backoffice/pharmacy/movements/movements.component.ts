import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { PharmacyRealtimeService } from '../../../../core/services/pharmacy-realtime.service';
import { StockMovement } from '../../../../core/models/pharmacy.models';

@Component({
  selector: 'app-movements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './movements.component.html'
})
export class MovementsComponent implements OnInit, OnDestroy {
  private svc = inject(PharmacyService);
  private realtime = inject(PharmacyRealtimeService);
  private sub = new Subscription();

  movements = signal<StockMovement[]>([]);
  filtered = signal<StockMovement[]>([]);
  loading = signal(false);

  filterType: 'ALL' | 'EQUIPMENT' | 'DIALYSIS' = 'ALL';
  filterTabs: { key: string; label: string }[] = [
    { key: 'ALL', label: 'All Movements' },
    { key: 'EQUIPMENT', label: 'Equipment' },
    { key: 'DIALYSIS', label: 'Dialysis' }
  ];

  get equipmentCount() { return this.movements().filter(m => m.stockType === 'EQUIPMENT').length; }
  get dialysisCount() { return this.movements().filter(m => m.stockType === 'DIALYSIS').length; }

  ngOnInit() {
    this.load();
    this.sub.add(
      this.realtime.movements$().subscribe(() => this.load())
    );
  }

  ngOnDestroy() { this.sub.unsubscribe(); }

  load() {
    this.loading.set(true);
    this.svc.getStockMovements().subscribe({
      next: d => { this.movements.set(d); this.applyFilter(); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  setFilter(type: 'ALL' | 'EQUIPMENT' | 'DIALYSIS') {
    this.filterType = type;
    this.applyFilter();
  }

  applyFilter() {
    if (this.filterType === 'ALL') {
      this.filtered.set(this.movements());
    } else {
      this.filtered.set(this.movements().filter(m => m.stockType === this.filterType));
    }
  }
}