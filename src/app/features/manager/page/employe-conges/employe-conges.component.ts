// src/app/features/manager/pages/employe-conges/employe-conges.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ManagerService } from '../../../../core/services/manager.service';
import { DemandeConge } from '../../../employee/models/conge.model';

@Component({
  selector: 'app-employe-conges',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employe-conges.component.html',
  styleUrls: ['./employe-conges.component.scss']
})
export class EmployeCongesComponent implements OnInit {
  conges: DemandeConge[] = [];
  loading = true;
  error = false;
  employeNom = '';
  employeId!: number;

  absenteisme: number | null = null;
  scoreTurnover: number | null = null;
  scoreTurnoverNiveau: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private managerService: ManagerService
  ) {}

  ngOnInit(): void {
    this.employeId = +this.route.snapshot.params['id'];
    if (this.employeId) {
      this.managerService.getEmployeDetails(this.employeId).subscribe({
        next: (res) => {
          if (res.success && res.data) {
            const emp = res.data as any;
            this.employeNom = `${emp.prenom} ${emp.nom}`;
          }
        },
        error: () => console.error('Erreur chargement employé')
      });

      this.managerService.getEmployeConges(this.employeId).subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success) this.conges = res.data as DemandeConge[];
          else this.error = true;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });

      forkJoin({
        score: this.managerService.getDernierScoreTurnover(this.employeId),
        abs: this.managerService.getDernierAbsenteisme(this.employeId)
      }).subscribe({
        next: (res) => {
          // Score
          this.scoreTurnover = res.score?.data?.score ?? null;
          this.scoreTurnoverNiveau = res.score?.data?.niveauRisque ?? null;

          // Absentéisme
          const absData = res.abs?.data;
          if (Array.isArray(absData) && absData.length > 0) {
            const monAbs = absData.find((item: any) => item.employeId === this.employeId);
            if (monAbs) {
              this.absenteisme = monAbs.valeur ?? null;
            } else {
              this.absenteisme = null;
            }
          } else {
            this.absenteisme = null;
          }
        },
        error: (err) => console.error(err)
      });
    } else {
      this.loading = false;
      this.error = true;
    }
  }

  getAbsenteismeClass(taux: number | null): string {
    if (taux === null) return 'badge-neutral';
    if (taux < 5) return 'abs-low';
    if (taux < 10) return 'abs-medium';
    return 'abs-high';
  }

  getScoreClass(score: number | null): string {
    if (score === null) return 'badge-neutral';
    if (score < 20) return 'score-low';
    if (score < 40) return 'score-medium';
    if (score < 70) return 'score-high';
    return 'score-critical';
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'statut-approuve';
      case 'EN_ATTENTE': return 'statut-attente';
      case 'REFUSE': return 'statut-refuse';
      case 'ANNULE': return 'statut-annule';
      default: return '';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return '✅ Approuvé';
      case 'EN_ATTENTE': return '⏳ En attente';
      case 'REFUSE': return '❌ Refusé';
      case 'ANNULE': return '🗑️ Annulé';
      default: return statut;
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'ANNUEL': return 'Annuel';
      case 'MALADIE': return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE': return 'Maternité';
      case 'PATERNITE': return 'Paternité';
      default: return type;
    }
  }

  formatDate(date: any): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR');
  }
}