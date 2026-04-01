import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';

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
  styleUrls: ['./liste-conges.component.css']
})
export class ListeCongesComponent implements OnInit {

  conges: DemandeConge[] = [];
  loading = true;

  // Année courante exposée au template
  currentYear: number = new Date().getFullYear();

  // Solde
  soldeTotal     = 0;
  soldePris      = 0;
  soldeRestant   = 0;
  soldeEnAttente = 0;

  displayedColumns = ['periode', 'type', 'jours', 'statut', 'actions'];

  constructor(
    private congeService: EmployeeCongeService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadConges();
    this.loadSolde();
  }

  loadConges(): void {
    this.loading = true;
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        this.loading = false;
        if (response.success) {
          this.conges = response.data as DemandeConge[];
        }
      },
      error: (err) => {
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
      error: (err) => {
        console.error('Erreur solde:', err);
        const message = err?.message?.includes('Refresh')
          ? 'Session expirée'
          : 'Erreur chargement solde';
        this.snackBar.open(message, 'Fermer', { duration: 3000 });
      }
    });
  }

  annulerConge(conge: DemandeConge): void {
    if (!confirm(`Annuler cette demande du ${this.formatDate(conge.dateDebut)} ?`)) return;

    this.congeService.annulerConge(conge.id!).subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          this.snackBar.open('Demande annulée', 'Fermer', { duration: 3000 });
          this.loadConges();
          this.loadSolde();
        }
      },
      error: (err) => {
        console.error('Erreur annulation:', err);
        this.snackBar.open('Erreur lors de l\'annulation', 'Fermer', { duration: 3000 });
      }
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

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'APPROUVE':   return 'Approuvé';
      case 'EN_ATTENTE': return 'En attente';
      case 'REFUSE':     return 'Refusé';
      case 'ANNULE':     return 'Annulé';
      default:           return statut;
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

  getSoldeProgress(): number {
    if (this.soldeTotal === 0) return 0;
    return Math.round((this.soldePris / this.soldeTotal) * 100);
  }
}