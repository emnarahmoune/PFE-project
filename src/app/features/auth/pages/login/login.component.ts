import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { KeycloakInitService } from '../../../../core/services/keycloak-init.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-icon class="logo">people</mat-icon>
          <mat-card-title>RH Platform</mat-card-title>
          <mat-card-subtitle>Gestion intelligente des ressources humaines</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="description">Authentification unique (SSO) avec Keycloak</p>
          <button mat-raised-button color="primary" (click)="login()" style="width: 100%; height: 48px;">
            <mat-icon>login</mat-icon> Se connecter avec Keycloak
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container { height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    .login-card { max-width: 400px; width: 100%; padding: 30px; text-align: center; }
    .logo { font-size: 48px; width: 48px; height: 48px; margin: 0 auto; color: #667eea; }
    .description { margin: 20px 0; color: #666; }
  `]
})
export class LoginComponent implements OnInit {
  
  constructor(
    private keycloakService: KeycloakInitService,
    private router: Router
  ) {}

  async ngOnInit() {
    // Keycloak gère automatiquement le callback - pas de check manuel [web:22]
    const isLogged = await this.keycloakService.isLoggedIn();
    if (isLogged) {
      await this.router.navigate(['/admin/dashboard'], { replaceUrl: true });
    }
  }

  login(): void {
    this.keycloakService.login();
  }
}
