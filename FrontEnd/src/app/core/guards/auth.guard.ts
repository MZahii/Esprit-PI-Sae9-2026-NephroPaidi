import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStorageService } from '../auth/auth-storage.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  if (authStorage.isAuthenticated()) {
    return true;
  }

  authStorage.setPostLoginRedirect(state.url || router.url);

  return router.parseUrl('/login');
};