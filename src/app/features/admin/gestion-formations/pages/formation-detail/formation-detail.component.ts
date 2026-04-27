import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormationService } from '../../../../../core/services/formation.service';
import { Formation, EmployeFormation } from '../../models/formation.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTableModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './formation-detail.component.html',
  styleUrls: ['./formation-detail.component.css']
})
export class FormationDetailComponent implements OnInit {
  formation: Formation | null = null;
  participants: EmployeFormation[] = [];
  loading = true;
  displayedColumns: string[] = ['nom', 'poste', 'departement', 'statut', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private formationService: FormationService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadFormation(id);
      this.loadParticipants(id);
    } else {
      this.router.navigate(['/admin/formations']);
    }
  }

  loadFormation(id: number): void {
    this.formationService.getById(id).subscribe({
      next: (response) => {
        this.formation = response.data as Formation;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement formation:', error);
        this.snackBar.open('Erreur lors du chargement de la formation', 'Fermer', { duration: 3000 });
        this.router.navigate(['/admin/formations']);
      }
    });
  }

  loadParticipants(id: number): void {
    this.formationService.getParticipants(id).subscribe({
      next: (response) => {
        // ✅ CORRECTION : Vérifier que les données sont un tableau et les convertir correctement
        const data = response.data;
        if (Array.isArray(data)) {
          this.participants = data as unknown as EmployeFormation[];
        } else {
          this.participants = [];
        }
      },
      error: (error) => {
        console.error('Erreur chargement participants:', error);
        this.participants = [];
      }
    });
  }

  getCompletionRate(): number {
    if (!this.participants.length) return 0;
    const termines = this.participants.filter(p => p.statut === 'TERMINE').length;
    return Math.round((termines / this.participants.length) * 100);
  }

  getAvgSatisfaction(): number {
    return 4.2;
  }

  toggleStatut(): void {
    if (!this.formation) return;
    
    const action = this.formation.actif ? 'désactiver' : 'activer';
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: `Confirmation`,
        message: `Êtes-vous sûr de vouloir ${action} cette formation ?`,
        confirmText: action === 'activer' ? 'Activer' : 'Désactiver',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.formation) {
        const serviceCall = this.formation.actif 
          ? this.formationService.desactiver(this.formation.id!)
          : this.formationService.activer(this.formation.id!);
        
        serviceCall.subscribe({
          next: () => {
            this.snackBar.open(`Formation ${action}e avec succès`, 'Fermer', { duration: 3000 });
            this.loadFormation(this.formation!.id!);
          },
          error: (error) => {
            console.error('Erreur changement statut:', error);
            this.snackBar.open('Erreur lors du changement de statut', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  deleteFormation(): void {
    if (!this.formation) return;
    
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirmation de suppression',
        message: `Êtes-vous sûr de vouloir supprimer la formation "${this.formation.titre}" ?`,
        confirmText: 'Supprimer',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.formation) {
        this.formationService.delete(this.formation.id!).subscribe({
          next: () => {
            this.snackBar.open('Formation supprimée avec succès', 'Fermer', { duration: 3000 });
            this.router.navigate(['/admin/formations']);
          },
          error: (error) => {
            console.error('Erreur suppression:', error);
            this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  retirerParticipant(participant: EmployeFormation): void {
    if (!this.formation) return;
    
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirmation',
        message: `Êtes-vous sûr de vouloir retirer ${participant.prenom} ${participant.nom} de cette formation ?`,
        confirmText: 'Retirer',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.formation) {
        this.formationService.retirerParticipant(this.formation.id!, participant.id).subscribe({
          next: () => {
            this.snackBar.open('Participant retiré avec succès', 'Fermer', { duration: 3000 });
            this.loadParticipants(this.formation!.id!);
          },
          error: (error) => {
            console.error('Erreur retrait participant:', error);
            this.snackBar.open('Erreur lors du retrait', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  getDomaineColor(domaine: string): string {
    switch(domaine) {
      case 'TECHNIQUE': return 'primary';
      case 'SOFT_SKILLS': return 'accent';
      case 'MANAGEMENT': return 'info';
      case 'LANGUES': return 'warn';
      case 'SECURITE': return 'danger';
      default: return '';
    }
  }

  formatDuree(heures: number): string {
    if (heures < 24) return `${heures}h`;
    const jours = Math.floor(heures / 24);
    const reste = heures % 24;
    return reste > 0 ? `${jours}j ${reste}h` : `${jours}j`;
  }

  getStatutColor(statut: string): string {
    switch(statut) {
      case 'INSCRIT': return 'accent';
      case 'EN_COURS': return 'primary';
      case 'TERMINE': return 'primary';
      case 'ABANDON': return 'warn';
      default: return '';
    }
  }
}