import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { ManagerService } from '../../../../core/services/manager.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';
import { EmployeeAvatarComponent } from '../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

interface EmployeExtended extends Employe {
  absenteismeDate?: string;
  scoreTurnover?: number;
  scoreTurnoverNiveau?: string;
}

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './equipe.component.html',
  styleUrls: ['./equipe.component.scss']
})
export class EquipeComponent implements OnInit {
  employes: EmployeExtended[] = [];
  filteredEmployes: EmployeExtended[] = [];

  loading = true;
  loadingIndicators = false;

  searchTerm = '';
  sortBy = 'nom';

  sortOptions = [
    { value: 'nom', label: 'Nom A-Z' },
    { value: 'poste', label: 'Poste A-Z' },
    { value: 'departement', label: 'Département A-Z' },
    { value: 'statut', label: 'Statut' },
    { value: 'absenteismeDesc', label: 'Absentéisme élevé' },
    { value: 'scoreDesc', label: 'Risque élevé' },
    { value: 'dateEmbaucheDesc', label: 'Embauche récente' }
  ];

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.loadEquipe();
  }

  loadEquipe(): void {
    this.loading = true;

    this.managerService.getEquipe().subscribe({
      next: (res: any) => {
        const data = this.unwrapResponse<EmployeExtended[]>(res, []);
        this.employes = Array.isArray(data) ? data : [];

        this.applyFiltersAndSort();
        this.loading = false;

        this.loadIndicators();
      },
      error: (err: any) => {
        console.error('Erreur chargement équipe:', err);
        this.employes = [];
        this.filteredEmployes = [];
        this.loading = false;
      }
    });
  }

  private loadIndicators(): void {
    if (!this.employes.length) {
      return;
    }

    this.loadingIndicators = true;

    const requests = this.employes
      .filter(emp => !!emp.id)
      .map(emp => {
        return forkJoin({
          score: this.managerService.getDernierScoreTurnover(emp.id!),
          abs: this.managerService.getDernierAbsenteisme(emp.id!)
        });
      });

    if (!requests.length) {
      this.loadingIndicators = false;
      return;
    }

    forkJoin(requests).subscribe({
      next: (results: any[]) => {
        const employeesWithIds = this.employes.filter(emp => !!emp.id);

        results.forEach((result, index) => {
          const emp = employeesWithIds[index];

          if (!emp) {
            return;
          }

          emp.scoreTurnover = result?.score?.data?.score ?? null;
          emp.scoreTurnoverNiveau = result?.score?.data?.niveauRisque ?? null;

          const absData = result?.abs?.data;

          if (Array.isArray(absData) && absData.length > 0) {
            const monAbs = absData.find((item: any) => Number(item.employeId) === Number(emp.id));

            emp.absenteisme = monAbs?.valeur ?? undefined;
            emp.absenteismeDate = monAbs?.dateCalcul ?? undefined;
          } else if (absData && typeof absData === 'object') {
            emp.absenteisme = absData.valeur ?? absData.taux ?? null;
            emp.absenteismeDate = absData.dateCalcul ?? null;
          } else {
            emp.absenteisme = undefined;
            emp.absenteismeDate = undefined;
          }
        });

        this.applyFiltersAndSort();
        this.loadingIndicators = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement indicateurs équipe:', err);
        this.loadingIndicators = false;
      }
    });
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

  filterEmployes(): void {
    this.applyFiltersAndSort();
  }

  sortEmployes(): void {
    this.applyFiltersAndSort();
  }

  applyFiltersAndSort(): void {
    const term = this.normalizeText(this.searchTerm);

    let result = [...this.employes];

    if (term) {
      result = result.filter(emp => {
        const fullName = this.normalizeText(`${emp.prenom || ''} ${emp.nom || ''}`);
        const reverseName = this.normalizeText(`${emp.nom || ''} ${emp.prenom || ''}`);
        const poste = this.normalizeText(emp.poste || '');
        const departement = this.normalizeText(emp.departement || '');
        const email = this.normalizeText(emp.email || '');

        return (
          fullName.includes(term) ||
          reverseName.includes(term) ||
          poste.includes(term) ||
          departement.includes(term) ||
          email.includes(term)
        );
      });
    }

    result.sort((a, b) => this.compareEmployes(a, b));

    this.filteredEmployes = result;
  }

  private compareEmployes(a: EmployeExtended, b: EmployeExtended): number {
    switch (this.sortBy) {
      case 'poste':
        return this.compareText(a.poste, b.poste) || this.compareText(a.nom, b.nom);

      case 'departement':
        return this.compareText(a.departement, b.departement) || this.compareText(a.nom, b.nom);

      case 'statut':
        return this.compareText(a.statut, b.statut) || this.compareText(a.nom, b.nom);

      case 'absenteismeDesc':
        return this.compareNumberDesc(a.absenteisme, b.absenteisme) || this.compareText(a.nom, b.nom);

      case 'scoreDesc':
        return this.compareNumberDesc(a.scoreTurnover, b.scoreTurnover) || this.compareText(a.nom, b.nom);

      case 'dateEmbaucheDesc':
        return this.compareDateDesc(a.dateEmbauche, b.dateEmbauche) || this.compareText(a.nom, b.nom);

      case 'nom':
      default:
        return this.compareText(
          `${a.nom || ''} ${a.prenom || ''}`,
          `${b.nom || ''} ${b.prenom || ''}`
        );
    }
  }

  private compareText(a?: string | null, b?: string | null): number {
    return this.normalizeText(a || '').localeCompare(
      this.normalizeText(b || ''),
      'fr',
      { sensitivity: 'base' }
    );
  }

  private compareNumberDesc(a?: number | null, b?: number | null): number {
    const valueA = a ?? -1;
    const valueB = b ?? -1;

    return valueB - valueA;
  }

  private compareDateDesc(a?: string | Date | null, b?: string | Date | null): number {
    const dateA = a ? new Date(a).getTime() : 0;
    const dateB = b ? new Date(b).getTime() : 0;

    return dateB - dateA;
  }

  private normalizeText(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  getFullName(emp: EmployeExtended): string {
    return `${emp.prenom || ''} ${emp.nom || ''}`.trim() || 'Employé';
  }

  getStatutClass(statut?: string | null): string {
    const normalized = this.normalizeText(statut || '');

    if (normalized.includes('actif')) {
      return 'actif';
    }

    if (normalized.includes('conge') || normalized.includes('congé')) {
      return 'conge';
    }

    if (normalized.includes('inactif')) {
      return 'inactif';
    }

    return 'default';
  }

  getScoreClass(score: number | null | undefined): string {
    if (score == null) {
      return 'badge-neutral';
    }

    if (score < 20) {
      return 'score-low';
    }

    if (score < 40) {
      return 'score-medium';
    }

    if (score < 70) {
      return 'score-high';
    }

    return 'score-critical';
  }

  getAbsenteismeClass(taux: number | null | undefined): string {
    if (taux == null) {
      return 'badge-neutral';
    }

    if (taux < 5) {
      return 'abs-low';
    }

    if (taux < 10) {
      return 'abs-medium';
    }

    return 'abs-high';
  }

  trackByEmployeId(index: number, emp: EmployeExtended): number | string {
    return emp.id || index;
  }
}