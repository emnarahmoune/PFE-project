import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { FormationService } from '../../../../core/services/formation.service';

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
        
        <div class="stat-card" routerLink="/employee/competences">
  <h3>🧠 Mes compétences</h3>
  <p>Modifier mes compétences pour IA</p>
</div>
      </div>

      <!-- 🔥 RECOMMANDATIONS IA -->
      <h2 style="margin-top:30px;">🎓 Recommandations intelligentes</h2>

      <div class="cards" *ngIf="recommendations.length > 0; else noReco">

        <div class="card" *ngFor="let r of recommendations">
          <h3>{{ r.titre }}</h3>

          <p>Score IA: {{ r.score | number:'1.2-2' }}</p>

          <div class="bar">
            <div class="fill" [style.width.%]="r.score * 100"></div>
          </div>
        </div>

      </div>

      <ng-template #noReco>
        <p>Aucune recommandation disponible</p>
      </ng-template>

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

    .info-card {
      background: #f8f9fa;
      padding: 24px;
      border-radius: 16px;
      margin-bottom: 24px;
      border: 1px solid #e9ecef;
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

    /* 🔥 RECO UI */
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 15px;
      margin-top: 15px;
    }

    .card {
      background: white;
      padding: 16px;
      border-radius: 10px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }

    .bar {
      height: 8px;
      background: #eee;
      border-radius: 5px;
      margin-top: 5px;
    }

    .fill {
      height: 100%;
      background: #4CAF50;
    }
  `]
})
export class DashboardEmployeeComponent implements OnInit {

  username: string = '';
  fullName: string = '';
  email: string = '';

  recommendations: any[] = [];

  constructor(
    private authService: AuthService,
    private formationService: FormationService
  ) {}

  async ngOnInit() {
    const token = await this.authService.getToken();

    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));

        this.username = payload.preferred_username || 'Employé';
        this.fullName = `${payload.given_name || ''} ${payload.family_name || ''}`.trim() || this.username;
        this.email = payload.email || '';

      } catch (e) {
        console.error('Erreur décodage token:', e);
      }
    }

    // 🔥 charger recommandations
    this.loadRecommendations();
  }

  loadRecommendations() {
    const userId = 1;

   this.formationService.getRecommendations().subscribe({
      next: (data: any[]) => {
        console.log("RECO:", data);
        this.recommendations = data;
      },
      error: (err: any) => {
        console.error("Erreur recommandations", err);
      }
    });
  }
}