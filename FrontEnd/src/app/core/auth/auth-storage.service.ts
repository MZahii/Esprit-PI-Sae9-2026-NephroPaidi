import { Injectable } from '@angular/core';
import { LoginResponse } from './auth-api.service';
import { extractAppRolesFromToken, getPrimaryRoleFromToken } from './keycloak.service';

@Injectable({
  providedIn: 'root'
})
export class AuthStorageService {
  private readonly ACCESS_TOKEN_KEY = 'np_access_token';
  private readonly REFRESH_TOKEN_KEY = 'np_refresh_token';
  private readonly ROLE_KEY = 'np_role';
  private readonly REDIRECT_KEY = 'np_redirect_to';
  private readonly POST_LOGIN_REDIRECT_KEY = 'np_post_login_redirect_to';
  private readonly USER_KEY = 'np_user';
  private readonly PREF_THEME_KEY = 'np_pref_theme';
  private readonly PREF_LANGUAGE_KEY = 'np_pref_language';
  private readonly PREF_NOTIFICATIONS_KEY = 'np_pref_notifications_enabled';

  private normalizeRole(role: string | null | undefined): string | null {
    const normalized = String(role ?? '').trim().toUpperCase();
    return normalized || null;
  }

  private getStoredRole(): string | null {
    const localRole = this.normalizeRole(localStorage.getItem(this.ROLE_KEY));
    if (localRole) {
      return localRole;
    }

    return this.normalizeRole(sessionStorage.getItem(this.ROLE_KEY));
  }

  saveSession(response: LoginResponse, rememberMe: boolean = true): void {
    this.clear();

    const storage = rememberMe ? localStorage : sessionStorage;
    const derivedRole = getPrimaryRoleFromToken(response.accessToken);
    const fallbackRole = this.normalizeRole(response.role);

    storage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
    storage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
    storage.setItem(this.ROLE_KEY, derivedRole ?? fallbackRole ?? '');
    storage.setItem(this.REDIRECT_KEY, response.redirectTo);

    storage.setItem(
      this.USER_KEY,
      JSON.stringify({
        userId: response.userId,
        keycloakId: response.keycloakId,
        username: response.username,
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
        mustChangePassword: response.mustChangePassword
      })
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY)
      ?? sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY)
      ?? sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getRole(): string | null {
    const token = this.getAccessToken();
    const derived = getPrimaryRoleFromToken(token);
    const storedRole = this.getStoredRole();
    return derived ?? storedRole;
  }

  getRoles(): string[] {
    const resolvedRoles = new Set<string>(extractAppRolesFromToken(this.getAccessToken()));
    const singleRole = this.getRole();
    if (singleRole) {
      resolvedRoles.add(singleRole);
    }
    return Array.from(resolvedRoles);
  }

  hasAnyRole(expectedRoles: readonly string[]): boolean {
    const roles = this.getRoles();
    return expectedRoles.some((role) => roles.includes(role));
  }

  getRedirectTo(): string | null {
    return localStorage.getItem(this.REDIRECT_KEY)
      ?? sessionStorage.getItem(this.REDIRECT_KEY);
  }

  setPostLoginRedirect(url: string): void {
    const normalized = (url || '').trim();
    if (!normalized || normalized === '/login') {
      return;
    }

    const storage = localStorage.getItem(this.ACCESS_TOKEN_KEY) ? localStorage : sessionStorage;
    storage.setItem(this.POST_LOGIN_REDIRECT_KEY, normalized);
  }

  consumePostLoginRedirect(): string | null {
    const fromLocal = localStorage.getItem(this.POST_LOGIN_REDIRECT_KEY);
    if (fromLocal) {
      localStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);
      return fromLocal;
    }

    const fromSession = sessionStorage.getItem(this.POST_LOGIN_REDIRECT_KEY);
    if (fromSession) {
      sessionStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);
      return fromSession;
    }

    return null;
  }

  getUser(): any | null {
    const raw =
      localStorage.getItem(this.USER_KEY)
      ?? sessionStorage.getItem(this.USER_KEY);

    return raw ? JSON.parse(raw) : null;
  }

  isPasswordChangeRequired(): boolean {
    return !!this.getUser()?.mustChangePassword;
  }

  setPasswordChangeRequired(required: boolean): void {
    const user = this.getUser();
    if (!user) return;
    user.mustChangePassword = required;
    const storage = localStorage.getItem(this.USER_KEY) ? localStorage : sessionStorage;
    storage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  clear(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    localStorage.removeItem(this.REDIRECT_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.PREF_THEME_KEY);
    localStorage.removeItem(this.PREF_LANGUAGE_KEY);
    localStorage.removeItem(this.PREF_NOTIFICATIONS_KEY);
    localStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);

    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(this.ROLE_KEY);
    sessionStorage.removeItem(this.REDIRECT_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    sessionStorage.removeItem(this.PREF_THEME_KEY);
    sessionStorage.removeItem(this.PREF_LANGUAGE_KEY);
    sessionStorage.removeItem(this.PREF_NOTIFICATIONS_KEY);
    sessionStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);
  }

  setPreferences(preferences: { theme?: string; preferredLanguage?: string; notificationsEnabled?: boolean }): void {
    const storage = localStorage.getItem(this.USER_KEY) ? localStorage : sessionStorage;
    if (preferences.theme) {
      storage.setItem(this.PREF_THEME_KEY, preferences.theme);
    }
    if (preferences.preferredLanguage) {
      storage.setItem(this.PREF_LANGUAGE_KEY, preferences.preferredLanguage);
    }
    if (preferences.notificationsEnabled !== undefined && preferences.notificationsEnabled !== null) {
      storage.setItem(this.PREF_NOTIFICATIONS_KEY, String(preferences.notificationsEnabled));
    }
  }

  getPreferences(): { theme: string; preferredLanguage: string; notificationsEnabled: boolean } {
    const storage = localStorage.getItem(this.USER_KEY) ? localStorage : sessionStorage;
    const theme = storage.getItem(this.PREF_THEME_KEY) ?? 'light';
    const preferredLanguage = storage.getItem(this.PREF_LANGUAGE_KEY) ?? 'en';
    const notificationsEnabledRaw = storage.getItem(this.PREF_NOTIFICATIONS_KEY);
    const notificationsEnabled = notificationsEnabledRaw === null ? true : notificationsEnabledRaw === 'true';
    return { theme, preferredLanguage, notificationsEnabled };
  }
}
