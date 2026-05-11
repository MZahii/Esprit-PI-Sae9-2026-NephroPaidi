import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HospitalizationLaunchDraft } from '../consultation-workspace.models';

@Component({
  selector: 'app-hospitalization-action-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="modal fade show d-block" tabindex="-1" style="background: rgba(9,18,33,0.45)">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content clinical-modal">
          <div class="modal-header">
            <div>
              <div class="kicker">Hospitalization handoff</div>
              <h5 class="modal-title mb-0">Inpatient transition workflow</h5>
            </div>
            <button type="button" class="btn-close" aria-label="Close" (click)="close.emit()"></button>
          </div>
          <div class="modal-body">
            <p class="text-muted small">Prepare inpatient care-adherence slots here, then launch the full hospitalization workflow for ops and nursing.</p>
            <div class="d-flex justify-content-between align-items-center mb-2">
              <strong>Medication schedule</strong>
              <button class="btn btn-light btn-sm" type="button" (click)="addDose.emit()">Add slot</button>
            </div>
            <div *ngIf="draft.carePlanDoses.length === 0" class="text-muted small">No hospitalization care slot yet.</div>
            <div class="dose-card" *ngFor="let item of draft.carePlanDoses; let i = index">
              <div class="row g-2 align-items-end">
                <div class="col-md-4">
                  <label class="form-label">Medication</label>
                  <input class="form-control" [(ngModel)]="item.medication" />
                </div>
                <div class="col-md-3">
                  <label class="form-label">Time</label>
                  <input class="form-control" [(ngModel)]="item.scheduleTime" placeholder="08:00" />
                </div>
                <div class="col-md-2">
                  <label class="form-label">Taken</label>
                  <div><input type="checkbox" [(ngModel)]="item.taken" /></div>
                </div>
                <div class="col-md-3">
                  <button class="btn btn-outline-danger w-100" type="button" (click)="removeDose.emit(i)">Remove</button>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-light" (click)="close.emit()">Close</button>
            <a class="btn btn-outline-primary" [routerLink]="['/backoffice/consultations', consultationId, 'hospitalization', 'new']" [queryParams]="draft.queryParams" (click)="close.emit()">
              Open full hospitalization workflow
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .clinical-modal { border-radius:26px; border:1px solid #d7e3ef; box-shadow:0 28px 70px rgba(15,23,42,.24); }
    .kicker { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    .dose-card { border:1px solid #e3ebf3; border-radius:18px; padding:1rem; background:#fbfdff; margin-top:.8rem; }
  `]
})
export class HospitalizationActionDialogComponent {
  @Input({ required: true }) draft!: HospitalizationLaunchDraft;
  @Input({ required: true }) consultationId!: string;
  @Output() close = new EventEmitter<void>();
  @Output() addDose = new EventEmitter<void>();
  @Output() removeDose = new EventEmitter<number>();
}
