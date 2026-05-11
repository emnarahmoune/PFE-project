import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakInitService } from '../../core/services/keycloak-init.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="display:flex; justify-content:center; align-items:center; height:100vh;">
      <p>Connexion en cours...</p>
    </div>
  `
})
export class LoginComponent implements OnInit {

  constructor(
    private keycloakService: KeycloakInitService,
    private router: Router
  ) {}

  async ngOnInit(): Promise<void> {
    const isLogged = await this.keycloakService.isLoggedIn();

    if (!isLogged) {
      console.log('🔑 Non connecté → redirection Keycloak');
      this.keycloakService.login('/auth/login');
      return;
    }

    const roles = this.keycloakService.getUserRoles();
    const user = this.keycloakService.getUser();

    console.log('👤 Roles Keycloak détectés:', roles);
    console.log('👤 User backend détecté:', user);

    this.redirectByRole(roles, user);
  }

  private redirectByRole(roles: string[], user: any): void {
    const normalizedRoles = roles.map(r =>
      r.toLowerCase()
        .replace('role_', '')
        .replace('realm_', '')
        .trim()
    );

    const backendRole = (
      user?.role ||
      user?.typeEmploye ||
      user?.type_employe ||
      ''
    ).toString().toLowerCase();

    console.log('👤 Rôles normalisés:', normalizedRoles);
    console.log('👤 Rôle backend:', backendRole);

    const isAdmin =
      backendRole === 'admin_rh' ||
      backendRole === 'admin' ||
      normalizedRoles.includes('admin_rh') ||
      normalizedRoles.includes('admin') ||
      normalizedRoles.includes('rh');

    const isManager =
      backendRole === 'manager' ||
      normalizedRoles.includes('manager');

    const isEmployee =
      backendRole === 'employe' ||
      backendRole === 'employee' ||
      backendRole === 'user' ||
      normalizedRoles.includes('employe') ||
      normalizedRoles.includes('employee') ||
      normalizedRoles.includes('user');

    if (isAdmin) {
      console.log('🎯 Redirection admin');
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    if (isManager) {
      console.log('🎯 Redirection manager');
      this.router.navigate(['/manager/dashboard']);
      return;
    }

    if (isEmployee) {
      console.log('🎯 Redirection employé');
      this.router.navigate(['/employee/dashboard']);
      return;
    }

    console.warn('⚠️ Rôle inconnu. Redirection fallback vers employee/dashboard');
    this.router.navigate(['/employee/dashboard']);
  }
}