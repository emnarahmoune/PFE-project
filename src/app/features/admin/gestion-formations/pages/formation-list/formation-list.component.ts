import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation.model';
// ✅ CORRECTION : Importer depuis le bon chemin
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';
import { MatProgressSpinner } from "@angular/material/progress-spinner";

@Component({
  selector: 'app-formation-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    MatProgressSpinner,
    ConfirmationDialogComponent  // ✅ Ajout de l'import du composant dans les imports
  ],
  templateUrl: './formation-list.component.html',
  styleUrls: ['./formation-list.component.css']
})
export class FormationListComponent implements OnInit {
  displayedColumns: string[] = ['titre', 'domaine', 'duree', 'participants', 'statut', 'actions'];
  dataSource = new MatTableDataSource<Formation>([]);
  loading = false;
  searchText = '';
  selectedDomaine = 'TOUS';

  domaines = ['TOUS', 'TECHNIQUE', 'SOFT_SKILLS', 'MANAGEMENT', 'LANGUES', 'SECURITE'];

  stats = {
    total: 0,
    actives: 0,
    dureeMoyenne: 0,
    participantsMoyens: 0,
    totalParticipants: 0,
    technique: 0,
    softSkills: 0,
    management: 0,
    langues: 0,
    securite: 0
  };

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private formationService: FormationService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadFormations();
    this.loadStats();
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    this.dataSource.filterPredicate = this.customFilterPredicate();
  }

  customFilterPredicate(): (data: Formation, filter: string) => boolean {
    return (data: Formation, filter: string): boolean => {
      const searchMatch = !this.searchText || 
        data.titre.toLowerCase().includes(this.searchText.toLowerCase()) ||
        data.description.toLowerCase().includes(this.searchText.toLowerCase());

      const domaineMatch = this.selectedDomaine === 'TOUS' || data.domaine === this.selectedDomaine;

      return searchMatch && domaineMatch;
    };
  }

  loadFormations(): void {
    this.loading = true;
    this.formationService.getAll().subscribe({
      next: (response) => {
        this.dataSource.data = response.data as Formation[];
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement formations:', error);
        this.snackBar.open('Erreur lors du chargement des formations', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.formationService.getStats().subscribe({
      next: (response) => {
        const stats = response.data as any;
        this.stats = {
          total: stats.totalFormations || 0,
          actives: stats.formationsActives || 0,
          dureeMoyenne: stats.dureeMoyenne || 0,
          participantsMoyens: stats.participantsMoyens || 0,
          totalParticipants: stats.totalParticipants || 0,
          technique: stats.TECHNIQUE || 0,
          softSkills: stats.SOFT_SKILLS || 0,
          management: stats.MANAGEMENT || 0,
          langues: stats.LANGUES || 0,
          securite: stats.SECURITE || 0
        };
      },
      error: (error) => console.error('Erreur chargement stats:', error)
    });
  }

  applyFilter(): void {
    this.dataSource.filter = 'apply';
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedDomaine = 'TOUS';
    this.applyFilter();
  }

  deleteFormation(id: number, titre: string): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirmation de suppression',
        message: `Êtes-vous sûr de vouloir désactiver la formation "${titre}" ?`,
        confirmText: 'Désactiver',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.formationService.delete(id).subscribe({
          next: () => {
            this.snackBar.open('Formation désactivée avec succès', 'Fermer', { duration: 3000 });
            this.loadFormations();
            this.loadStats();
          },
          error: (error) => {
            console.error('Erreur suppression:', error);
            this.snackBar.open('Erreur lors de la désactivation', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  toggleStatut(formation: Formation): void {
    const action = formation.actif ? 'désactiver' : 'activer';
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: `Confirmation`,
        message: `Êtes-vous sûr de vouloir ${action} la formation "${formation.titre}" ?`,
        confirmText: action === 'activer' ? 'Activer' : 'Désactiver',
        cancelText: 'Annuler'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        const serviceCall = formation.actif 
          ? this.formationService.desactiver(formation.id!)
          : this.formationService.activer(formation.id!);
        
        serviceCall.subscribe({
          next: () => {
            this.snackBar.open(`Formation ${action}e avec succès`, 'Fermer', { duration: 3000 });
            this.loadFormations();
            this.loadStats();
          },
          error: (error) => {
            console.error('Erreur changement statut:', error);
            this.snackBar.open('Erreur lors du changement de statut', 'Fermer', { duration: 3000 });
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

  getStatutColor(actif: boolean): string {
    return actif ? 'primary' : 'warn';
  }

  getParticipantPercentage(nombre: number): number {
    return Math.min((nombre / 50) * 100, 100);
  }
}