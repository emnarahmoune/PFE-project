import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CompetenceService } from '../../services/competence.service';
import { Competence } from '../../models/competence.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-competence-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './competence-detail.component.html',
  styleUrls: ['./competence-detail.component.css']
})
export class CompetenceDetailComponent implements OnInit {
  competence?: Competence;
  loading = true;
  error = false;

  // Exemples d'employés possédant cette compétence (à remplacer par vraies données API)
  employesAvecCompetence = [
    { nom: 'Jean Dupont', niveau: 'EXPERT', niveauValue: 4 },
    { nom: 'Sophie Martin', niveau: 'AVANCE', niveauValue: 3 },
    { nom: 'Pierre Bernard', niveau: 'INTERMEDIAIRE', niveauValue: 2 },
    { nom: 'Marie Petit', niveau: 'DEBUTANT', niveauValue: 1 }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private competenceService: CompetenceService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCompetence(+id);
    } else {
      this.router.navigate(['/admin/competences']);
    }
  }

  loadCompetence(id: number): void {
    this.loading = true;
    this.competenceService.getById(id).subscribe({
      next: (response) => {
        this.competence = response.data as Competence;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement compétence:', error);
        this.error = true;
        this.loading = false;
        this.snackBar.open('Erreur lors du chargement de la compétence', 'Fermer', { duration: 3000 });
      }
    });
  }

  editCompetence(): void {
    if (this.competence?.id) {
      this.router.navigate(['/admin/competences', this.competence.id, 'edit']);
    }
  }

  deleteCompetence(): void {
    if (!this.competence?.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirmation de suppression',
        message: `Êtes-vous sûr de vouloir supprimer la compétence "${this.competence.nom}" ?`,
        confirmText: 'Supprimer',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.competenceService.delete(this.competence!.id!).subscribe({
          next: () => {
            this.snackBar.open('Compétence supprimée avec succès', 'Fermer', { duration: 3000 });
            this.router.navigate(['/admin/competences']);
          },
          error: (error) => {
            console.error('Erreur suppression:', error);
            this.snackBar.open('Erreur lors de la suppression', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/competences']);
  }

  getCategorieColor(categorie: string): string {
    switch(categorie) {
      case 'TECHNIQUE': return 'primary';
      case 'SOFT_SKILL': return 'accent';
      case 'LINGUISTIQUE': return 'warn';
      case 'MANAGEMENT': return 'info';
      default: return '';
    }
  }

  getCategorieIcon(categorie: string): string {
    switch(categorie) {
      case 'TECHNIQUE': return 'code';
      case 'SOFT_SKILL': return 'people';
      case 'LINGUISTIQUE': return 'language';
      case 'MANAGEMENT': return 'business';
      default: return 'school';
    }
  }

  getNiveauColor(niveau: string): string {
    switch(niveau) {
      case 'EXPERT': return 'primary';
      case 'AVANCE': return 'accent';
      case 'INTERMEDIAIRE': return 'warn';
      case 'DEBUTANT': return 'info';
      default: return '';
    }
  }

  getNiveauPourcentage(niveau: string): number {
    switch(niveau) {
      case 'EXPERT': return 100;
      case 'AVANCE': return 75;
      case 'INTERMEDIAIRE': return 50;
      case 'DEBUTANT': return 25;
      default: return 0;
    }
  }

  getNiveauValue(niveau: string): number {
    switch(niveau) {
      case 'EXPERT': return 4;
      case 'AVANCE': return 3;
      case 'INTERMEDIAIRE': return 2;
      case 'DEBUTANT': return 1;
      default: return 0;
    }
  }
}