import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { EmployeService } from '../../../services/employe.service';
import { Employe } from '../../../models/employe.model';
import { ConfirmationDialogComponent } from '../../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-employe-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatSnackBarModule, MatDialogModule],
  templateUrl: './employe-detail.component.html',
  styleUrls: ['./employe-detail.component.css']
})
export class EmployeDetailComponent implements OnInit {

  employe?: Employe;
  loading  = false;
  activeTab = 'competences';

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
    private route:  ActivatedRoute,
    private router: Router,
    private svc:    EmployeService,
    private snack:  MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    // Rechargement automatique si on revient sur la page après un edit
    this.route.params.subscribe(params => {
      const id = params['id'];
      id ? this.loadEmploye(+id) : this.router.navigate(['/admin/employes']);
    });
  }

  /* ── Chargement ─────────────────────────── */
  loadEmploye(id: number): void {
    this.loading = true;
    this.svc.getById(id)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.employe = res.data as Employe;
          } else {
            this.toast(res.message || 'Employé introuvable', 'error');
            this.router.navigate(['/admin/employes']);
          }
        },
        error: () => {
          this.toast('Erreur de connexion au serveur', 'error');
          this.router.navigate(['/admin/employes']);
        }
      });
  }

  /* ── Suppression / désactivation ────────── */
  deleteEmploye(): void {
    if (!this.employe?.id) return;

    // Fix aria-hidden : enlever le focus du bouton avant d'ouvrir le dialog
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title:       'Désactiver l\'employé',
        message:     `Êtes-vous sûr de vouloir désactiver ${this.employe.prenom} ${this.employe.nom} ?`,
        confirmText: 'Désactiver',
        cancelText:  'Annuler'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.svc.delete(this.employe!.id!).subscribe({
        next: (res) => {
          if (res.success) {
            this.toast('Employé désactivé avec succès', 'success');
            this.router.navigate(['/admin/employes']);
          } else {
            this.toast(res.message || 'Erreur de suppression', 'error');
          }
        },
        error: (err) => {
          const serverMsg: string =
            err?.error?.message ||
            err?.error?.error   ||
            (typeof err?.error === 'string' ? err.error : null) ||
            `Erreur serveur (${err?.status ?? 'inconnu'})`;
          console.error('❌ Erreur suppression:', err);
          this.toast(serverMsg, 'error');
        }
      });
    });
  }

  /* ── Navigation ─────────────────────────── */
  editEmploye(): void {
    this.router.navigate(['/admin/employes', this.employe?.id, 'edit']);
  }

  goBack(): void {
    this.router.navigate(['/admin/employes']);
  }

  /* ── Utilitaires d'affichage ────────────── */
  getAvatarColor(): string {
    return this.avatarColors[this.employe?.departement ?? ''] ?? '#6366f1';
  }

  getInitials(): string {
    return (
      (this.employe?.prenom?.charAt(0) ?? '') +
      (this.employe?.nom?.charAt(0) ?? '')
    ).toUpperCase();
  }

  getSalaireFormate(): string {
    if (!this.employe?.salaire) return '—';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency', currency: 'EUR', maximumFractionDigits: 0
    }).format(this.employe.salaire);
  }

  getDateEmbaucheFormatee(): string {
    if (!this.employe?.dateEmbauche) return '—';
    return new Date(this.employe.dateEmbauche).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
  }

  getAnciennete(): string {
    if (!this.employe?.dateEmbauche) return '—';
    const ms   = Date.now() - new Date(this.employe.dateEmbauche).getTime();
    const yrs  = Math.floor(ms / (365.25 * 24 * 3600 * 1000));
    const mths = Math.floor((ms % (365.25 * 24 * 3600 * 1000)) / (30.44 * 24 * 3600 * 1000));
    if (yrs === 0) return `${mths} mois`;
    if (mths === 0) return `${yrs} an${yrs > 1 ? 's' : ''}`;
    return `${yrs} an${yrs > 1 ? 's' : ''} ${mths} mois`;
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