import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { ClinicalApiService } from './clinical-api.service';
import { DrugSafetyService } from './drug-safety.service';
import { ConsultationWorkspaceService, ConsultationWorkspaceDraft } from '../../features/clinical/consultations/consultation-workspace.service';
import { clearSession, seedSession } from '../../testing/test-helpers';

describe('Logique métier des services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    seedSession('DOCTOR');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    clearSession();
    TestBed.resetTestingModule();
  });

  describe('ClinicalApiService — recherche', () => {
    let api: ClinicalApiService;
    beforeEach(() => (api = TestBed.inject(ClinicalApiService)));

    it('ne lance aucune requête sous 2 caractères', async () => {
      await expect(firstValueFrom(api.searchPatients(' a '))).resolves.toEqual([]);
      await expect(firstValueFrom(api.searchDoctors(''))).resolves.toEqual([]);
    });

    it('filtre les patients par nom, groupe sanguin ou allergie (3 max)', async () => {
      const p = firstValueFrom(api.searchPatients('ben'));
      http.expectOne((r) => r.url.endsWith('/api/patients')).flush([
        { id: 1, firstName: 'Ali', lastName: 'Ben Salah' },
        { id: 2, firstName: 'Benoit', lastName: 'X' },
        { id: 3, firstName: 'Sara', lastName: 'Y', allergies: 'benzyl' },
        { id: 4, firstName: 'Omar', lastName: 'Benali' },
        { id: 5, firstName: 'Nour', lastName: 'Z', bloodType: 'A+' }
      ]);
      const res = await p;
      expect(res.map((x) => x.id)).toEqual([1, 2, 3]);
    });

    it('tolère une réponse nulle', async () => {
      const p = firstValueFrom(api.searchPatients('A+'));
      http.expectOne((r) => r.url.endsWith('/api/patients')).flush(null);
      await expect(p).resolves.toEqual([]);
    });

    it('recherche les médecins via /staff/search', async () => {
      const p = firstValueFrom(api.searchDoctors('  Dr H ', 5));
      const req = http.expectOne((r) => r.url.endsWith('/api/users/staff/search'));
      expect(req.request.body).toMatchObject({ query: 'Dr H', roles: ['DOCTOR'], size: 5 });
      req.flush({ items: [{ id: 9 }] });
      await expect(p).resolves.toEqual([{ id: 9 }]);
    });

    it('renvoie [] si la réponse ne contient pas items', async () => {
      const p = firstValueFrom(api.searchDoctors('Dr'));
      http.expectOne((r) => r.url.endsWith('/api/users/staff/search')).flush({});
      await expect(p).resolves.toEqual([]);
    });
  });

  describe('DrugSafetyService — niveau de risque OpenFDA', () => {
    let svc: DrugSafetyService;
    beforeEach(() => (svc = TestBed.inject(DrugSafetyService)));

    const signal = async (result?: object) => {
      const p = firstValueFrom(svc.getSignal('  amoxicillin '));
      const req = http.expectOne((r) => r.url.startsWith('https://api.fda.gov'));
      expect(req.request.params.get('search')).toBe('openfda.generic_name:"amoxicillin"');
      req.flush(result ? { results: [result] } : {});
      return p;
    };

    it('HIGH si boxed warning', async () => {
      expect((await signal({ boxed_warning: ['x'] })).level).toBe('HIGH');
    });
    it('MODERATE si ≥ 3 avertissements', async () => {
      expect((await signal({ warnings: ['a', 'b', 'c'] })).level).toBe('MODERATE');
    });
    it('MODERATE si ≥ 8 effets indésirables', async () => {
      expect((await signal({ adverse_reactions: Array(8).fill('r') })).level).toBe('MODERATE');
    });
    it('LOW si étiquette sans alerte', async () => {
      const s = await signal({ warnings: ['a'] });
      expect(s).toEqual({ queriedDrug: 'amoxicillin', level: 'LOW', boxedWarning: false, warningCount: 1, adverseReactionCount: 0 });
    });
    it('UNKNOWN si aucune étiquette', async () => {
      expect((await signal()).level).toBe('UNKNOWN');
    });
  });

  describe('ConsultationWorkspaceService', () => {
    let ws: ConsultationWorkspaceService;
    let api: Record<string, ReturnType<typeof vi.fn>>;

    const draft = (): ConsultationWorkspaceDraft => ({
      soap: { subjective: ' fatigue ', objective: '', assessment: 'SN', plan: '' },
      diagnosisList: [{ label: ' Syndrome néphrotique ', code: 'N04' }, { label: '', code: '' }],
      treatmentPlan: ' Prednisone ',
      guardianInstructions: 'Peu de sel',
      followUpDate: '2026-11-02',
      labRequests: [],
      prescriptions: [{ medication: ' Prednisone ', dosage: '2 mg/kg', frequency: 'QD' }, { medication: '', dosage: '', frequency: '' }],
      carePlanDoses: [],
      metrics: { heightCm: 120, weightKg: '22' as any, sex: 'M', egfr: 58, alertLowEgfr: false, ckdStage: '3a', creatinineMgDl: NaN }
    });

    beforeEach(() => {
      api = {
        updateConsultationNotes: vi.fn(() => of({})),
        updateConsultationDiagnosis: vi.fn(() => of({})),
        updateConsultationTreatmentPlan: vi.fn(() => of({})),
        updateConsultationPrescriptions: vi.fn(() => of({})),
        upsertConsultationMetrics: vi.fn(() => of({})),
        updateConsultationLabRequests: vi.fn(() => of({})),
        createLabRequest: vi.fn(() => of({ id: 'LR-1' }))
      };
      ws = new ConsultationWorkspaceService(api as unknown as ClinicalApiService);
    });

    it('getDraft : brouillon vide, puis copie locale, et ignore un JSON corrompu', async () => {
      const empty = await firstValueFrom(ws.getDraft('C1'));
      expect(empty.labRequests).toEqual([]);
      localStorage.setItem('clinical_workspace_C1', JSON.stringify({ ...empty, treatmentPlan: 'local' }));
      expect((await firstValueFrom(ws.getDraft('C1'))).treatmentPlan).toBe('local');
      localStorage.setItem('clinical_workspace_C2', '{oops');
      expect((await firstValueFrom(ws.getDraft('C2'))).treatmentPlan).toBe('');
    });

    it('saveDraft sérialise, normalise les métriques et garde une copie locale', async () => {
      await expect(firstValueFrom(ws.saveDraft('C1', draft()))).resolves.toBe(true);
      expect(JSON.parse(api['updateConsultationNotes'].mock.calls[0][1])).toEqual({ subjective: 'fatigue', objective: '', assessment: 'SN', plan: '' });
      expect(JSON.parse(api['updateConsultationDiagnosis'].mock.calls[0][1])).toHaveLength(1);
      expect(JSON.parse(api['updateConsultationPrescriptions'].mock.calls[0][1])).toHaveLength(1);
      const metrics = api['upsertConsultationMetrics'].mock.calls[0][1];
      expect(metrics).toMatchObject({ heightCm: 120, weightKg: 22, sex: 'M', egfr: 58, alertLowEgfr: false, ckdStage: '3a' });
      expect(metrics.creatinineMgDl).toBeUndefined();
      expect(localStorage.getItem('clinical_workspace_C1')).toContain('Prednisone');
    });

    it('saveDraft renvoie false si une partie obligatoire échoue (métriques facultatives)', async () => {
      api['upsertConsultationMetrics'].mockReturnValue(throwError(() => new Error('x')));
      await expect(firstValueFrom(ws.completeConsultation('C1', draft()))).resolves.toBe(true);
      api['updateConsultationNotes'].mockReturnValue(throwError(() => new Error('x')));
      await expect(firstValueFrom(ws.saveDraft('C1', draft()))).resolves.toBe(false);
    });

    it('submitLabRequests crée une demande avec l’urgence la plus haute', async () => {
      const d = draft();
      d.labRequests = [
        { test: 'Créatinine', urgency: 'Routine', note: 'à jeun' },
        { test: 'Albumin', urgency: 'STAT' },
        { test: '  ', urgency: 'Urgent' }
      ];
      const res = await firstValueFrom(ws.submitLabRequests('C1', 7, d));
      expect(res).toEqual({ ok: true, requestId: 'LR-1' });
      const body = api['createLabRequest'].mock.calls[0][0];
      expect(body).toMatchObject({ patientId: 7, consultationId: 'C1', testType: 'Créatinine, Albumin', urgency: 'STAT', notes: 'Créatinine: à jeun' });
      expect(d.labRequests.every((l) => l.backendRequestId === 'LR-1' && l.status === 'PENDING')).toBe(true);
    });

    it('submitLabRequests réutilise une demande ouverte et choisit Urgent', async () => {
      const d = draft();
      d.labRequests = [
        { test: 'CRP', urgency: 'Routine', backendRequestId: 'LR-9', status: 'IN_PROGRESS' },
        { test: 'NFS', urgency: 'Urgent' }
      ];
      await firstValueFrom(ws.submitLabRequests('C1', 7, d));
      expect(api['createLabRequest'].mock.calls[0][0].urgency).toBe('URGENT');

      api['createLabRequest'].mockClear();
      const onlyOpen = draft();
      onlyOpen.labRequests = [{ test: 'CRP', urgency: 'Routine', backendRequestId: 'LR-9' }];
      const res = await firstValueFrom(ws.submitLabRequests('C1', 7, onlyOpen));
      expect(api['createLabRequest']).not.toHaveBeenCalled();
      expect(res.requestId).toBe('LR-9');
    });

    it('submitLabRequests : échec de création → ok:false et copie locale', async () => {
      api['createLabRequest'].mockReturnValue(throwError(() => new Error('down')));
      api['updateConsultationLabRequests'].mockReturnValue(throwError(() => new Error('down')));
      const d = draft();
      d.labRequests = [{ test: 'CRP', urgency: 'Routine' }];
      await expect(firstValueFrom(ws.submitLabRequests('C3', 7, d))).resolves.toEqual({ ok: false });
      expect(localStorage.getItem('clinical_workspace_C3')).not.toBeNull();
    });

    it('serializeLabRequests calcule la clé depuis le libellé', () => {
      const out = JSON.parse(ws.serializeLabRequests([{ test: ' C3 / C4 ', urgency: 'Routine' }, { test: '', urgency: 'STAT' }]));
      expect(out).toEqual([{ key: 'c3-c4', test: 'C3 / C4', category: '', urgency: 'Routine', note: '', uploadedFileNames: [] }]);
    });

    it('mergeServerOutcome privilégie le serveur s’il est plus récent', () => {
      const local = { ...draft(), updatedAt: '2026-10-01T00:00:00Z' };
      expect(ws.mergeServerOutcome(local, null)).toBe(local);
      expect(ws.mergeServerOutcome(local, { updatedAt: '2026-09-01T00:00:00Z', notes: 'x' })).toBe(local);

      const merged = ws.mergeServerOutcome(local, {
        updatedAt: '2026-10-10T00:00:00Z',
        notes: JSON.stringify({ subjective: 'S', plan: 'P' }),
        diagnosis: JSON.stringify([{ label: 'D' }]),
        prescriptions: JSON.stringify([{ medication: 'M', dosage: '', frequency: '' }]),
        labRequests: JSON.stringify([{ test: 'T', urgency: 'Routine' }]),
        treatmentPlan: JSON.stringify({ guardianInstructions: 'G' })
      });
      expect(merged.soap).toEqual({ subjective: 'S', objective: '', assessment: '', plan: 'P' });
      expect(merged.diagnosisList).toEqual([{ label: 'D' }]);
      expect(merged.prescriptions).toHaveLength(1);
      expect(merged.labRequests).toHaveLength(1);
      expect(merged.guardianInstructions).toBe('G');
      expect(merged.treatmentPlan).toBe(' Prednisone ');
      expect(merged.updatedAt).toBe('2026-10-10T00:00:00Z');
    });

    it('mergeServerOutcome : notes en texte brut et JSON invalides ignorés', () => {
      const merged = ws.mergeServerOutcome(draft(), {
        notes: 'note libre', diagnosis: '{bad', prescriptions: '"str"', labRequests: 'null', treatmentPlan: 'not json'
      });
      expect(merged.soap.subjective).toBe('note libre');
      expect(merged.diagnosisList).toHaveLength(2);
    });

    it('mergeServerMetrics fusionne uniquement un objet', () => {
      const d = draft();
      expect(ws.mergeServerMetrics(d, null)).toBe(d);
      expect(ws.mergeServerMetrics(d, { egfr: 40 }).metrics.egfr).toBe(40);
    });
  });
});
