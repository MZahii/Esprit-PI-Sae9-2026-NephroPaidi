import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStorageService } from '../auth/auth-storage.service';

export const actorHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('/api/')) {
    return next(req);
  }

  const authStorage = inject(AuthStorageService);
  const user = authStorage.getUser();
  const role = authStorage.getRole() ?? '';

  if (!user?.username) {
    return next(req);
  }

  const cloned = req.clone({
    setHeaders: {
      'X-Actor-Username': String(user.username),
      'X-Actor-Id': String(user.userId ?? ''),
      'X-Actor-Role': String(role)
    }
  });

  return next(cloned);
};
