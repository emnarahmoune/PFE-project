// src/app/features/admin/pages/manager-detail/manager-detail.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { EmployeService } from '../../../../../core/services/employe.service';
import { Employe } from '../../models/employe.model';
import { HistoriqueCongesModalComponent } from '../historique-conges-modal/historique-conges-modal.component';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-manager-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSnackBarModule,
    MatDialogModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './manager-detail.component.html',
  styleUrls: ['./manager-detail.component.scss']
})
export class ManagerDetailComponent implements OnInit {
  manager: Employe | null = null;
  equipe: Employe[] = [];
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private employeService: EmployeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];

    if (id) {
      this.loadManager(+id);
    } else {
      this.loading = false;
      this.snackBar.open('Identifiant manager manquant', 'Fermer', {
        duration: 3000
      });
    }
  }

  loadManager(id: number): void {
    this.loading = true;

    this.employeService.getById(id).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.manager = res.data as Employe;
          this.loadEquipe(id);
        } else {
          this.snackBar.open('Manager introuvable', 'Fermer', {
            duration: 3000
          });
          this.loading = false;
        }
      },
      error: () => {
        this.snackBar.open('Erreur chargement manager', 'Fermer', {
          duration: 3000
        });
        this.loading = false;
      }
    });
  }

  loadEquipe(managerId: number): void {
    this.employeService.getEquipeByManagerId(managerId).subscribe({
      next: (res: any) => {
        this.equipe = res.success && Array.isArray(res.data) ? res.data : [];
        this.loading = false;
      },
      error: () => {
        this.equipe = [];
        this.loading = false;

        this.snackBar.open('Erreur chargement équipe', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  voirHistorique(employe: Employe): void {
    if (!employe?.id) {
      this.snackBar.open('Employé invalide', 'Fermer', {
        duration: 3000
      });
      return;
    }

    this.dialog.open(HistoriqueCongesModalComponent, {
      width: '800px',
      data: {
        employeId: employe.id,
        employeNom: `${employe.prenom || ''} ${employe.nom || ''}`.trim()
      }
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
}