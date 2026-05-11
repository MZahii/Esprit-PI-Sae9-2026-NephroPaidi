import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ConsultationSummaryViewModel } from '../consultation-workspace.models';

@Component({
  selector: 'app-encounter-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="encounter-header">
      <div>
        <span class="eyebrow">Doctor encounter</span>
        <h3>{{ summary.patientName }}</h3>
        <div class="subtitle">
          {{ summary.subtitle }}
          <span *ngIf="summary.scheduledAt">• {{ summary.scheduledAt | date:'medium' }}</span>
        </div>
      </div>
      <div class="meta">
        <span class="badge" [ngClass]="summary.statusClass">{{ summary.statusLabel }}</span>
        <span class="badge" [ngClass]="summary.completenessClass">{{ summary.completenessScore }}/100</span>
        <span class="risk">{{ summary.renalRiskLabel }}</span>
      </div>
      <div class="actions">
        <button class="btn btn-light" type="button" (click)="back.emit()">Back</button>
        <button class="btn btn-light" type="button" (click)="save.emit()" [disabled]="saving">Save Progress</button>
        <button class="btn btn-outline-primary" type="button" (click)="summaryRequested.emit()">Generate Summary</button>
        <button class="btn btn-success" type="button" (click)="complete.emit()" [disabled]="saving || completed || !canComplete">Conclude Consultation</button>
      </div>
    </section>
  `,
  styles: [`
    .encounter-header { display:grid; gap:1rem; padding:1.2rem 1.3rem; border:1px solid #dbe6f0; border-radius:24px; background:linear-gradient(135deg,#f9fcff,#ffffff); box-shadow:0 16px 34px rgba(15,23,42,.06); }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h3 { margin:.2rem 0; color:#16324f; font-weight:800; }
    .subtitle { color:#64748b; font-size:.92rem; }
    .meta, .actions { display:flex; gap:.6rem; flex-wrap:wrap; align-items:center; }
    .risk { display:inline-flex; align-items:center; padding:.35rem .7rem; border-radius:999px; background:#eef6ff; color:#255bb7; font-weight:700; font-size:.8rem; }
  `]
})
export class EncounterHeaderComponent {
  @Input({ required: true }) summary!: ConsultationSummaryViewModel;
  @Input() canComplete = false;
  @Input() saving = false;
  @Input() completed = false;
  @Output() back = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() complete = new EventEmitter<void>();
  @Output() summaryRequested = new EventEmitter<void>();
}
