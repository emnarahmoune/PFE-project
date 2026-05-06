import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakInitService } from './keycloak-init.service';
import { TokenService, UtilisateurInfo } from './token.service';
import { Observable, lastValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  
  private apiUrl = environment.apiUrl || 'http://localhost:8082/api';
  
  constructor(
    private keycloakService: KeycloakInitService,
    private tokenService: TokenService,
    private router: Router,
    private http: HttpClient
  ) {
    console.log('✅ AuthService initialisé');
  }

  async isLoggedIn(): Promise<boolean> {
    return await this.keycloakService.isLoggedIn();
  }

  getCurrentUser(): UtilisateurInfo | null {
    // Essayer d'abord le TokenService, puis le localStorage
    const user = this.tokenService.getUser();
    if (user) return user;
    
    const storedUser = localStorage.getItem('user_info');
    if (storedUser) {
      try {
        return JSON.parse(storedUser);
      } catch {
        return null;
      }
    }
    return null;
  }

 getUserRole(): string {
  const user = this.getCurrentUser();
  return user?.role ?? 'USER';
}
  login(redirectUrl?: string): void {
    console.log('🔑 Redirection vers Keycloak');
    this.keycloakService.login(redirectUrl);
  }

  logout(): void {
    this.keycloakService.logout();
    this.tokenService.clear();
    localStorage.removeItem('user_info');
    this.router.navigate(['/auth/login']);
  }

  async refreshToken(): Promise<boolean> {
    console.log('🔄 [KEYCLOAK] Rafraîchissement du token...');
    try {
      const isLogged = await this.keycloakService.isLoggedIn();
      console.log('👤 [KEYCLOAK] isLoggedIn():', isLogged);
      
      if (!isLogged) {
        console.warn('❌ [KEYCLOAK] Utilisateur non connecté → Arrêt refresh');
        return false;
      }

      const refreshed = await this.keycloakService.refreshToken();
      console.log('🔄 [KEYCLOAK] refreshToken() →', refreshed ? '✅ SUCCESS' : '❌ FAILED');

      if (refreshed) {
        const newToken = await this.keycloakService.getToken();
        if (newToken) {
          this.tokenService.setToken(newToken);
          console.log('🎉 [KEYCLOAK] Refresh COMPLET');
          await this.syncUserWithBackend();
          return true;
        }
      }
      return false;
    } catch (error) {
      console.error('💥 [KEYCLOAK] ERREUR refreshToken():', error);
      return false;
    }
  }

  async syncUserWithBackend(): Promise<UtilisateurInfo | null> {
    console.log('🔄 [BACKEND] Synchronisation utilisateur...');
    
    try {
      const token = await this.keycloakService.getToken();
      if (!token) {
        console.error('❌ [BACKEND] Pas de token');
        return null;
      }
      
      const headers = { Authorization: `Bearer ${token}` };
      
      const userInfo = await lastValueFrom(
        this.http.get<UtilisateurInfo>(`${this.apiUrl}/auth/sync`, { headers })
      );
      
      console.log('✅ [BACKEND] Utilisateur synchronisé:', userInfo);
      
      this.tokenService.setUser(userInfo);
      localStorage.setItem('user_info', JSON.stringify(userInfo));
      
      return userInfo;
    } catch (error) {
      console.error('❌ [BACKEND] Erreur synchronisation:', error);
      return null;
    }
  }

  async initAfterLogin(): Promise<boolean> {
    console.log('🚀 [AUTH] Initialisation après connexion...');
    
    try {
      const token = await this.keycloakService.getToken();
      if (!token) {
        console.error('❌ [AUTH] Pas de token');
        return false;
      }
      
      this.tokenService.setToken(token);
      
      const user = await this.syncUserWithBackend();
      if (!user) {
        console.error('❌ [AUTH] Échec synchronisation utilisateur');
        return false;
      }
      
      console.log('✅ [AUTH] Initialisation terminée avec succès');
      return true;
      
    } catch (error) {
      console.error('❌ [AUTH] Erreur initialisation:', error);
      return false;
    }
  }

  register(userData: RegisterRequest): Observable<any> {
    console.log('📝 Inscription:', userData.email);
    return this.http.post(`${this.apiUrl}/auth/register`, userData);
  }

  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  async getToken(): Promise<string> {
    return await this.keycloakService.getToken();
  }

  async hasRole(role: string): Promise<boolean> {
    return await this.keycloakService.hasRole(role);
  }

  async isAdmin(): Promise<boolean> {
    return await this.keycloakService.isAdmin();
  }

  async isManager(): Promise<boolean> {
    return await this.keycloakService.isManager();
  }

  async isUser(): Promise<boolean> {
    return await this.keycloakService.isUser();
  }

  getUserInfo(): UtilisateurInfo | null {
    return this.getCurrentUser();
  }
}