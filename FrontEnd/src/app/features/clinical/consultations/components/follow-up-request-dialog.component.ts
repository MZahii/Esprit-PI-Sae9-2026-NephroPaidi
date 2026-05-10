import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FollowUpDecisionDraft } from '../consultation-workspace.models';

@Component({
  selector: 'app-follow-up-request-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal fade show d-block" tabindex="-1" style="background: rgba(9,18,33,0.45)">
      <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content clinical-modal">
          <div class="modal-header">
            <div>
              <div class="kicker">Appointments & follow-up</div>
              <h5 class="modal-title mb-0">Follow-up request</h5>
            </div>
            <button type="button" class="btn-close" aria-label="Close" (click)="close.emit()"></button>
          </div>
          <div class="modal-body">
            <div class="form-check mb-3">
              <input class="form-check-input" type="checkbox" id="followupDecision" [(ngModel)]="draft.enabled" (ngModelChange)="changed.emit()" />
              <label class="form-check-label fw-semibold" for="followupDecision">Enable follow-up request</label>
            </div>
            <div class="row g-3" *ngIf="draft.enabled">
              <div class="col-md-3">
                <label class="form-label">Return in</label>
                <input type="number" class="form-control" min="1" [(ngModel)]="draft.offsetAmount" (ngModelChange)="changed.emit()" />
              </div>
              <div class="col-md-3">
                <label class="form-label">Unit</label>
                <select class="form-select" [(ngModel)]="draft.offsetUnit" (ngModelChange)="changed.emit()">
                  <option value="DAYS">Days</option>
                  <option value="WEEKS">Weeks</option>
                  <option value="MONTHS">Months</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="form-label">Suggested date</label>
                <div class="readonly">{{ draft.previewDate || 'Define return interval' }}</div>
              </div>
            </div>
            <div class="tags">
              <span class="tag">{{ draft.requestSent ? 'Queued' : 'Not queued' }}</span>
              <span class="tag">Treatment plan {{ draft.treatmentPlanReady ? 'ready' : 'pending' }}</span>
              <span class="tag">Guardian instructions {{ draft.guardianInstructionsReady ? 'ready' : 'pending' }}</span>
            </div>
            <div class="alert alert-success mt-3 mb-0" *ngIf="draft.message">{{ draft.message }}</div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-light" (click)="close.emit()">Close</button>
            <button type="button" class="btn btn-primary" (click)="submit.emit()" [disabled]="!draft.enabled">Queue request now</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .clinical-modal { border-radius:26px; border:1px solid #d7e3ef; box-shadow:0 28px 70px rgba(15,23,42,.24); }
    .kicker { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    .readonly { min-height:48px; padding:.75rem .85rem; border-radius:14px; background:#f5f9fd; border:1px solid #dce7f2; color:#16324f; }
    .tags { display:flex; flex-wrap:wrap; gap:.55rem; margin-top:1rem; }
    .tag { display:inline-flex; padding:.35rem .7rem; border-radius:999px; background:#eef4fa; color:#48647f; font-size:.78rem; font-weight:700; }
  `]
})
export class FollowUpRequestDialogComponent {
  @Input({ required: true }) draft!: FollowUpDecisionDraft;
  @Output() changed = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<void>();
}
