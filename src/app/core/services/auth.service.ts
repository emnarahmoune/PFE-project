import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { keycloakService } from './keycloak-init.service';
import { TokenService } from './token.service';
import { Observable, of } from 'rxjs';

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
    private tokenService: TokenService,
    private router: Router
  ) {
    console.log('✅ AuthService initialisé');
  }

  // ===== AUTH STATUS =====

  isLoggedIn(): boolean {
    return !!keycloakService.getToken();
  }

  getCurrentUser(): any {
    return this.tokenService.getUser();
  }

  getUserRole(): string {
    const roles = keycloakService.getRoles();
    return roles.includes('admin') ? 'ADMIN' : 'MANAGER';
  }

  // ===== AUTH ACTIONS =====

  login(): void {
    console.log('🔑 Redirection vers Keycloak');
    keycloakService.login();
  }

  logout(): void {
    keycloakService.logout();
    this.tokenService.clear();
    this.router.navigate(['/auth/login']);
  }

  // ===== REGISTER (FAKE / MOCK) =====

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

  // ===== KEYCLOAK =====

  getUserRoles(): string[] {
    return keycloakService.getRoles();
  }

  getToken(): string {
    return keycloakService.getToken();
  }
}