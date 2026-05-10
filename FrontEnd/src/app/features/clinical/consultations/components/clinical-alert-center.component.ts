import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ClinicalAlertViewModel } from '../consultation-workspace.models';

@Component({
  selector: 'app-clinical-alert-center',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Alerts</span>
          <h5>Clinical alert center</h5>
        </div>
        <button class="btn btn-light btn-sm" type="button" (click)="open.emit()">Open</button>
      </div>
      <div *ngIf="alerts.length === 0" class="empty">No active alert is currently triggered.</div>
      <div *ngFor="let alert of alerts" class="alert-card" [attr.data-severity]="alert.severity">
        {{ alert.title }}
      </div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .head { display:flex; justify-content:space-between; gap:1rem; align-items:flex-start; margin-bottom:1rem; }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0; font-weight:800; color:#16324f; }
    .empty { color:#64748b; }
    .alert-card { padding:.8rem .9rem; border-radius:16px; border:1px solid #ffe0bf; background:#fff5ea; color:#9a4a05; margin-bottom:.7rem; }
    .alert-card[data-severity='critical'] { background:#fff0f2; border-color:#ffd1d9; color:#b4233d; }
    .alert-card[data-severity='info'] { background:#eef6ff; border-color:#d5e7ff; color:#255bb7; }
  `]
})
export class ClinicalAlertCenterComponent {
  @Input() alerts: ClinicalAlertViewModel[] = [];
  @Output() open = new EventEmitter<void>();
}
