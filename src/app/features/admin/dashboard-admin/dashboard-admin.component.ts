import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedChartsModule } from '../../../shared/shared-charts.module';   // ← module enveloppe
import { ChartConfiguration, ChartData } from 'chart.js';
import { Subject, takeUntil, finalize, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DashboardService, DashboardStats, EmployeRecent, Alerte, Competence } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-dashboard-admin',
  standalone: true,
  imports: [CommonModule, RouterModule, SharedChartsModule],   // ← import du module
  templateUrl: './dashboard-admin.component.html',
  styleUrls: ['./dashboard-admin.component.scss']
})
export class DashboardAdminComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  currentDate = new Date();
  currentYear = this.currentDate.getFullYear();
  currentMonth = this.currentDate.toLocaleString('fr-FR', { month: 'long' });
  welcomeMessage = '';

  loading = true;
  refreshing = false;
  errorMessage: string | null = null;

  userNom = '';
  userPrenom = '';
  userEmail = '';
  userRole = '';

  stats: DashboardStats = {
    employesActifs: 0,
    totalEmployes: 0,
    turnover: 0,
    absenteisme: 0,
    demandesConge: 0,
    formationsEnCours: 0,
    scoresRisque: 0,
    masseSalariale: 0,
    salaireMoyen: 0,
    parDepartement: {},
    parStatut: {}
  };

  statCards: any[] = [];
  recentEmployees: EmployeRecent[] = [];
  alerts: Alerte[] = [];
  topCompetences: Competence[] = [];

  private readonly deptColors: Record<string, string> = {
    'RH': '#5B3FA6',
    'Technique': '#0C6E8C',
    'Commercial': '#B45309',
    'Finance': '#1A5C3A',
    'Marketing': '#9C2461',
    'Direction': '#1B3A6B',
    'Logistique': '#3D5A9E',
  };

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
    private router: Router // 👈 utile pour navigate()
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.setWelcomeMessage();
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userEmail = user.email || '';
      this.userRole = user.role || user.typeUtilisateur || 'EMPLOYE';
    }
  }

  setWelcomeMessage(): void {
    const hour = new Date().getHours();
    if (hour < 12) {
      this.welcomeMessage = 'Bonjour';
    } else if (hour < 18) {
      this.welcomeMessage = 'Bon après‑midi';
    } else {
      this.welcomeMessage = 'Bonsoir';
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = null;

    forkJoin({
      stats: this.dashboardService.getDashboardStats().pipe(catchError(() => of(this.stats))),
      employes: this.dashboardService.getEmployesRecents(5).pipe(catchError(() => of([]))),
      alertes: this.dashboardService.getAlertes().pipe(catchError(() => of([]))),
      turnover: this.dashboardService.getTurnoverData().pipe(catchError(() => of({ labels: [], data: [] }))),
      repartition: this.dashboardService.getRepartitionEmployes().pipe(catchError(() => of({ labels: [], data: [] }))),
      competences: this.dashboardService.getTopCompetences(5).pipe(catchError(() => of([])))
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.refreshing = false;
        })
      )
      .subscribe({
        next: (data: any) => {
          this.stats = data.stats || this.stats;
          this.buildStatCards();
          this.recentEmployees = data.employes || [];
          this.alerts = data.alertes || [];
          this.topCompetences = data.competences || [];

          if (data.turnover?.labels) {
            this.turnoverChartData = {
              ...this.turnoverChartData,
              labels: data.turnover.labels,
              datasets: [{ ...this.turnoverChartData.datasets[0], data: data.turnover.data }]
            };
          }
          if (data.repartition?.labels) {
            this.employeesChartData = {
              ...this.employeesChartData,
              labels: data.repartition.labels,
              datasets: [{ ...this.employeesChartData.datasets[0], data: data.repartition.data }]
            };
          }
        },
        error: () => {
          this.errorMessage = 'Impossible de charger les données. Veuillez réessayer.';
        }
      });
  }

  buildStatCards(): void {
    const t = this.stats.turnover || 0;
    const a = this.stats.absenteisme || 0;
    this.statCards = [
      {
        title: 'Employés actifs',
        value: this.stats.employesActifs,
        icon: '👥',
        trend: 'stable',
        change: null,
        link: '/admin/employes'
      },
      {
        title: 'Turnover',
        value: t.toFixed(1) + '%',
        icon: '🔄',
        trend: t > 10 ? 'up' : 'down',
        change: t > 10 ? '↑ élevé' : '↓ normal',
        link: '/admin/indicateurs'
      },
      {
        title: 'Absentéisme',
        value: a.toFixed(1) + '%',
        icon: '📅',
        trend: a > 5 ? 'up' : 'down',
        change: a > 5 ? '↑ haut' : '↓ bas',
        link: '/admin/indicateurs'
      },
      {
        title: 'Congés en attente',
        value: this.stats.demandesConge,
        icon: '🏖️',
        trend: 'stable',
        change: null,
        link: '/admin/conges'
      },
      {
        title: 'Formations actives',
        value: this.stats.formationsEnCours,
        icon: '📚',
        trend: 'stable',
        change: null,
        link: '/admin/formations'
      },
      {
        title: 'Risques de départ',
        value: this.stats.scoresRisque,
        icon: '⚠️',
        trend: this.stats.scoresRisque > 5 ? 'up' : 'stable',
        change: this.stats.scoresRisque > 5 ? '↑ alerte' : null,
        link: '/admin/scores'
      }
    ];
  }

  getAlertEmoji(type: string): string {
    return { danger: '🔴', warning: '🟡', info: '🔵', success: '🟢' }[type] ?? '⚪';
  }

  getAvatarBg(dept: string): string {
    return this.deptColors[dept] ?? '#3D4F5F';
  }

  getDepartements(): string[] {
    return Object.keys(this.stats.parDepartement).slice(0, 5);
  }

  turnoverChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{
      data: [],
      label: 'Turnover (%)',
      backgroundColor: '#4A72B0',
      borderRadius: 6,
      borderWidth: 0,
      hoverBackgroundColor: '#1B3A6B'
    }]
  };

  turnoverChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#0F1923',
        titleColor: '#fff',
        bodyColor: '#B8C4CC'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: { color: '#E8EAE6' },
        ticks: { font: { family: 'Inter', size: 11 } }
      },
      x: {
        grid: { display: false },
        ticks: { font: { family: 'Inter', size: 12 } }
      }
    }
  };

  employeesChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: [{
      data: [],
      backgroundColor: ['#1A5C3A', '#B45309', '#8B1A1A', '#1B3A6B'],
      borderWidth: 0,
      hoverOffset: 8
    }]
  };

  employeesChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          font: { family: 'Inter', size: 12 },
          color: '#4B5563',
          boxWidth: 12,
          padding: 16
        }
      }
    },
    cutout: '65%'
  };

  rafraichir(): void {
    this.refreshing = true;
    this.loadDashboardData();
  }

  exporterRapport(): void {
    this.dashboardService.exporterRapport();
  }

  getMasseSalarialeFormatee(): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(this.stats.masseSalariale || 0);
  }

  getSalaireMoyenFormate(): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0
    }).format(this.stats.salaireMoyen || 0);
  }

  getTauxRemplissage(dept: string): number {
    const max = Math.max(...Object.values(this.stats.parDepartement).map(Number), 1);
    return Math.round(((this.stats.parDepartement[dept] || 0) / max) * 100);
  }

  navigateTo(link: string): void {
    if (link) {
      this.router.navigate([link]);
    }
  }

  goToProfile(): void {
    this.router.navigate(['/admin/profile']);
  }

  openNotifications(): void {
    this.router.navigate(['/admin/notifications']);
    console.log('Ouverture du panneau des notifications');
  }
}