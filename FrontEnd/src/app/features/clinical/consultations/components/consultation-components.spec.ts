import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ClinicalAlertCenterComponent } from './clinical-alert-center.component';
import { ClinicalAssessmentFormComponent } from './clinical-assessment-form.component';
import { DispositionPanelComponent } from './disposition-panel.component';
import { EncounterHeaderComponent } from './encounter-header.component';
import { FollowUpRequestDialogComponent } from './follow-up-request-dialog.component';
import { HospitalizationActionDialogComponent } from './hospitalization-action-dialog.component';
import { LabOrdersPanelComponent } from './lab-orders-panel.component';
import { MedicationReviewPanelComponent } from './medication-review-panel.component';
import { PatientSnapshotCardComponent } from './patient-snapshot-card.component';
import { RenalMetricsPanelComponent } from './renal-metrics-panel.component';

function mount<T>(cmp: new (...a: any[]) => T, inputs: Record<string, unknown>) {
  TestBed.configureTestingModule({ imports: [cmp], providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(cmp);
  for (const [k, v] of Object.entries(inputs)) fixture.componentRef.setInput(k, v);
  fixture.detectChanges();
  return fixture;
}

const text = (el: HTMLElement) => el.textContent?.replace(/\s+/g, ' ') ?? '';

describe('Consultation workspace — composants de présentation', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('ClinicalAlertCenter affiche les alertes et émet open', () => {
    const f = mount(ClinicalAlertCenterComponent, {
      alerts: [{ title: 'eGFR < 30', severity: 'critical' }, { title: 'TA élevée', severity: 'warning' }]
    });
    expect(text(f.nativeElement)).toContain('eGFR < 30');
    const spy = vi.fn();
    f.componentInstance.open.subscribe(spy);
    f.componentInstance.open.emit();
    expect(spy).toHaveBeenCalled();
  });

  it('ClinicalAssessmentForm rend le SOAP et la liste de diagnostics', () => {
    const f = mount(ClinicalAssessmentFormComponent, {
      draft: {
        soap: { subjective: 'Fatigue', objective: 'Œdème', assessment: 'SN', plan: 'Bilan' },
        treatmentPlan: 'Corticoïdes',
        guardianInstructions: 'Régime pauvre en sel',
        diagnosisList: [{ label: 'Syndrome néphrotique', code: 'N04', notes: '' }]
      }
    });
    expect(f.componentInstance.draft.diagnosisList.length).toBe(1);
    const add = vi.fn();
    f.componentInstance.addDiagnosis.subscribe(add);
    f.componentInstance.addDiagnosis.emit();
    expect(add).toHaveBeenCalledTimes(1);
  });

  it('DispositionPanel reflète un suivi programmé', () => {
    const f = mount(DispositionPanelComponent, {
      state: {
        followUpStatusLabel: 'Programmé',
        followUpStatusClass: 'ok',
        followUpPreviewDate: '2026-11-02',
        followUpEnabled: true,
        followUpQueued: true,
        hospitalizationActive: false,
        activeMedicationCount: 2
      }
    });
    expect(text(f.nativeElement)).toContain('Programmé');
  });

  it('EncounterHeader affiche le patient et le score de complétude', () => {
    const f = mount(EncounterHeaderComponent, {
      summary: {
        consultationId: 'C-1',
        patientName: 'Yassine B.',
        subtitle: '8 ans',
        scheduledAt: '2026-10-19T09:00:00',
        statusLabel: 'En cours',
        statusClass: 'info',
        completenessScore: 75,
        completenessLabel: '75 %',
        completenessClass: 'warn',
        hasUnsavedChanges: true,
        renalRiskLabel: 'Modéré'
      },
      canComplete: true
    });
    expect(text(f.nativeElement)).toContain('Yassine B.');
  });

  it('FollowUpRequestDialog rend le brouillon de suivi', () => {
    const f = mount(FollowUpRequestDialogComponent, {
      draft: {
        enabled: true,
        offsetAmount: 2,
        offsetUnit: 'WEEKS',
        previewDate: '2026-11-02',
        requestSent: false,
        message: '',
        treatmentPlanReady: true,
        guardianInstructionsReady: false
      }
    });
    expect(f.componentInstance.draft.offsetUnit).toBe('WEEKS');
  });

  it('HospitalizationActionDialog liste les doses du plan de soins', () => {
    const f = mount(HospitalizationActionDialogComponent, {
      draft: {
        active: true,
        carePlanDoses: [{ medication: 'Furosémide', scheduleTime: '08:00', taken: false }],
        queryParams: { consultationId: 'C-1' }
      },
      consultationId: 'C-1'
    });
    expect(f.componentInstance.draft.carePlanDoses.length).toBe(1);
  });

  describe('LabOrdersPanel', () => {
    const model = () => ({ labRequests: [] as any[], submitting: false, selectedCount: 0 });

    it('ajoute un examen coché puis le retire', () => {
      const m = model();
      const f = mount(LabOrdersPanelComponent, { model: m });
      const c = f.componentInstance;
      const test = { key: 'albumin', label: 'Albumin', hint: '', category: 'Immunology & Extended' };

      c.toggleTest(test, true);
      expect(m.labRequests).toHaveLength(1);
      expect(c.isSelected('albumin')).toBe(true);

      c.toggleTest(test, true); // déjà sélectionné : pas de doublon
      expect(m.labRequests).toHaveLength(1);

      const removed = vi.fn();
      c.remove.subscribe(removed);
      c.toggleTest(test, false);
      expect(removed).toHaveBeenCalledWith(0);
    });

    it('déduit la clé depuis le libellé quand elle manque', () => {
      const m = model();
      m.labRequests.push({ test: 'C3 / C4', category: '', urgency: 'Routine', note: '', uploadedFileNames: [] });
      const f = mount(LabOrdersPanelComponent, { model: m });
      expect(f.componentInstance.isSelected('c3-c4')).toBe(true);
    });

    it('émet les fichiers sélectionnés', () => {
      const f = mount(LabOrdersPanelComponent, { model: model() });
      const spy = vi.fn();
      f.componentInstance.filesChanged.subscribe(spy);
      const file = new File(['a'], 'bilan.pdf');
      f.componentInstance.onFilesSelected('albumin', 'Albumin', { target: { files: [file] } } as any);
      expect(spy).toHaveBeenCalledWith({ testKey: 'albumin', testLabel: 'Albumin', files: [file] });
      f.componentInstance.onFilesSelected('albumin', 'Albumin', { target: null } as any);
      expect(spy).toHaveBeenLastCalledWith({ testKey: 'albumin', testLabel: 'Albumin', files: [] });
    });
  });

  it('MedicationReviewPanel émet une recherche nettoyée', () => {
    const f = mount(MedicationReviewPanelComponent, {
      model: {
        prescriptions: [{ medication: 'Amox', dosage: '250 mg', frequency: 'BID', durationDays: 7 }],
        suggestions: {},
        doseAlerts: ['Dose > max pédiatrique']
      }
    });
    expect(text(f.nativeElement)).toContain('Dose > max pédiatrique');
    const spy = vi.fn();
    f.componentInstance.search.subscribe(spy);
    f.componentInstance.emitSearch(0, { target: { value: '  amox  ' } } as any);
    expect(spy).toHaveBeenCalledWith({ index: 0, query: 'amox' });
    f.componentInstance.emitSearch(1, { target: null } as any);
    expect(spy).toHaveBeenLastCalledWith({ index: 1, query: '' });
  });

  it('PatientSnapshotCard affiche le résumé patient', () => {
    const f = mount(PatientSnapshotCardComponent, {
      snapshot: {
        patientName: 'Yassine B.',
        subtitle: 'Dossier #12',
        allergiesLabel: 'Aucune',
        ageLabel: '8 ans',
        sexLabel: 'M',
        growthNarrative: 'Croissance normale',
        timelineSummary: '3 consultations',
        activeProblemCount: 1,
        followUpStatusLabel: 'À planifier'
      }
    });
    expect(text(f.nativeElement)).toContain('Yassine B.');
  });

  it('RenalMetricsPanel affiche le DFG et la tendance', () => {
    const f = mount(RenalMetricsPanelComponent, {
      metrics: {
        egfrLabel: '58', egfrFormulaLabel: 'Schwartz', ckdStage: '3a',
        bloodPressurePercentileLabel: 'P90', heightPercentileLabel: 'P50', weightPercentileLabel: 'P40',
        trajectoryLabel: 'Stable', previousEgfrLabel: '60', latestDeltaLabel: '-2',
        clinicalAiRecommendationLabel: 'Surveiller', clinicalAiConfidencePercent: '80 %', clinicalAiSummary: 'RAS',
        egfrAiRiskLabel: 'Faible', egfrAiProbabilityLabel: '12 %', egfrAiConfidenceLabel: 'Haute',
        egfrAiPredicted3mLabel: '57', egfrAiPredicted6mLabel: '56', egfrAiPredicted12mLabel: '54',
        egfrAiHint: '', latestLabSourceFileName: 'bilan.pdf', latestLabSourceAvailable: true,
        trendPoints: [{ dateTime: '2026-09-01', egfr: 60 }, { dateTime: '2026-10-01', egfr: 58 }]
      }
    });
    expect(text(f.nativeElement)).toContain('Schwartz');
  });
});
