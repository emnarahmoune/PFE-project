import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { FormationService } from '../../../../core/services/formation.service';

@Component({
  selector: 'app-dashboard-employee',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-employee.component.html',
  styleUrls: ['./dashboard-employee.component.css']
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