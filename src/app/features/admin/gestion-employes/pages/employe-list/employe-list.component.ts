// src/app/features/admin/pages/employe-list/employe-list.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { EmployeService } from '../../../../../core/services/employe.service';
import { Employe } from '../../models/employe.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';
import { AuthService } from '../../../../../core/services/auth.service';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component'; 

@Component({
  selector: 'app-employe-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatSnackBarModule,
    MatDialogModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './employe-list.component.html',
  styleUrls: ['./employe-list.component.scss']
})
export class EmployeListComponent implements OnInit {
  dataSource = new MatTableDataSource<Employe>([]);

  loading = false;
  searchText = '';
  selectedStatut = 'TOUS';
  selectedDepartement = 'TOUS';
  currentPage = 0;

  readonly pageSize = 12;

  Math = Math;

  userNom = '';
  userPrenom = '';
  userRole = '';

  statuts = ['TOUS', 'ACTIF', 'INACTIF', 'CONGE'];

  departements = [
    'TOUS',
    'RH',
    'Technique',
    'Commercial',
    'Finance',
    'Marketing',
    'Direction',
    'Logistique'
  ];

  managersList: Employe[] = [];
  showAssignModal = false;
  selectedEmploye: Employe | null = null;
  selectedManagerId: number | null = null;

  readonly avatarColors: Record<string, string> = {
    RH: '#8b5cf6',
    Technique: '#0891b2',
    Commercial: '#d97706',
    Finance: '#059669',
    Marketing: '#db2777',
    Direction: '#7c3aed',
    Logistique: '#4f46e5'
  };

  constructor(
    private employeService: EmployeService,
    private snack: MatSnackBar,
    private dialog: MatDialog,
    private router: Router,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadEmployes();
    this.loadManagers();
  }

  loadUserInfo(): void {
    const user = this.auth.getCurrentUser();

    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userRole = user.role || user.typeUtilisateur || 'EMPLOYE';
    }
  }

  loadManagers(): void {
    this.employeService.getAllManagers().subscribe({
      next: (res: any) => {
        this.managersList = res.success && Array.isArray(res.data) ? res.data : [];
      },
      error: (err: any) => {
        console.error('Erreur chargement managers', err);
        this.managersList = [];
      }
    });
  }

  loadEmployes(): void {
    this.loading = true;

    this.employeService.getAll()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res: any) => {
          if (res.success) {
            this.dataSource.data = Array.isArray(res.data) ? res.data : [];
            this.currentPage = 0;
          } else {
            this.toast(res.message || 'Erreur de chargement', 'error');
          }
        },
        error: () => {
          this.toast('Erreur de connexion au serveur', 'error');
        }
      });
  }






  isManager(emp: Employe | null | undefined): boolean {
  if (!emp) {
    return false;
  }

  const role = String(
    emp.role ||
    (emp as any).typeUtilisateur ||
    (emp as any).typeEmploye ||
    ''
  ).toUpperCase();

  return role === 'MANAGER';
}
 openAssignModal(employe: Employe): void {
  if (this.isManager(employe)) {
    this.toast('Un manager ne peut pas avoir de manager assigné', 'error');
    return;
  }

  this.selectedEmploye = employe;
  this.selectedManagerId = employe.managerId ?? null;
  this.showAssignModal = true;
}

  closeAssignModal(): void {
    this.showAssignModal = false;
    this.selectedEmploye = null;
    this.selectedManagerId = null;
  }

  assignManager(): void {
    if (!this.selectedEmploye || !this.selectedManagerId) {
      this.toast('Veuillez sélectionner un manager', 'error');
      return;
    }

    this.loading = true;

    this.employeService.assignManager(this.selectedEmploye.id!, this.selectedManagerId)
      .subscribe({
        next: (res: any) => {
          this.loading = false;

          if (res.success) {
            this.toast(
              `Manager assigné à ${this.selectedEmploye!.prenom} ${this.selectedEmploye!.nom}`,
              'success'
            );

            this.closeAssignModal();
            this.loadEmployes();
          } else {
            this.toast(res.message || 'Erreur lors de l\'assignation', 'error');
          }
        },
        error: (err: any) => {
          this.loading = false;
          this.toast(err.error?.message || 'Erreur lors de l\'assignation', 'error');
        }
      });
  }


  unassignManager(): void {
  if (!this.selectedEmploye?.id) {
    return;
  }

  const employeName = `${this.selectedEmploye.prenom || ''} ${this.selectedEmploye.nom || ''}`.trim();

  if (!confirm(`Désassigner le manager de ${employeName} ?`)) {
    return;
  }

  this.loading = true;

  this.employeService.unassignManager(this.selectedEmploye.id)
    .pipe(finalize(() => this.loading = false))
    .subscribe({
      next: (res: any) => {
        if (res?.success) {
          this.toast(`Manager désassigné de ${employeName}`, 'success');

          this.closeAssignModal();
          this.loadEmployes();
        } else {
          this.toast(res?.message || 'Erreur lors de la désassignation', 'error');
        }
      },
      error: (err: any) => {
        this.toast(err?.error?.message || 'Erreur lors de la désassignation', 'error');
      }
    });
}

hasCurrentManager(): boolean {
  return !!this.selectedEmploye?.managerId;
}


getCurrentManagerName(): string {
  if (!this.selectedEmploye?.managerId) {
    return 'Aucun manager';
  }

  const manager = this.managersList.find(m => m.id === this.selectedEmploye?.managerId);

  if (!manager) {
    return 'Manager actuel';
  }

  return `${manager.prenom || ''} ${manager.nom || ''}`.trim();
}

getManagersForModal(): Employe[] {
  if (!this.selectedEmploye?.id) {
    return this.managersList;
  }

  // évite d’assigner l’employé comme son propre manager
  return this.managersList.filter(m => m.id !== this.selectedEmploye?.id);
}

  getFilteredData(): Employe[] {
    const search = this.searchText.trim().toLowerCase();

    return this.dataSource.data.filter(e => {
      const matchSearch = !search || [
        e.nom,
        e.prenom,
        e.matricule,
        e.email,
        e.poste,
        e.departement
      ].some(v => v?.toLowerCase().includes(search));

      const matchStatut =
        this.selectedStatut === 'TOUS' ||
        e.statut === this.selectedStatut;

      const matchDept =
        this.selectedDepartement === 'TOUS' ||
        e.departement === this.selectedDepartement;

      return matchSearch && matchStatut && matchDept;
    });
  }

  getPagedData(): Employe[] {
    const filtered = this.getFilteredData();

    return filtered.slice(
      this.currentPage * this.pageSize,
      (this.currentPage + 1) * this.pageSize
    );
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void {
    this.currentPage = 0;
  }

  setStatut(statut: string): void {
    this.selectedStatut = statut;
    this.currentPage = 0;
  }

  getCount(statut: string): number {
    return this.dataSource.data.filter(e => e.statut === statut).length;
  }

  getMasseSalariale(): number {
    return this.dataSource.data
      .filter(e => e.statut === 'ACTIF')
      .reduce((sum, e) => sum + (e.salaire ?? 0), 0);
  }

  viewDetail(id: number): void {
    this.router.navigate(['/admin/employes', id]);
  }

  getAvatarColor(dept: string): string {
    return this.avatarColors[dept] ?? '#6366f1';
  }

 formatTnd(value: number | null | undefined): string {
  const amount = Number(value || 0);

  const formatted = new Intl.NumberFormat('fr-FR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(amount);

  return `${formatted} DT`;
}

  deleteEmploye(id: number, nom: string): void {
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title: 'Supprimer définitivement',
        message: `Êtes-vous sûr de vouloir supprimer définitivement ${nom} ?\n\nCette action est irréversible.`,
        confirmText: 'Supprimer définitivement',
        cancelText: 'Annuler'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) {
        return;
      }

      this.loading = true;

      this.employeService.delete(id).subscribe({
        next: (res: any) => {
          if (res.success) {
            const updatedData = this.dataSource.data.filter(e => e.id !== id);

            this.dataSource.data = updatedData;
            this.currentPage = 0;

            this.toast(`Employé ${nom} supprimé avec succès`, 'success');
          } else {
            this.toast(res.message || 'Erreur de suppression', 'error');
          }

          this.loading = false;
        },
        error: (err: any) => {
          this.loading = false;

          const serverMsg =
            err?.error?.message ||
            err?.error?.error ||
            `Erreur serveur (${err?.status ?? 'inconnu'})`;

          this.toast(serverMsg, 'error');
        }
      });
    });
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedStatut = 'TOUS';
    this.selectedDepartement = 'TOUS';
    this.applyFilter();
  }

  private toast(msg: string, type: 'success' | 'error' | 'warn'): void {
    this.snack.open(msg, '×', {
      duration: 4000,
      panelClass: [`snack-${type}`],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}