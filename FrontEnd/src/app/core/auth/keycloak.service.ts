import { environment } from '../../../environments/environment';

const ACCESS_TOKEN_KEY = 'np_access_token';
const REFRESH_TOKEN_KEY = 'np_refresh_token';
const ROLE_KEY = 'np_role';
const REDIRECT_KEY = 'np_redirect_to';
const USER_KEY = 'np_user';
const API_BASE_URL = environment.apiBaseUrl;

let refreshInFlight: Promise<string> | null = null;

type JwtPayload = {
  exp?: number;
  realm_access?: { roles?: unknown };
  resource_access?: Record<string, { roles?: unknown }>;
};

export const APP_ROLE_PRIORITY = [
  'ADMIN',
  'HR',
  'DOCTOR',
  'NURSE',
  'SURGEON',
  'PHARMACIST',
  'RECEPTIONIST',
  'LAB_AGENT',
  'GUARDIAN'
] as const;

export const STAFF_BACKOFFICE_ROLES = APP_ROLE_PRIORITY.filter((role) => role !== 'GUARDIAN');

const APP_ROLE_SET = new Set<string>(APP_ROLE_PRIORITY);

function readFromStorage(key: string): string | null {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key);
}

function getTokenStorageType(): 'local' | 'session' {
  if (localStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(REFRESH_TOKEN_KEY)) {
    return 'local';
  }
  return 'session';
}

function writeTokenPair(accessToken: string, refreshToken: string): void {
  const storageType = getTokenStorageType();
  const storage = storageType === 'local' ? localStorage : sessionStorage;
  storage.setItem(ACCESS_TOKEN_KEY, accessToken);
  storage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

function parseJwtPayload(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;

    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - (base64.length % 4 || 4)) % 4, '=');
    const json = atob(padded);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function normalizeRole(role: unknown): string | null {
  if (role == null) return null;
  let normalized = String(role).trim().toUpperCase();
  if (normalized.startsWith('ROLE_')) {
    normalized = normalized.slice(5);
  }
  return normalized || null;
}

function getStoredRole(): string | null {
  return normalizeRole(readFromStorage(ROLE_KEY));
}

function isAppRole(role: string): boolean {
  return APP_ROLE_SET.has(role);
}

export function extractRolesFromToken(token: string | null | undefined): string[] {
  if (!token) return [];

  const payload = parseJwtPayload(token);
  if (!payload) return [];

  const roles = new Set<string>();
  const appendRoles = (candidate: unknown) => {
    if (!Array.isArray(candidate)) return;
    for (const role of candidate) {
      const normalized = normalizeRole(role);
      if (normalized) {
        roles.add(normalized);
      }
    }
  };

  appendRoles(payload.realm_access?.roles);

  if (payload.resource_access && typeof payload.resource_access === 'object') {
    for (const value of Object.values(payload.resource_access)) {
      appendRoles(value?.roles);
    }
  }

  return Array.from(roles);
}

export function extractAppRolesFromToken(token: string | null | undefined): string[] {
  return extractRolesFromToken(token).filter((role) => isAppRole(role));
}

export function getPrimaryRoleFromToken(token: string | null | undefined): string | null {
  const roles = extractAppRolesFromToken(token);
  for (const role of APP_ROLE_PRIORITY) {
    if (roles.includes(role)) {
      return role;
    }
  }

  return null;
}

function clearSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(REDIRECT_KEY);
  localStorage.removeItem(USER_KEY);

  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(ROLE_KEY);
  sessionStorage.removeItem(REDIRECT_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export const keycloak = {
  async login(_options?: { redirectUri?: string }): Promise<void> {
    window.location.href = `${window.location.origin}/login`;
  },

  async logout(options?: { redirectUri?: string }): Promise<void> {
    clearSession();
    const target = options?.redirectUri ?? `${window.location.origin}/login`;
    window.location.href = target;
  }
};

export async function initializeKeycloak(): Promise<boolean> {
  return true;
}

export async function login(): Promise<void> {
  await keycloak.login();
}

export async function logout(): Promise<void> {
  await keycloak.logout();
}

export function isAuthenticated(): boolean {
  return !!readFromStorage(ACCESS_TOKEN_KEY);
}

export function getUserRoles(): string[] {
  const token = readFromStorage(ACCESS_TOKEN_KEY);
  const rolesFromToken = extractAppRolesFromToken(token);
  const storedRole = getStoredRole();
  if (rolesFromToken.length > 0) {
    return rolesFromToken;
  }

  return storedRole ? [storedRole] : [];
}

export function hasAnyRole(expectedRoles: readonly string[]): boolean {
  const roles = getUserRoles();
  return expectedRoles.some(role => roles.includes(role));
}

export function getLandingRouteByRole(): string {
  const roles = getUserRoles();
  if (roles.includes('PHARMACIST')) {
    return '/backoffice/pharmacy/dashboard';
  }

  if (roles.some((role) => STAFF_BACKOFFICE_ROLES.includes(role as typeof STAFF_BACKOFFICE_ROLES[number]))) {
    return '/backoffice/dashboard';
  }

  if (roles.includes('GUARDIAN')) {
    return '/frontoffice/home';
  }

  return '/login';
}

async function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  const currentRefreshToken = readFromStorage(REFRESH_TOKEN_KEY);
  if (!currentRefreshToken) {
    clearSession();
    throw new Error('Session expired. Please login again.');
  }

  refreshInFlight = (async () => {
    const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken: currentRefreshToken })
    });

    if (!response.ok) {
      clearSession();
      throw new Error('Session expired. Please login again.');
    }

    const data = await response.json() as {
      accessToken?: string;
      refreshToken?: string;
    };

    if (!data.accessToken || !data.refreshToken) {
      clearSession();
      throw new Error('Session expired. Please login again.');
    }

    writeTokenPair(data.accessToken, data.refreshToken);
    const storageType = getTokenStorageType();
    const storage = storageType === 'local' ? localStorage : sessionStorage;
    const derivedRole = getPrimaryRoleFromToken(data.accessToken);
    if (derivedRole) {
      storage.setItem(ROLE_KEY, derivedRole);
    }
    return data.accessToken;
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

export async function getValidToken(): Promise<string> {
  let token = readFromStorage(ACCESS_TOKEN_KEY);

  if (!token) {
    throw new Error('Access token not found. Please login again.');
  }

  let payload = parseJwtPayload(token);
  const nowSeconds = Math.floor(Date.now() / 1000);

  if (!payload?.exp || payload.exp <= nowSeconds) {
    token = await refreshAccessToken();
    payload = parseJwtPayload(token);
    if (!payload?.exp || payload.exp <= nowSeconds) {
      clearSession();
      throw new Error('Session expired. Please login again.');
    }
  }

  return token;
}
