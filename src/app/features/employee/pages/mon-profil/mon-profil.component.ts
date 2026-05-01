import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { KeycloakService } from 'keycloak-angular';
import { EmployeProfilService } from '../../services/employe-profil.service';
import { EmployeProfil } from '../../models/employe-profil.model';
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
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './mon-profil.component.html',
  styleUrls: ['./mon-profil.component.css']
})
export class MonProfilComponent implements OnInit {

  profil: EmployeProfil | null = null;
  loading = true;
  editMode = false;
  profilForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private profilService: EmployeProfilService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private keycloak: KeycloakService
  ) {
    this.profilForm = this.fb.group({
      telephone: ['', [Validators.pattern(/^[0-9+\-\s]{8,20}$/)]],
      adresse: ['']
    });
  }

  ngOnInit(): void {
    this.loadProfil();
  }

  loadProfil(): void {
    this.loading = true;

    this.profilService.getMonProfil().subscribe({
      next: (response) => {
        this.profil = response?.data as EmployeProfil;

        this.profilForm.patchValue({
          telephone: this.profil?.telephone || '',
          adresse: this.profil?.adresse || ''
        });

        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement profil:', err);

        this.snackBar.open('Erreur lors du chargement du profil', 'Fermer', {
          duration: 3000
        });

        this.loading = false;
      }
    });
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;

    if (!this.editMode && this.profil) {
      this.profilForm.patchValue({
        telephone: this.profil.telephone || '',
        adresse: this.profil.adresse || ''
      });
    }
  }

  saveProfil(): void {
    if (this.profilForm.invalid) {
      this.profilForm.markAllAsTouched();

      this.snackBar.open('Veuillez vérifier les informations saisies', 'Fermer', {
        duration: 3000
      });

      return;
    }

    const payload = {
      telephone: this.profilForm.value.telephone,
      adresse: this.profilForm.value.adresse
    };

    console.log('Payload profil envoyé:', payload);

    this.profilService.updateMonProfil(payload).subscribe({
      next: (response) => {
        console.log('Profil mis à jour:', response);

        this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
          duration: 3000
        });

        this.editMode = false;
        this.loadProfil();
      },
      error: (err) => {
        console.error('Erreur mise à jour profil:', err);

        this.snackBar.open('Erreur lors de la mise à jour', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

 openChangePasswordDialog(): void {
  const dialogRef = this.dialog.open(ChangePasswordDialogComponent, {
    width: '450px',
    disableClose: false
  });

  dialogRef.afterClosed().subscribe((result: any) => {
    if (!result) {
      return;
    }

    this.profilService.changePassword(result.oldPassword, result.newPassword).subscribe({
      next: () => {
        this.snackBar.open(
          'Mot de passe changé avec succès. Veuillez vous reconnecter.',
          'Fermer',
          {
            duration: 2500
          }
        );

        setTimeout(() => {
          this.logoutAfterPasswordChange();
        }, 1500);
      },
      error: (err: any) => {
        console.error('Erreur changement mot de passe:', err);

        this.snackBar.open(
          err.error?.message || 'Erreur lors du changement du mot de passe',
          'Fermer',
          {
            duration: 3000
          }
        );
      }
    });
  });
}


private logoutAfterPasswordChange(): void {
  localStorage.clear();
  sessionStorage.clear();

  this.keycloak.logout('http://localhost:4200/login');
}
  calculerAnciennete(): string {
    if (!this.profil?.dateEmbauche) {
      return 'Non renseignée';
    }

    const aujourdhui = new Date();
    const embauche = new Date(this.profil.dateEmbauche);

    let annees = aujourdhui.getFullYear() - embauche.getFullYear();
    let mois = aujourdhui.getMonth() - embauche.getMonth();

    if (mois < 0) {
      annees--;
      mois += 12;
    }

    if (annees === 0) {
      return `${mois} mois`;
    }

    if (mois === 0) {
      return `${annees} an${annees > 1 ? 's' : ''}`;
    }

    return `${annees} an${annees > 1 ? 's' : ''} et ${mois} mois`;
  }

  formatDate(date: Date | string | undefined | null): string {
    if (!date) {
      return '';
    }

    return new Date(date).toLocaleDateString('fr-FR');
  }

  getUserInitials(): string {
    if (this.profil?.prenom && this.profil?.nom) {
      return `${this.profil.prenom.charAt(0)}${this.profil.nom.charAt(0)}`.toUpperCase();
    }

    return 'U';
  }
}