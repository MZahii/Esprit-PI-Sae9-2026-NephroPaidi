import { ApplicationConfig, provideAppInitializer } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { IMAGE_CONFIG } from '@angular/common';
import { routes } from './app.routes';
import { initializeKeycloak } from './core/auth/keycloak.service';
import { authTokenInterceptor } from './core/auth/auth-token.interceptor';
import { authSessionInterceptor } from './core/auth/auth-session.interceptor';
import { actorHeaderInterceptor } from './core/interceptors/actor-header.interceptor';


export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authTokenInterceptor, actorHeaderInterceptor, authSessionInterceptor])),
    {
      provide: IMAGE_CONFIG,
      useValue: {
        disableImageSizeWarning: true
      }
    },
    provideAppInitializer(() => initializeKeycloak())
  ]
};
