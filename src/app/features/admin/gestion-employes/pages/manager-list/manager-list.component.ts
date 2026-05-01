import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EmployeService } from '../../../../../core/services/employe.service';
import { Employe } from '../../models/employe.model';
import { HistoriqueCongesModalComponent } from '../historique-conges-modal/historique-conges-modal.component';

interface ManagerWithEquipe {
  manager: Employe;
  equipe: Employe[];
  expanded: boolean;
  loading?: boolean;
}

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './manager-list.component.html',
  styleUrls: ['./manager-list.component.scss']
})
export class ManagerListComponent implements OnInit {

  managers: ManagerWithEquipe[] = [];
  filteredManagers: ManagerWithEquipe[] = [];

  loading = false;
  teamsLoaded = false;          // ✅ indique si les équipes sont toutes préchargées
  searchTerm = '';
  selectedDept = 'ALL';
  sortMode: 'nameAsc' | 'nameDesc' | 'teamDesc' | 'teamAsc' = 'nameAsc';

  private cacheEquipes = new Map<number, Employe[]>();

  constructor(
    private employeService: EmployeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadManagers();
  }

  trackByManager(index: number, manager: ManagerWithEquipe): number {
    return manager.manager.id ?? index;
  }

  trackByEmploye(index: number, emp: Employe): number {
    return emp.id ?? index;
  }

  // ========================= 1. Chargement des managers (immédiat) =========================
  loadManagers(): void {
    this.loading = true;
    this.teamsLoaded = false;
    this.employeService.getAllManagers().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.managers = res.data.map((m: Employe) => ({
            manager: m,
            equipe: [],
            expanded: false,
            loading: false
          }));
          this.applyFilters();
          this.loading = false;
          this.cdr.detectChanges();
          // On lance le préchargement silencieux de toutes les équipes
          this.preloadAllEquipes();
        } else {
          this.error('Erreur chargement managers');
        }
      },
      error: () => this.error('Erreur de connexion')
    });
  }

  // ========================= 2. Préchargement silencieux (non bloquant) =========================
  private preloadAllEquipes(): void {
    if (!this.managers.length) {
      this.teamsLoaded = true;
      return;
    }
    let completed = 0;
    const total = this.managers.length;

    this.managers.forEach((mgr, idx) => {
      const id = mgr.manager.id;
      if (!id) {
        completed++;
        if (completed === total) this.onPreloadComplete();
        return;
      }
      if (this.cacheEquipes.has(id)) {
        this.managers[idx].equipe = this.cacheEquipes.get(id)!;
        completed++;
        if (completed === total) this.onPreloadComplete();
        return;
      }
      this.employeService.getEquipeByManagerId(id).subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.managers[idx].equipe = res.data;
            this.cacheEquipes.set(id, res.data);
            this.applyFilters(); // met à jour les KPI
          }
          completed++;
          if (completed === total) this.onPreloadComplete();
        },
        error: () => {
          completed++;
          if (completed === total) this.onPreloadComplete();
        }
      });
    });
  }

  private onPreloadComplete(): void {
    this.teamsLoaded = true;
    this.cdr.detectChanges();
  }

  // ========================= 3. Chargement à la demande (au clic, si manquant) =========================
  private loadEquipe(manager: ManagerWithEquipe): void {
    if (manager.equipe.length > 0) return;
    const id = manager.manager.id;
    if (!id) return;

    manager.loading = true;
    this.cdr.detectChanges();

    if (this.cacheEquipes.has(id)) {
      manager.equipe = this.cacheEquipes.get(id)!;
      manager.loading = false;
      this.applyFilters();
      this.cdr.detectChanges();
      return;
    }

    this.employeService.getEquipeByManagerId(id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          manager.equipe = res.data;
          this.cacheEquipes.set(id, res.data);
        } else {
          manager.equipe = [];
        }
        manager.loading = false;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: () => {
        manager.loading = false;
        manager.equipe = [];
        this.cdr.detectChanges();
        this.snackBar.open('Erreur chargement équipe', 'Fermer', { duration: 2000 });
      }
    });
  }

  // ========================= 4. Toggle =========================
  toggleEquipe(manager: ManagerWithEquipe): void {
    this.managers.forEach(m => {
      if (m !== manager && m.expanded) {
        m.expanded = false;
      }
    });
    manager.expanded = !manager.expanded;
    if (manager.expanded) {
      this.loadEquipe(manager);
    }
    this.cdr.detectChanges();
  }

  // ========================= 5. Filtre et tri =========================
  applyFilters(): void {
    let data = [...this.managers];
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      data = data.filter(m => (`${m.manager.prenom} ${m.manager.nom}`).toLowerCase().includes(term));
    }
    if (this.selectedDept !== 'ALL') {
      data = data.filter(m => m.manager.departement === this.selectedDept);
    }
    switch (this.sortMode) {
      case 'nameAsc': data.sort((a,b) => (a.manager.nom ?? '').localeCompare(b.manager.nom ?? '')); break;
      case 'nameDesc': data.sort((a,b) => (b.manager.nom ?? '').localeCompare(a.manager.nom ?? '')); break;
      case 'teamDesc': data.sort((a,b) => b.equipe.length - a.equipe.length); break;
      case 'teamAsc': data.sort((a,b) => a.equipe.length - b.equipe.length); break;
    }
    this.filteredManagers = data;
    this.cdr.detectChanges();
  }

  // ========================= STATS (réactifs) =========================
  get totalEmployees(): number {
    return this.managers.reduce((sum, m) => sum + m.equipe.length, 0);
  }

  get totalManagers(): number {
    return this.managers.length;
  }

  get totalTeams(): number {
    return this.managers.length;
  }

  get biggestTeam(): ManagerWithEquipe | null {
    if (!this.managers.length) return null;
    return this.managers.reduce((prev, curr) => curr.equipe.length > prev.equipe.length ? curr : prev);
  }

  get hasNoExpandedManager(): boolean {
    return this.filteredManagers.every(m => !m.expanded);
  }

  // ========================= MODAL =========================
  voirHistoriqueConges(emp: Employe): void {
    this.dialog.open(HistoriqueCongesModalComponent, {
      width: '800px',
      data: { employeId: emp.id, employeNom: `${emp.prenom} ${emp.nom}` }
    });
  }

  refresh(): void {
    this.cacheEquipes.clear();
    this.loadManagers();
  }

  // ========================= MÉTHODES D'UI (avatar, risque, etc.) =========================
  getAvatarColor(dept: string): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6', Technique: '#0891b2', Commercial: '#d97706',
      Finance: '#059669', Marketing: '#db2777', Direction: '#7c3aed', Logistique: '#4f46e5'
    };
    return colors[dept] || '#6366f1';
  }

  getRiskScore(manager: ManagerWithEquipe): number {
    const teamSize = manager.equipe.length;
    let score = 20;
    if (teamSize > 10) score += 20;
    if (teamSize > 20) score += 30;
    if (teamSize === 0) score += 40;
    return Math.min(score, 100);
  }

  getRiskLabel(score: number): string {
    if (score < 30) return 'Faible';
    if (score < 60) return 'Moyen';
    return 'Élevé';
  }

  getRiskColor(score: number): string {
    if (score < 30) return '#10b981';
    if (score < 60) return '#f59e0b';
    return '#ef4444';
  }

  getTeamIntensity(manager: ManagerWithEquipe): number {
    return Math.min(manager.equipe.length * 8, 100);
  }

  getPerformanceClass(riskScore: number): string {
    if (riskScore < 30) return 'excellent';
    if (riskScore < 60) return 'good';
    return 'warning';
  }

  getPerformanceLabel(riskScore: number): string {
    if (riskScore < 30) return '🌟 Excellent';
    if (riskScore < 60) return '👍 Bon';
    return '⚠️ À surveiller';
  }

  getTurnOverRate(manager: ManagerWithEquipe): number {
    const baseTurnover = 5;
    const risk = this.getRiskScore(manager);
    let extra = 0;
    if (risk > 70) extra = 20;
    else if (risk > 50) extra = 10;
    return Math.min(baseTurnover + extra, 35);
  }

  private error(msg: string): void {
    this.loading = false;
    this.snackBar.open(msg, 'Fermer', { duration: 3000 });
  }
}