import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { forkJoin, Subject, takeUntil } from 'rxjs';

import {
  ManagerService,
  ManagerStats
} from '../../core/services/manager.service';

import { Employe } from '../../core/models/employe.model';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

interface EmployeIndicateur extends Employe {
  absenteisme?: number;
  absenteismeDate?: string;
  scoreTurnover?: number;
  scoreTurnoverNiveau?: string;
  loadingIndicators?: boolean;
}

@Component({
  selector: 'app-indicateurs',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './indicateurs.component.html',
  styleUrls: ['./indicateurs.component.scss']
})
export class IndicateursComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  stats: ManagerStats | null = null;

  equipe: EmployeIndicateur[] = [];
  filteredEquipe: EmployeIndicateur[] = [];

  loading = false;
  loadingStats = false;
  loadingIndicators = false;

  errorMessage = '';

  searchTerm = '';
  selectedRisk = 'TOUS';
  sortBy = 'scoreDesc';

  riskFilters = [
    { value: 'TOUS', label: 'Tous' },
    { value: 'FAIBLE', label: 'Risque faible' },
    { value: 'MOYEN', label: 'Risque moyen' },
    { value: 'ELEVE', label: 'Risque élevé' }
  ];

  sortOptions = [
    { value: 'scoreDesc', label: 'Score turnover élevé' },
    { value: 'scoreAsc', label: 'Score turnover faible' },
    { value: 'absDesc', label: 'Absentéisme élevé' },
    { value: 'absAsc', label: 'Absentéisme faible' },
    { value: 'nomAsc', label: 'Nom A-Z' },
    { value: 'posteAsc', label: 'Poste A-Z' }
  ];

  readonly avatarColors: Record<string, string> = {
    RH: '#8b5cf6',
    Technique: '#0891b2',
    Commercial: '#d97706',
    Finance: '#059669',
    Marketing: '#db2777',
    Direction: '#7c3aed',
    Logistique: '#4f46e5'
  };

  constructor(
    private managerService: ManagerService
  ) {}

  ngOnInit(): void {
    this.loadIndicateurs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadIndicateurs(): void {
    this.errorMessage = '';
    this.loadStats();
    this.loadEquipe();
  }

  refresh(): void {
    this.loadIndicateurs();
  }

  private loadStats(): void {
    this.loadingStats = true;

    this.managerService.getStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: response => {
          this.stats = response?.success ? response.data : null;
          this.loadingStats = false;
        },
        error: error => {
          console.error('Erreur chargement stats manager:', error);
          this.stats = null;
          this.loadingStats = false;
        }
      });
  }

  private loadEquipe(): void {
    this.loading = true;
    this.errorMessage = '';

    this.managerService.getEquipe()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: response => {
          const data = this.unwrapResponse<EmployeIndicateur[]>(response, []);
          this.equipe = Array.isArray(data) ? data : [];
          this.filteredEquipe = [...this.equipe];

          this.loading = false;
          this.loadIndicatorsForEquipe();
        },
        error: error => {
          console.error('Erreur chargement équipe:', error);
          this.equipe = [];
          this.filteredEquipe = [];
          this.loading = false;
          this.errorMessage = 'Impossible de charger les indicateurs de votre équipe.';
        }
      });
  }

  private loadIndicatorsForEquipe(): void {
    if (!this.equipe.length) {
      this.applyFiltersAndSort();
      return;
    }

    this.loadingIndicators = true;

    const employeesWithIds = this.equipe.filter(emp => !!emp.id);

    if (!employeesWithIds.length) {
      this.loadingIndicators = false;
      this.applyFiltersAndSort();
      return;
    }

    const requests = employeesWithIds.map(emp => {
      emp.loadingIndicators = true;

      return forkJoin({
        score: this.managerService.getDernierScoreTurnover(emp.id!),
        abs: this.managerService.getDernierAbsenteisme(emp.id!)
      });
    });

    forkJoin(requests)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: results => {
          results.forEach((result: any, index: number) => {
            const emp = employeesWithIds[index];

            if (!emp) {
              return;
            }

            emp.scoreTurnover = result?.score?.data?.score ?? null;
            emp.scoreTurnoverNiveau = result?.score?.data?.niveauRisque ?? null;

            const absData = result?.abs?.data;

if (Array.isArray(absData) && absData.length > 0) {
  const currentAbs = absData.find(
    (item: any) => Number(item.employeId) === Number(emp.id)
  );

  emp.absenteisme = currentAbs?.valeur ?? undefined;
  emp.absenteismeDate = currentAbs?.dateCalcul ?? undefined;
} else if (absData && typeof absData === 'object') {
  emp.absenteisme = absData.valeur ?? absData.taux ?? undefined;
  emp.absenteismeDate = absData.dateCalcul ?? undefined;
} else {
  emp.absenteisme = undefined;
  emp.absenteismeDate = undefined;
}

emp.loadingIndicators = false;
          });

          this.loadingIndicators = false;
          this.applyFiltersAndSort();
        },
        error: error => {
          console.error('Erreur chargement indicateurs équipe:', error);

          employeesWithIds.forEach(emp => {
            emp.loadingIndicators = false;
          });

          this.loadingIndicators = false;
          this.applyFiltersAndSort();
        }
      });
  }

  applyFiltersAndSort(): void {
    let data = [...this.equipe];

    const search = this.normalizeText(this.searchTerm);

    if (search) {
      data = data.filter(emp => {
        const values = [
          emp.nom,
          emp.prenom,
          emp.email,
          emp.matricule,
          emp.poste,
          emp.departement,
          emp.scoreTurnoverNiveau
        ];

        return values.some(value =>
          this.normalizeText(value || '').includes(search)
        );
      });
    }

    if (this.selectedRisk !== 'TOUS') {
      data = data.filter(emp =>
        this.normalizeRisk(emp.scoreTurnoverNiveau) === this.selectedRisk
      );
    }

    switch (this.sortBy) {
      case 'scoreDesc':
        data.sort((a, b) => Number(b.scoreTurnover || 0) - Number(a.scoreTurnover || 0));
        break;

      case 'scoreAsc':
        data.sort((a, b) => Number(a.scoreTurnover || 0) - Number(b.scoreTurnover || 0));
        break;

      case 'absDesc':
        data.sort((a, b) => Number(b.absenteisme || 0) - Number(a.absenteisme || 0));
        break;

      case 'absAsc':
        data.sort((a, b) => Number(a.absenteisme || 0) - Number(b.absenteisme || 0));
        break;

      case 'posteAsc':
        data.sort((a, b) => String(a.poste || '').localeCompare(String(b.poste || '')));
        break;

      case 'nomAsc':
      default:
        data.sort((a, b) => String(a.nom || '').localeCompare(String(b.nom || '')));
        break;
    }

    this.filteredEquipe = data;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedRisk = 'TOUS';
    this.sortBy = 'scoreDesc';
    this.applyFiltersAndSort();
  }

  getTotalEmployes(): number {
    return this.equipe.length;
  }

  getEmployesActifs(): number {
    if (this.stats?.employesActifs !== undefined) {
      return Number(this.stats.employesActifs || 0);
    }

    return this.equipe.filter(emp =>
      this.normalizeText(emp.statut || '').includes('actif')
    ).length;
  }

  getCongesEnAttente(): number {
    return Number(this.stats?.congesEnAttente || 0);
  }

  getAbsenteismeMoyen(): number {
    const values = this.equipe
      .map(emp => Number(emp.absenteisme || 0))
      .filter(value => value > 0);

    if (!values.length) {
      return Number(this.stats?.absenteisme || 0);
    }

    const moyenne = values.reduce((sum, value) => sum + value, 0) / values.length;
    return Number(moyenne.toFixed(1));
  }

  getTurnoverMoyen(): number {
    const values = this.equipe
      .map(emp => Number(emp.scoreTurnover || 0))
      .filter(value => value > 0);

    if (!values.length) {
      return Number(this.stats?.turnover || 0);
    }

    const moyenne = values.reduce((sum, value) => sum + value, 0) / values.length;
    return Number(moyenne.toFixed(1));
  }

  getHighRiskCount(): number {
    return this.equipe.filter(emp =>
      this.normalizeRisk(emp.scoreTurnoverNiveau) === 'ELEVE' ||
      Number(emp.scoreTurnover || 0) >= 70
    ).length;
  }

  getRiskClass(emp: EmployeIndicateur): string {
    const normalized = this.normalizeRisk(emp.scoreTurnoverNiveau);
    const score = Number(emp.scoreTurnover || 0);

    if (normalized === 'ELEVE' || score >= 70) {
      return 'risk-high';
    }

    if (normalized === 'MOYEN' || score >= 40) {
      return 'risk-medium';
    }

    if (normalized === 'FAIBLE' || score > 0) {
      return 'risk-low';
    }

    return 'risk-neutral';
  }

  getRiskLabel(emp: EmployeIndicateur): string {
    if (emp.scoreTurnoverNiveau) {
      return emp.scoreTurnoverNiveau;
    }

    const score = Number(emp.scoreTurnover || 0);

    if (score >= 70) return 'Élevé';
    if (score >= 40) return 'Moyen';
    if (score > 0) return 'Faible';

    return 'Non calculé';
  }

  getScorePercent(emp: EmployeIndicateur): number {
    const value = Number(emp.scoreTurnover || 0);

    if (value < 0) return 0;
    if (value > 100) return 100;

    return value;
  }

  getAbsenteismePercent(emp: EmployeIndicateur): number {
    const value = Number(emp.absenteisme || 0);

    if (value < 0) return 0;
    if (value > 100) return 100;

    return value;
  }

  getAvatarColor(departement?: string | null): string {
    return this.avatarColors[departement || ''] || '#6366f1';
  }

  getFullName(emp: EmployeIndicateur): string {
    return `${emp.prenom || ''} ${emp.nom || ''}`.trim() ||
      emp.email ||
      'Employé';
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

  private normalizeText(value: string): string {
    return String(value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  private normalizeRisk(value?: string | null): string {
    const normalized = this.normalizeText(value || '');

    if (
      normalized.includes('eleve') ||
      normalized.includes('haut') ||
      normalized.includes('high')
    ) {
      return 'ELEVE';
    }

    if (
      normalized.includes('moyen') ||
      normalized.includes('medium')
    ) {
      return 'MOYEN';
    }

    if (
      normalized.includes('faible') ||
      normalized.includes('bas') ||
      normalized.includes('low')
    ) {
      return 'FAIBLE';
    }

    return '';
  }
}