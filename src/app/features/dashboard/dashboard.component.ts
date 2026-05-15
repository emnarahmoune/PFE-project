import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Subject, forkJoin, of } from 'rxjs';
import { catchError, takeUntil } from 'rxjs/operators';

import { environment } from '../../../environments/environment';

import { AuthService } from '../../core/services/auth.service';
import { FormationService } from '../../core/services/formation.service';
import { ManagerService, ManagerStats } from '../../core/services/manager.service';
import {
  EmployeeDashboardService,
  EmployeeDashboardStats
} from '../../core/services/employee-dashboard.service';
import {
  BiDashboardService,
  DashboardAdminBi,
  DashboardManagerBi,
  RecentEmployeeBi,
  TopCompetenceBi
} from '../../core/services/bi-dashboard.service';
import {
  FormationRecommendationService,
} from '../../core/services/formation-recommendation.service';
import { DemandeConge } from '../../core/models/conge.model';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

type DashboardRole = 'ADMIN' | 'MANAGER' | 'EMPLOYE';

interface AdminStatCard {
  title: string;
  value: string | number;
  icon: string;
  change?: string;
  trend?: 'positive' | 'negative' | 'warning' | 'neutral';
  link?: string;
}

interface ManagerStatCard {
  title: string;
  value: string | number;
  emojiIcon: string;
  trend?: string;
  change?: string;
}

interface AlertItem {
  type: 'danger' | 'warning' | 'info' | 'success';
  message: string;
  time: string;
  lien?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  roleActif: DashboardRole = 'EMPLOYE';

  loading = false;
  refreshing = false;
  errorMessage: string | null = null;

  currentDate = new Date();
  currentYear = this.currentDate.getFullYear();
  currentMonth = this.currentDate.toLocaleString('fr-FR', { month: 'long' });
  welcomeMessage = '';

  userNom = '';
  userPrenom = '';
  userEmail = '';
  userRole = '';

  username = '';
  fullName = '';
  email = '';

  // ================= ADMIN =================
  biStats: DashboardAdminBi | null = null;
  adminStatCards: AdminStatCard[] = [];
  recentEmployees: RecentEmployeeBi[] = [];
  alerts: AlertItem[] = [];
  powerBiAdminUrl: SafeResourceUrl | null = null;

  // ================= MANAGER =================
  equipe: any[] = [];
  stats: ManagerStats = {} as ManagerStats;
  conges: DemandeConge[] = [];
  managerBi: DashboardManagerBi | null = null;
  managerStatCards: ManagerStatCard[] = [];
  topCompetences: TopCompetenceBi[] = [];
  powerBiManagerUrl: SafeResourceUrl | null = null;

  // ================= EMPLOYE =================
  recommendations: any[] = [];
  powerBiEmployeeUrl: SafeResourceUrl | null = null;


  employeeStats: EmployeeDashboardStats | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private sanitizer: DomSanitizer,
    private authService: AuthService,
    private formationService: FormationService,
    private managerService: ManagerService,
    private biDashboardService: BiDashboardService,
    private employeeDashboardService: EmployeeDashboardService,
    private formationRecommendationService: FormationRecommendationService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.setWelcomeMessage();
    await this.loadUserInfo();
    this.resolveRole();

    this.initPowerBiUrls();

    if (this.isAdmin()) {
      this.loadAdminDashboard();
      return;
    }

    if (this.isManager()) {
      this.loadManagerDashboard();
      return;
    }

    this.loadEmployeeDashboard();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // =========================================================
  // ROLE
  // =========================================================

  private resolveRole(): void {
    const forceRole = this.route.snapshot.data?.['forceRole'];

    if (forceRole) {
      this.roleActif = this.normalizeRole(forceRole);
      return;
    }

    const currentUser = this.authService.getCurrentUser() as any;

    const rawRole =
      currentUser?.role ||
      currentUser?.typeUtilisateur ||
      currentUser?.typeEmploye ||
      this.userRole ||
      '';

    this.roleActif = this.normalizeRole(rawRole);
  }

private normalizeRole(role: string): DashboardRole {
  const value = String(role || '').toUpperCase();

  if (
    value.includes('MANAGER') ||
    value.includes('ROLE_MANAGER')
  ) {
    return 'MANAGER';
  }

  if (
    value.includes('ADMIN_RH') ||
    value.includes('ROLE_ADMIN_RH') ||
    value.includes('ADMIN') ||
    value.includes('ROLE_ADMIN')
  ) {
    return 'ADMIN';
  }

  return 'EMPLOYE';
}
  isAdmin(): boolean {
    return this.roleActif === 'ADMIN';
  }

  isManager(): boolean {
    return this.roleActif === 'MANAGER';
  }

  isEmployee(): boolean {
    return this.roleActif === 'EMPLOYE';
  }

  // =========================================================
  // USER
  // =========================================================

  private async loadUserInfo(): Promise<void> {
    const currentUser = this.authService.getCurrentUser() as any;

    if (currentUser) {
      this.userNom = currentUser.nom || currentUser.lastName || '';
      this.userPrenom = currentUser.prenom || currentUser.firstName || '';
      this.userEmail = currentUser.email || '';
      this.userRole = currentUser.role || currentUser.typeUtilisateur || '';

      this.username =
        currentUser.username ||
        currentUser.login ||
        this.userEmail ||
        'Employé';

      this.fullName =
        `${this.userPrenom || ''} ${this.userNom || ''}`.trim() ||
        currentUser.fullName ||
        this.username;

      this.email = this.userEmail;
    }

    try {
      const token = await this.authService.getToken();

      if (!token) {
        return;
      }

      const payload = JSON.parse(atob(token.split('.')[1]));

      this.username = payload.preferred_username || this.username || 'Employé';

      this.fullName =
        `${payload.given_name || this.userPrenom || ''} ${payload.family_name || this.userNom || ''}`.trim() ||
        this.fullName ||
        this.username;

      this.email = payload.email || this.email || '';

      if (!this.userRole) {
        const roles: string[] = [
          ...(payload.realm_access?.roles || []),
          ...(payload.resource_access?.portail_rh_frontend?.roles || [])
        ];

        this.userRole = roles.join(',');
      }
    } catch (error) {
      console.error('Erreur décodage token:', error);
    }
  }

  setWelcomeMessage(): void {
    const hour = new Date().getHours();

    if (hour < 12) {
      this.welcomeMessage = 'Bonjour';
    } else if (hour < 18) {
      this.welcomeMessage = 'Bon après-midi';
    } else {
      this.welcomeMessage = 'Bonsoir';
    }
  }

 getAdminDisplayName(): string {
  if (this.fullName && this.fullName !== 'Employé') {
    return this.fullName;
  }

  const name = `${this.userPrenom || ''} ${this.userNom || ''}`.trim();

  if (name) return name;
  if (this.username && this.username !== 'Employé') return this.username;
  if (this.email) return this.email;
  if (this.userEmail) return this.userEmail;

  return 'Administrateur';
}

getManagerDisplayName(): string {
  if (this.fullName && this.fullName !== 'Employé') {
    return this.fullName;
  }

  const name = `${this.userPrenom || ''} ${this.userNom || ''}`.trim();

  if (name) return name;
  if (this.username && this.username !== 'Employé') return this.username;
  if (this.email) return this.email;
  if (this.userEmail) return this.userEmail;

  return 'Manager';
}

getEmployeeDisplayName(): string {
  if (this.fullName && this.fullName !== 'Employé') {
    return this.fullName;
  }

  if (this.username && this.username !== 'Employé') {
    return this.username;
  }

  if (this.email) {
    return this.email;
  }

  return 'Employé';
}


  // =========================================================
  // POWER BI
  // =========================================================

  private initPowerBiUrls(): void {
  if (environment.powerBiAdminUrl) {
    this.powerBiAdminUrl =
      this.sanitizer.bypassSecurityTrustResourceUrl(environment.powerBiAdminUrl);
  }

  this.powerBiManagerUrl = null;
  this.powerBiEmployeeUrl = null;
}

  // =========================================================
  // ADMIN
  // =========================================================

  loadAdminDashboard(): void {
    this.loading = true;
    this.refreshing = true;
    this.errorMessage = null;

    this.biDashboardService
      .getDashboardAdminBi()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.biStats = data;
          this.recentEmployees = data.recentEmployees || [];

          this.buildAdminStatCards(data);
          this.buildAdminAlerts(data);

          this.loading = false;
          this.refreshing = false;
        },
        error: (error) => {
          console.error('Erreur chargement BI dashboard admin:', error);

          this.errorMessage = 'Impossible de charger les indicateurs BI admin.';
          this.loading = false;
          this.refreshing = false;
        }
      });
  }

  buildAdminStatCards(data: DashboardAdminBi): void {
    const effectifTotal = Number(data.effectifTotal || 0);
    const employesActifs = Number(data.employesActifs || 0);
    const employesInactifs = Number(data.employesInactifs || 0);
    const joursAbsence = Number(data.joursAbsence || 0);
    const congesEnAttente = Number(data.congesEnAttente || 0);
    const congesApprouves = Number(data.congesApprouves || 0);
    const totalFormations = Number(data.totalFormations || 0);
    const totalCompetences = Number(data.totalCompetences || 0);
    const employesRisqueEleve = Number(data.employesRisqueEleve || 0);
    const scoreRisqueMoyen = Number(data.scoreRisqueMoyen || 0);

    const tauxTurnover =
      effectifTotal > 0 ? (employesInactifs / effectifTotal) * 100 : 0;

    const tauxAbsenteisme =
      employesActifs > 0 ? (joursAbsence / (employesActifs * 22)) * 100 : 0;

    this.adminStatCards = [
      {
        title: 'Employés actifs',
        value: employesActifs,
        icon: '👥',
        trend: 'positive',
        change: `${effectifTotal} total`,
        link: '/admin/employes'
      },
      {
        title: 'Turnover',
        value: `${this.formatNumber(tauxTurnover)}%`,
        icon: '🔄',
        trend: tauxTurnover > 10 ? 'warning' : 'positive',
        change: tauxTurnover > 10 ? 'élevé' : 'normal'
      },
      {
        title: 'Absentéisme',
        value: `${this.formatNumber(tauxAbsenteisme)}%`,
        icon: '🗓️',
        trend: tauxAbsenteisme > 5 ? 'warning' : 'positive',
        change: tauxAbsenteisme > 5 ? 'à surveiller' : 'bas'
      },
      {
        title: 'Congés en attente',
        value: congesEnAttente,
        icon: '🏖️',
        trend: congesEnAttente > 0 ? 'warning' : 'positive',
        change: `${congesApprouves} approuvés`,
        link: '/admin/conges'
      },
      {
        title: 'Formations actives',
        value: totalFormations,
        icon: '📚',
        trend: 'neutral',
        change: `${totalCompetences} compétences`,
        link: '/admin/formations'
      },
      {
        title: 'Risques de départ',
        value: employesRisqueEleve,
        icon: '⚠️',
        trend: employesRisqueEleve > 0 ? 'negative' : 'positive',
        change: `Score moyen ${this.formatNumber(scoreRisqueMoyen)}`
      }
    ];
  }

  buildAdminAlerts(data: DashboardAdminBi): void {
    const alerts: AlertItem[] = [];

    if (Number(data.congesEnAttente || 0) > 0) {
      alerts.push({
        type: 'warning',
        message: `${data.congesEnAttente} demande(s) de congé en attente.`,
        time: 'À traiter',
        lien: '/admin/conges'
      });
    }

    if (Number(data.employesRisqueEleve || 0) > 0) {
      alerts.push({
        type: 'danger',
        message: `${data.employesRisqueEleve} employé(s) avec risque de départ élevé.`,
        time: 'Analyse BI',
        lien: '/admin/scores'
      });
    }

    if (
      Number(data.noteMoyenneGlobale || 0) > 0 &&
      Number(data.noteMoyenneGlobale) < 3
    ) {
      alerts.push({
        type: 'warning',
        message: 'La note moyenne globale est faible.',
        time: 'Évaluations',
        lien: '/admin/evaluations'
      });
    }

    this.alerts = alerts;
  }

  getMasseSalarialeFormatee(): string {
    return this.formatTnd(Number(this.biStats?.masseSalariale || 0));
  }

  getSalaireMoyenFormate(): string {
    return this.formatTnd(Number(this.biStats?.salaireMoyen || 0));
  }

  getDepartements(): string[] {
    if (!this.biStats?.parDepartement) return [];
    return Object.keys(this.biStats.parDepartement);
  }

  getDepartementCount(dept: string): number {
    return Number(this.biStats?.parDepartement?.[dept] || 0);
  }

  getTauxRemplissage(dept: string): number {
    if (!this.biStats?.parDepartement) return 0;

    const values = Object.values(this.biStats.parDepartement).map(value =>
      Number(value || 0)
    );

    const max = Math.max(...values, 1);
    const current = Number(this.biStats.parDepartement[dept] || 0);

    return (current * 100) / max;
  }

  // =========================================================
  // MANAGER
  // =========================================================

  loadManagerDashboard(): void {
    this.loading = true;
    this.errorMessage = null;

    this.loadManagerOperationalData();
    this.loadManagerBi();
  }

  loadManagerOperationalData(): void {
    forkJoin({
      statsResponse: this.managerService.getStats().pipe(
        catchError((err) => {
          console.error('Erreur stats manager:', err);
          return of({});
        })
      ),

      equipeResponse: this.managerService.getEquipe().pipe(
        catchError((err) => {
          console.error('Erreur équipe manager:', err);
          return of([]);
        })
      ),

      congesResponse: this.managerService.getConges().pipe(
        catchError((err) => {
          console.error('Erreur congés manager:', err);
          return of([]);
        })
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ statsResponse, equipeResponse, congesResponse }) => {
          this.stats = this.unwrapResponse<ManagerStats>(
            statsResponse,
            {} as ManagerStats
          );

          this.equipe = this.unwrapResponse<any[]>(
            equipeResponse,
            []
          );

          this.conges = this.unwrapResponse<DemandeConge[]>(
            congesResponse,
            []
          );

          if (!this.managerBi) {
            this.buildManagerDashboardFromFallbackData();
          }

          this.loading = false;
          this.refreshing = false;
        },
        error: (err) => {
          console.error('Erreur dashboard manager:', err);

          this.stats = {} as ManagerStats;
          this.equipe = [];
          this.conges = [];

          this.buildManagerDashboardFromFallbackData();

          this.loading = false;
          this.refreshing = false;
        }
      });
  }

  loadManagerBi(): void {
    const currentUser = this.authService.getCurrentUser() as any;

    const managerId = Number(
      currentUser?.id ||
      currentUser?.employeId ||
      currentUser?.employeeId ||
      currentUser?.userId ||
      0
    );

    if (!managerId) {
      console.warn('ID manager introuvable pour le dashboard BI manager.');
      this.buildManagerDashboardFromFallbackData();
      return;
    }

    this.biDashboardService
      .getDashboardManagerBi(managerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.managerBi = data;
          this.applyManagerBi(data);

          this.loading = false;
          this.refreshing = false;
        },
        error: (err) => {
          console.error('Erreur chargement dashboard BI manager:', err);

          // Important : on ne bloque jamais le dashboard manager.
          this.managerBi = null;
          this.buildManagerDashboardFromFallbackData();

          this.loading = false;
          this.refreshing = false;
        }
      });
  }

  applyManagerBi(data: DashboardManagerBi): void {
    const totalEmployes = Number(data.totalEmployes || 0);
    const employesActifs = Number(data.employesActifs || 0);
    const congesEnAttente = Number(data.congesEnAttente || 0);
    const tauxPresence = Number(data.tauxPresence || 0);
    const scoreRisqueMoyen = Number(data.scoreRisqueMoyen || 0);
    const employesRisqueEleve = Number(data.employesRisqueEleve || 0);

    this.managerStatCards = [
      {
        title: 'Total employés',
        value: totalEmployes,
        emojiIcon: '👥',
        change: 'Équipe manager'
      },
      {
        title: 'Employés actifs',
        value: employesActifs,
        emojiIcon: '💼',
        change: `${totalEmployes} total`
      },
      {
        title: 'Congés en attente',
        value: congesEnAttente,
        emojiIcon: '⏳',
        change: congesEnAttente > 0 ? 'À traiter' : 'Aucun en attente'
      },
      {
        title: 'Taux de présence',
        value: `${this.formatNumber(tauxPresence)}%`,
        emojiIcon: '📊',
        change: 'Présence équipe'
      },
      {
        title: 'Score risque moyen',
        value: this.formatNumber(scoreRisqueMoyen),
        emojiIcon: '⚠️',
        change: scoreRisqueMoyen >= 60 ? 'À surveiller' : 'Normal'
      },
      {
        title: 'Risques élevés',
        value: employesRisqueEleve,
        emojiIcon: '🚨',
        change: employesRisqueEleve > 0 ? 'Suivi requis' : 'Aucun risque élevé'
      }
    ];

    this.topCompetences = data.topCompetences || [];
  }

  buildManagerDashboardFromFallbackData(): void {
    this.updateManagerStatCardsFallback();
    this.updateTopCompetencesFallback();
  }

  updateManagerStatCardsFallback(): void {
    const totalEmployes = this.getTotalEmployesReel();
    const employesActifs = this.getEmployesActifsReel();
    const congesEnAttente = this.getCongesEnAttenteReel();
    const tauxPresence = this.getTauxPresenceReel();

    this.managerStatCards = [
      {
        title: 'Total employés',
        value: totalEmployes,
        emojiIcon: '👥',
        change: 'Données opérationnelles'
      },
      {
        title: 'Employés actifs',
        value: employesActifs,
        emojiIcon: '💼',
        change: `${totalEmployes} total`
      },
      {
        title: 'Congés en attente',
        value: congesEnAttente,
        emojiIcon: '⏳',
        change: congesEnAttente > 0 ? 'À traiter' : 'Aucun en attente'
      },
      {
        title: 'Taux de présence',
        value: `${tauxPresence}%`,
        emojiIcon: '📊',
        change: 'Présence équipe'
      }
    ];
  }

  updateTopCompetencesFallback(): void {
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

  private unwrapResponse<T>(response: any, fallback: T): T {
    if (!response) {
      return fallback;
    }

    if (response.data !== undefined) {
      return response.data as T;
    }

    return response as T;
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
    if (this.managerBi?.congesEnAttente !== undefined) {
      return Number(this.managerBi.congesEnAttente || 0);
    }

    const statsAny = this.stats as any;

    if (typeof statsAny.congesEnAttente === 'number') {
      return statsAny.congesEnAttente;
    }

    if (typeof statsAny.demandesEnAttente === 'number') {
      return statsAny.demandesEnAttente;
    }

    if (typeof statsAny.pendingLeaves === 'number') {
      return statsAny.pendingLeaves;
    }

    if (Array.isArray(this.conges) && this.conges.length > 0) {
      return this.conges.length;
    }

    return 0;
  }

  getTauxPresenceReel(): number {
    if (this.managerBi?.tauxPresence !== undefined) {
      return Math.round(Number(this.managerBi.tauxPresence || 0));
    }

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

  getCongesEnCoursReel(): number {
    if (!Array.isArray(this.conges)) {
      return 0;
    }

    return this.conges.filter((conge: any) => this.isCongeEnCours(conge)).length;
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

  getManagerTotalEmployes(): number {
    return Number(this.managerBi?.totalEmployes || this.equipe.length || 0);
  }

  getManagerCongesCharges(): number {
    return Number(this.managerBi?.congesEnAttente || this.conges.length || 0);
  }

  // =========================================================
  // EMPLOYE
  // =========================================================

 loadEmployeeDashboard(): void {
  this.loading = true;
  this.errorMessage = null;

  this.employeeDashboardService
    .getMesStats()
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (response) => {
        this.employeeStats = response.data;
        this.loading = false;
        this.loadRecommendations();
      },
      error: (err) => {
        console.error('Erreur stats employé:', err);
        this.employeeStats = null;
        this.loading = false;
        this.loadRecommendations();
      }
    });
}
loadRecommendations(): void {
  this.formationRecommendationService
    .generateMyRecommendations()
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (data: any[]) => {
        console.log('DASHBOARD RECOMMANDATIONS IA =', data);
        this.recommendations = Array.isArray(data) ? data : [];
      },
      error: (err: any) => {
        console.error('Erreur recommandations dashboard:', err);
        this.recommendations = [];
      }
    });
}
private getCurrentEmployeId(): number | null {
  const currentUser = this.authService.getCurrentUser() as any;

  const id =
    currentUser?.employeId ||
    currentUser?.employeeId ||
    currentUser?.userId ||
    currentUser?.id;

  if (id && !Number.isNaN(Number(id))) {
    return Number(id);
  }

  return null;
}
  // =========================================================
  // ACTIONS
  // =========================================================

  rafraichir(): void {
    this.refreshing = true;

    if (this.isAdmin()) {
      this.loadAdminDashboard();
      return;
    }

    if (this.isManager()) {
      this.loadManagerDashboard();
      return;
    }

    this.loadEmployeeDashboard();
  }

  navigateTo(link?: string): void {
    if (!link) return;
    this.router.navigate([link]);
  }

  goToProfile(): void {
    if (this.isAdmin()) {
      this.router.navigate(['/admin/profile']);
      return;
    }

    if (this.isManager()) {
      this.router.navigate(['/manager/profile']);
      return;
    }

    this.router.navigate(['/employee/mon-profil']);
  }

  openNotifications(): void {
    if (this.isAdmin()) {
      this.router.navigate(['/admin/notifications']);
      return;
    }

    if (this.isManager()) {
      this.router.navigate(['/manager/notifications']);
      return;
    }

    this.router.navigate(['/employee/notifications']);
  }

  exporterRapport(): void {
    console.log('Export rapport BI à implémenter si nécessaire.');
  }

  // =========================================================
  // FORMAT
  // =========================================================

  getAlertEmoji(type: string): string {
    return {
      danger: '🔴',
      warning: '🟡',
      info: '🔵',
      success: '🟢'
    }[type] ?? '⚪';
  }

  private formatTnd(value: number | null | undefined): string {
    const amount = Number(value || 0);

    const formatted = new Intl.NumberFormat('fr-FR', {
      maximumFractionDigits: 0
    }).format(amount);

    return `${formatted} DT`;
  }

  formatCurrency(value?: number): string {
    return this.formatTnd(value || 0);
  }

  formatNumber(value?: number): string {
    return Number(value || 0).toFixed(1);
  }


getProgressionChartStyle(): string {
  const value = Math.min(Math.max(Number(this.employeeStats?.progressionFormations || 0), 0), 100);

  return `conic-gradient(#ffffff ${value * 3.6}deg, rgba(255,255,255,0.25) 0deg)`;
}

getEmployeeBarWidth(value?: number): number {
  const max = Math.max(
    Number(this.employeeStats?.formationsTerminees || 0),
    Number(this.employeeStats?.competencesValidees || 0),
    Number(this.employeeStats?.certificatsObtenus || 0),
    Number(this.employeeStats?.recommandationsIA || 0),
    1
  );

  return Math.round((Number(value || 0) / max) * 100);
}
  
}