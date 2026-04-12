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
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';
import { NotificationApiService } from '../../../../../core/services/notification-api.service';

@Component({
  selector: 'app-liste-conges',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatProgressBarModule,
    MatTooltipModule, MatDialogModule
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

  displayedColumns = ['periode', 'type', 'jours', 'statut', 'actions'];

  constructor(
    private congeService: EmployeeCongeService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private notifApi: NotificationApiService
  ) {}

  ngOnInit(): void {
    this.loadConges();
    this.loadSolde();
    this.startPolling();
  }

  ngOnDestroy(): void {
    if (this.pollingInterval) clearInterval(this.pollingInterval);
  }

  startPolling(): void {
    this.pollingInterval = setInterval(() => {
      this.checkForUpdates();
    }, this.POLLING_INTERVAL_MS);
  }

  checkForUpdates(): void {
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          const nouvellesDemandes = response.data as DemandeConge[];
          this.detectStatutChanges(nouvellesDemandes);
          this.conges = nouvellesDemandes;
        }
      },
      error: (err: any) => console.error('Polling error:', err)
    });
  }

  private detectStatutChanges(nouvelles: DemandeConge[]): void {
    nouvelles.forEach(nouvelle => {
      const ancienStatut = this.previousStatuts.get(nouvelle.id!);
      if (ancienStatut && ancienStatut !== nouvelle.statut) {
        if (nouvelle.statut === 'APPROUVE') {
          const msg = `✅ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été approuvée !`;
          this.notifApi.createNotification(msg, 'success', nouvelle.id).subscribe();
          this.showToast(msg, 'success');
        } else if (nouvelle.statut === 'REFUSE') {
          const motif = nouvelle.motifRefus || 'aucun motif fourni';
          const msg = `❌ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été refusée. Motif : ${motif}`;
          this.notifApi.createNotification(msg, 'error', nouvelle.id).subscribe();
          this.showToast(msg, 'error');
        }
      }
      this.previousStatuts.set(nouvelle.id!, nouvelle.statut!);
    });
  }

  loadConges(): void {
    this.loading = true;
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        this.loading = false;
        if (response.success) {
          this.conges = response.data as DemandeConge[];
          this.conges.forEach(c => {
            this.previousStatuts.set(c.id!, c.statut!);
          });
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
      next: (response: CongeResponse) => {
        if (response.success) {
          const data = response.data as any;
          this.soldeTotal     = data.total     || 25;
          this.soldePris      = data.pris       || 0;
          this.soldeRestant   = data.restant    || 25;
          this.soldeEnAttente = data.enAttente  || 0;
        }
      },
      error: (err: any) => {
        console.error('Erreur solde:', err);
      }
    });
  }

  annulerConge(conge: DemandeConge): void {
    if (!confirm(`Annuler cette demande du ${this.formatDate(conge.dateDebut)} ?`)) return;

    this.congeService.annulerConge(conge.id!).subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          this.showToast('✅ Demande annulée avec succès', 'success');
          this.loadConges();
          this.loadSolde();
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