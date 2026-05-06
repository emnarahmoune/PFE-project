// src/app/features/manager/pages/employe-detail/employe-detail.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { ManagerService, Manager } from '../../../../core/services/manager.service';
import { EmployeService } from '../../../../core/services/employe.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';
import { EmployeeAvatarComponent } from '../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-employe-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './employe-detail.component.html',
  styleUrls: ['./employe-detail.component.scss']
})
export class EmployeDetailComponent implements OnInit {
  employe: Employe | null = null;

  loading = true;
  error = false;

  managersList: Manager[] = [];

  absenteisme: number | null = null;
  absenteismeDate: string | null = null;

  scoreTurnover: number | null = null;
  scoreTurnoverNiveau: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private managerService: ManagerService,
    private employeService: EmployeService,
    private location: Location
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.params['id']);

    if (id) {
      this.loadEmploye(id);
      this.loadManagers();
      this.loadIndicateurs(id);
    } else {
      this.loading = false;
      this.error = true;
    }
  }

  goBack(): void {
    this.location.back();
  }

  loadEmploye(id: number): void {
    this.loading = true;
    this.error = false;

    this.employeService.getById(id).subscribe({
      next: (res: any) => {
        this.loading = false;

        if (res?.success && res?.data) {
          this.employe = res.data;
        } else {
          this.error = true;
        }
      },
      error: (err: any) => {
        console.error('Erreur chargement employé:', err);
        this.loading = false;
        this.error = true;
      }
    });
  }

  loadManagers(): void {
    this.managerService.getAllManagers().subscribe({
      next: (response: any) => {
        if (response?.success) {
          this.managersList = response.data || [];
        }
      },
      error: () => {
        this.managersList = [];
      }
    });
  }

  private loadIndicateurs(employeId: number): void {
    forkJoin({
      score: this.managerService.getDernierScoreTurnover(employeId),
      abs: this.managerService.getDernierAbsenteisme(employeId)
    }).subscribe({
      next: (res: any) => {
        this.scoreTurnover = res?.score?.data?.score ?? null;
        this.scoreTurnoverNiveau = res?.score?.data?.niveauRisque ?? null;

        const absData = res?.abs?.data;

        if (Array.isArray(absData) && absData.length > 0) {
          const monAbs = absData.find((a: any) => Number(a.employeId) === Number(employeId));

          this.absenteisme = monAbs?.valeur ?? null;
          this.absenteismeDate = monAbs?.dateCalcul ?? null;
        } else if (absData && typeof absData === 'object') {
          this.absenteisme = absData.valeur ?? absData.taux ?? null;
          this.absenteismeDate = absData.dateCalcul ?? null;
        }
      },
      error: (err: any) => {
        console.error('Erreur chargement indicateurs:', err);
      }
    });
  }

  updateManager(newManagerId: number): void {
    if (!this.employe?.id) {
      return;
    }

    this.employeService.updateManager(this.employe.id, newManagerId).subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.employe = res.data;
        }
      }
    });
  }

  getFullName(): string {
    if (!this.employe) {
      return 'Employé';
    }

    return `${this.employe.prenom || ''} ${this.employe.nom || ''}`.trim() || 'Employé';
  }

  getStatusClass(statut?: string | null): string {
    const normalized = (statut || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    if (normalized.includes('actif')) {
      return 'status-actif';
    }

    if (normalized.includes('conge')) {
      return 'status-conge';
    }

    if (normalized.includes('inactif')) {
      return 'status-inactif';
    }

    return 'status-default';
  }

  formatEur(value: number | null | undefined): string {
    if (value == null) {
      return '0 €';
    }

    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0
    }).format(value);
  }

  getAbsenteismeClass(taux: number | null): string {
    if (taux === null) {
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

  getScoreClass(score: number | null): string {
    if (score === null) {
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
}