import { ApplicationConfig, APP_INITIALIZER, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { KeycloakService } from 'keycloak-angular';
import { KeycloakInitService } from './core/services/keycloak-init.service';

// ── Fix NG0701 : enregistrement de la locale française ──────────────────────
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
registerLocaleData(localeFr, 'fr');
// ───────────────────────────────────────────────────────────────────────────

export function initializeKeycloak(keycloak: KeycloakInitService): () => Promise<boolean> {
  return () => keycloak.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    provideAnimations(),
    KeycloakService,
    KeycloakInitService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakInitService]
    },
    // Locale par défaut → résout NG0701 sur les pipes date / currency / number
    { provide: LOCALE_ID, useValue: 'fr' }
  ]
};