// src/app/features/manager/pages/employe-conges/employe-conges.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
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
  employePoste = '';
  employeDepartement = '';
  employeId!: number;

  absenteisme: number | null = null;
  scoreTurnover: number | null = null;
  scoreTurnoverNiveau: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private managerService: ManagerService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.employeId = Number(this.route.snapshot.params['id']);

    if (!this.employeId) {
      this.loading = false;
      this.error = true;
      return;
    }

    this.loadEmployeeInfo();
    this.loadConges();
    this.loadIndicateurs();
  }

  goBack(): void {
    this.location.back();
  }

  private loadEmployeeInfo(): void {
    this.managerService.getEmployeDetails(this.employeId).subscribe({
      next: (res: any) => {
        if (res?.success && res?.data) {
          const emp = res.data as any;

          this.employeNom = `${emp.prenom || ''} ${emp.nom || ''}`.trim();
          this.employePoste = emp.poste || '';
          this.employeDepartement = emp.departement || '';
        }
      },
      error: () => {
        console.error('Erreur chargement employé');
      }
    });
  }

  private loadConges(): void {
    this.loading = true;
    this.error = false;

    this.managerService.getEmployeConges(this.employeId).subscribe({
      next: (res: any) => {
        this.loading = false;

        if (res?.success) {
          this.conges = Array.isArray(res.data) ? res.data : [];
        } else {
          this.error = true;
        }
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  private loadIndicateurs(): void {
    forkJoin({
      score: this.managerService.getDernierScoreTurnover(this.employeId),
      abs: this.managerService.getDernierAbsenteisme(this.employeId)
    }).subscribe({
      next: (res: any) => {
        this.scoreTurnover = res?.score?.data?.score ?? null;
        this.scoreTurnoverNiveau = res?.score?.data?.niveauRisque ?? null;

        const absData = res?.abs?.data;

        if (Array.isArray(absData) && absData.length > 0) {
          const monAbs = absData.find((item: any) => Number(item.employeId) === Number(this.employeId));
          this.absenteisme = monAbs?.valeur ?? null;
        } else if (absData && typeof absData === 'object') {
          this.absenteisme = absData.valeur ?? absData.taux ?? null;
        } else {
          this.absenteisme = null;
        }
      },
      error: (err) => {
        console.error('Erreur chargement indicateurs:', err);
      }
    });
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

  getStatutClass(statut: string | undefined | null): string {
    switch (statut) {
      case 'APPROUVE':
        return 'statut-approuve';
      case 'EN_ATTENTE':
        return 'statut-attente';
      case 'REFUSE':
        return 'statut-refuse';
      case 'ANNULE':
        return 'statut-annule';
      default:
        return 'statut-default';
    }
  }

  getStatutLabel(statut: string | undefined | null): string {
    switch (statut) {
      case 'APPROUVE':
        return 'Approuvé';
      case 'EN_ATTENTE':
        return 'En attente';
      case 'REFUSE':
        return 'Refusé';
      case 'ANNULE':
        return 'Annulé';
      default:
        return statut || 'Non défini';
    }
  }

  getTypeClass(type: string | undefined | null): string {
    return (type || 'conge').toLowerCase();
  }

  getTypeLabel(type: string | undefined | null): string {
    switch (type) {
      case 'ANNUEL':
        return 'Annuel';
      case 'MALADIE':
        return 'Maladie';
      case 'SANS_SOLDE':
        return 'Sans solde';
      case 'MATERNITE':
        return 'Maternité';
      case 'PATERNITE':
        return 'Paternité';
      default:
        return type || 'Congé';
    }
  }

  formatDate(date: any): string {
    if (!date) return 'Non renseignée';

    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  trackByCongeId(index: number, conge: DemandeConge): number | string {
    return (conge as any).id || index;
  }
}