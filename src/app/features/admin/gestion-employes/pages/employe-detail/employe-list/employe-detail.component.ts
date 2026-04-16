import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { EmployeService } from '../../../services/employe.service';
import { Employe } from '../../../models/employe.model';
import { ManagerService, Manager } from '../../../../../../core/services/manager.service';
import { ConfirmationDialogComponent } from '../../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

interface ApiResponse {
  success: boolean;
  message?: string;
  data?: any;
}

@Component({
  selector: 'app-employe-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatSnackBarModule, MatDialogModule, ReactiveFormsModule],
  templateUrl: './employe-detail.component.html',
  styleUrls: ['./employe-detail.component.css']
})
export class EmployeDetailComponent implements OnInit {

  employe?: Employe;
  loading  = false;
  activeTab = 'competences';
  managersList: Manager[] = [];
  
  // Pour l'édition du manager
  editManagerMode = false;
  managerForm!: FormGroup;
  updatingManager = false;

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
    private dialog: MatDialog,
    private managerSvc: ManagerService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initManagerForm();
    this.loadManagers();
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.loadEmploye(+id);
      } else {
        this.router.navigate(['/admin/employes']);
      }
    });
  }

  initManagerForm(): void {
    this.managerForm = this.fb.group({
      managerId: [null]
    });
  }

  // ✅ CORRECTION : la méthode getAll() retourne un objet { success, data }
  loadManagers(): void {
    tgoghis.managerSvc.getAll().subscribe({
      next: (response: { success: boolean; data: Manager[] }) => {
        if (response.success) {
          this.managersList = response.data;
        }
      },
      error: (err: any) => {
        console.error('Erreur chargement managers', err);
        this.managersList = [];
      }
    });
  }

  loadEmploye(id: number): void {
    this.loading = true;
    this.svc.getById(id)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res: ApiResponse) => {
          if (res.success && res.data) {
            this.employe = res.data as Employe;
            this.managerForm.patchValue({ managerId: this.employe.managerId });
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

  updateManager(): void {
    if (!this.employe?.id) return;
    
    this.updatingManager = true;
    const newManagerId = this.managerForm.get('managerId')?.value;
    
    this.svc.updateManager(this.employe.id, newManagerId)
      .pipe(finalize(() => this.updatingManager = false))
      .subscribe({
        next: (res: ApiResponse) => {
          if (res.success) {
            this.toast('Manager mis à jour avec succès', 'success');
            this.editManagerMode = false;
            if (this.employe?.id) {
              this.loadEmploye(this.employe.id);
            }
          } else {
            this.toast(res.message || 'Erreur lors de la mise à jour', 'error');
          }
        },
        error: (err: any) => {
          const msg = err?.error?.message || 'Erreur serveur';
          this.toast(msg, 'error');
        }
      });
  }

  cancelEditManager(): void {
    this.editManagerMode = false;
    this.managerForm.patchValue({ managerId: this.employe?.managerId });
  }

  deleteEmploye(): void {
    if (!this.employe?.id) return;

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

    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.svc.delete(this.employe!.id!).subscribe({
        next: (res: ApiResponse) => {
          if (res.success) {
            this.toast('Employé désactivé avec succès', 'success');
            this.router.navigate(['/admin/employes']);
          } else {
            this.toast(res.message || 'Erreur de suppression', 'error');
          }
        },
        error: (err: any) => {
          const serverMsg = err?.error?.message || err?.error?.error ||
            (typeof err?.error === 'string' ? err.error : null) ||
            `Erreur serveur (${err?.status ?? 'inconnu'})`;
          console.error('❌ Erreur suppression:', err);
          this.toast(serverMsg, 'error');
        }
      });
    });
  }

  editEmploye(): void {
    this.router.navigate(['/admin/employes', this.employe?.id, 'edit']);
  }

  goBack(): void {
    this.router.navigate(['/admin/employes']);
  }

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

  getRoleLabel(role?: string): string {
    switch (role) {
      case 'admin_rh': return 'Administrateur RH';
      case 'manager':  return 'Manager';
      default:         return 'Employé';
    }
  }

  getManagerNom(managerId?: number | null): string {
    if (!managerId) return '—';
    const manager = this.managersList.find(m => m.id === managerId);
    return manager ? `${manager.prenom} ${manager.nom}` : `Manager #${managerId}`;
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