import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { EmployeProfilService } from '../../services/employe-profil.service';
import { EmployeProfil, SoldeConges, CompetenceEmploye, FormationEmploye, HistoriqueConge } from '../../models/employe-profil.model';
import { ChangePasswordDialogComponent } from '../../components/change-password-dialog/change-password-dialog.component';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatTabsModule,
    MatProgressBarModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTableModule
  ],
  templateUrl: './mon-profil.component.html',
  styleUrls: ['./mon-profil.component.css']
})
export class MonProfilComponent implements OnInit {
  profil: EmployeProfil | null = null;
  soldeConges: SoldeConges | null = null;
  competences: CompetenceEmploye[] = [];
  formations: FormationEmploye[] = [];
  historiqueConges: HistoriqueConge[] = [];

  loading = true;
  editMode = false;
  profilForm: FormGroup;

  displayedColumnsCompetences: string[] = ['nom', 'categorie', 'niveau', 'certifie'];
  displayedColumnsFormations: string[] = ['titre', 'domaine', 'statut', 'progression'];
  displayedColumnsConges: string[] = ['dates', 'type', 'jours', 'statut'];

  constructor(
    private fb: FormBuilder,
    private profilService: EmployeProfilService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.profilForm = this.fb.group({
      telephone: ['', [Validators.pattern(/^[0-9+\-\s]{10,15}$/)]],
      adresse: ['']
    });
  }

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.loadProfil();
    this.loadSoldeConges();
    this.loadCompetences();
    this.loadFormations();
    this.loadHistoriqueConges();
  }

  loadProfil(): void {
    this.profilService.getMonProfil().subscribe({
      next: (response) => {
        this.profil = response.data as EmployeProfil;
        this.profilForm.patchValue({
          telephone: this.profil?.telephone || '',
          adresse: this.profil?.adresse || ''
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement profil:', error);
        this.snackBar.open('Erreur lors du chargement du profil', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadSoldeConges(): void {
    this.profilService.getMonSoldeConges().subscribe({
      next: (response) => {
        this.soldeConges = response.data as SoldeConges;
      },
      error: (error) => console.error('Erreur chargement solde congés:', error)
    });
  }

  loadCompetences(): void {
    this.profilService.getMesCompetences().subscribe({
      next: (response) => {
        this.competences = response.data as CompetenceEmploye[];
      },
      error: (error) => console.error('Erreur chargement compétences:', error)
    });
  }

  loadFormations(): void {
    this.profilService.getMesFormations().subscribe({
      next: (response) => {
        this.formations = response.data as FormationEmploye[];
      },
      error: (error) => console.error('Erreur chargement formations:', error)
    });
  }

  loadHistoriqueConges(): void {
    this.profilService.getHistoriqueConges().subscribe({
      next: (response) => {
        this.historiqueConges = response.data as HistoriqueConge[];
      },
      error: (error) => console.error('Erreur chargement historique:', error)
    });
  }

  // ✅ AJOUT DE LA MÉTHODE MANQUANTE
  calculerAnciennete(): string {
    if (!this.profil?.dateEmbauche) return 'Non renseignée';
    const aujourdhui = new Date();
    const embauche = new Date(this.profil.dateEmbauche);
    const diffAnnee = aujourdhui.getFullYear() - embauche.getFullYear();
    const diffMois = aujourdhui.getMonth() - embauche.getMonth();
    
    let annees = diffAnnee;
    let mois = diffMois;
    if (mois < 0) {
      annees--;
      mois += 12;
    }
    
    if (annees === 0) return `${mois} mois`;
    if (mois === 0) return `${annees} an${annees > 1 ? 's' : ''}`;
    return `${annees} an${annees > 1 ? 's' : ''} et ${mois} mois`;
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;
  }

  saveProfil(): void {
    if (this.profilForm.valid) {
      this.profilService.updateInformationsPersonnelles(this.profilForm.value).subscribe({
        next: (response) => {
          this.snackBar.open('Profil mis à jour avec succès', 'Fermer', { duration: 3000 });
          this.editMode = false;
          this.loadProfil();
        },
        error: (error) => {
          console.error('Erreur mise à jour:', error);
          this.snackBar.open('Erreur lors de la mise à jour', 'Fermer', { duration: 3000 });
        }
      });
    }
  }

  openChangePasswordDialog(): void {
    const dialogRef = this.dialog.open(ChangePasswordDialogComponent, {
      width: '450px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.profilService.changePassword(result.oldPassword, result.newPassword).subscribe({
          next: () => {
            this.snackBar.open('Mot de passe changé avec succès', 'Fermer', { duration: 3000 });
          },
          error: (error) => {
            this.snackBar.open(error.error?.message || 'Erreur lors du changement', 'Fermer', { duration: 3000 });
          }
        });
      }
    });
  }

  getNiveauColor(niveau: string): string {
    switch(niveau) {
      case 'EXPERT': return 'primary';
      case 'AVANCE': return 'accent';
      case 'INTERMEDIAIRE': return 'info';
      case 'DEBUTANT': return '';
      default: return '';
    }
  }

  getStatutColor(statut: string): string {
    switch(statut) {
      case 'APPROUVE': return 'primary';
      case 'EN_ATTENTE': return 'accent';
      case 'REFUSE': return 'warn';
      case 'ANNULE': return '';
      default: return '';
    }
  }

  getFormationStatutColor(statut: string): string {
    switch(statut) {
      case 'TERMINE': return 'primary';
      case 'EN_COURS': return 'accent';
      case 'INSCRIT': return 'info';
      case 'ABANDON': return 'warn';
      default: return '';
    }
  }

  getCongesProgress(): number {
    if (this.soldeConges && this.soldeConges.total > 0) {
      return (this.soldeConges.pris / this.soldeConges.total) * 100;
    }
    return 0;
  }

  formatDate(date: Date | undefined | null): string {
  if (!date) return '';
  return new Date(date).toLocaleDateString('fr-FR');
}
}