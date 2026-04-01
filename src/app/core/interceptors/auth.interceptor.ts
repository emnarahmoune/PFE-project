import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, from, BehaviorSubject, EMPTY } from 'rxjs';  // ✅ EMPTY
import { catchError, switchMap, mergeMap, filter, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.url.includes('/auth/login') || 
        req.url.includes('/auth/register') ||
        req.url.includes('/auth/refresh') ||
        req.url.includes('/assets/') ||
        req.url.includes('keycloak')) {
      return next.handle(req);
    }

    return from(this.authService.getToken()).pipe(
      mergeMap(token => {
        let authReq = req;
        if (token) {
          authReq = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
          });
        }
        return next.handle(authReq).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status === 401) {
              return this.handle401Error(req, next);
            }
            return throwError(() => error);
          })
        );
      })
    );
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (this.isRefreshing) {
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        mergeMap(token => next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })))
      );
    }

    this.isRefreshing = true;
    this.refreshTokenSubject.next(null);

    return from(this.authService.refreshToken()).pipe(
      switchMap((refreshed: boolean) => {
        this.isRefreshing = false;
        if (refreshed) {
          return from(this.authService.getToken()).pipe(
            mergeMap((newToken: string) => {
              if (newToken) {
                this.refreshTokenSubject.next(newToken);
                return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }));
              }
              return EMPTY;
            })
          );
        }
        console.log('🔄 [PERFECT] Refresh échoué → SILENCE');
        return EMPTY;  // ✅ ZÉRO ERREUR !
      }),
      catchError(() => {
        this.isRefreshing = false;
        return EMPTY;  // ✅ ZÉRO ERREUR !
      })
    );
  }
}
