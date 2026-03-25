import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class KeycloakInitService {
  private initialized = false;

  constructor(private keycloak: KeycloakService) {}

  async init(): Promise<boolean> {
    if (this.initialized) return true;
    
    try {
      const authenticated = await this.keycloak.init({
        config: {
          url: 'http://localhost:8083',
          realm: 'rh-platform',
          clientId: 'rh-frontend'
        },
        initOptions: {
          onLoad: 'check-sso',
          silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html',
          checkLoginIframe: false,
          pkceMethod: 'S256'
        },
        enableBearerInterceptor: true,
        bearerExcludedUrls: ['/assets']
      });
      
      this.initialized = true;
      console.log('✅ Keycloak initialisé:', authenticated);
      return authenticated;
    } catch (error) {
      console.error('❌ Keycloak init failed:', error);
      return false;
    }
  }

  async getToken(): Promise<string> {
    try {
      return await this.keycloak.getToken();
    } catch (error) {
      console.warn('⚠️ Impossible d\'obtenir le token:', error);
      return '';
    }
  }

  login(redirectUrl?: string): void {
    this.keycloak.login({
      redirectUri: window.location.origin + (redirectUrl || '/admin/dashboard')
    });
  }

  logout(): void {
    this.keycloak.logout(window.location.origin + '/auth/login');
  }

  async isLoggedIn(): Promise<boolean> {
    try {
      return await this.keycloak.isLoggedIn();
    } catch {
      return false;
    }
  }

  getUserRoles(): string[] {
    return this.keycloak.getUserRoles(true);
  }
}