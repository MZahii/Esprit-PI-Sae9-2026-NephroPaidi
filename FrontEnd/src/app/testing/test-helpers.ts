import { HttpTestingController, TestRequest } from '@angular/common/http/testing';

/** Encode a fake (unsigned) JWT carrying the given realm roles. */
export function fakeJwt(roles: string[], extra: Record<string, unknown> = {}): string {
  const b64 = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  const payload = {
    sub: 'kc-123',
    preferred_username: 'test.user',
    exp: Math.floor(Date.now() / 1000) + 3600,
    realm_access: { roles },
    ...extra
  };
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64(payload)}.sig`;
}

/** Store a logged-in session the way AuthStorageService does. */
export function seedSession(role = 'ADMIN'): void {
  localStorage.setItem('np_access_token', fakeJwt([role]));
  localStorage.setItem('np_refresh_token', 'refresh-token');
  localStorage.setItem('np_role', role);
  localStorage.setItem('np_redirect_to', '/backoffice');
  localStorage.setItem(
    'np_user',
    JSON.stringify({
      userId: 1,
      keycloakId: 'kc-123',
      username: 'test.user',
      email: 'test@nephropaidi.tn',
      firstName: 'Test',
      lastName: 'User',
      mustChangePassword: false
    })
  );
}

export function clearSession(): void {
  localStorage.clear();
  sessionStorage.clear();
}

/** Answer every pending HTTP call: GET → empty list, writes → empty object. */
export function flushAll(http: HttpTestingController, rounds = 3): TestRequest[] {
  const all: TestRequest[] = [];
  for (let i = 0; i < rounds; i++) {
    const pending = http.match(() => true);
    if (pending.length === 0) break;
    for (const req of pending) {
      if (req.cancelled) continue;
      const type = req.request.responseType;
      if (type === 'blob') req.flush(new Blob(['x']));
      else if (type === 'text') req.flush('');
      else if (type === 'arraybuffer') req.flush(new ArrayBuffer(1));
      else req.flush(req.request.method === 'GET' ? [] : {});
      all.push(req);
    }
  }
  return all;
}

/** Enregistrement « type » réutilisé par les jeux de données des tests de fumée. */
export function sampleRecord(id = 1): Record<string, unknown> {
  const now = '2026-10-19T09:30:00';
  return {
    id, userId: id, patientId: id, doctorId: id, staffId: id, consultationId: `C-${id}`,
    firstName: 'Yassine', lastName: 'Ben Salah', fullName: 'Yassine Ben Salah', name: 'Item ' + id,
    username: 'y.bensalah', email: 'y@nephropaidi.tn', phone: '+21620000000', role: 'DOCTOR', roles: ['DOCTOR'],
    status: 'PENDING', type: 'GENERAL', priority: 'HIGH', urgency: 'Routine', title: 'Titre', description: 'Desc',
    message: 'Bonjour', content: 'Contenu', subject: 'Sujet', notes: 'Notes', reason: 'Contrôle',
    createdAt: now, updatedAt: now, date: '2026-10-19', startTime: now, endTime: now, scheduledAt: now,
    appointmentDate: '2026-10-19', dateTime: now, timestamp: now, expirationDate: '2026-12-31',
    quantity: 5, quantityAvailable: 5, count: 3, total: 10, amount: 2, price: 1.5, egfr: 58, active: true, enabled: true,
    messages: [], participants: [], attachments: [], tags: [], recipients: [], variables: [], channels: [], replies: [], members: []
  };
}

/** Réponse polymorphe : tableau de 2 enregistrements, aussi exposé sous items/content/data. */
export function sampleResponse(): Record<string, unknown>[] {
  const list: any = [sampleRecord(1), { ...sampleRecord(2), status: 'COMPLETED' }];
  Object.assign(list, { items: list, content: list, data: list, results: list, totalElements: 2, totalPages: 1, page: 0 });
  return list;
}

export function flushAllWithData(http: HttpTestingController, rounds = 3): void {
  for (let i = 0; i < rounds; i++) {
    const pending = http.match(() => true);
    if (pending.length === 0) break;
    for (const req of pending) {
      if (req.cancelled) continue;
      const type = req.request.responseType;
      if (type === 'blob') req.flush(new Blob(['x']));
      else if (type === 'text') req.flush('ok');
      else if (type === 'arraybuffer') req.flush(new ArrayBuffer(1));
      else req.flush(req.request.method === 'GET' ? sampleResponse() : sampleRecord(9));
    }
  }
}
