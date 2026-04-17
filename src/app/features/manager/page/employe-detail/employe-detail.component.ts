import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ManagerService, Manager } from '../../../../core/services/manager.service';
import { EmployeService } from '../../../admin/gestion-employes/services/employe.service';
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
    } else {
      this.loading = false;
      this.error = true;
    }
  }

  loadEmploye(id: number): void {
    this.employeService.getById(id).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.employe = res.data as Employe;
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

  // ✅ Version corrigée (subscribe classique)
  loadManagers(): void {
    this.managerService.getAllManagers().subscribe(
      (response) => {
        if (response.success) {
          this.managersList = response.data;
        }
      },
      (error) => {
        console.error('Erreur chargement managers', error);
        this.managersList = [];
      }
    );
  }

  updateManager(newManagerId: number): void {
    if (!this.employe) return;
    this.employeService.updateManager(this.employe.id!, newManagerId).subscribe({
      next: (res) => {
        if (res.success) {
          this.employe = res.data;
        }
      },
      error: (err) => console.error(err)
    });
  }

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

  formatEur(value: number | null | undefined): string {
    if (value == null) return '0 €';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }
}