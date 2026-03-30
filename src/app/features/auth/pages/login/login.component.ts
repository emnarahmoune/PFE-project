import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { keycloakService } from '../../../../core/services/keycloak-init.service';

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
          <mat-card-subtitle>SSO avec Keycloak</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <button mat-raised-button color="primary" (click)="login()">
            <mat-icon>login</mat-icon> Se connecter
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class LoginComponent {

login(): void {
  console.log("CLICK LOGIN"); // 👈 AJOUTE ÇA

  keycloakService.login({
    redirectUri: window.location.origin + '/redirect'
  });
}
}