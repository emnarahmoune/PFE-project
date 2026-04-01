import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-employee',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-container">
      <div class="welcome-card">
        <h1>Bienvenue, {{ username }} !</h1>
        <p>Vous êtes connecté à l'espace employé du Portail RH.</p>
      </div>
      
      <div class="info-card">
        <h3>Vos informations</h3>
        <ul>
          <li><strong>Nom:</strong> {{ fullName }}</li>
          <li><strong>Email:</strong> {{ email }}</li>
          <li><strong>Rôle:</strong> Employé</li>
        </ul>
      </div>
      
      <div class="stats-grid">
        <div class="stat-card" routerLink="/employee/mes-conges" style="cursor:pointer">
          <h3>📅 Mes congés</h3>
          <p>Consultez et gérez vos demandes de congé</p>
        </div>
        <div class="stat-card" routerLink="/employee/mes-formations" style="cursor:pointer">
          <h3>🎓 Mes formations</h3>
          <p>Découvrez les formations disponibles</p>
        </div>
        <div class="stat-card" routerLink="/employee/mon-profil" style="cursor:pointer">
          <h3>👤 Mon profil</h3>
          <p>Mettez à jour vos informations personnelles</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    .welcome-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 32px;
      border-radius: 16px;
      margin-bottom: 24px;
    }
    .welcome-card h1 {
      margin: 0 0 8px 0;
      font-size: 28px;
    }
    .info-card {
      background: #f8f9fa;
      padding: 24px;
      border-radius: 16px;
      margin-bottom: 24px;
      border: 1px solid #e9ecef;
    }
    .info-card ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }
    .info-card li {
      padding: 8px 0;
      border-bottom: 1px solid #e9ecef;
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
    }
    .stat-card {
      background: white;
      padding: 24px;
      border-radius: 16px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      border: 1px solid #e9ecef;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .stat-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 6px 16px rgba(0,0,0,0.15);
    }
    .stat-card h3 {
      margin: 0 0 12px 0;
      color: #495057;
    }
    .stat-card p {
      margin: 0;
      color: #6c757d;
    }
  `]
})
export class DashboardEmployeeComponent implements OnInit {
  username: string = '';
  fullName: string = '';
  email: string = '';

  constructor(private authService: AuthService) {}

  async ngOnInit() {
    const token = await this.authService.getToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        
        // ← Données dynamiques depuis le token Keycloak
        this.username   = payload.preferred_username || 'Employé';
        this.fullName   = `${payload.given_name || ''} ${payload.family_name || ''}`.trim() || this.username;
        this.email      = payload.email || '';

      } catch (e) {
        console.error('Erreur décodage token:', e);
        this.username = 'Employé';
      }
    }
  }
}