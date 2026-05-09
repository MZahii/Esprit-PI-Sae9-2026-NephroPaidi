import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PharmacyService } from '../../../../core/services/pharmacy.service';
import { DispensationLog } from '../../../../core/models/pharmacy.models';

@Component({
  selector: 'app-dispensations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dispensations.component.html'
})
export class DispensationsComponent implements OnInit {
  private svc = inject(PharmacyService);

  logs = signal<DispensationLog[]>([]);
  loading = signal(false);
  selectedDate = new Date().toISOString().split('T')[0];

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.svc.getDispensationHistory(this.selectedDate).subscribe({
      next: d => { this.logs.set(d); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  get totalDispensed(): number {
    return this.logs().reduce((sum, l) => sum + l.quantity, 0);
  }

  get distinctMedications(): number {
    return new Set(this.logs().map(l => l.medicationName).filter(Boolean)).size;
  }

  get distinctPatients(): number {
    return new Set(this.logs().map(l => l.patientId).filter(Boolean)).size;
  }

  formatTime(dt: string): string {
    return new Date(dt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  goToday() {
    this.selectedDate = new Date().toISOString().split('T')[0];
    this.load();
  }

  isToday(): boolean {
    return this.selectedDate === new Date().toISOString().split('T')[0];
  }
}
