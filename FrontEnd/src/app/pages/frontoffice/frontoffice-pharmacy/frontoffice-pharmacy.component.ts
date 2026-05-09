import { Component, OnInit, AfterViewInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PharmacyService } from '../../../core/services/pharmacy.service';
import { Medication, Stock, Batch } from '../../../core/models/pharmacy.models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-frontoffice-pharmacy',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './frontoffice-pharmacy.component.html',
  styleUrl: './frontoffice-pharmacy.component.scss'
})
export class FrontofficePharmacyComponent implements OnInit, AfterViewInit {
  private svc = inject(PharmacyService);

  medications = signal<Medication[]>([]);
  stockMap = signal<Record<number, number>>({}); // batchId -> qty
  batchMap = signal<Record<number, number[]>>({}); // medicationId -> batchIds
  loading = signal(true);
  searchQuery = '';
  selectedForm = '';
  selectedAvailability: 'all' | 'available' | 'unavailable' = 'all';
  sortBy: 'az' | 'za' | '' = '';

  ngOnInit() { this.loadData(); }

  ngAfterViewInit() {
    const observer = new IntersectionObserver(
      entries => entries.forEach(e => {
        if (e.isIntersecting) {
          e.target.classList.add('revealed');
          observer.unobserve(e.target);
        }
      }),
      { threshold: 0.12 }
    );
    document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
  }

  loadData() {
    this.loading.set(true);
    this.svc.getMedications().subscribe({
      next: medications => {
        this.medications.set(medications);
        if (medications.length === 0) { this.loading.set(false); return; }

        const batchCalls = medications.map(m => this.svc.getBatches(m.medicationId!));
        forkJoin(batchCalls).subscribe({
          next: batchArrays => {
            const bMap: Record<number, number[]> = {};
            batchArrays.forEach((batches, i) => {
              const medId = medications[i].medicationId!;
              bMap[medId] = batches.filter(b => !b.expired).map(b => b.batchId!);
            });
            this.batchMap.set(bMap);

            this.svc.getAllStock().subscribe({
              next: stocks => {
                const sMap: Record<number, number> = {};
                stocks.forEach(s => { sMap[s.batchId] = s.quantityAvailable; });
                this.stockMap.set(sMap);
                this.loading.set(false);
              },
              error: () => this.loading.set(false)
            });
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => this.loading.set(false)
    });
  }

  get totalAvailable(): number {
    return this.medications().filter(m => this.isAvailable(m.medicationId!)).length;
  }

  get uniqueForms(): string[] {
    return [...new Set(this.medications().map(m => m.form).filter(Boolean))];
  }

  get filtered(): Medication[] {
    const q = this.searchQuery.toLowerCase().trim();
    let list = this.medications().filter(m => {
      const matchSearch = !q || m.name.toLowerCase().includes(q) || m.form?.toLowerCase().includes(q);
      const matchForm   = !this.selectedForm || m.form === this.selectedForm;
      const avail       = this.isAvailable(m.medicationId!);
      const matchAvail  = this.selectedAvailability === 'all'
        || (this.selectedAvailability === 'available'   && avail)
        || (this.selectedAvailability === 'unavailable' && !avail);
      return matchSearch && matchForm && matchAvail;
    });
    if (this.sortBy === 'az') list = [...list].sort((a, b) => a.name.localeCompare(b.name));
    if (this.sortBy === 'za') list = [...list].sort((a, b) => b.name.localeCompare(a.name));
    return list;
  }

  get hasActiveFilters(): boolean {
    return !!this.searchQuery || !!this.selectedForm || this.selectedAvailability !== 'all' || !!this.sortBy;
  }

  clearFilters() {
    this.searchQuery = '';
    this.selectedForm = '';
    this.selectedAvailability = 'all';
    this.sortBy = '';
  }

  isAvailable(medId: number): boolean {
    const batchIds = this.batchMap()[medId] ?? [];
    return batchIds.some(bid => (this.stockMap()[bid] ?? 0) > 0);
  }

  formIcon(form: string): string {
    const icons: Record<string, string> = {
      tablet: 'bi-grid-3x3-gap-fill',
      capsule: 'bi-capsule',
      syrup: 'bi-droplet-fill',
      injection: 'bi-activity',
      drops: 'bi-droplet-half',
      cream: 'bi-circle-half',
      powder: 'bi-snow2'
    };
    return icons[form?.toLowerCase()] ?? 'bi-box2-fill';
  }
}
