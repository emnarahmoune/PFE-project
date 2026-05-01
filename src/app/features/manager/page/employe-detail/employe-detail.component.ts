// src/app/features/manager/pages/employe-detail/employe-detail.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ManagerService, Manager } from '../../../../core/services/manager.service';
import { EmployeService } from '../../../../core/services/employe.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';

@Component({
  selector: 'app-employe-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
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
    private employeService: EmployeService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadEmploye(+id);
      this.loadManagers();
      this.loadIndicateurs(+id);
    } else {
      this.loading = false;
      this.error = true;
    }
  }

  loadEmploye(id: number): void {
    this.employeService.getById(id).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success) this.employe = res.data as Employe;
        else this.error = true;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  loadManagers(): void {
    this.managerService.getAllManagers().subscribe(
      (response) => { if (response.success) this.managersList = response.data; },
      (error) => { console.error(error); this.managersList = []; }
    );
  }

  private loadIndicateurs(employeId: number): void {
    forkJoin({
      score: this.managerService.getDernierScoreTurnover(employeId),
      abs: this.managerService.getDernierAbsenteisme(employeId)
    }).subscribe({
      next: (res: any) => {
        // Score
        this.scoreTurnover = res.score?.data?.score ?? null;
        this.scoreTurnoverNiveau = res.score?.data?.niveauRisque ?? null;

        // Absentéisme
        const absData = res.abs?.data;
        if (Array.isArray(absData) && absData.length > 0) {
          const monAbs = absData.find((item: any) => item.employeId === employeId);
          if (monAbs) {
            this.absenteisme = monAbs.valeur ?? null;
            this.absenteismeDate = monAbs.dateCalcul ?? null;
          } else {
            this.absenteisme = null;
          }
        } else {
          this.absenteisme = null;
        }
      },
      error: (err: any) => console.error(err)
    });
  }

  updateManager(newManagerId: number): void {
    if (!this.employe) return;
    this.employeService.updateManager(this.employe.id!, newManagerId).subscribe({
      next: (res: any) => { if (res.success) this.employe = res.data; },
      error: (err: any) => console.error(err)
    });
  }

  getAvatarColor(dept: string): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6', Technique: '#0891b2', Commercial: '#d97706',
      Finance: '#059669', Marketing: '#db2777', Direction: '#7c3aed',
      Logistique: '#4f46e5'
    };
    return colors[dept] || '#6366f1';
  }

  formatEur(value: number | null | undefined): string {
    if (value == null) return '0 €';
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value);
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
}