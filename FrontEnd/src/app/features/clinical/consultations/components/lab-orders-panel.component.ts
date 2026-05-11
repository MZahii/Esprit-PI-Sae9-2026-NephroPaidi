import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LabOrdersViewModel } from '../consultation-workspace.models';

interface LabCatalogTest {
  key: string;
  label: string;
  hint: string;
  category: string;
}

interface LabCatalogCategory {
  label: string;
  hint: string;
  tests: LabCatalogTest[];
}

@Component({
  selector: 'app-lab-orders-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Orders</span>
          <h5>Lab requests</h5>
        </div>
        <div class="toolbar">
          <button class="btn btn-primary btn-sm" type="button" (click)="send.emit()" [disabled]="model.submitting">
            {{ model.submitting ? 'Sending…' : 'Send lab orders' }}
          </button>
        </div>
      </div>

      <div class="category-grid">
        <section class="category-card" *ngFor="let category of catalog">
          <div class="category-head">
            <div>
              <h6>{{ category.label }}</h6>
              <p>{{ category.hint }}</p>
            </div>
          </div>
          <label class="test-check" *ngFor="let test of category.tests">
            <input
              type="checkbox"
              [checked]="isSelected(test.key)"
              (change)="toggleTest(test, $any($event.target).checked)"
            />
            <span>
              <strong>{{ test.label }}</strong>
              <small>{{ test.hint }}</small>
            </span>
          </label>
        </section>
      </div>

      <div *ngIf="model.labRequests.length === 0" class="empty">Select one or more tests to build a grouped request for the lab team.</div>

      <div class="cardline" *ngFor="let item of model.labRequests; let i = index">
        <div class="row g-2 align-items-end">
          <div class="col-lg-4">
            <label class="form-label">Selected test</label>
            <input class="form-control" [ngModel]="item.test" disabled />
          </div>
          <div class="col-lg-3">
            <label class="form-label">Urgency</label>
            <select class="form-select" [(ngModel)]="item.urgency">
              <option value="Routine">Routine</option>
              <option value="Urgent">Urgent</option>
              <option value="STAT">STAT</option>
            </select>
          </div>
          <div class="col-lg-3">
            <label class="form-label">Supporting files</label>
            <input class="form-control" type="file" multiple (change)="onFilesSelected(item.key || item.test, item.test, $event)" />
          </div>
          <div class="col-lg-2">
            <button class="btn btn-outline-danger w-100" type="button" (click)="remove.emit(i)" [disabled]="!!item.backendRequestId">Remove</button>
          </div>
          <div class="col-12">
            <label class="form-label">Clinical note for lab agents</label>
            <input class="form-control" [(ngModel)]="item.note" placeholder="Preparation notes, clinical context, warning flags..." />
          </div>
          <div class="col-12" *ngIf="item.uploadedFileNames?.length">
            <div class="file-pills">
              <span class="file-pill" *ngFor="let fileName of item.uploadedFileNames">{{ fileName }}</span>
            </div>
          </div>
          <div class="col-12" *ngIf="item.backendRequestId">
            <div class="result-state">
              <strong>{{ item.status || 'Submitted' }}</strong>
              <span *ngIf="item.latestResultUploadedAt">Result uploaded {{ item.latestResultUploadedAt | date:'medium' }}</span>
              <span *ngIf="item.latestAiSummary">{{ item.latestAiSummary }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .head { display:flex; justify-content:space-between; gap:1rem; align-items:flex-start; margin-bottom:1rem; }
    .toolbar { display:flex; gap:.5rem; flex-wrap:wrap; }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0; font-weight:800; color:#16324f; }
    .empty { color:#64748b; }
    .cardline { border:1px solid #e3ebf3; border-radius:18px; padding:1rem; background:#fbfdff; margin-top:.8rem; }
    .category-grid { display:grid; grid-template-columns:repeat(auto-fit, minmax(220px,1fr)); gap:.9rem; margin-bottom:1rem; }
    .category-card { border:1px solid #e3ebf3; border-radius:18px; padding:1rem; background:#f8fbff; }
    .category-head h6 { margin:0 0 .25rem; color:#16324f; font-weight:800; }
    .category-head p { margin:0 0 .75rem; color:#64748b; font-size:.88rem; }
    .test-check { display:flex; gap:.7rem; align-items:flex-start; padding:.55rem 0; cursor:pointer; }
    .test-check input { margin-top:.22rem; }
    .test-check strong { display:block; color:#17324b; }
    .test-check small { display:block; color:#6b7f95; }
    .file-pills { display:flex; gap:.45rem; flex-wrap:wrap; }
    .file-pill { display:inline-flex; padding:.28rem .65rem; border-radius:999px; background:#eef6ff; color:#2458b5; font-size:.78rem; font-weight:700; }
    .result-state { display:grid; gap:.2rem; padding:.75rem .9rem; border-radius:14px; background:#eef8f3; color:#285943; }
  `]
})
export class LabOrdersPanelComponent {
  readonly catalog: LabCatalogCategory[] = [
    {
      label: 'Hematology',
      hint: 'Cell counts and inflammatory screening.',
      tests: [
        { key: 'cbc', label: 'CBC', hint: 'Complete blood count', category: 'Hematology' },
        { key: 'crp', label: 'CRP', hint: 'Inflammation marker', category: 'Hematology' }
      ]
    },
    {
      label: 'Renal & Biochemistry',
      hint: 'Kidney function and electrolyte safety.',
      tests: [
        { key: 'creatinine', label: 'Serum Creatinine', hint: 'Core renal marker', category: 'Renal & Biochemistry' },
        { key: 'urea', label: 'Urea', hint: 'Nitrogen balance', category: 'Renal & Biochemistry' },
        { key: 'electrolytes', label: 'Electrolytes Panel', hint: 'Na / K / Cl / HCO3', category: 'Renal & Biochemistry' },
        { key: 'calcium-phosphate', label: 'Calcium / Phosphate', hint: 'Mineral bone profile', category: 'Renal & Biochemistry' }
      ]
    },
    {
      label: 'Urine Studies',
      hint: 'Proteinuria, sediment, and infection clues.',
      tests: [
        { key: 'urinalysis', label: 'Urinalysis', hint: 'Dipstick and microscopy', category: 'Urine Studies' },
        { key: 'protein-creatinine-ratio', label: 'Protein/Creatinine Ratio', hint: 'Proteinuria quantification', category: 'Urine Studies' },
        { key: 'urine-culture', label: 'Urine Culture', hint: 'Bacterial investigation', category: 'Urine Studies' }
      ]
    },
    {
      label: 'Immunology & Extended',
      hint: 'Targeted workup for nephrotic and immune causes.',
      tests: [
        { key: 'albumin', label: 'Albumin', hint: 'Nephrotic syndrome support', category: 'Immunology & Extended' },
        { key: 'c3-c4', label: 'C3 / C4', hint: 'Complement profile', category: 'Immunology & Extended' },
        { key: 'ana', label: 'ANA', hint: 'Autoimmune screening', category: 'Immunology & Extended' }
      ]
    }
  ];

  @Input({ required: true }) model!: LabOrdersViewModel;
  @Output() remove = new EventEmitter<number>();
  @Output() send = new EventEmitter<void>();
  @Output() filesChanged = new EventEmitter<{ testKey: string; testLabel: string; files: File[] }>();

  isSelected(testKey: string): boolean {
    return this.model.labRequests.some((item) => (item.key || this.toItemKey(item.test)) === testKey);
  }

  toggleTest(test: LabCatalogTest, checked: boolean): void {
    const existingIndex = this.model.labRequests.findIndex((item) => (item.key || this.toItemKey(item.test)) === test.key);
    if (checked && existingIndex === -1) {
      this.model.labRequests.push({
        key: test.key,
        test: test.label,
        category: test.category,
        urgency: 'Routine',
        note: '',
        uploadedFileNames: []
      });
      return;
    }
    if (!checked && existingIndex >= 0) {
      this.remove.emit(existingIndex);
    }
  }

  onFilesSelected(testKey: string, testLabel: string, event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const files = Array.from(input?.files ?? []);
    this.filesChanged.emit({ testKey, testLabel, files });
  }

  private toItemKey(label: string): string {
    return String(label ?? '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }
}
