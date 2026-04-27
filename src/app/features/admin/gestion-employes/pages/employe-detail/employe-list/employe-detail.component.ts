// src/app/features/admin/gestion-employes/pages/employe-detail/employe-list/employe-detail.component.ts
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
  imports: [
    CommonModule,
    RouterModule,
    MatSnackBarModule,
    MatDialogModule,
    ReactiveFormsModule
  ],
  templateUrl: './employe-detail.component.html',
  styleUrls: ['./employe-detail.component.css']
})
export class EmployeDetailComponent implements OnInit {
  employe?: Employe;
  loading = false;
  activeTab = 'competences';
  managersList: Manager[] = [];

  editManagerMode = false;
  managerForm!: FormGroup;
  updatingManager = false;

  // Liste des onglets pour le rendu dynamique
  tabs = [
    { id: 'competences', icon: '🧠', label: 'Compétences' },
    { id: 'formations', icon: '📚', label: 'Formations' },
    { id: 'conges', icon: '🏖️', label: 'Congés' },
    { id: 'evaluations', icon: '⭐', label: 'Évaluations' }
  ];

  readonly avatarColors: Record<string, string> = {
    'RH':          '#8b5cf6',
    'Technique':   '#0891b2',
    'Commercial':  '#d97706',
    'Finance':     '#059669',
    'Marketing':   '#db2777',
    'Direction':   '#7c3aed',
    'Logistique':  '#4f46e5',
  };
ancienneteColor: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: EmployeService,
    private snack: MatSnackBar,
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

  // ---------------------------------------------------------------------------
  // Initialisation du formulaire manager
  // ---------------------------------------------------------------------------
  initManagerForm(): void {
    this.managerForm = this.fb.group({
      managerId: [null]
    });
  }

  // ---------------------------------------------------------------------------
  // Chargement de la liste des managers (depuis le service)
  // ---------------------------------------------------------------------------
  loadManagers(): void {
    this.managerSvc.getAllManagers().subscribe({
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

  // ---------------------------------------------------------------------------
  // Chargement d’un employé par son ID
  // ---------------------------------------------------------------------------
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

  // ---------------------------------------------------------------------------
  // Mise à jour du manager (appel API)
  // ---------------------------------------------------------------------------
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

  // ---------------------------------------------------------------------------
  // Annuler l’édition du manager
  // ---------------------------------------------------------------------------
  cancelEditManager(): void {
    this.editManagerMode = false;
    this.managerForm.patchValue({ managerId: this.employe?.managerId });
  }

  // ---------------------------------------------------------------------------
  // Désactivation (suppression logique) d’un employé
  // ---------------------------------------------------------------------------
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

  // ---------------------------------------------------------------------------
  // Redirection vers l’édition de l’employé
  // ---------------------------------------------------------------------------
  editEmploye(): void {
    this.router.navigate(['/admin/employes', this.employe?.id, 'edit']);
  }

  // ---------------------------------------------------------------------------
  // Retour à la liste
  // ---------------------------------------------------------------------------
  goBack(): void {
    this.router.navigate(['/admin/employes']);
  }

  // ---------------------------------------------------------------------------
  // Utilitaires d’affichage (inchangés)
  // ---------------------------------------------------------------------------
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

  // ---------------------------------------------------------------------------
  // NOUVEAUX GETTERS POUR L’UI AMÉLIORÉE
  // ---------------------------------------------------------------------------
  get profileCompletion(): number {
    if (!this.employe) return 0;
    let filled = 0;
    const fields = [
      this.employe.nom, this.employe.prenom, this.employe.email,
      this.employe.telephone, this.employe.poste, this.employe.departement,
      this.employe.matricule, this.employe.dateEmbauche
    ];
    filled = fields.filter(f => f && f.toString().trim() !== '').length;
    // On ajoute deux champs implicites : salaire et statut (toujours fournis)
    const total = 10;
    return Math.min(100, Math.round((filled / total) * 100));
  }

  get ancienneteMois(): number {
    if (!this.employe?.dateEmbauche) return 0;
    const diff = Date.now() - new Date(this.employe.dateEmbauche).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24 * 30.44));
  }

  get salaryTrend(): 'up' | 'down' | 'stable' {
    // Simulation basée sur l’ancienneté (à adapter selon vos données réelles)
    const mois = this.ancienneteMois;
    if (mois > 24) return 'up';
    if (mois < 12) return 'down';
    return 'stable';
  }

  get managerColor(): string {
    // Couleur aléatoire mais stable pour le manager (basée sur son nom)
    if (!this.employe?.managerNom) return '#6c757d';
    let hash = 0;
    for (let i = 0; i < this.employe.managerNom.length; i++) {
      hash = this.employe.managerNom.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 65%, 55%)`;
  }

  // ---------------------------------------------------------------------------
  // Toast helper
  // ---------------------------------------------------------------------------
  private toast(msg: string, type: 'success'|'error'|'warn'): void {
    this.snack.open(msg, '×', {
      duration: 4000,
      panelClass: [`snack-${type}`],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}