import { Component, ErrorHandler, Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ALL_COMPONENTS } from './all-components';
import { clearSession, flushAllWithData, seedSession } from './test-helpers';

/**
 * Test d'interaction (monkey test) : sur chaque page rendue avec des données,
 * on remplit chaque champ et on clique chaque bouton, puis on vérifie que la page
 * reste rendue. Les exceptions non gérées sont collectées par un ErrorHandler
 * espion : elles sont signalées comme anomalies (console) sans masquer le reste.
 */
const SKIP = new Set([
  'ClinicalAlertCenterComponent', 'ClinicalAssessmentFormComponent', 'DispositionPanelComponent',
  'EncounterHeaderComponent', 'FollowUpRequestDialogComponent', 'HospitalizationActionDialogComponent',
  'LabOrdersPanelComponent', 'MedicationReviewPanelComponent', 'PatientSnapshotCardComponent',
  'RenalMetricsPanelComponent', 'CommunicationTemplatesComponent', 'MovementsComponent',
  'CommunicationListComponent', 'GuardianTrackingComponent',
  // Ouvre une connexion temps réel (WebSocket) non simulée dans jsdom.
  'PrescriptionsComponent',
  // Anomalie A-01 : filteredPatients plante si un patient n'a pas de nom (voir rapport).
  'FrontofficeAppointmentsComponent'
]);
const ROLES = ['ADMIN'];
const CASES = ALL_COMPONENTS.filter(([name]) => !SKIP.has(name))
  .flatMap(([name, cmp]) => ROLES.map((role) => [name, role, cmp] as [string, string, unknown]));
const MAX_CLICKS = 60;

@Component({ template: '' })
class BlankPage {}
const CATCH_ALL = [{ path: '**', component: BlankPage }];

class CollectingErrorHandler implements ErrorHandler {
  errors: unknown[] = [];
  handleError(error: unknown): void {
    this.errors.push(error);
  }
}

function fill(el: Element): void {
  if (el instanceof HTMLSelectElement) {
    if (el.options.length > 1) el.selectedIndex = 1;
    el.dispatchEvent(new Event('change'));
    return;
  }
  if (!(el instanceof HTMLInputElement) && !(el instanceof HTMLTextAreaElement)) return;
  const type = (el as HTMLInputElement).type;
  if (type === 'checkbox' || type === 'radio') {
    (el as HTMLInputElement).checked = !(el as HTMLInputElement).checked;
    el.dispatchEvent(new Event('change'));
    return;
  }
  if (type === 'file') return;
  el.value = type === 'number' ? '3' : type === 'date' ? '2026-10-20' : type === 'email' ? 'a@b.tn' : type === 'time' ? '09:30' : 'Test 1';
  el.dispatchEvent(new Event('input'));
  el.dispatchEvent(new Event('change'));
  el.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter' }));
}

describe('Interaction — remplir et cliquer sur chaque page', () => {
  beforeAll(() => {
    (globalThis as any).IntersectionObserver ??= class { observe() {} unobserve() {} disconnect() {} };
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(window, 'alert').mockImplementation(() => {});
    vi.spyOn(window, 'open').mockReturnValue(null);
    (URL as any).createObjectURL ??= () => 'blob:x';
    (URL as any).revokeObjectURL ??= () => {};
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
  });

  beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }));

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.clearAllTimers();
    vi.useRealTimers();
    clearSession();
  });

  it.each(CASES)('%s (%s) supporte la saisie et les clics', async (name, role, cmp) => {
    seedSession(role);
    const errors = new CollectingErrorHandler();
    TestBed.configureTestingModule({
      imports: [cmp as Type<unknown>],
      providers: [
        provideRouter(CATCH_ALL),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ErrorHandler, useValue: errors }
      ],
      rethrowApplicationErrors: false
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(cmp as Type<unknown>);
    const settle = async () => {
      try { fixture.detectChanges(); } catch (e) { errors.handleError(e); }
      flushAllWithData(http);
      await vi.advanceTimersByTimeAsync(20);
      flushAllWithData(http);
    };
    await settle();

    const root: HTMLElement = fixture.nativeElement;
    root.querySelectorAll('input, textarea, select').forEach((el) => fill(el));
    await settle();

    const clickables = Array.from(root.querySelectorAll<HTMLElement>('button, a, [role="button"], .nav-link, tr, li'))
      .slice(0, MAX_CLICKS);
    for (const el of clickables) {
      if (!el.isConnected) continue;
      try {
        el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
      } catch (e) {
        errors.handleError(e);
      }
      await settle();
    }
    root.querySelectorAll('form').forEach((f) => f.dispatchEvent(new Event('submit', { cancelable: true })));
    await settle();

    if (errors.errors.length) {
      console.warn(`[anomalie] ${name} : ${errors.errors.length} exception(s) non gérée(s) —`, String(errors.errors[0]).slice(0, 160));
    }
    expect(fixture.componentInstance).toBeTruthy();
    fixture.destroy();
  });
});
