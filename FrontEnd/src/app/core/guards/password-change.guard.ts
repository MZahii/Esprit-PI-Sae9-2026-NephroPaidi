import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStorageService } from '../auth/auth-storage.service';

export const passwordChangeGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  if (!authStorage.isAuthenticated()) {
    authStorage.setPostLoginRedirect(state.url || router.url);
    return router.parseUrl('/login');
  }

  const mustChangePassword = authStorage.isPasswordChangeRequired();
  if (!mustChangePassword) {
    return true;
  }

  const targetUrl = state.url ?? '';
  if (targetUrl.startsWith('/backoffice/account-settings')) {
    return true;
  }

  return router.parseUrl('/backoffice/account-settings?forcePasswordChange=true');
};
