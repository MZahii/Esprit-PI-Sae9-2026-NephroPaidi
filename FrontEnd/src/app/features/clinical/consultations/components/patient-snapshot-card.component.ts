import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PatientSnapshotViewModel } from '../consultation-workspace.models';

@Component({
  selector: 'app-patient-snapshot-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="panel">
      <div class="panel-head">
        <div>
          <span class="eyebrow">Patient management</span>
          <h5>{{ snapshot.patientName }}</h5>
          <div class="muted">{{ snapshot.subtitle }}</div>
        </div>
        <button class="btn btn-light btn-sm" type="button" (click)="openProfile.emit()">Open profile</button>
      </div>
      <div class="stat-grid">
        <div class="stat"><span>Age</span><strong>{{ snapshot.ageLabel }}</strong></div>
        <div class="stat"><span>Sex</span><strong>{{ snapshot.sexLabel }}</strong></div>
        <div class="stat"><span>Allergies</span><strong>{{ snapshot.allergiesLabel }}</strong></div>
        <div class="stat"><span>Problems</span><strong>{{ snapshot.activeProblemCount }}</strong></div>
      </div>
      <div class="note">{{ snapshot.growthNarrative }}</div>
      <div class="muted">{{ snapshot.timelineSummary }}</div>
      <div class="followup">Follow-up: <strong>{{ snapshot.followUpStatusLabel }}</strong></div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .panel-head { display:flex; justify-content:space-between; gap:1rem; align-items:flex-start; margin-bottom:1rem; }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0; font-weight:800; color:#16324f; }
    .muted { color:#64748b; font-size:.9rem; }
    .stat-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:.7rem; margin:1rem 0; }
    .stat { padding:.8rem .9rem; border-radius:16px; background:#f8fbfe; border:1px solid #e6eef6; }
    .stat span { display:block; font-size:.72rem; font-weight:700; color:#7590aa; text-transform:uppercase; }
    .stat strong { color:#16324f; font-size:.92rem; }
    .note { margin-bottom:.5rem; color:#27435e; font-weight:600; }
    .followup { margin-top:.7rem; color:#48627c; }
  `]
})
export class PatientSnapshotCardComponent {
  @Input({ required: true }) snapshot!: PatientSnapshotViewModel;
  @Output() openProfile = new EventEmitter<void>();
}
