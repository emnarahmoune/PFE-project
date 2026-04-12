import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../../environments/environment';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class KeycloakInitService {
  private initialized = false;
  private isRefreshing = false;
  private apiUrl = environment.apiUrl || 'http://localhost:8082/api';

  constructor(
    private keycloak: KeycloakService,
    private router: Router,
    private http: HttpClient
  ) {}

  async init(): Promise<boolean> {
    if (this.initialized) return true;
    
    try {
      const authenticated = await this.keycloak.init({
        config: {
          url: environment.keycloak.url,
          realm: environment.keycloak.realm,
          clientId: environment.keycloak.clientId
        },
        initOptions: {
          onLoad: 'check-sso',
          redirectUri: environment.keycloak.redirectUri,
          checkLoginIframe: false,
          pkceMethod: 'S256'
        },
        enableBearerInterceptor: false,
        bearerExcludedUrls: environment.keycloak.bearerExcludedUrls
      });
      
      this.initialized = true;
      console.log('✅ Keycloak initialisé, authentifié:', authenticated);
      
      if (authenticated) {
        await this.syncUserWithBackend();
        await this.cleanUrlAfterAuth();
      }
      
      return authenticated;
    } catch (error) {
      console.error('❌ Keycloak init failed:', error);
      return false;
    }
  }

  private async syncUserWithBackend(): Promise<void> {
    try {
      const token = await this.getToken();
      if (!token) {
        console.error('❌ Pas de token pour synchronisation');
        return;
      }
      
      const headers = { Authorization: `Bearer ${token}` };
      console.log('🔄 Synchronisation utilisateur avec le backend...');
      
      const userInfo = await lastValueFrom(
        this.http.get(`${this.apiUrl}/auth/sync`, { headers })
      );
      
      console.log('✅ Utilisateur synchronisé:', userInfo);
      localStorage.setItem('user_info', JSON.stringify(userInfo));
      
    } catch (error) {
      console.error('❌ Erreur synchronisation:', error);
    }
  }

 private async cleanUrlAfterAuth(): Promise<void> {
  const hasFragment = window.location.hash && (
    window.location.hash.includes('state=') ||
    window.location.hash.includes('session_state=') ||
    window.location.hash.includes('code=')
  );
  
  if (hasFragment) {
    console.log('🧹 Nettoyage URL');
    const userRoles = this.getUserRoles();
    console.log('👤 Rôles détectés:', userRoles);
    
    let targetPath = '/dashboard';
    
    // Vérifier d'abord manager, puis admin
    if (userRoles.includes('manager')) {
      targetPath = '/manager/dashboard';
      console.log('🎯 Manager détecté → /manager/dashboard');
    } else if (userRoles.includes('ADMIN')) {
      targetPath = '/admin/dashboard';
      console.log('🎯 Admin détecté → /admin/dashboard');
    } else if (userRoles.includes('user')) {
      targetPath = '/employee/dashboard';
      console.log('🎯 User détecté → /employee/dashboard');
    }
    
    console.log('🎯 Redirection finale vers:', targetPath);
    window.history.replaceState({}, document.title, targetPath);
    await this.router.navigateByUrl(targetPath);
  }
}

  async getToken(): Promise<string> {
    try {
      await this.keycloak.updateToken(-1);
      const token = await this.keycloak.getToken();
      return token ?? '';
    } catch (error) {
      console.warn('⚠️ Impossible d\'obtenir le token:', error);
      return '';
    }
  }

  async refreshToken(): Promise<boolean> {
    if (this.isRefreshing) return false;
    
    this.isRefreshing = true;
    try {
      const refreshed = await this.keycloak.updateToken(30);
      if (refreshed) {
        await this.syncUserWithBackend();
      }
      return refreshed;
    } catch (error) {
      return false;
    } finally {
      this.isRefreshing = false;
    }
  }

  login(redirectUrl?: string): void {
    const redirectUri = redirectUrl 
      ? window.location.origin + redirectUrl 
      : environment.keycloak.redirectUri;
    console.log('🔑 Redirection Keycloak vers:', redirectUri);
    this.keycloak.login({ redirectUri });
  }

  logout(): void {
    console.log('🚪 Déconnexion');
    localStorage.removeItem('user_info');
    this.keycloak.logout(environment.keycloak.postLogoutRedirectUri);
  }

  async isLoggedIn(): Promise<boolean> {
    try {
      return await this.keycloak.isLoggedIn();
    } catch {
      return false;
    }
  }

  getUserRoles(): string[] {
    try {
      return this.keycloak.getUserRoles(true);
    } catch {
      return [];
    }
  }

  async hasRole(role: string): Promise<boolean> {
    return this.getUserRoles().includes(role);
  }

  async isAdmin(): Promise<boolean> {
    return this.hasRole('admin');
  }

  async isManager(): Promise<boolean> {
    return this.hasRole('manager');
  }

  async isUser(): Promise<boolean> {
    return this.hasRole('user');
  }
}