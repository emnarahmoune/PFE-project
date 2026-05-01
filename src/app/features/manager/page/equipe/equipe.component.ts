import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ManagerService } from '../../../../core/services/manager.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';

/** ✅ Extension du modèle */
interface EmployeExtended extends Employe {
  absenteisme?: number | null;
  absenteismeDate?: string;
  scoreTurnover?: number;
  scoreTurnoverNiveau?: string;
}

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './equipe.component.html',
  styleUrls: ['./equipe.component.scss']
})
export class EquipeComponent implements OnInit {

  employes: EmployeExtended[] = [];
  filteredEmployes: EmployeExtended[] = [];

  loading = true;
  searchTerm = '';
  sortBy = 'nom';

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getEquipe().subscribe({
      next: (res) => {
        if (res.success) {
          this.employes = res.data as EmployeExtended[];
          this.filteredEmployes = [...this.employes];
          this.loadIndicators();
        }
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  private loadIndicators(): void {
    this.employes.forEach(emp => {
      if (!emp.id) return;

      forkJoin({
        score: this.managerService.getDernierScoreTurnover(emp.id),
        abs: this.managerService.getDernierAbsenteisme(emp.id)
      }).subscribe({
        next: (results: any) => {

          // Score turnover
          emp.scoreTurnover = results.score?.data?.score ?? null;
          emp.scoreTurnoverNiveau = results.score?.data?.niveauRisque ?? null;

          // Absenteisme
          const absData = results.abs?.data;

          if (Array.isArray(absData) && absData.length > 0) {
            const monAbs = absData.find((item: any) => item.employeId === emp.id);

            if (monAbs) {
              emp.absenteisme = monAbs.valeur ?? null;
              emp.absenteismeDate = monAbs.dateCalcul ?? null;
            } else {
              emp.absenteisme = null;
            }
          } else {
            emp.absenteisme = null;
          }

          this.filterEmployes();

        },
        error: (err) => console.error(`Erreur pour ${emp.id}`, err)
      });
    });
  }

  filterEmployes(): void {
    const term = this.searchTerm.toLowerCase().trim();

    if (!term) {
      this.filteredEmployes = [...this.employes];
    } else {
      this.filteredEmployes = this.employes.filter(emp =>
        (emp.prenom || '').toLowerCase().includes(term) ||
        (emp.nom || '').toLowerCase().includes(term)
      );
    }

    this.sortEmployes();
  }

  sortEmployes(): void {
    const sorted = [...this.filteredEmployes];

    switch (this.sortBy) {
      case 'poste':
        sorted.sort((a, b) => (a.poste || '').localeCompare(b.poste || ''));
        break;

      case 'departement':
        sorted.sort((a, b) => (a.departement || '').localeCompare(b.departement || ''));
        break;

      default:
        sorted.sort((a, b) => {
          const nomA = (a.nom || '') + (a.prenom || '');
          const nomB = (b.nom || '') + (b.prenom || '');
          return nomA.localeCompare(nomB);
        });
    }

    this.filteredEmployes = sorted;
  }

  getScoreClass(score: number | null | undefined): string {
    if (score == null) return 'badge-neutral';
    if (score < 20) return 'score-low';
    if (score < 40) return 'score-medium';
    if (score < 70) return 'score-high';
    return 'score-critical';
  }

  getAbsenteismeClass(taux: number | null | undefined): string {
    if (taux == null) return 'badge-neutral';
    if (taux < 5) return 'abs-low';
    if (taux < 10) return 'abs-medium';
    return 'abs-high';
  }
}