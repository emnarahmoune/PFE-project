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

@Component({
  selector: 'app-employe-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatSnackBarModule, MatDialogModule
  ],
  templateUrl: './employe-list.component.html',
  styleUrls: ['./employe-list.component.css']
})
export class EmployeListComponent implements OnInit {

  dataSource       = new MatTableDataSource<Employe>([]);
  loading          = false;
  searchText       = '';
  selectedStatut   = 'TOUS';
  selectedDepartement = 'TOUS';
  currentPage      = 0;
  readonly pageSize = 12;

  statuts      = ['TOUS', 'ACTIF', 'INACTIF', 'CONGE'];
  departements = ['TOUS','RH','Technique','Commercial','Finance','Marketing','Direction','Logistique'];

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
    private svc:    EmployeService,
    private snack:  MatSnackBar,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEmployes();
  }

  /* ── Chargement ─────────────────────────── */
  loadEmployes(): void {
    this.loading = true;
    this.svc.getAll()
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

  /* ── Filtrage côté client ───────────────── */
  getFilteredData(): Employe[] {
    const search = this.searchText.trim().toLowerCase();
    return this.dataSource.data.filter(e => {
      const matchSearch = !search || [
        e.nom, e.prenom, e.matricule, e.email, e.poste, e.departement
      ].some(v => v?.toLowerCase().includes(search));

      const matchStatut = this.selectedStatut   === 'TOUS' || e.statut      === this.selectedStatut;
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

  /* ── Stats affichées ────────────────────── */
  getCount(statut: string): number {
    return this.dataSource.data.filter(e => e.statut === statut).length;
  }

  getMasseSalariale(): number {
    return this.dataSource.data
      .filter(e => e.statut === 'ACTIF')
      .reduce((sum, e) => sum + (e.salaire ?? 0), 0);
  }

  /* ── Navigation ─────────────────────────── */
  viewDetail(id: number): void {
    this.router.navigate(['/admin/employes', id]);
  }

  getAvatarColor(dept: string): string {
    return this.avatarColors[dept] ?? '#6366f1';
  }

  /** Formate un montant en EUR sans dépendance de locale Angular */
  formatEur(value: number | null | undefined): string {
    if (value == null) return '0 €';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  /* ── Suppression physique (hard delete) ── */
  deleteEmploye(id: number, nom: string): void {
    // Fix aria-hidden : déplacer le focus avant d'ouvrir le dialog
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title:       '⚠️ Supprimer définitivement',
        message:     `Êtes-vous sûr de vouloir SUPPRIMER DÉFINITIVEMENT ${nom} ?\n\nCette action est irréversible et supprimera toutes les données associées (congés, formations, etc.).`,
        confirmText: 'Supprimer définitivement',
        cancelText:  'Annuler'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      
      // Afficher un loader local
      this.loading = true;
      
      this.svc.delete(id).subscribe({
        next: (res) => {
          if (res.success) {
            // 🔴 MODIFICATION: Supprimer l'employé de la liste locale (suppression physique)
            const updatedData = this.dataSource.data.filter(e => e.id !== id);
            this.dataSource.data = updatedData;
            this.currentPage = 0; // Réinitialiser la pagination
            
            this.toast(`Employé ${nom} supprimé définitivement avec succès`, 'success');
            
            // Recharger les statistiques (optionnel)
            // this.loadEmployes(); // Décommenter si besoin de recharger complètement
          } else {
            this.toast(res.message || 'Erreur de suppression', 'error');
          }
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          const serverMsg: string =
            err?.error?.message ||
            err?.error?.error   ||
            (typeof err?.error === 'string' ? err.error : null) ||
            `Erreur serveur (${err?.status ?? 'inconnu'})`;
          
          console.error('❌ Erreur delete:', err);
          
          // Message d'erreur plus spécifique pour les contraintes de clés étrangères
          if (err?.status === 409 || serverMsg.includes('contrainte') || serverMsg.includes('foreign key')) {
            this.toast('Impossible de supprimer : cet employé a des données associées (congés, formations, etc.)', 'error');
          } else {
            this.toast(serverMsg, 'error');
          }
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
}