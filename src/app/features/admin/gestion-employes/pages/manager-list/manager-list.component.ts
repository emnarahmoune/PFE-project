// src/app/features/admin/gestion-employes/pages/manager-list/manager-list.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EmployeService } from '../../services/employe.service';
import { Employe } from '../../models/employe.model';
import { HistoriqueCongesModalComponent } from '../historique-conges-modal/historique-conges-modal.component';

interface ManagerWithEquipe {
  manager: Employe;
  equipe: Employe[];
  expanded: boolean;
}

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [CommonModule, RouterModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './manager-list.component.html',
  styleUrls: ['./manager-list.component.scss']
})
export class ManagerListComponent implements OnInit {
  managers: ManagerWithEquipe[] = [];
  loading = false;

  constructor(
    private employeService: EmployeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadManagers();
  }

  loadManagers(): void {
    this.loading = true;
    this.employeService.getAllManagers().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          // Typage explicite de 'm'
          this.managers = res.data.map((m: Employe) => ({ manager: m, equipe: [], expanded: false }));
          this.loadAllEquipes();
        } else {
          this.loading = false;
          this.snackBar.open('Erreur chargement managers', 'Fermer', { duration: 3000 });
        }
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Erreur de connexion', 'Fermer', { duration: 3000 });
      }
    });
  }

  loadAllEquipes(): void {
    let completed = 0;
    this.managers.forEach((m, idx) => {
      const managerId = m.manager.id;
      if (!managerId) {
        completed++;
        if (completed === this.managers.length) this.loading = false;
        return;
      }
      this.employeService.getEquipeByManagerId(managerId).subscribe({
        next: (res) => {
          if (res.success) this.managers[idx].equipe = res.data;
          completed++;
          if (completed === this.managers.length) this.loading = false;
        },
        error: () => {
          completed++;
          if (completed === this.managers.length) this.loading = false;
        }
      });
    });
  }

  toggleEquipe(manager: ManagerWithEquipe): void {
    manager.expanded = !manager.expanded;
  }

  voirHistoriqueConges(employe: Employe): void {
    this.dialog.open(HistoriqueCongesModalComponent, {
      width: '800px',
      data: { employeId: employe.id, employeNom: `${employe.prenom} ${employe.nom}` }
    });
  }

  getAvatarColor(dept: string): string {
    const colors: Record<string, string> = {
      'RH': '#8b5cf6',
      'Technique': '#0891b2',
      'Commercial': '#d97706',
      'Finance': '#059669',
      'Marketing': '#db2777',
      'Direction': '#7c3aed',
      'Logistique': '#4f46e5',
    };
    return colors[dept] || '#6366f1';
  }
}