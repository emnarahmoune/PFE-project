// src/app/features/admin/gestion-conges/pages/validation-rh/validation-rh.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { AdminCongeService, TacheRh, StatsConges, DemandeRefusManager } from '../../../../../core/services/admin-conge.service';

@Component({
  selector: 'app-validation-rh',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatDialogModule,
    MatTooltipModule, MatTabsModule, MatBadgeModule
  ],
  templateUrl: './validation-rh.component.html',
  styleUrls: ['./validation-rh.component.scss']
})
export class ValidationRhComponent implements OnInit, OnDestroy {
  
  // Onglet 1 : demandes à valider
  taches: TacheRh[] = [];
  loadingTaches = false;
  
  // Onglet 2 : refus manager
  refusManager: DemandeRefusManager[] = [];
  loadingRefus = false;
  
  selectedTabIndex = 0;
  
  // Modals
  showApproveModal = false;
  showRejectModal = false;
  selectedTache: TacheRh | null = null;
  commentaire = '';
  motifRefus = '';
  isSubmitting = false;
  
  stats: StatsConges = {
    EN_ATTENTE: 0,
    APPROUVE: 0,
    REFUSE: 0
  };
  
  private refreshInterval: any;
  private readonly REFRESH_INTERVAL_MS = 15000;

  displayedColumns = ['employe', 'periode', 'jours', 'type', 'statut', 'actions'];
  displayedColumnsRefus = ['employe', 'manager', 'periode', 'motif', 'dateDecision'];

  constructor(
    private adminCongeService: AdminCongeService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadTaches();
    this.loadRefusManager();
    this.loadStats();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.loadTaches(false);
      this.loadRefusManager();
      this.loadStats();
    }, this.REFRESH_INTERVAL_MS);
  }

  loadTaches(showLoading = true): void {
    if (showLoading) this.loadingTaches = true;
    this.adminCongeService.getDemandesAValider().subscribe({
      next: (data) => {
        this.taches = data || [];
        this.stats.EN_ATTENTE = this.taches.length;
        this.loadingTaches = false;
      },
      error: (error) => {
        this.loadingTaches = false;
        console.error('Erreur chargement tâches:', error);
        this.showToast('Erreur de connexion au serveur', 'error');
      }
    });
  }

  loadRefusManager(): void {
    this.loadingRefus = true;
    this.adminCongeService.getRefusManager().subscribe({
      next: (data) => {
        this.refusManager = data || [];
        this.loadingRefus = false;
      },
      error: (err) => {
        console.error('Erreur chargement refus manager:', err);
        this.loadingRefus = false;
        this.showToast('Erreur chargement des refus manager', 'error');
      }
    });
  }

  loadStats(): void {
    this.adminCongeService.getStats().subscribe({
      next: (data) => { if (data) this.stats = data; },
      error: (err) => console.error('Erreur chargement stats:', err)
    });
  }

  openApproveModal(tache: TacheRh): void {
    this.selectedTache = tache;
    this.commentaire = '';
    this.showApproveModal = true;
  }

  openRejectModal(tache: TacheRh): void {
    this.selectedTache = tache;
    this.motifRefus = '';
    this.showRejectModal = true;
  }

  closeModals(): void {
    this.showApproveModal = false;
    this.showRejectModal = false;
    this.selectedTache = null;
    this.commentaire = '';
    this.motifRefus = '';
  }

  confirmApprove(): void {
    if (!this.selectedTache || this.isSubmitting) return;
    this.isSubmitting = true;
    this.adminCongeService.approuverDemande(this.selectedTache.demandeId, this.commentaire).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('✅ Demande approuvée avec succès', 'success');
        this.closeModals();
        this.loadTaches();
        this.loadRefusManager();
        this.loadStats();
      },
      error: (error) => {
        this.isSubmitting = false;
        const message = error.error?.message || 'Erreur lors de l\'approbation';
        this.showToast(message, 'error');
      }
    });
  }

  confirmReject(): void {
    if (!this.selectedTache || this.isSubmitting) return;
    if (!this.motifRefus.trim()) {
      this.showToast('Veuillez saisir un motif de refus', 'error');
      return;
    }
    this.isSubmitting = true;
    this.adminCongeService.refuserDemande(this.selectedTache.demandeId, this.motifRefus).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('❌ Demande refusée avec succès', 'success');
        this.closeModals();
        this.loadTaches();
        this.loadRefusManager();
        this.loadStats();
      },
      error: (error) => {
        this.isSubmitting = false;
        const message = error.error?.message || 'Erreur lors du refus';
        this.showToast(message, 'error');
      }
    });
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'statut-approuve';
      case 'EN_ATTENTE': return 'statut-attente';
      case 'REFUSE': return 'statut-refuse';
      case 'ANNULE': return 'statut-annule';
      default: return '';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return '✅ Approuvé';
      case 'EN_ATTENTE': return '⏳ En attente';
      case 'REFUSE': return '❌ Refusé';
      case 'ANNULE': return '🗑️ Annulé';
      default: return statut;
    }
  }

  getTypeLabel(type?: string): string {
    switch (type) {
      case 'ANNUEL': return 'Annuel';
      case 'MALADIE': return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE': return 'Maternité';
      case 'PATERNITE': return 'Paternité';
      default: return type || 'Non spécifié';
    }
  }

  getTypeColor(type?: string): string {
    switch (type) {
      case 'ANNUEL': return '#1976d2';
      case 'MALADIE': return '#dc3545';
      case 'SANS_SOLDE': return '#ffc107';
      case 'MATERNITE': return '#28a745';
      case 'PATERNITE': return '#28a745';
      default: return '#6c757d';
    }
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  trackByTaskId(index: number, item: TacheRh): string {
    return item.taskId;
  }
}