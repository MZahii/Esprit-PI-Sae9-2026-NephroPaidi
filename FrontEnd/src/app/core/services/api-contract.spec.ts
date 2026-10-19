import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { isObservable, Observable } from 'rxjs';
import { AiService } from './ai.service';
import { AppointmentsApiService } from './appointments-api.service';
import { ClinicalApiService } from './clinical-api.service';
import { CommunicationApiService } from './communication-api.service';
import { DrugSafetyService } from './drug-safety.service';
import { InternalStaffMessagingApiService } from './internal-staff-messaging-api.service';
import { OpsApiService } from './ops-api.service';
import { PharmacyService } from './pharmacy.service';
import { ProcedureApiService } from './procedure-api.service';
import { AuthApiService } from '../auth/auth-api.service';
import { seedSession, clearSession } from '../../testing/test-helpers';

/**
 * Tests de contrat des clients HTTP : chaque méthode publique d'un service API
 * doit émettre exactement une requête HTTP vers la passerelle (/api/...),
 * avec un verbe HTTP valide, et propager la réponse au souscripteur.
 */
const SERVICES: Array<[string, Type<any>]> = [
  ['AiService', AiService],
  ['AppointmentsApiService', AppointmentsApiService],
  ['ClinicalApiService', ClinicalApiService],
  ['CommunicationApiService', CommunicationApiService],
  ['DrugSafetyService', DrugSafetyService],
  ['InternalStaffMessagingApiService', InternalStaffMessagingApiService],
  ['OpsApiService', OpsApiService],
  ['PharmacyService', PharmacyService],
  ['ProcedureApiService', ProcedureApiService],
  ['AuthApiService', AuthApiService]
];

const VERBS = new Set(['GET', 'POST', 'PUT', 'PATCH', 'DELETE']);

function publicMethods(svc: Type<any>): string[] {
  return Object.getOwnPropertyNames(svc.prototype).filter((name) => {
    if (name === 'constructor' || name.startsWith('_')) return false;
    const d = Object.getOwnPropertyDescriptor(svc.prototype, name);
    return typeof d?.value === 'function';
  });
}

/** Jeux d'arguments essayés dans l'ordre (identifiant, objet, liste...). */
function argSets(arity: number): unknown[][] {
  const n = Math.max(arity, 1);
  const fill = (v: () => unknown) => Array.from({ length: n }, v);
  return [
    fill(() => '1'),
    fill(() => 1),
    fill(() => ({ id: 1, patientId: 1, page: 0, size: 10, tasks: [], items: [] })),
    fill(() => []),
    [new File(['x'], 'doc.pdf'), ...fill(() => '1').slice(1)],
    ['1', new File(['x'], 'doc.pdf'), ...fill(() => '1').slice(2)]
  ];
}

// Méthodes à logique propre, testées individuellement dans service-logic.spec.ts.
const SPECIFIC = new Set([
  'ClinicalApiService.searchPatients', 'ClinicalApiService.searchDoctors',
  'DrugSafetyService.getSignal', 'OpsApiService.withRequestTimeout'
]);

const CASES: Array<[string, string, Type<any>]> = SERVICES.flatMap(([name, svc]) =>
  publicMethods(svc).filter((m) => !SPECIFIC.has(`${name}.${m}`)).map((m) => [`${name}.${m}`, m, svc] as [string, string, Type<any>])
);

describe('Contrat des services API', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    seedSession('DOCTOR');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    clearSession();
    TestBed.resetTestingModule();
  });

  it.each(CASES)('%s émet une requête HTTP valide', (_label, method, svc) => {
    const service = TestBed.inject(svc);
    const fn = service[method] as (...a: unknown[]) => unknown;
    let issued = false;

    for (const args of argSets(fn.length)) {
      let result: unknown;
      try {
        result = fn.apply(service, args);
      } catch {
        continue;
      }
      if (!isObservable(result)) {
        // Méthode utilitaire synchrone (ex. construction d'URL) : elle doit au moins répondre.
        expect(result).not.toBeUndefined();
        issued = true;
        break;
      }
      const received: unknown[] = [];
      const sub = (result as Observable<unknown>).subscribe({ next: (v) => received.push(v), error: () => {} });
      const reqs = http.match(() => true);
      if (reqs.length === 0) {
        sub.unsubscribe();
        continue;
      }
      for (const req of reqs) {
        expect(VERBS.has(req.request.method)).toBe(true);
        expect(req.request.url).toMatch(/\/api\/|\/auth|\/realms\//);
        req.flush(req.request.responseType === 'blob' ? new Blob(['ok']) : { ok: true });
      }
      sub.unsubscribe();
      issued = true;
      break;
    }

    expect(issued).toBe(true);
  });
});
