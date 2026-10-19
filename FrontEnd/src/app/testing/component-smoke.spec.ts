import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ALL_COMPONENTS } from './all-components';
import { clearSession, flushAll, flushAllWithData, seedSession } from './test-helpers';

/**
 * Smoke test (test de fumée) : chaque composant standalone doit pouvoir
 * être instancié, rendre son template et survivre à des réponses API vides.
 * C'est le premier filet de non-régression avant les tests unitaires ciblés.
 */
// Composants de présentation à entrées obligatoires : testés dans leur propre spec.
const WITH_REQUIRED_INPUTS = new Set([
  'ClinicalAlertCenterComponent', 'ClinicalAssessmentFormComponent', 'DispositionPanelComponent',
  'EncounterHeaderComponent', 'FollowUpRequestDialogComponent', 'HospitalizationActionDialogComponent',
  'LabOrdersPanelComponent', 'MedicationReviewPanelComponent', 'PatientSnapshotCardComponent',
  'RenalMetricsPanelComponent'
]);
const PAGES = ALL_COMPONENTS.filter(([name]) => !WITH_REQUIRED_INPUTS.has(name));
// Pages dont le modèle de réponse est trop spécifique pour le jeu de données générique.
const NEEDS_TYPED_FIXTURE = new Set([
  'CommunicationTemplatesComponent', 'MovementsComponent', 'CommunicationListComponent', 'GuardianTrackingComponent'
]);
const PAGES_WITH_DATA = PAGES.filter(([name]) => !NEEDS_TYPED_FIXTURE.has(name));

class FakeIntersectionObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() { return []; }
}

describe('Smoke — tous les composants', () => {
  beforeAll(() => {
    (globalThis as any).IntersectionObserver ??= FakeIntersectionObserver;
  });

  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    seedSession('ADMIN');
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.clearAllTimers();
    vi.useRealTimers();
    clearSession();
  });

  it.each(PAGES)('%s se crée et rend son template', async (_name, cmp) => {
    TestBed.configureTestingModule({
      imports: [cmp as Type<unknown>],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(cmp as Type<unknown>);
    fixture.detectChanges();
    flushAll(http);
    await vi.advanceTimersByTimeAsync(50);
    flushAll(http);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement).toBeInstanceOf(HTMLElement);
    fixture.destroy();
  });

  it.each(PAGES_WITH_DATA)('%s rend un jeu de données non vide', async (_name, cmp) => {
    TestBed.configureTestingModule({
      imports: [cmp as Type<unknown>],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(cmp as Type<unknown>);
    fixture.detectChanges();
    flushAllWithData(http);
    await vi.advanceTimersByTimeAsync(50);
    flushAllWithData(http);
    fixture.detectChanges();
    expect(fixture.nativeElement.innerHTML.length).toBeGreaterThan(0);
    fixture.destroy();
  });

  const ROLES = ['DOCTOR', 'NURSE', 'HR', 'PHARMACIST', 'GUARDIAN'];
  const PAGE_ROLE_CASES = PAGES_WITH_DATA.flatMap(([name, cmp]) =>
    ROLES.map((role) => [name, role, cmp] as [string, string, unknown]));

  it.each(PAGE_ROLE_CASES)('%s reste utilisable pour le rôle %s', async (_name, role, cmp) => {
    clearSession();
    seedSession(role);
    TestBed.configureTestingModule({
      imports: [cmp as Type<unknown>],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(cmp as Type<unknown>);
    fixture.detectChanges();
    flushAllWithData(http);
    await vi.advanceTimersByTimeAsync(50);
    flushAllWithData(http);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
    fixture.destroy();
  });
});
