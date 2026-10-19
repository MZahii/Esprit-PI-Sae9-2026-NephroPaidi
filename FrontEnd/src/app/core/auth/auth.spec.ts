import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthStorageService } from './auth-storage.service';
import { authSessionInterceptor } from './auth-session.interceptor';
import { authTokenInterceptor } from './auth-token.interceptor';
import {
  extractAppRolesFromToken,
  extractRolesFromToken,
  getLandingRouteByRole,
  getPrimaryRoleFromToken,
  getUserRoles,
  getValidToken,
  hasAnyRole,
  initializeKeycloak,
  isAuthenticated
} from './keycloak.service';
import { authGuard } from '../guards/auth.guard';
import { passwordChangeGuard } from '../guards/password-change.guard';
import { roleGuard } from '../guards/role.guard';
import { actorHeaderInterceptor } from '../interceptors/actor-header.interceptor';
import { clearSession, fakeJwt, seedSession } from '../../testing/test-helpers';

const API = 'http://localhost:8083';

describe('E1 — Authentification et contrôle d’accès', () => {
  afterEach(() => {
    clearSession();
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('Décodage du JWT Keycloak', () => {
    it('extrait et normalise les rôles realm et client', () => {
      const token = fakeJwt(['role_doctor', 'offline_access'], {
        resource_access: { 'nephro-front': { roles: ['NURSE'] }, other: {} }
      });
      expect(extractRolesFromToken(token).sort()).toEqual(['DOCTOR', 'NURSE', 'OFFLINE_ACCESS']);
      expect(extractAppRolesFromToken(token).sort()).toEqual(['DOCTOR', 'NURSE']);
    });

    it('retourne une liste vide pour un jeton absent ou illisible', () => {
      expect(extractRolesFromToken(null)).toEqual([]);
      expect(extractRolesFromToken('pas-un-jwt')).toEqual([]);
      expect(extractRolesFromToken('a.%%%.c')).toEqual([]);
    });

    it('choisit le rôle principal selon la priorité ADMIN > HR > DOCTOR…', () => {
      expect(getPrimaryRoleFromToken(fakeJwt(['GUARDIAN', 'DOCTOR']))).toBe('DOCTOR');
      expect(getPrimaryRoleFromToken(fakeJwt(['NURSE', 'ADMIN']))).toBe('ADMIN');
      expect(getPrimaryRoleFromToken(fakeJwt(['uma_authorization']))).toBeNull();
    });
  });

  describe('Session et redirection par rôle', () => {
    it('non authentifié → /login', () => {
      expect(isAuthenticated()).toBe(false);
      expect(getUserRoles()).toEqual([]);
      expect(getLandingRouteByRole()).toBe('/login');
    });

    it.each([
      ['PHARMACIST', '/backoffice/pharmacy/dashboard'],
      ['DOCTOR', '/backoffice/dashboard'],
      ['ADMIN', '/backoffice/dashboard'],
      ['GUARDIAN', '/frontoffice/home']
    ])('rôle %s → %s', (role, route) => {
      seedSession(role);
      expect(isAuthenticated()).toBe(true);
      expect(hasAnyRole([role])).toBe(true);
      expect(getLandingRouteByRole()).toBe(route);
    });

    it('se replie sur le rôle stocké si le jeton ne porte aucun rôle applicatif', () => {
      localStorage.setItem('np_access_token', fakeJwt([]));
      localStorage.setItem('np_role', 'role_nurse');
      expect(getUserRoles()).toEqual(['NURSE']);
    });

    it('initializeKeycloak résout à true', async () => {
      await expect(initializeKeycloak()).resolves.toBe(true);
    });
  });

  describe('getValidToken (rafraîchissement)', () => {
    it('rejette sans jeton', async () => {
      await expect(getValidToken()).rejects.toThrow(/Access token not found/);
    });

    it('renvoie le jeton encore valide sans appel réseau', async () => {
      seedSession('DOCTOR');
      const fetchSpy = vi.spyOn(globalThis, 'fetch');
      await expect(getValidToken()).resolves.toBe(localStorage.getItem('np_access_token'));
      expect(fetchSpy).not.toHaveBeenCalled();
    });

    it('rafraîchit un jeton expiré et met à jour le rôle', async () => {
      localStorage.setItem('np_access_token', fakeJwt(['DOCTOR'], { exp: 1 }));
      localStorage.setItem('np_refresh_token', 'r1');
      const fresh = fakeJwt(['NURSE']);
      vi.spyOn(globalThis, 'fetch').mockResolvedValue(
        new Response(JSON.stringify({ accessToken: fresh, refreshToken: 'r2' }), { status: 200 })
      );
      await expect(getValidToken()).resolves.toBe(fresh);
      expect(localStorage.getItem('np_refresh_token')).toBe('r2');
      expect(localStorage.getItem('np_role')).toBe('NURSE');
    });

    it('vide la session si le refresh est refusé', async () => {
      localStorage.setItem('np_access_token', fakeJwt(['DOCTOR'], { exp: 1 }));
      localStorage.setItem('np_refresh_token', 'r1');
      vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{}', { status: 401 }));
      await expect(getValidToken()).rejects.toThrow(/Session expired/);
      expect(localStorage.getItem('np_access_token')).toBeNull();
    });

    it('vide la session si la réponse de refresh est incomplète', async () => {
      localStorage.setItem('np_access_token', fakeJwt(['DOCTOR'], { exp: 1 }));
      localStorage.setItem('np_refresh_token', 'r1');
      vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"accessToken":"x"}', { status: 200 }));
      await expect(getValidToken()).rejects.toThrow(/Session expired/);
    });

    it('échoue sans refresh token', async () => {
      sessionStorage.setItem('np_access_token', fakeJwt(['DOCTOR'], { exp: 1 }));
      await expect(getValidToken()).rejects.toThrow(/Session expired/);
    });
  });

  describe('AuthStorageService', () => {
    let store: AuthStorageService;
    const login = {
      accessToken: fakeJwt(['HR']),
      refreshToken: 'rt',
      role: 'hr',
      redirectTo: '/backoffice/dashboard',
      userId: 7,
      keycloakId: 'kc',
      username: 'hr.user',
      email: 'hr@x.tn',
      firstName: 'H',
      lastName: 'R',
      mustChangePassword: true
    } as any;

    beforeEach(() => (store = TestBed.inject(AuthStorageService)));

    it('« Se souvenir de moi » → localStorage, sinon sessionStorage', () => {
      store.saveSession(login, true);
      expect(localStorage.getItem('np_access_token')).toBe(login.accessToken);
      store.saveSession(login, false);
      expect(localStorage.getItem('np_access_token')).toBeNull();
      expect(sessionStorage.getItem('np_access_token')).toBe(login.accessToken);
      expect(store.getRefreshToken()).toBe('rt');
      expect(store.getRole()).toBe('HR');
      expect(store.getRoles()).toContain('HR');
      expect(store.getRedirectTo()).toBe('/backoffice/dashboard');
    });

    it('gère le changement de mot de passe obligatoire', () => {
      expect(store.isPasswordChangeRequired()).toBe(false);
      store.setPasswordChangeRequired(true); // sans utilisateur : sans effet
      store.saveSession(login);
      expect(store.isPasswordChangeRequired()).toBe(true);
      store.setPasswordChangeRequired(false);
      expect(store.getUser().mustChangePassword).toBe(false);
    });

    it('mémorise puis consomme la redirection post-login', () => {
      store.setPostLoginRedirect('/login');
      store.setPostLoginRedirect('   ');
      expect(store.consumePostLoginRedirect()).toBeNull();
      store.setPostLoginRedirect('/backoffice/patients');
      expect(store.consumePostLoginRedirect()).toBe('/backoffice/patients');
      expect(store.consumePostLoginRedirect()).toBeNull();
      store.saveSession(login);
      store.setPostLoginRedirect('/backoffice/logs');
      expect(store.consumePostLoginRedirect()).toBe('/backoffice/logs');
    });

    it('stocke les préférences avec des valeurs par défaut', () => {
      expect(store.getPreferences()).toEqual({ theme: 'light', preferredLanguage: 'en', notificationsEnabled: true });
      store.saveSession(login);
      store.setPreferences({ theme: 'dark', preferredLanguage: 'fr', notificationsEnabled: false });
      expect(store.getPreferences()).toEqual({ theme: 'dark', preferredLanguage: 'fr', notificationsEnabled: false });
      store.clear();
      expect(store.isAuthenticated()).toBe(false);
    });
  });

  describe('Guards de routage', () => {
    const state = (url: string) => ({ url }) as RouterStateSnapshot;
    const route = (roles?: string[]) => ({ data: roles ? { roles } : {} }) as unknown as ActivatedRouteSnapshot;
    const run = (guard: any, r: ActivatedRouteSnapshot, s: RouterStateSnapshot) =>
      TestBed.runInInjectionContext(() => guard(r, s));

    beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

    const asUrl = (v: unknown) => (v instanceof UrlTree ? TestBed.inject(Router).serializeUrl(v) : v);

    it('authGuard : refuse et mémorise la cible si non connecté', () => {
      expect(asUrl(run(authGuard, route(), state('/backoffice/patients')))).toBe('/login');
      seedSession('DOCTOR');
      expect(run(authGuard, route(), state('/backoffice/patients'))).toBe(true);
    });

    it('roleGuard : autorise uniquement les rôles déclarés sur la route', () => {
      expect(asUrl(run(roleGuard, route(['ADMIN']), state('/backoffice/logs')))).toBe('/login');
      seedSession('NURSE');
      expect(run(roleGuard, route(), state('/x'))).toBe(true);
      expect(run(roleGuard, route(['NURSE', 'DOCTOR']), state('/x'))).toBe(true);
      expect(asUrl(run(roleGuard, route(['ADMIN']), state('/backoffice/logs')))).toBe('/login');
    });

    it('passwordChangeGuard : force la page de changement de mot de passe', () => {
      expect(asUrl(run(passwordChangeGuard, route(), state('/backoffice')))).toBe('/login');
      seedSession('HR');
      expect(run(passwordChangeGuard, route(), state('/backoffice'))).toBe(true);
      const user = JSON.parse(localStorage.getItem('np_user')!);
      localStorage.setItem('np_user', JSON.stringify({ ...user, mustChangePassword: true }));
      expect(run(passwordChangeGuard, route(), state('/backoffice/account-settings'))).toBe(true);
      expect(asUrl(run(passwordChangeGuard, route(), state('/backoffice/dashboard'))))
        .toBe('/backoffice/account-settings?forcePasswordChange=true');
    });
  });

  describe('Intercepteurs HTTP', () => {
    let http: HttpClient;
    let ctrl: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(withInterceptors([authTokenInterceptor, actorHeaderInterceptor, authSessionInterceptor])),
          provideHttpClientTesting()
        ]
      });
      http = TestBed.inject(HttpClient);
      ctrl = TestBed.inject(HttpTestingController);
    });

    it('ajoute Bearer et en-têtes X-Actor sur les appels API', async () => {
      seedSession('DOCTOR');
      const p = firstValueFrom(http.get(`${API}/api/patients`));
      const req = await vi.waitFor(() => ctrl.expectOne(`${API}/api/patients`));
      expect(req.request.headers.get('Authorization')).toMatch(/^Bearer /);
      expect(req.request.headers.get('X-Actor-Username')).toBe('test.user');
      expect(req.request.headers.get('X-Actor-Role')).toBe('DOCTOR');
      req.flush([]);
      await expect(p).resolves.toEqual([]);
    });

    it('n’ajoute rien sur /api/auth/login ni hors API', () => {
      seedSession('DOCTOR');
      http.post(`${API}/api/auth/login`, {}).subscribe();
      http.get('https://api.fda.gov/drug/label.json').subscribe();
      const login = ctrl.expectOne(`${API}/api/auth/login`);
      expect(login.request.headers.has('Authorization')).toBe(false);
      const ext = ctrl.expectOne('https://api.fda.gov/drug/label.json');
      expect(ext.request.headers.has('X-Actor-Username')).toBe(false);
    });

    it('sans jeton : requête envoyée sans Authorization', () => {
      http.get(`${API}/api/users/public/doctors`).subscribe();
      const req = ctrl.expectOne(`${API}/api/users/public/doctors`);
      expect(req.request.headers.has('Authorization')).toBe(false);
    });

    it.each([
      [0, 'Unable to connect'],
      [401, 'not authorized'],
      [403, 'permission'],
      [404, 'not found'],
      [500, 'server error'],
      [418, 'unexpected error']
    ])('traduit l’erreur HTTP %i en message lisible', async (status, msg) => {
      const p = firstValueFrom(http.get('/public/x'));
      ctrl.expectOne('/public/x').flush('boom', { status, statusText: 'E' });
      const err = (await p.catch((e) => e)) as HttpErrorResponse;
      expect(err.error.message).toContain(msg);
    });

    it('conserve le message métier du backend pour 400/404/409/422', async () => {
      const p = firstValueFrom(http.get('/public/y'));
      ctrl.expectOne('/public/y').flush({ message: 'Patient déjà existant', code: 'DUP' }, { status: 409, statusText: 'Conflict' });
      const err = (await p.catch((e) => e)) as HttpErrorResponse;
      expect(err.error).toEqual({ message: 'Patient déjà existant', code: 'DUP' });
    });

    it('jeton expiré sans refresh → 401 « session expirée »', async () => {
      localStorage.setItem('np_access_token', fakeJwt(['DOCTOR'], { exp: 1 }));
      const err = (await firstValueFrom(http.get(`${API}/api/patients`)).catch((e) => e)) as HttpErrorResponse;
      expect(err.status).toBe(401);
    });
  });
});
