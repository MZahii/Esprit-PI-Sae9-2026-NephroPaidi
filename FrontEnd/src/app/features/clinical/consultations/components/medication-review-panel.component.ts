import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MedicationReviewViewModel } from '../consultation-workspace.models';

@Component({
  selector: 'app-medication-review-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Medications</span>
          <h5>{{ title }}</h5>
        </div>
        <button class="btn btn-primary btn-sm" type="button" (click)="add.emit()">Add Prescription</button>
      </div>
      <ng-container *ngIf="model.doseAlerts.length">
        <div class="dose-alert" *ngFor="let alert of model.doseAlerts">{{ alert }}</div>
      </ng-container>
      <div *ngIf="model.prescriptions.length === 0" class="empty">No medication line has been added yet.</div>
      <div class="cardline" *ngFor="let item of model.prescriptions; let i = index">
        <div class="row g-2 align-items-end">
          <div class="col-md-3 position-relative">
            <label class="form-label">Medication</label>
            <input class="form-control" [(ngModel)]="item.medication" (input)="emitSearch(i, $event)" placeholder="Search pharmacy catalog..." />
            <ul class="list-group position-absolute w-100 shadow-sm mt-1 z-3" *ngIf="model.suggestions[i]?.length">
              <li class="list-group-item list-group-item-action py-1 small" *ngFor="let med of model.suggestions[i]" (click)="pick.emit({ index: i, med })">
                {{ med.name }} <span class="text-muted" *ngIf="med.form">({{ med.form }})</span>
              </li>
            </ul>
          </div>
          <div class="col-md-2">
            <label class="form-label">Dosage</label>
            <input class="form-control" [(ngModel)]="item.dosage" placeholder="10 mg" />
          </div>
          <div class="col-md-2">
            <label class="form-label">Frequency</label>
            <input class="form-control" [(ngModel)]="item.frequency" placeholder="BID" />
          </div>
          <div class="col-md-2">
            <label class="form-label">Duration</label>
            <input type="number" class="form-control" [(ngModel)]="item.durationDays" />
          </div>
          <div class="col-md-2">
            <label class="form-label">mg/kg/day</label>
            <input type="number" class="form-control" [(ngModel)]="item.doseMgPerKg" />
          </div>
          <div class="col-md-1">
            <button class="btn btn-outline-danger w-100" type="button" (click)="remove.emit(i)">Remove</button>
          </div>
          <div class="col-md-3">
            <label class="form-label">Min</label>
            <input type="number" class="form-control" [(ngModel)]="item.minDoseMgPerKg" />
          </div>
          <div class="col-md-3">
            <label class="form-label">Max</label>
            <input type="number" class="form-control" [(ngModel)]="item.maxDoseMgPerKg" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Notes</label>
            <input class="form-control" [(ngModel)]="item.note" placeholder="Clinical notes" />
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .head { display:flex; justify-content:space-between; gap:1rem; align-items:flex-start; margin-bottom:1rem; }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0; font-weight:800; color:#16324f; }
    .empty { color:#64748b; }
    .cardline { border:1px solid #e3ebf3; border-radius:18px; padding:1rem; background:#fbfdff; margin-top:.8rem; }
    .dose-alert { padding:.75rem .85rem; border-radius:16px; border:1px solid #ffd9b3; background:#fff5ea; color:#9a4a05; margin-bottom:.7rem; }
  `]
})
export class MedicationReviewPanelComponent {
  @Input({ required: true }) model!: MedicationReviewViewModel;
  @Input() title = 'Medication review';
  @Output() add = new EventEmitter<void>();
  @Output() remove = new EventEmitter<number>();
  @Output() search = new EventEmitter<{ index: number; query: string }>();
  @Output() pick = new EventEmitter<{ index: number; med: any }>();

  emitSearch(index: number, event: Event): void {
    const query = ((event.target as HTMLInputElement)?.value ?? '').trim();
    this.search.emit({ index, query });
  }
}
