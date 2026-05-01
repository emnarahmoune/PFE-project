import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { ManagerService, ManagerStats } from '../../../../core/services/manager.service';
import { DemandeConge } from '../../../employee/models/conge.model';
import { AuthService } from '../../../../core/services/auth.service';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface StatCard {
  title: string;
  value: string | number;
  emojiIcon: string;
  trend?: string;
  change?: string;
}

interface TopCompetence {
  nom: string;
  count: number;
  pourcentage: number;
}

@Component({
  selector: 'app-dashboard-manager',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-manager.component.html',
  styleUrls: ['./dashboard-manager.component.css']
})
export class DashboardManagerComponent implements OnInit {

  equipe: any[] = [];
  stats: ManagerStats = {} as ManagerStats;
  conges: DemandeConge[] = [];

  currentMonth = '';
  currentYear = 0;
  refreshing = false;

  statCards: StatCard[] = [];
  recentEmployees: any[] = [];
  alerts: any[] = [];
  topCompetences: TopCompetence[] = [];

  loading = true;
  errorMessage = '';
  currentDate = new Date();

  userNom = '';
  userPrenom = '';
  userEmail = '';

  constructor(
    private managerService: ManagerService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.initDates();
    this.loadData();
  }

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();

    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userEmail = user.email || '';
    }
  }

  getManagerDisplayName(): string {
    const fullName = `${this.userPrenom || ''} ${this.userNom || ''}`.trim();

    if (fullName) {
      return fullName;
    }

    if (this.userEmail) {
      return this.userEmail;
    }

    return 'Manager';
  }

  initDates(): void {
    this.currentMonth = this.getCurrentMonth();
    this.currentYear = new Date().getFullYear();
  }

  getCurrentMonth(): string {
    const months = [
      'janvier',
      'février',
      'mars',
      'avril',
      'mai',
      'juin',
      'juillet',
      'août',
      'septembre',
      'octobre',
      'novembre',
      'décembre'
    ];

    return months[new Date().getMonth()];
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      statsResponse: this.managerService.getStats().pipe(
        catchError((err) => {
          console.error('Erreur stats:', err);
          return of({ success: false, data: {} as ManagerStats });
        })
      ),

      equipeResponse: this.managerService.getEquipe().pipe(
        catchError((err) => {
          console.error('Erreur équipe:', err);
          return of({ success: false, data: [] as any[] });
        })
      ),

      congesResponse: this.managerService.getConges().pipe(
        catchError((err) => {
          console.error('Erreur congés:', err);
          return of({ success: false, data: [] as DemandeConge[] });
        })
      )
    }).subscribe({
      next: ({ statsResponse, equipeResponse, congesResponse }) => {
        this.stats = statsResponse.success ? statsResponse.data : {} as ManagerStats;
        this.equipe = equipeResponse.success ? equipeResponse.data || [] : [];
        this.conges = congesResponse.success ? congesResponse.data || [] : [];

        if (!statsResponse.success || !equipeResponse.success || !congesResponse.success) {
          this.errorMessage = 'Certaines données n’ont pas pu être chargées depuis le backend.';
        }

        this.buildDashboardFromRealData();
        this.loading = false;
        this.refreshing = false;
      },

      error: (err) => {
        console.error('Erreur dashboard manager:', err);
        this.errorMessage = 'Erreur lors du chargement du dashboard.';
        this.stats = {} as ManagerStats;
        this.equipe = [];
        this.conges = [];
        this.buildDashboardFromRealData();
        this.loading = false;
        this.refreshing = false;
      }
    });
  }

  buildDashboardFromRealData(): void {
    this.updateStatCards();
    this.updateRecentEmployees();
    this.updateAlerts();
    this.updateTopCompetences();
  }

  rafraichir(): void {
    this.refreshing = true;
    this.loadData();
  }

  exporterRapport(): void {
    console.log('Exportation du rapport...');
    alert('Exportation en cours...');
  }

  updateStatCards(): void {
    const totalEmployes = this.getTotalEmployesReel();
    const employesActifs = this.getEmployesActifsReel();
    const congesEnAttente = this.getCongesEnAttenteReel();
    const tauxPresence = this.getTauxPresenceReel();

    this.statCards = [
      {
        title: 'Total employés',
        value: totalEmployes,
        emojiIcon: '👥',
        trend: '',
        change: ''
      },
      {
        title: 'Employés actifs',
        value: employesActifs,
        emojiIcon: '💼',
        trend: '',
        change: ''
      },
      {
        title: 'Congés en attente',
        value: congesEnAttente,
        emojiIcon: '⏳',
        trend: '',
        change: ''
      },
      {
        title: 'Taux de présence',
        value: `${tauxPresence}%`,
        emojiIcon: '📊',
        trend: '',
        change: ''
      }
    ];
  }

  getTotalEmployesReel(): number {
    if (Array.isArray(this.equipe) && this.equipe.length > 0) {
      return this.equipe.length;
    }

    const statsAny = this.stats as any;

    if (typeof statsAny.totalEmployes === 'number') {
      return statsAny.totalEmployes;
    }

    if (typeof statsAny.employes === 'number') {
      return statsAny.employes;
    }

    return 0;
  }

  getEmployesActifsReel(): number {
    if (Array.isArray(this.equipe) && this.equipe.length > 0) {
      return this.equipe.filter((employe: any) => {
        const statut = String(
          employe.statut ||
          employe.status ||
          employe.etat ||
          ''
        ).toUpperCase();

        return statut === 'ACTIF' || statut === 'ACTIVE' || statut === 'EN_POSTE';
      }).length;
    }

    const statsAny = this.stats as any;

    if (typeof statsAny.employesActifs === 'number') {
      return statsAny.employesActifs;
    }

    return 0;
  }

  getCongesEnAttenteReel(): number {
    if (!Array.isArray(this.conges)) {
      return 0;
    }

    return this.conges.filter((conge: any) => {
      const statut = String(conge.statut || conge.status || '').toUpperCase();
      return statut === 'EN_ATTENTE' || statut === 'PENDING';
    }).length;
  }

  getCongesApprouvesReel(): number {
    if (!Array.isArray(this.conges)) {
      return 0;
    }

    return this.conges.filter((conge: any) => {
      const statut = String(conge.statut || conge.status || '').toUpperCase();
      return statut === 'APPROUVE' || statut === 'APPROUVÉ' || statut === 'APPROVED';
    }).length;
  }

  getCongesEnCoursReel(): number {
    if (!Array.isArray(this.conges)) {
      return 0;
    }

    return this.conges.filter((conge: any) => this.isCongeEnCours(conge)).length;
  }

  getTauxPresenceReel(): number {
    const statsAny = this.stats as any;

    if (typeof statsAny.tauxPresence === 'number') {
      return Math.round(statsAny.tauxPresence);
    }

    const employesActifs = this.getEmployesActifsReel();

    if (employesActifs === 0) {
      return 0;
    }

    const congesEnCours = this.getCongesEnCoursReel();
    const presents = Math.max(employesActifs - congesEnCours, 0);

    return Math.round((presents / employesActifs) * 100);
  }

  private isCongeEnCours(conge: any): boolean {
    const statut = String(conge.statut || conge.status || '').toUpperCase();

    const isApproved =
      statut === 'APPROUVE' ||
      statut === 'APPROUVÉ' ||
      statut === 'APPROVED';

    if (!isApproved || !conge.dateDebut || !conge.dateFin) {
      return false;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const dateDebut = new Date(conge.dateDebut);
    dateDebut.setHours(0, 0, 0, 0);

    const dateFin = new Date(conge.dateFin);
    dateFin.setHours(23, 59, 59, 999);

    return today >= dateDebut && today <= dateFin;
  }

  updateRecentEmployees(): void {
    if (!Array.isArray(this.equipe) || this.equipe.length === 0) {
      this.recentEmployees = [];
      return;
    }

    this.recentEmployees = [...this.equipe]
      .sort((a: any, b: any) => {
        const dateA = a.dateEmbauche ? new Date(a.dateEmbauche).getTime() : 0;
        const dateB = b.dateEmbauche ? new Date(b.dateEmbauche).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 5);
  }

  updateAlerts(): void {
    this.alerts = [];

    const congesEnAttente = this.getCongesEnAttenteReel();

    if (congesEnAttente > 0) {
      this.alerts.push({
        type: 'warning',
        message: `${congesEnAttente} demande(s) de congé en attente de validation`,
        time: 'En attente',
        lien: '/manager/conges'
      });
    }

    const congesEnCours = this.getCongesEnCoursReel();

    if (congesEnCours > 0) {
      this.alerts.push({
        type: 'info',
        message: `${congesEnCours} employé(s) actuellement en congé`,
        time: 'Aujourd’hui',
        lien: '/manager/conges'
      });
    }

    if (this.alerts.length === 0) {
      this.alerts.push({
        type: 'success',
        message: 'Aucune alerte RH pour le moment',
        time: 'Maintenant',
        lien: null
      });
    }
  }

  updateTopCompetences(): void {
    const competenceCounter = new Map<string, number>();

    if (!Array.isArray(this.equipe)) {
      this.topCompetences = [];
      return;
    }

    this.equipe.forEach((employe: any) => {
      const competences =
        employe.competences ||
        employe.competenceList ||
        employe.skills ||
        [];

      if (!Array.isArray(competences)) {
        return;
      }

      competences.forEach((competence: any) => {
        const nom =
          typeof competence === 'string'
            ? competence
            : competence.nom || competence.name || competence.libelle;

        if (!nom) {
          return;
        }

        competenceCounter.set(nom, (competenceCounter.get(nom) || 0) + 1);
      });
    });

    const maxCount = Math.max(...Array.from(competenceCounter.values()), 0);

    this.topCompetences = Array.from(competenceCounter.entries())
      .map(([nom, count]) => ({
        nom,
        count,
        pourcentage: maxCount > 0 ? Math.round((count / maxCount) * 100) : 0
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);
  }

  getMasseSalarialeFormatee(): string {
    const statsAny = this.stats as any;
    const masseSalariale = Number(statsAny.masseSalariale || 0);

    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(masseSalariale);
  }

  getSalaireMoyenFormate(): string {
    const statsAny = this.stats as any;
    const salaireMoyen = Number(statsAny.salaireMoyen || 0);

    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(salaireMoyen);
  }

  getDepartements(): string[] {
    const statsAny = this.stats as any;

    if (statsAny.parDepartement && Object.keys(statsAny.parDepartement).length > 0) {
      return Object.keys(statsAny.parDepartement);
    }

    const departements = new Set<string>();

    this.equipe.forEach((employe: any) => {
      if (employe.departement) {
        departements.add(employe.departement);
      }
    });

    return Array.from(departements);
  }

  getTauxRemplissage(dept: string): number {
    if (!dept) {
      return 0;
    }

    const totalEmployes = this.getTotalEmployesReel();

    if (totalEmployes === 0) {
      return 0;
    }

    const count = this.equipe.filter((employe: any) => employe.departement === dept).length;

    return Math.round((count / totalEmployes) * 100);
  }

  getAvatarBg(departement: string): string {
    const colors: { [key: string]: string } = {
      IT: '#667eea',
      RH: '#48bb78',
      Marketing: '#ed8936',
      Finance: '#4299e1',
      Commercial: '#9f7aea',
      Technique: '#0ea5e9',
      Direction: '#1e293b'
    };

    return colors[departement] || '#718096';
  }

  getAlertEmoji(type: string): string {
    switch (type) {
      case 'warning':
        return '⚠️';
      case 'info':
        return 'ℹ️';
      case 'success':
        return '✅';
      default:
        return '📌';
    }
  }
}