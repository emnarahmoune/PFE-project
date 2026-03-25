import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { KeycloakInitService } from '../services/keycloak-init.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakInitService);

  // Ne pas intercepter les requêtes vers Keycloak lui-même
  if (req.url.includes('localhost:8083')) {
    return next(req);
  }

  return from(keycloakService.getToken()).pipe(
    switchMap(token => {
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      }
      return next(req);
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.warn('🔒 Token expiré, redirection vers Keycloak');
        keycloakService.login();
      }
      return throwError(() => error);
    })
  );
};