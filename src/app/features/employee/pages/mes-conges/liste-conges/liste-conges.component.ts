import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse, SoldeConges } from '../../../models/conge.model';
import { NotificationService } from '../../../../../core/services/notification.service';

@Component({
  selector: 'app-liste-conges',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatProgressBarModule,
    MatTooltipModule, MatDialogModule,
    MatCheckboxModule
  ],
  templateUrl: './liste-conges.component.html',
  styleUrls: ['./liste-conges.component.scss']
})
export class ListeCongesComponent implements OnInit, OnDestroy {

  conges: DemandeConge[] = [];
  loading = true;
  private previousStatuts = new Map<number, string>();
  private pollingInterval: any;
  private readonly POLLING_INTERVAL_MS = 10000;

  currentYear: number = new Date().getFullYear();

  soldeTotal     = 0;
  soldePris      = 0;
  soldeRestant   = 0;
  soldeEnAttente = 0;

  // Mode sélection activé/désactivé
  selectionMode = false;

  // IDs sélectionnés
  selectedDemandes: Set<number> = new Set();
  selectAll = false;

  // ✅ IDs des demandes masquées (ne seront plus jamais affichées)
  hiddenDemandeIds: Set<number> = new Set();

  constructor(
    private congeService: EmployeeCongeService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadHiddenIdsFromStorage(); // charger les IDs masqués depuis localStorage
    this.loadConges();
    this.loadSolde();
    this.startPolling();
    this.notificationService.loadNotifications();
    this.notificationService.loadUnreadCount();
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
  }

  // ==================== GESTION DU MASQUAGE PERSISTANT ====================
  private loadHiddenIdsFromStorage(): void {
    const saved = localStorage.getItem('hiddenDemandeIds');
    if (saved) {
      try {
        const arr = JSON.parse(saved);
        this.hiddenDemandeIds = new Set(arr);
      } catch(e) {
        console.error('Erreur chargement hiddenDemandeIds', e);
      }
    }
  }

  private saveHiddenIdsToStorage(): void {
    localStorage.setItem('hiddenDemandeIds', JSON.stringify(Array.from(this.hiddenDemandeIds)));
  }

  // Filtrer les demandes pour exclure celles masquées
  private filterHiddenDemandes(demandes: DemandeConge[]): DemandeConge[] {
    return demandes.filter(d => !this.hiddenDemandeIds.has(d.id!));
  }

  // ==================== POLLING ====================
  startPolling(): void {
    this.pollingInterval = setInterval(() => {
      this.checkForUpdates();
    }, this.POLLING_INTERVAL_MS);
  }

  checkForUpdates(): void {
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          const toutesDemandes = response.data as DemandeConge[];
          this.detectStatutChanges(toutesDemandes);
          // Appliquer le filtre des masquées
          this.conges = this.filterHiddenDemandes(toutesDemandes);
        }
      },
      error: (err: any) => console.error('Polling error:', err)
    });
  }

  private detectStatutChanges(toutesDemandes: DemandeConge[]): void {
    toutesDemandes.forEach(nouvelle => {
      const ancienStatut = this.previousStatuts.get(nouvelle.id!);
      if (ancienStatut && ancienStatut !== nouvelle.statut) {
        if (nouvelle.statut === 'APPROUVE') {
          const msg = `✅ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été approuvée !`;
          this.notificationService.showSuccess(msg);
          this.notificationService.loadNotifications();
          this.loadSolde();
        } else if (nouvelle.statut === 'REFUSE') {
          const motif = nouvelle.motifRefus || 'aucun motif fourni';
          const msg = `❌ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été refusée. Motif : ${motif}`;
          this.notificationService.showError(msg);
          this.notificationService.loadNotifications();
        }
      }
      this.previousStatuts.set(nouvelle.id!, nouvelle.statut!);
    });
  }

  // ==================== CHARGEMENT DES DONNÉES ====================
  loadConges(): void {
    this.loading = true;
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        this.loading = false;
        if (response.success) {
          const toutesDemandes = response.data as DemandeConge[];
          // Mettre à jour les statuts pour les notifications
          toutesDemandes.forEach(c => {
            this.previousStatuts.set(c.id!, c.statut!);
          });
          // Appliquer le filtre des masquées
          this.conges = this.filterHiddenDemandes(toutesDemandes);
          // Réinitialiser la sélection
          this.selectedDemandes.clear();
          this.selectAll = false;
        }
      },
      error: (err: any) => {
        this.loading = false;
        console.error('Erreur chargement congés:', err);
        const message = err?.message?.includes('Refresh')
          ? 'Session expirée, veuillez vous reconnecter'
          : 'Erreur lors du chargement des congés';
        this.snackBar.open(message, 'Fermer', { duration: 5000 });
      }
    });
  }

  loadSolde(): void {
    this.congeService.getMonSoldeConges().subscribe({
      next: (response) => {
        if (response.success) {
          const data = response.data as SoldeConges;
          this.soldeTotal     = data.total     ?? 25;
          this.soldePris      = data.pris      ?? 0;
          this.soldeRestant   = data.restant   ?? (this.soldeTotal - this.soldePris);
          this.soldeEnAttente = data.enAttente ?? 0;
        }
      },
      error: (err: any) => {
        console.error('Erreur solde:', err);
      }
    });
  }

  // ==================== MODE SÉLECTION ====================
  toggleSelectionMode(): void {
    this.selectionMode = !this.selectionMode;
    if (!this.selectionMode) {
      this.selectedDemandes.clear();
      this.selectAll = false;
    }
  }

  isSelectable(conge: DemandeConge): boolean {
    return conge.statut !== 'EN_ATTENTE';
  }

  toggleSelection(id: number): void {
    const conge = this.conges.find(c => c.id === id);
    if (conge && !this.isSelectable(conge)) {
      this.snackBar.open('Les demandes en attente ne peuvent pas être sélectionnées', 'Fermer', { duration: 2000 });
      return;
    }
    if (this.selectedDemandes.has(id)) {
      this.selectedDemandes.delete(id);
    } else {
      this.selectedDemandes.add(id);
    }
    this.updateSelectAllState();
  }

  updateSelectAllState(): void {
    const selectableCount = this.conges.filter(c => this.isSelectable(c)).length;
    this.selectAll = selectableCount > 0 && this.selectedDemandes.size === selectableCount;
  }

  toggleSelectAll(): void {
    if (this.selectAll) {
      this.selectedDemandes.clear();
    } else {
      this.conges.forEach(c => {
        if (this.isSelectable(c)) {
          this.selectedDemandes.add(c.id!);
        }
      });
    }
    this.selectAll = !this.selectAll;
  }

  isSelected(id: number): boolean {
    return this.selectedDemandes.has(id);
  }

  // ==================== SUPPRESSION DÉFINITIVE (MASQUAGE PERSISTANT) ====================
  deleteSelectedDemandes(): void {
    if (this.selectedDemandes.size === 0) {
      this.snackBar.open('Aucune demande sélectionnée', 'Fermer', { duration: 2000 });
      return;
    }

    if (confirm(`Masquer définitivement ${this.selectedDemandes.size} demande(s) ? Elles ne réapparaîtront plus dans la liste.`)) {
      // Ajouter les IDs à l'ensemble des masqués
      this.selectedDemandes.forEach(id => this.hiddenDemandeIds.add(id));
      // Persister dans localStorage
      this.saveHiddenIdsToStorage();
      // Recharger la liste (le filtre s'appliquera)
      this.loadConges();
      // Réinitialiser la sélection et désactiver le mode
      this.selectedDemandes.clear();
      this.selectAll = false;
      this.selectionMode = false;
      this.snackBar.open(`${this.selectedDemandes.size} demande(s) masquée(s) définitivement`, 'Fermer', { duration: 3000 });
    }
  }

  // ==================== ANNULATION (API) ====================
  annulerConge(conge: DemandeConge): void {
    if (!confirm(`Annuler cette demande du ${this.formatDate(conge.dateDebut)} ?`)) return;

    this.congeService.annulerConge(conge.id!).subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          this.showToast('✅ Demande annulée avec succès', 'success');
          this.loadConges();
          this.loadSolde();
          this.notificationService.loadNotifications();
        }
      },
      error: (err: any) => {
        console.error('Erreur annulation:', err);
        this.showToast('❌ Erreur lors de l\'annulation', 'error');
      }
    });
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  // ==================== MÉTHODES UTILITAIRES ====================
  getStatutColor(statut: string): string {
    switch (statut) {
      case 'APPROUVE':   return 'statut-approuve';
      case 'EN_ATTENTE': return 'statut-attente';
      case 'REFUSE':     return 'statut-refuse';
      case 'ANNULE':     return 'statut-annule';
      default:           return '';
    }
  }

  getStatutLabel(statut: string | undefined): string {
    switch (statut) {
      case 'APPROUVE':   return '✅ Approuvé';
      case 'EN_ATTENTE': return '⏳ En attente';
      case 'REFUSE':     return '❌ Refusé';
      case 'ANNULE':     return '🗑️ Annulé';
      default:           return statut || 'Inconnu';
    }
  }

  getStatutClass(statut: string | undefined): string {
    switch (statut) {
      case 'APPROUVE': return 'approuve';
      case 'EN_ATTENTE': return 'en_attente';
      case 'REFUSE': return 'refuse';
      case 'ANNULE': return 'annule';
      default: return '';
    }
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'ANNUEL':     return 'Annuel';
      case 'MALADIE':    return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE':  return 'Maternité';
      case 'PATERNITE':  return 'Paternité';
      default:           return type;
    }
  }

  peutAnnuler(conge: DemandeConge): boolean {
    return conge.statut === 'EN_ATTENTE';
  }

  peutModifier(conge: DemandeConge): boolean {
    return conge.statut === 'EN_ATTENTE';
  }

  formatDate(date: any): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  getMotifRefus(conge: DemandeConge): string {
    if (conge.statut === 'REFUSE' && conge.motifRefus) {
      return `Motif: ${conge.motifRefus}`;
    }
    return '';
  }

  getSoldeProgress(): number {
    if (this.soldeTotal === 0) return 0;
    return Math.round((this.soldePris / this.soldeTotal) * 100);
  }

  trackById(index: number, item: DemandeConge): number {
    return item.id!;
  }
}