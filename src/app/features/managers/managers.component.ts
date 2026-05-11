import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { EmployeService } from '../../core/services/employe.service';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

import { Employe } from '../../core/models/employe.model';
import { DemandeConge } from '../employee/models/conge.model';

type ManagersMode =
  | 'ADMIN_MANAGER_LISTE'
  | 'ADMIN_MANAGER_DETAIL';

interface ManagerWithEquipe {
  manager: Employe;
  equipe: Employe[];
  expanded: boolean;
  loading?: boolean;
}

@Component({
  selector: 'app-managers',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatSnackBarModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './managers.component.html',
  styleUrls: ['./managers.component.scss']
})
export class ManagersComponent implements OnInit {
  mode: ManagersMode = 'ADMIN_MANAGER_LISTE';

  managers: ManagerWithEquipe[] = [];
  filteredManagers: ManagerWithEquipe[] = [];

  manager: Employe | null = null;
  equipe: Employe[] = [];

  loading = false;
  teamsLoaded = false;

  searchTerm = '';
  selectedDept = 'ALL';
  sortMode: 'nameAsc' | 'nameDesc' | 'teamDesc' | 'teamAsc' = 'nameAsc';

  private cacheEquipes = new Map<number, Employe[]>();

  showCongesModal = false;
  modalEmployeId: number | null = null;
  modalEmployeNom = '';
  conges: DemandeConge[] = [];
  loadingConges = false;

  constructor(
    private route: ActivatedRoute,
    private employeService: EmployeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.mode = this.route.snapshot.data['managersMode'] || 'ADMIN_MANAGER_LISTE';

    if (this.isListMode()) {
      this.loadManagers();
      return;
    }

    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (id) {
      this.loadManager(id);
    } else {
      this.loading = false;
      this.snackBar.open('Identifiant manager manquant', 'Fermer', {
        duration: 3000
      });
    }
  }

  isListMode(): boolean {
    return this.mode === 'ADMIN_MANAGER_LISTE';
  }

  isDetailMode(): boolean {
    return this.mode === 'ADMIN_MANAGER_DETAIL';
  }

  trackByManager(index: number, manager: ManagerWithEquipe): number {
    return manager.manager.id ?? index;
  }

  trackByEmploye(index: number, emp: Employe): number {
    return emp.id ?? index;
  }

  // =========================================================
  // LISTE MANAGERS
  // =========================================================

  loadManagers(): void {
    this.loading = true;
    this.teamsLoaded = false;

    this.employeService.getAllManagers().subscribe({
      next: (res: any) => {
        if (res?.success && Array.isArray(res.data)) {
          this.managers = res.data.map((m: Employe) => ({
            manager: m,
            equipe: [],
            expanded: false,
            loading: false
          }));

          this.applyFilters();
          this.loading = false;
          this.cdr.detectChanges();

          this.preloadAllEquipes();
        } else {
          this.loading = false;
          this.error('Erreur chargement managers');
        }
      },
      error: () => {
        this.loading = false;
        this.error('Erreur de connexion');
      }
    });
  }

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
        next: (res: any) => {
          if (res?.success && Array.isArray(res.data)) {
            this.managers[idx].equipe = res.data;
            this.cacheEquipes.set(id, res.data);
            this.applyFilters();
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
    this.applyFilters();
    this.cdr.detectChanges();
  }

  private loadEquipe(manager: ManagerWithEquipe): void {
    if (manager.equipe.length > 0) {
      return;
    }

    const id = manager.manager.id;

    if (!id) {
      return;
    }

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
      next: (res: any) => {
        if (res?.success && Array.isArray(res.data)) {
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

        this.snackBar.open('Erreur chargement équipe', 'Fermer', {
          duration: 2000
        });
      }
    });
  }

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

  applyFilters(): void {
    let data = [...this.managers];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();

      data = data.filter(m =>
        `${m.manager.prenom || ''} ${m.manager.nom || ''}`.toLowerCase().includes(term) ||
        (m.manager.email || '').toLowerCase().includes(term) ||
        (m.manager.matricule || '').toLowerCase().includes(term)
      );
    }

    if (this.selectedDept !== 'ALL') {
      data = data.filter(m => m.manager.departement === this.selectedDept);
    }

    switch (this.sortMode) {
      case 'nameAsc':
        data.sort((a, b) => (a.manager.nom ?? '').localeCompare(b.manager.nom ?? ''));
        break;

      case 'nameDesc':
        data.sort((a, b) => (b.manager.nom ?? '').localeCompare(a.manager.nom ?? ''));
        break;

      case 'teamDesc':
        data.sort((a, b) => b.equipe.length - a.equipe.length);
        break;

      case 'teamAsc':
        data.sort((a, b) => a.equipe.length - b.equipe.length);
        break;
    }

    this.filteredManagers = data;
    this.cdr.detectChanges();
  }

  refresh(): void {
    this.cacheEquipes.clear();
    this.loadManagers();
  }

  get totalEmployees(): number {
    return this.managers.reduce((sum, m) => sum + m.equipe.length, 0);
  }

  get totalManagers(): number {
    return this.managers.length;
  }

  get totalTeams(): number {
    return this.managers.filter(m => m.equipe.length > 0).length;
  }

  get biggestTeam(): ManagerWithEquipe | null {
    if (!this.managers.length) {
      return null;
    }

    return this.managers.reduce((prev, curr) =>
      curr.equipe.length > prev.equipe.length ? curr : prev
    );
  }

  get hasNoExpandedManager(): boolean {
    return this.filteredManagers.every(m => !m.expanded);
  }

  // =========================================================
  // DETAIL MANAGER
  // =========================================================

  loadManager(id: number): void {
    this.loading = true;

    this.employeService.getById(id).subscribe({
      next: (res: any) => {
        if (res?.success && res.data) {
          this.manager = res.data as Employe;
          this.loadEquipeForDetail(id);
        } else {
          this.snackBar.open('Manager introuvable', 'Fermer', {
            duration: 3000
          });
          this.loading = false;
        }
      },
      error: () => {
        this.snackBar.open('Erreur chargement manager', 'Fermer', {
          duration: 3000
        });
        this.loading = false;
      }
    });
  }

  private loadEquipeForDetail(managerId: number): void {
    this.employeService.getEquipeByManagerId(managerId).subscribe({
      next: (res: any) => {
        this.equipe = res?.success && Array.isArray(res.data) ? res.data : [];
        this.loading = false;
      },
      error: () => {
        this.equipe = [];
        this.loading = false;

        this.snackBar.open('Erreur chargement équipe', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  // =========================================================
  // HISTORIQUE CONGÉS MODAL INTÉGRÉ
  // =========================================================

  voirHistoriqueConges(employe: Employe): void {
    if (!employe?.id) {
      this.snackBar.open('Employé invalide', 'Fermer', {
        duration: 3000
      });
      return;
    }

    this.modalEmployeId = employe.id;
    this.modalEmployeNom = `${employe.prenom || ''} ${employe.nom || ''}`.trim();
    this.showCongesModal = true;

    this.loadConges(employe.id);
  }

  voirHistorique(employe: Employe): void {
    this.voirHistoriqueConges(employe);
  }

  private loadConges(employeId: number): void {
    this.loadingConges = true;
    this.conges = [];

    this.employeService.getEmployeConges(employeId).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.conges = res.data || [];
        } else {
          this.snackBar.open('Erreur chargement des congés', 'Fermer', {
            duration: 3000
          });
        }

        this.loadingConges = false;
      },
      error: () => {
        this.snackBar.open('Erreur technique', 'Fermer', {
          duration: 3000
        });

        this.loadingConges = false;
      }
    });
  }

  closeCongesModal(): void {
    this.showCongesModal = false;
    this.modalEmployeId = null;
    this.modalEmployeNom = '';
    this.conges = [];
    this.loadingConges = false;
  }

  // =========================================================
  // HELPERS
  // =========================================================

  getAvatarColor(dept: string): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6',
      Technique: '#0891b2',
      Commercial: '#d97706',
      Finance: '#059669',
      Marketing: '#db2777',
      Direction: '#7c3aed',
      Logistique: '#4f46e5'
    };

    return colors[dept] || '#6366f1';
  }

  getManagerFullName(manager: Employe | null | undefined): string {
    if (!manager) return '';

    return `${manager.prenom || ''} ${manager.nom || ''}`.trim() ||
      manager.email ||
      'Manager';
  }

  getEmployeFullName(employe: Employe): string {
    return `${employe.prenom || ''} ${employe.nom || ''}`.trim() ||
      employe.email ||
      'Employé';
  }

  private error(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000
    });
  }
}