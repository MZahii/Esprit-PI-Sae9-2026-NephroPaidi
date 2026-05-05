import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthStorageService } from '../auth/auth-storage.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state) => {
  const router = inject(Router);
  const authStorage = inject(AuthStorageService);

  if (!authStorage.isAuthenticated()) {
    authStorage.setPostLoginRedirect(state.url || router.url);
    return router.parseUrl('/login');
  }

  const allowedRoles = route.data?.['roles'] as string[] | undefined;

  if (!allowedRoles || allowedRoles.length === 0) {
    return true;
  }

  if (authStorage.hasAnyRole(allowedRoles)) {
    return true;
  }

  authStorage.setPostLoginRedirect(state.url || router.url);
  return router.parseUrl('/login');
};
