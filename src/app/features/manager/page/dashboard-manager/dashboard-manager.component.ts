import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ManagerService, ManagerStats } from '../../../../core/services/manager.service';
import { DemandeConge } from '../../../employee/models/conge.model';

@Component({
  selector: 'app-dashboard-manager',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-manager.component.html'
})
export class DashboardManagerComponent implements OnInit {

  equipe: any[] = [];
   stats: ManagerStats = {} as ManagerStats; 
  conges: DemandeConge[] = [];

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
      next: (response: { success: boolean; data: ManagerStats }) => {
        if (response.success) {
          this.stats = response.data;
          this.updateStatCards();
          this.updateRecentEmployees();
          this.updateAlerts();
          this.updateTopCompetences();
        } else {
          this.errorMessage = 'Erreur lors du chargement des statistiques';
          this.setDefaultData();
        }
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur stats:', err);
        this.errorMessage = 'Erreur lors du chargement des statistiques';
        this.loading = false;
        this.setDefaultData();
      }
    });

    this.managerService.getEquipe().subscribe({
      next: (response: { success: boolean; data: any[] }) => {
        if (response.success) {
          this.equipe = response.data || [];
        } else {
          this.equipe = [];
        }
        this.updateRecentEmployees();
      },
      error: (err: any) => {
        console.error('Erreur équipe:', err);
        this.equipe = [];
      }
    });

    this.managerService.getConges().subscribe({
      next: (response: { success: boolean; data: DemandeConge[] }) => {
        if (response.success) {
          this.conges = response.data || [];
        } else {
          this.conges = [];
        }
        console.log('Congés chargés:', this.conges);
        this.updateStatCards();
        this.updateAlerts();
      },
      error: (err: any) => {
        console.error('Erreur congés:', err);
        this.conges = [];
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
      employes: 45,
      congesEnAttente: 0,
      absenteisme: 0,
      turnover: 0,
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
    const congesList = Array.isArray(this.conges) ? this.conges : [];
    
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
        title: 'Congés en attente',
        value: congesList.filter(c => c.statut === 'EN_ATTENTE').length,
        emojiIcon: '⏳',
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

  private isCongeEnCours(conge: DemandeConge): boolean {
    if (!conge.dateDebut || !conge.dateFin) return false;
    const today = new Date();
    const dateDebut = new Date(conge.dateDebut);
    const dateFin = new Date(conge.dateFin);
    return today >= dateDebut && today <= dateFin;
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
        { prenom: 'Marie', nom: 'Lambert', email: 'marie.lambert@entreprise.com', poste: 'Développeur Frontend', departement: 'IT', dateEmbauche: '2024-01-15', statut: 'Actif' },
        { prenom: 'Thomas', nom: 'Bernard', email: 'thomas.bernard@entreprise.com', poste: 'Chef de projet', departement: 'Marketing', dateEmbauche: '2024-02-01', statut: 'Actif' },
        { prenom: 'Sophie', nom: 'Martin', email: 'sophie.martin@entreprise.com', poste: 'Responsable RH', departement: 'RH', dateEmbauche: '2024-02-20', statut: 'Actif' }
      ];
    }
  }

  updateAlerts() {
    this.alerts = [];
    const congesList = Array.isArray(this.conges) ? this.conges : [];
    
    const congesEnAttente = congesList.filter(c => c.statut === 'EN_ATTENTE');
    if (congesEnAttente.length > 0) {
      this.alerts.push({
        type: 'warning',
        message: `${congesEnAttente.length} demande(s) de congé en attente de validation`,
        time: 'En attente',
        lien: '/manager/conges'
      });
    }

    const congesApprouves = congesList.filter(c => c.statut === 'APPROUVE');
    if (congesApprouves.length > 0) {
      this.alerts.push({
        type: 'info',
        message: `${congesApprouves.length} congé(s) approuvé(s) à planifier`,
        time: 'À venir',
        lien: '/manager/conges'
      });
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