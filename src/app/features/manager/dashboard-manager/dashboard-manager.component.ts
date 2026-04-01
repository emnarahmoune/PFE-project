import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ManagerService } from '../../../core/services/manager.service';

@Component({
  selector: 'app-dashboard-manager',
  standalone: true,
  imports: [CommonModule, RouterModule], // ✅ Plus besoin de Chart.js
  templateUrl: './dashboard-manager.component.html'
})
export class DashboardManagerComponent implements OnInit {

  stats: any = {};
  equipe: any[] = [];
  conges: any[] = [];
  
  currentMonth: string = '';
  currentYear: number = 0;
  refreshing: boolean = false;
  statCards: any[] = [];
  recentEmployees: any[] = [];
  alerts: any[] = [];
  topCompetences: any[] = [];

  loading = true;
  errorMessage = '';
  currentDate = new Date();

  constructor(private managerService: ManagerService) {}

  ngOnInit() {
    this.initDates();
    this.loadData();
  }

  initDates() {
    this.currentMonth = this.getCurrentMonth();
    this.currentYear = new Date().getFullYear();
  }

  getCurrentMonth(): string {
    const months = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 
                    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
    return months[new Date().getMonth()];
  }

  loadData() {
    this.loading = true;
    this.errorMessage = '';

    this.managerService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.updateStatCards();
        this.updateRecentEmployees();
        this.updateAlerts();
        this.updateTopCompetences();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur stats:', err);
        this.errorMessage = 'Erreur lors du chargement des statistiques';
        this.loading = false;
        this.setDefaultData();
      }
    });

    this.managerService.getEquipe().subscribe({
      next: (data) => {
        this.equipe = data;
        this.updateRecentEmployees();
      },
      error: (err) => {
        console.error('Erreur équipe:', err);
      }
    });

    this.managerService.getConges().subscribe({
      next: (data) => {
        this.conges = data;
        this.updateAlerts();
      },
      error: (err) => {
        console.error('Erreur congés:', err);
      }
    });

    setTimeout(() => {
      if (this.loading) {
        this.loading = false;
      }
    }, 3000);
  }

  setDefaultData() {
    this.stats = {
      totalEmployes: 45,
      employesActifs: 42,
      masseSalariale: 125000,
      salaireMoyen: 2850,
      parDepartement: {
        'IT': 12,
        'RH': 5,
        'Marketing': 8,
        'Finance': 6,
        'Commercial': 14
      }
    };
    this.updateStatCards();
  }

  rafraichir() {
    this.refreshing = true;
    this.loadData();
    setTimeout(() => {
      this.refreshing = false;
    }, 1000);
  }

  exporterRapport() {
    console.log('Exportation du rapport...');
    alert('Exportation en cours...');
  }

  updateStatCards() {
    this.statCards = [
      {
        title: 'Total employés',
        value: this.stats.totalEmployes || 0,
        emojiIcon: '👥',
        trend: '',
        change: ''
      },
      {
        title: 'Employés actifs',
        value: this.stats.employesActifs || 0,
        emojiIcon: '💼',
        trend: '',
        change: ''
      },
      {
        title: 'Congés en cours',
        value: this.conges ? this.conges.filter(c => c.statut === 'En cours').length : 0,
        emojiIcon: '🏖️',
        trend: '',
        change: ''
      },
      {
        title: 'Taux de présence',
        value: this.stats.tauxPresence ? `${this.stats.tauxPresence}%` : '95%',
        emojiIcon: '📊',
        trend: 'up',
        change: '+2%'
      }
    ];
  }

  updateRecentEmployees() {
    if (this.equipe && this.equipe.length > 0) {
      this.recentEmployees = this.equipe
        .sort((a, b) => {
          const dateA = a.dateEmbauche ? new Date(a.dateEmbauche).getTime() : 0;
          const dateB = b.dateEmbauche ? new Date(b.dateEmbauche).getTime() : 0;
          return dateB - dateA;
        })
        .slice(0, 5);
    } else {
      this.recentEmployees = [
        {
          prenom: 'Marie',
          nom: 'Lambert',
          email: 'marie.lambert@entreprise.com',
          poste: 'Développeur Frontend',
          departement: 'IT',
          dateEmbauche: '2024-01-15',
          statut: 'Actif'
        },
        {
          prenom: 'Thomas',
          nom: 'Bernard',
          email: 'thomas.bernard@entreprise.com',
          poste: 'Chef de projet',
          departement: 'Marketing',
          dateEmbauche: '2024-02-01',
          statut: 'Actif'
        },
        {
          prenom: 'Sophie',
          nom: 'Martin',
          email: 'sophie.martin@entreprise.com',
          poste: 'Responsable RH',
          departement: 'RH',
          dateEmbauche: '2024-02-20',
          statut: 'Actif'
        }
      ];
    }
  }

  updateAlerts() {
    this.alerts = [];
    
    if (this.conges && this.conges.length > 0) {
      const congesEnCours = this.conges.filter(c => c.statut === 'En cours');
      if (congesEnCours.length > 0) {
        this.alerts.push({
          type: 'info',
          message: `${congesEnCours.length} employé(s) en congés cette semaine`,
          time: 'Aujourd\'hui',
          lien: '/manager/conges'
        });
      }
    }

    if (this.alerts.length === 0) {
      this.alerts.push({
        type: 'success',
        message: 'Tous les indicateurs sont au vert',
        time: 'Maintenant',
        lien: null
      });
    }
  }

  updateTopCompetences() {
    this.topCompetences = [
      { nom: 'JavaScript', count: 12, pourcentage: 85 },
      { nom: 'Angular', count: 8, pourcentage: 65 },
      { nom: 'Python', count: 6, pourcentage: 45 },
      { nom: 'React', count: 5, pourcentage: 35 },
      { nom: 'Java', count: 4, pourcentage: 28 }
    ];
  }

  getMasseSalarialeFormatee(): string {
    const masseSalariale = this.stats.masseSalariale || 125000;
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(masseSalariale);
  }

  getSalaireMoyenFormate(): string {
    const salaireMoyen = this.stats.salaireMoyen || 2850;
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(salaireMoyen);
  }

  getDepartements(): string[] {
    if (this.stats.parDepartement) {
      return Object.keys(this.stats.parDepartement);
    }
    return ['IT', 'RH', 'Marketing', 'Finance', 'Commercial'];
  }

  getTauxRemplissage(dept: string): number {
    if (this.stats.parDepartement && this.stats.parDepartement[dept]) {
      const total = 20;
      const current = this.stats.parDepartement[dept];
      return Math.min((current / total) * 100, 100);
    }
    return 75;
  }

  getAvatarBg(departement: string): string {
    const colors: {[key: string]: string} = {
      'IT': '#667eea',
      'RH': '#48bb78',
      'Marketing': '#ed8936',
      'Finance': '#4299e1',
      'Commercial': '#9f7aea'
    };
    return colors[departement] || '#718096';
  }

  getAlertEmoji(type: string): string {
    switch(type) {
      case 'warning': return '⚠️';
      case 'info': return 'ℹ️';
      case 'success': return '✅';
      default: return '📌';
    }
  }
}