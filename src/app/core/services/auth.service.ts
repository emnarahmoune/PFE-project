import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakInitService } from './keycloak-init.service';
import { TokenService } from './token.service';
import { Observable, of } from 'rxjs';

// Interface pour RegisterRequest
export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  constructor(
    private keycloakService: KeycloakInitService,
    private tokenService: TokenService,
    private router: Router
  ) {
    console.log('✅ AuthService initialisé');
  }

  // ===== MÉTHODES DE BASE =====

  // isLoggedIn retourne maintenant une Promise
  async isLoggedIn(): Promise<boolean> {
    return await this.keycloakService.isLoggedIn();
  }

  getCurrentUser(): any {
    return this.tokenService.getUser();
  }

  getUserRole(): string {
    const user = this.tokenService.getUser();
    return user?.typeUtilisateur || 'EMPLOYE';
  }

  // ===== MÉTHODES D'AUTH =====

  login(): void {
    console.log('🔑 Redirection vers Keycloak');
    this.keycloakService.login();
  }

  logout(): void {
    this.keycloakService.logout();
    this.tokenService.clear();
    this.router.navigate(['/auth/login']);
  }

  // ===== MÉTHODE REGISTER =====
  register(userData: RegisterRequest): Observable<any> {
    console.log('📝 Inscription:', userData.email);
    
    return of({
      success: true,
      message: 'Inscription réussie',
      data: { ...userData, id: Date.now() },
      timestamp: new Date().toISOString(),
      statusCode: 200
    });
  }

  // ===== MÉTHODES KEYCLOAK =====
  
  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  getToken(): Promise<string> {
    return this.keycloakService.getToken();
  }
}