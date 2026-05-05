import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStorageService } from './auth-storage.service';

let sessionRedirectScheduled = false;

function isPublicApiRequest(url: string): boolean {
  return (
    url.includes('/api/users/public/doctors')
  );
}

function mapFriendlyMessage(status: number): string {
  switch (status) {
    case 0:
      return 'Unable to connect to the server. Please check your internet connection and try again.';
    case 401:
      return 'Your session has expired. You will be redirected to the login page.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 404:
      return 'The requested resource was not found.';
    case 500:
      return 'A server error occurred. Please try again in a moment.';
    default:
      return 'An unexpected error occurred. Please try again.';
  }
}

function buildFriendlyError(error: HttpErrorResponse): HttpErrorResponse {
  const payload = error?.error;
  const backendMessage =
    payload && typeof payload === 'object'
      ? (payload as Record<string, unknown>)['message']
      : undefined;
  const shouldKeepBackendMessage =
    typeof backendMessage === 'string' &&
    backendMessage.trim().length > 0 &&
    [400, 404, 409, 422].includes(error.status);
  const resolvedMessage = shouldKeepBackendMessage
    ? backendMessage as string
    : mapFriendlyMessage(error.status);
  const normalizedPayload = payload && typeof payload === 'object'
    ? { ...(payload as Record<string, unknown>), message: resolvedMessage }
    : { message: resolvedMessage };

  return new HttpErrorResponse({
    error: normalizedPayload,
    headers: error.headers,
    status: error.status,
    statusText: error.statusText,
    url: error.url ?? undefined
  });
}

export const authSessionInterceptor: HttpInterceptorFn = (req, next) => {
  const authStorage = inject(AuthStorageService);
  const router = inject(Router);
  const isLoginRequest = req.url.endsWith('/api/auth/login');

  if (isLoginRequest) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401
        && !isLoginRequest
        && !isPublicApiRequest(req.url)
        && authStorage.isAuthenticated()
      ) {
        authStorage.clear();
        authStorage.setPostLoginRedirect(router.url);
        if (!sessionRedirectScheduled && router.url !== '/login') {
          sessionRedirectScheduled = true;
          setTimeout(() => {
            void router.navigateByUrl('/login').finally(() => {
              sessionRedirectScheduled = false;
            });
          }, 2000);
        }
      }

      return throwError(() => buildFriendlyError(error));
    })
  );
};
