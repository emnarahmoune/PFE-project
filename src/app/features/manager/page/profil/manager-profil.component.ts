// features/manager/pages/manager-profil/manager-profil.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

import { finalize, Subscription } from 'rxjs';

import { ManagerProfileService } from '../../../../core/services/manager-profile.service';
import {
  ManagerProfile,
  UpdateProfileData,
  ChangePasswordData
} from '../../../../features/manager/models/manager-profile.model';
import { PictureService } from '../../../../core/services/picture.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-manager-profil',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule
  ],
  templateUrl: './manager-profil.component.html',
  styleUrls: ['./manager-profil.component.scss']
})
export class ManagerProfilComponent implements OnInit, OnDestroy {
  profileForm: FormGroup;
  passwordForm: FormGroup;

  loading = true;
  submitting = false;

  profileData: ManagerProfile | null = null;

  editMode = false;
  passwordEditMode = false;

  hideOld = true;
  hideNew = true;
  hideConfirm = true;

  selectedFile: File | null = null;
  uploadProgress = false;
  photoPreview: string | ArrayBuffer | null = null;
  currentPhotoUrl: string | null = null;


  loadingPassword = false;
passwordError = '';
passwordSuccess = '';
  private pictureSubscription: Subscription | null = null;

  constructor(
    private fb: FormBuilder,
    private profileService: ManagerProfileService,
    private snackBar: MatSnackBar,
    private pictureService: PictureService,
    private keycloakService: KeycloakService
  ) {
    this.profileForm = this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      telephone: ['', [Validators.pattern(/^$|^[0-9+\s()\-]{6,20}$/)]],
      poste: ['', [Validators.maxLength(100)]],
      departement: ['']
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  ngOnInit(): void {
    this.loadProfile();

    this.pictureSubscription = this.pictureService.picture$.subscribe(rawUrl => {
      this.currentPhotoUrl = PictureService.buildDisplayUrl(rawUrl);
    });
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPass = group.get('newPassword')?.value;
    const confirmPass = group.get('confirmPassword')?.value;

    return newPass === confirmPass ? null : { mismatch: true };
  }

  loadProfile(): void {
    this.loading = true;

    this.profileService.getProfile()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res: { success: boolean; data: ManagerProfile; message: string }) => {
          if (res.success && res.data) {
            this.profileData = res.data;

            this.profileForm.patchValue({
              nom: res.data.nom,
              prenom: res.data.prenom,
              email: res.data.email,
              telephone: res.data.telephone,
              poste: res.data.poste,
              departement: res.data.departement
            });

            this.pictureService.setPicture(res.data.photoUrl || null);
          }
        },
        error: () => {
          this.snackBar.open('Erreur chargement profil', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  onImageError(): void {
    console.warn('Erreur chargement image :', this.currentPhotoUrl);
    this.currentPhotoUrl = null;
    this.pictureService.setPicture(null);
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;

    if (!this.editMode && this.profileData) {
      this.profileForm.patchValue({
        nom: this.profileData.nom,
        prenom: this.profileData.prenom,
        email: this.profileData.email,
        telephone: this.profileData.telephone,
        poste: this.profileData.poste,
        departement: this.profileData.departement
      });
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      this.snackBar.open('Corrigez les erreurs du formulaire', 'Fermer', {
        duration: 4000
      });
      return;
    }

    this.submitting = true;

    const formValue = this.profileForm.value;

    const updateData: UpdateProfileData = {
      nom: formValue.nom,
      prenom: formValue.prenom,
      email: formValue.email,
      telephone: formValue.telephone || '',
      poste: formValue.poste || '',
      departement: formValue.departement || ''
    };

    this.profileService.updateProfile(updateData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (res: { success: boolean; data: ManagerProfile; message: string }) => {
          if (res.success) {
            this.profileData = res.data;
            this.editMode = false;

            this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur lors de la mise à jour', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err: any) => {
          this.snackBar.open(err.error?.message || 'Erreur lors de la mise à jour', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  togglePasswordEdit(): void {
    this.passwordEditMode = !this.passwordEditMode;

    if (!this.passwordEditMode) {
      this.passwordForm.reset();
      this.hideOld = true;
      this.hideNew = true;
      this.hideConfirm = true;
    }
  }

changePassword(): void {
  this.passwordError = '';
  this.passwordSuccess = '';

  if (this.passwordForm.invalid) {
    this.passwordForm.markAllAsTouched();

    if (this.passwordForm.hasError('mismatch')) {
      this.passwordError = 'La confirmation ne correspond pas au nouveau mot de passe.';
    } else {
      this.passwordError = 'Veuillez remplir correctement tous les champs.';
    }

    return;
  }

  const oldPassword = this.passwordForm.get('oldPassword')?.value;
  const newPassword = this.passwordForm.get('newPassword')?.value;
  const confirmPassword = this.passwordForm.get('confirmPassword')?.value;

  if (newPassword !== confirmPassword) {
    this.passwordError = 'La confirmation ne correspond pas au nouveau mot de passe.';
    return;
  }

 const payload: ChangePasswordData = {
  oldPassword,
  newPassword,
  confirmPassword
};

  this.loadingPassword = true;

  this.profileService.changePassword(payload)
    .pipe(finalize(() => this.loadingPassword = false))
    .subscribe({
      next: () => {
        this.passwordSuccess = 'Mot de passe modifié avec succès.';
        this.passwordError = '';

        this.passwordForm.reset();
        this.passwordEditMode = false;

        this.hideOld = true;
        this.hideNew = true;
        this.hideConfirm = true;

        this.snackBar.open('Mot de passe modifié avec succès', 'Fermer', {
          duration: 3000
        });
      },
      error: (error) => {
        console.error('Erreur changement mot de passe:', error);

        this.passwordSuccess = '';
        this.passwordError =
          error?.error?.message ||
          error?.error?.data?.message ||
          error?.error?.error ||
          error?.message ||
          'Erreur lors du changement du mot de passe.';

        this.snackBar.open(this.passwordError, 'Fermer', {
          duration: 5000
        });
      }
    });
}
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];

      const reader = new FileReader();
      reader.onload = () => {
        this.photoPreview = reader.result;
      };

      reader.readAsDataURL(this.selectedFile);
    }
  }

  uploadPhoto(): void {
    if (!this.selectedFile) {
      return;
    }

    this.uploadProgress = true;

    this.profileService.uploadPhoto(this.selectedFile)
      .pipe(finalize(() => this.uploadProgress = false))
      .subscribe({
        next: (res) => {
          if (res.success && res.data?.photoUrl) {
            const rawUrl = res.data.photoUrl;

            if (this.profileData) {
              this.profileData.photoUrl = rawUrl;
            }

            this.selectedFile = null;
            this.photoPreview = null;

            this.pictureService.setPicture(rawUrl);
            this.loadProfile();

            this.snackBar.open('Photo mise à jour avec succès', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur upload photo', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err) => {
          console.error('Erreur upload photo manager:', err);

          this.snackBar.open(
            err.error?.message || "Erreur lors de l'upload",
            'Fermer',
            { duration: 5000 }
          );
        }
      });
  }

  deletePhoto(): void {
    this.profileService.deletePhoto().subscribe({
      next: () => {
        if (this.profileData) {
          this.profileData.photoUrl = undefined;
        }

        this.currentPhotoUrl = null;
        this.photoPreview = null;
        this.selectedFile = null;

        this.pictureService.setPicture(null);
        this.loadProfile();

        this.snackBar.open('Photo supprimée avec succès', 'Fermer', {
          duration: 3000
        });
      },
      error: (err) => {
        console.error('Erreur suppression photo manager:', err);

        this.snackBar.open(
          err.error?.message || 'Erreur suppression',
          'Fermer',
          { duration: 5000 }
        );
      }
    });
  }

  get statutClass(): string {
    if (!this.profileData) {
      return '';
    }

    const statut = this.profileData.statutCompte || '';

    if (statut.includes('Verrouillé')) {
      return 'status-locked';
    }

    if (statut.includes('Inactif')) {
      return 'status-inactive';
    }

    return 'status-active';
  }

  getUserInitials(): string {
    if (!this.profileData) {
      return 'MG';
    }

    const prenom = this.profileData.prenom || '';
    const nom = this.profileData.nom || '';

    if (!prenom && !nom) {
      return 'MG';
    }

    return `${prenom.charAt(0)}${nom.charAt(0)}`.toUpperCase();
  }
}