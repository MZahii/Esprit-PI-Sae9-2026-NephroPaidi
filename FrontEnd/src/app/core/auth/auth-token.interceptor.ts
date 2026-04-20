import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStorageService } from './auth-storage.service';
import { getValidToken } from './keycloak.service';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authStorage = inject(AuthStorageService);

  const isApiRequest = req.url.startsWith(environment.apiBaseUrl);
  const isLoginRequest = req.url.endsWith('/api/auth/login');

  if (!isApiRequest || isLoginRequest) {
    return next(req);
  }

  const storedToken = authStorage.getAccessToken();
  if (!storedToken || !storedToken.trim()) {
    return next(req);
  }

  return from(getValidToken()).pipe(
    catchError(() => {
      return throwError(() => new HttpErrorResponse({
        error: { message: 'Your session has expired. Please login again.' },
        status: 401,
        statusText: 'Unauthorized',
        url: req.url
      }));
    }),
    switchMap((token) => {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      return next(authReq);
    })
  );
};
