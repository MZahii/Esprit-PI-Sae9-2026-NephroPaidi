import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RenalMetricsViewModel } from '../consultation-workspace.models';

@Component({
  selector: 'app-renal-metrics-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="panel">
      <div class="head">
        <div>
          <span class="eyebrow">Clinical data</span>
          <h5>Renal interpretation and AI support</h5>
        </div>
        <button
          *ngIf="metrics.latestLabSourceAvailable"
          class="btn btn-light btn-sm"
          type="button"
          (click)="openLatestLab.emit()">
          View Latest Lab
        </button>
      </div>
      <div class="metric-row"><span>eGFR</span><strong>{{ metrics.egfrLabel }}</strong></div>
      <div class="metric-row"><span>Formula</span><strong>{{ metrics.egfrFormulaLabel }}</strong></div>
      <div class="metric-row"><span>CKD Stage</span><strong>{{ metrics.ckdStage }}</strong></div>
      <div class="metric-row"><span>Blood Pressure</span><strong>{{ metrics.bloodPressurePercentileLabel }}</strong></div>
      <div class="metric-row"><span>Height Percentile</span><strong>{{ metrics.heightPercentileLabel }}</strong></div>
      <div class="metric-row"><span>Weight Percentile</span><strong>{{ metrics.weightPercentileLabel }}</strong></div>
      <div class="metric-row"><span>Trajectory</span><strong>{{ metrics.trajectoryLabel }}</strong></div>
      <div class="metric-row"><span>Previous eGFR</span><strong>{{ metrics.previousEgfrLabel }}</strong></div>
      <div class="metric-row"><span>Latest Delta</span><strong>{{ metrics.latestDeltaLabel }}</strong></div>

      <div class="subhead">Clinical AI</div>
      <div class="metric-row"><span>Recommendation</span><strong>{{ metrics.clinicalAiRecommendationLabel }}</strong></div>
      <div class="metric-row"><span>Confidence</span><strong>{{ metrics.clinicalAiConfidencePercent }}</strong></div>
      <div class="hint">{{ metrics.clinicalAiSummary }}</div>

      <div class="subhead">eGFR AI</div>
      <div class="metric-row"><span>Risk Label</span><strong>{{ metrics.egfrAiRiskLabel }}</strong></div>
      <div class="metric-row"><span>Rapid Decline Probability</span><strong>{{ metrics.egfrAiProbabilityLabel }}</strong></div>
      <div class="metric-row"><span>Model Confidence</span><strong>{{ metrics.egfrAiConfidenceLabel }}</strong></div>
      <div class="metric-row"><span>Predicted eGFR 3m</span><strong>{{ metrics.egfrAiPredicted3mLabel }}</strong></div>
      <div class="metric-row"><span>Predicted eGFR 6m</span><strong>{{ metrics.egfrAiPredicted6mLabel }}</strong></div>
      <div class="metric-row"><span>Predicted eGFR 12m</span><strong>{{ metrics.egfrAiPredicted12mLabel }}</strong></div>
      <div class="hint">{{ metrics.egfrAiHint }}</div>
      <div class="trend" *ngIf="metrics.trendPoints.length">
        <div class="trend-title">Recent renal trend</div>
        <div class="trend-row" *ngFor="let item of metrics.trendPoints">
          <span>{{ item.dateTime | date:'shortDate' }}</span>
          <strong>{{ item.egfr !== null ? item.egfr : 'N/A' }}</strong>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .panel { border:1px solid #dbe7f1; border-radius:22px; padding:1.15rem; background:#fff; box-shadow:0 14px 32px rgba(15,23,42,.06); }
    .eyebrow { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em; color:#2f6fde; font-weight:800; }
    h5 { margin:.2rem 0 1rem; font-weight:800; color:#16324f; }
    .subhead { margin-top:1rem; margin-bottom:.35rem; font-size:.76rem; text-transform:uppercase; letter-spacing:.06em; color:#6d86a0; font-weight:800; }
    .metric-row,.trend-row { display:flex; justify-content:space-between; gap:.7rem; padding:.5rem 0; border-bottom:1px solid #edf2f7; color:#48627c; }
    .metric-row:last-of-type { border-bottom:none; }
    .hint { margin-top:.9rem; padding:.8rem .9rem; border-radius:14px; background:#f4f8fc; color:#36516b; }
    .trend { margin-top:1rem; }
    .trend-title { font-size:.76rem; text-transform:uppercase; letter-spacing:.06em; color:#6d86a0; font-weight:700; margin-bottom:.45rem; }
  `]
})
export class RenalMetricsPanelComponent {
  @Input({ required: true }) metrics!: RenalMetricsViewModel;
  @Output() openLatestLab = new EventEmitter<void>();
}
