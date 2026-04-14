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
import { AdminCongeService, DemandeCongeAdmin } from '../../../../../core/services/admin-conge.service';
import { NotificationService } from '../../../../../core/services/notification.service';

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
  
  demandes: DemandeCongeAdmin[] = [];
  loading = false;
  selectedTabIndex = 0;
  
  showApproveModal = false;
  showRejectModal = false;
  selectedDemande: DemandeCongeAdmin | null = null;
  commentaire = '';
  motifRefus = '';
  isSubmitting = false;
  
  stats = {
    total: 0,
    approuvees: 0,
    refusees: 0,
    enAttente: 0
  };
  
  private refreshInterval: any;
  private readonly REFRESH_INTERVAL_MS = 15000;

  displayedColumns = ['employe', 'periode', 'jours', 'type', 'statut', 'actions'];

  constructor(
    private adminCongeService: AdminCongeService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadDemandes();
    this.loadStats();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.loadDemandes(false);
      this.loadStats();
    }, this.REFRESH_INTERVAL_MS);
  }

  loadDemandes(showLoading = true): void {
    if (showLoading) this.loading = true;
    
    this.adminCongeService.getDemandesAValider().subscribe({
      next: (data) => {
        this.loading = false;
        this.demandes = data || [];
        this.stats.enAttente = this.demandes.length;
        this.stats.total = this.stats.enAttente + this.stats.approuvees + this.stats.refusees;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur chargement demandes:', error);
        this.showToast('Erreur de connexion au serveur', 'error');
      }
    });
  }

  loadStats(): void {
    this.adminCongeService.getStats().subscribe({
      next: (data) => {
        if (data) {
          this.stats.enAttente = data['EN_ATTENTE'] || 0;
          this.stats.approuvees = data['APPROUVE'] || 0;
          this.stats.refusees = data['REFUSE'] || 0;
          this.stats.total = this.stats.enAttente + this.stats.approuvees + this.stats.refusees;
        }
      },
      error: (err) => console.error('Erreur chargement stats:', err)
    });
  }

  openApproveModal(demande: DemandeCongeAdmin): void {
    this.selectedDemande = demande;
    this.commentaire = '';
    this.showApproveModal = true;
  }

  openRejectModal(demande: DemandeCongeAdmin): void {
    this.selectedDemande = demande;
    this.motifRefus = '';
    this.showRejectModal = true;
  }

  closeModals(): void {
    this.showApproveModal = false;
    this.showRejectModal = false;
    this.selectedDemande = null;
    this.commentaire = '';
    this.motifRefus = '';
  }

  confirmApprove(): void {
    if (!this.selectedDemande || this.isSubmitting) return;
    this.isSubmitting = true;

    this.adminCongeService.approuverDemande(this.selectedDemande.id, this.commentaire).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast(`✅ Demande approuvée avec succès`, 'success');
        this.closeModals();
        this.loadDemandes();
        this.loadStats();
        this.notificationService?.showSuccess?.(`Demande de ${this.selectedDemande!.employePrenom} ${this.selectedDemande!.employeNom} approuvée`);
      },
      error: (error) => {
        this.isSubmitting = false;
        const message = error.error?.message || 'Erreur lors de l\'approbation';
        this.showToast(message, 'error');
      }
    });
  }

  confirmReject(): void {
    if (!this.selectedDemande || this.isSubmitting) return;
    
    if (!this.motifRefus.trim()) {
      this.showToast('Veuillez saisir un motif de refus', 'error');
      return;
    }

    this.isSubmitting = true;

    this.adminCongeService.refuserDemande(this.selectedDemande.id, this.motifRefus).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast(`❌ Demande refusée avec succès`, 'success');
        this.closeModals();
        this.loadDemandes();
        this.loadStats();
        this.notificationService?.showWarning?.(`Demande de ${this.selectedDemande!.employePrenom} ${this.selectedDemande!.employeNom} refusée`);
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

  getTypeLabel(type: string): string {
    switch (type) {
      case 'ANNUEL': return 'Annuel';
      case 'MALADIE': return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE': return 'Maternité';
      case 'PATERNITE': return 'Paternité';
      default: return type;
    }
  }

  getTypeColor(type: string): string {
    switch (type) {
      case 'ANNUEL': return '#1976d2';
      case 'MALADIE': return '#dc3545';
      case 'SANS_SOLDE': return '#ffc107';
      case 'MATERNITE': return '#28a745';
      case 'PATERNITE': return '#28a745';
      default: return '#6c757d';
    }
  }

  formatDate(dateStr: string): string {
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

  trackById(index: number, item: DemandeCongeAdmin): number {
    return item.id;
  }
}