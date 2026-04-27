// src/app/features/admin/pages/employe-list/employe-list.component.ts
// ─── MODIFICATION FRONT ONLY : ajout de `Math` pour la pagination avancée dans le template
// Aucun changement backend — toutes les méthodes de service restent identiques

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { EmployeService } from '../../services/employe.service';
import { Employe } from '../../models/employe.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';
import { AuthService } from '../../../../../core/services/auth.service';

@Component({
  selector: 'app-employe-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatSnackBarModule, MatDialogModule
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

  // ✅ FRONT ONLY : expose Math pour ngFor pagination dans le template
  Math = Math;

  userNom = '';
  userPrenom = '';
  userRole = '';

  statuts = ['TOUS', 'ACTIF', 'INACTIF', 'CONGE'];
  departements = ['TOUS','RH','Technique','Commercial','Finance','Marketing','Direction','Logistique'];

  managersList: any[] = [];
  showAssignModal = false;
  selectedEmploye: Employe | null = null;
  selectedManagerId: number | null = null;

  readonly avatarColors: Record<string, string> = {
    'RH':          '#8b5cf6',
    'Technique':   '#0891b2',
    'Commercial':  '#d97706',
    'Finance':     '#059669',
    'Marketing':   '#db2777',
    'Direction':   '#7c3aed',
    'Logistique':  '#4f46e5',
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
      next: (res) => {
        this.managersList = res.success && Array.isArray(res.data) ? res.data : [];
      },
      error: (err) => {
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
        next: (res) => {
          if (res.success) {
            this.dataSource.data = Array.isArray(res.data) ? res.data : [];
            this.currentPage = 0;
          } else {
            this.toast(res.message || 'Erreur de chargement', 'error');
          }
        },
        error: () => this.toast('Erreur de connexion au serveur', 'error')
      });
  }

  openAssignModal(employe: Employe): void {
    this.selectedEmploye = employe;
    this.selectedManagerId = employe.managerId || null;
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
    this.employeService.assignManager(this.selectedEmploye.id!, this.selectedManagerId).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.toast(`Manager assigné à ${this.selectedEmploye!.prenom} ${this.selectedEmploye!.nom}`, 'success');
          this.closeAssignModal();
          this.loadEmployes();
        } else {
          this.toast(res.message || "Erreur lors de l'assignation", 'error');
        }
      },
      error: (err) => {
        this.loading = false;
        this.toast(err.error?.message || "Erreur lors de l'assignation", 'error');
      }
    });
  }

  getFilteredData(): Employe[] {
    const search = this.searchText.trim().toLowerCase();
    return this.dataSource.data.filter(e => {
      const matchSearch = !search || [
        e.nom, e.prenom, e.matricule, e.email, e.poste, e.departement
      ].some(v => v?.toLowerCase().includes(search));

      const matchStatut = this.selectedStatut === 'TOUS' || e.statut === this.selectedStatut;
      const matchDept   = this.selectedDepartement === 'TOUS' || e.departement === this.selectedDepartement;
      return matchSearch && matchStatut && matchDept;
    });
  }

  getPagedData(): Employe[] {
    const f = this.getFilteredData();
    return f.slice(this.currentPage * this.pageSize, (this.currentPage + 1) * this.pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void   { this.currentPage = 0; }
  setStatut(s: string): void { this.selectedStatut = s; this.currentPage = 0; }

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

  formatEur(value: number | null | undefined): string {
    if (value == null) return '0 €';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  deleteEmploye(id: number, nom: string): void {
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title:       'Supprimer définitivement',
        message:     `Êtes-vous sûr de vouloir supprimer définitivement ${nom} ?\n\nCette action est irréversible.`,
        confirmText: 'Supprimer définitivement',
        cancelText:  'Annuler'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.loading = true;
      this.employeService.delete(id).subscribe({
        next: (res) => {
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
        error: (err) => {
          this.loading = false;
          const serverMsg = err?.error?.message || err?.error?.error || `Erreur serveur (${err?.status ?? 'inconnu'})`;
          this.toast(serverMsg, 'error');
        }
      });
    });
  }

  private toast(msg: string, type: 'success'|'error'|'warn'): void {
    this.snack.open(msg, '×', {
      duration: 4000,
      panelClass: [`snack-${type}`],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedStatut = 'TOUS';
    this.selectedDepartement = 'TOUS';
    this.applyFilter();
  }
}