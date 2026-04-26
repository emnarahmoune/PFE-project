// src/app/features/manager/pages/equipe/equipe.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ManagerService } from '../../../../core/services/manager.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './equipe.component.html',
  styleUrls: ['./equipe.component.scss']
})
export class EquipeComponent implements OnInit {
  employes: Employe[] = [];
  loading = true;

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getEquipe().subscribe({
      next: (res) => {
        if (res.success) {
          this.employes = res.data as Employe[];
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
          // Score
          emp.scoreTurnover = results.score?.data?.score ?? 0;
          emp.scoreTurnoverNiveau = results.score?.data?.niveauRisque ?? 'NON CALCULE';

          // Absentéisme : l'API retourne un tableau, on extrait l'indicateur pour cet employé
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
        },
        error: (err) => console.error(`Erreur pour ${emp.id}`, err)
      });
    });
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