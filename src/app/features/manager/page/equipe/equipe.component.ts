import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ManagerService } from '../../../../core/services/manager.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './equipe.component.html',
  styleUrls: ['./equipe.component.scss']
})
export class EquipeComponent implements OnInit {
  employes: Employe[] = [];
  filteredEmployes: Employe[] = [];
  loading = true;
  searchTerm = '';
  sortBy = 'nom';

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getEquipe().subscribe({
      next: (res) => {
        if (res.success) {
          this.employes = res.data as Employe[];
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
        next: (results) => {
          emp.scoreTurnover = results.score?.data?.score ?? 0;
          emp.scoreTurnoverNiveau = results.score?.data?.niveauRisque ?? 'NON CALCULE';

          const absData = results.abs?.data;
          if (Array.isArray(absData) && absData.length > 0) {
            const monAbs = absData.find((item: any) => item.employeId === emp.id);
            if (monAbs) {
              emp.absenteisme = monAbs.valeur ?? 0;
              emp.absenteismeDate = monAbs.dateCalcul;
            } else {
              emp.absenteisme = 0;
            }
          } else {
            emp.absenteisme = 0;
          }
          this.filterEmployes(); // met à jour la liste filtrée après chargement des indicateurs
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
    this.sortEmployes(); // tri après filtrage
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
      default: // nom
        sorted.sort((a, b) => {
          const nomA = (a.nom || '') + (a.prenom || '');
          const nomB = (b.nom || '') + (b.prenom || '');
          return nomA.localeCompare(nomB);
        });
    }
    this.filteredEmployes = sorted;
  }

  getScoreClass(score: number | undefined): string {
    if (score === undefined) return 'badge-neutral';
    if (score < 20) return 'score-low';
    if (score < 40) return 'score-medium';
    if (score < 70) return 'score-high';
    return 'score-critical';
  }

  getAbsenteismeClass(taux: number | undefined): string {
    if (taux === undefined) return 'badge-neutral';
    if (taux < 5) return 'abs-low';
    if (taux < 10) return 'abs-medium';
    return 'abs-high';
  }
}