import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClinicalAssessmentDraft } from '../consultation-workspace.models';

@Component({
  selector: 'app-clinical-assessment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Clinical assessment</span>
          <h5>Encounter documentation</h5>
        </div>
        <button class="btn btn-light btn-sm" type="button" (click)="addDiagnosis.emit()">Add diagnosis</button>
      </div>

      <div class="row g-3">
        <div class="col-12">
          <label class="form-label">Reason for Visit</label>
          <textarea class="form-control" rows="3" [(ngModel)]="draft.soap.subjective" placeholder="Chief complaint, symptoms, caregiver concerns..."></textarea>
        </div>
        <div class="col-12">
          <label class="form-label">Clinical Findings</label>
          <textarea class="form-control" rows="3" [(ngModel)]="draft.soap.objective" placeholder="Exam findings, vitals, and objective observations..."></textarea>
        </div>
        <div class="col-12">
          <label class="form-label">Impression</label>
          <textarea class="form-control" rows="3" [(ngModel)]="draft.soap.assessment" placeholder="Renal interpretation, active problems, working diagnosis..."></textarea>
        </div>
        <div class="col-12">
          <label class="form-label">Clinical Note</label>
          <textarea class="form-control" rows="3" [(ngModel)]="draft.soap.plan" placeholder="Encounter-specific note for the care team..."></textarea>
        </div>
        <div class="col-12">
          <label class="form-label">Management Plan</label>
          <textarea class="form-control" rows="4" [(ngModel)]="draft.treatmentPlan" placeholder="Management plan, renal strategy, medications, monitoring..."></textarea>
        </div>
        <div class="col-12">
          <label class="form-label">Guardian Communication</label>
          <textarea class="form-control" rows="3" [(ngModel)]="draft.guardianInstructions" placeholder="Instructions, red flags, home care, education..."></textarea>
        </div>
      </div>

      <div class="diagnoses">
        <div class="diagnosis-card" *ngFor="let item of draft.diagnosisList; let i = index">
          <div class="row g-2 align-items-end">
            <div class="col-md-4">
              <label class="form-label">Diagnosis</label>
              <input class="form-control" [(ngModel)]="item.label" placeholder="Diagnosis name" />
            </div>
            <div class="col-md-3">
              <label class="form-label">Code</label>
              <input class="form-control" [(ngModel)]="item.code" placeholder="ICD / local code" />
            </div>
            <div class="col-md-3">
              <label class="form-label">Severity</label>
              <select class="form-select" [(ngModel)]="item.severity">
                <option value="Mild">Mild</option>
                <option value="Moderate">Moderate</option>
                <option value="Severe">Severe</option>
              </select>
            </div>
            <div class="col-md-2">
              <button class="btn btn-outline-danger w-100" type="button" (click)="removeDiagnosis.emit(i)">Remove</button>
            </div>
            <div class="col-12">
              <label class="form-label">Notes</label>
              <input class="form-control" [(ngModel)]="item.notes" placeholder="Clinical notes for this diagnosis" />
            </div>
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
    textarea.form-control { min-height:110px; resize:vertical; }
    .diagnoses { margin-top:1rem; display:grid; gap:.9rem; }
    .diagnosis-card { border:1px solid #e3ebf3; border-radius:18px; padding:1rem; background:#fbfdff; }
  `]
})
export class ClinicalAssessmentFormComponent {
  @Input({ required: true }) draft!: ClinicalAssessmentDraft;
  @Output() addDiagnosis = new EventEmitter<void>();
  @Output() removeDiagnosis = new EventEmitter<number>();
}
