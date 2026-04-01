import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakInitService } from '../../../../core/services/keycloak-init.service';
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

  async ngOnInit() {
    const isLogged = await this.keycloakService.isLoggedIn();

    if (isLogged) {
      const roles = this.keycloakService.getUserRoles();
      console.log('👤 Roles détectés:', roles);
      this.redirectByRole(roles);
    } else {
      // Déclenche la page login Keycloak
      this.keycloakService.login();
    }
  }

  private redirectByRole(roles: string[]): void {
    const isAdmin   = roles.some(r => ['admin', 'ADMIN', 'manager', 'MANAGER'].includes(r));
    const isEmployee = roles.some(r => ['user', 'USER', 'EMPLOYEE', 'employee'].includes(r));

    if (isAdmin) {
      this.router.navigate(['/admin/dashboard']);
    } else if (isEmployee) {
      this.router.navigate(['/employee/dashboard']);
    } else {
      console.warn('⚠️ Rôle inconnu:', roles);
      this.keycloakService.login();
    }
  }
}