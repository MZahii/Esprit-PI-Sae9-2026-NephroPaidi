import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DispositionState } from '../consultation-workspace.models';

@Component({
  selector: 'app-disposition-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Disposition</span>
          <h5>Downstream decisions</h5>
        </div>
      </div>
      <div class="status-row">
        <span>Follow-up</span>
        <strong [ngClass]="state.followUpStatusClass">{{ state.followUpStatusLabel }}</strong>
      </div>
      <div class="status-row">
        <span>Target date</span>
        <strong>{{ state.followUpPreviewDate || 'Not configured' }}</strong>
      </div>
      <div class="status-row">
        <span>Hospitalization</span>
        <strong>{{ state.hospitalizationActive ? 'Workflow prepared' : 'Not initiated' }}</strong>
      </div>
      <div class="status-row">
        <span>Medication lines</span>
        <strong>{{ state.activeMedicationCount }}</strong>
      </div>
      <div class="actions">
        <button class="btn btn-outline-primary btn-sm" type="button" (click)="followUp.emit()">Follow-up request</button>
        <button class="btn btn-outline-primary btn-sm" type="button" (click)="hospitalization.emit()">Hospitalization handoff</button>
        <button class="btn btn-light btn-sm" type="button" (click)="medications.emit()">Medication safety</button>
      </div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0 1rem; font-weight:800; color:#16324f; }
    .status-row { display:flex; justify-content:space-between; gap:.75rem; padding:.55rem 0; border-bottom:1px solid #edf2f7; color:#48627c; }
    .actions { display:flex; flex-wrap:wrap; gap:.55rem; margin-top:1rem; }
  `]
})
export class DispositionPanelComponent {
  @Input({ required: true }) state!: DispositionState;
  @Output() followUp = new EventEmitter<void>();
  @Output() hospitalization = new EventEmitter<void>();
  @Output() medications = new EventEmitter<void>();
}
