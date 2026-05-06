import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { finalize, Subscription } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import {
  EmployeeProfileService,
  ChangePasswordData
} from '../../../../core/services/employe-profile.service';

import {
  EmployeProfil,
  EmployeProfilResponse,
  UpdateProfilRequest
} from '../../models/employe-profil.model';

import { PictureService } from '../../../../core/services/picture.service';

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
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './mon-profil.component.html',
  styleUrls: ['./mon-profil.component.css']
})
export class MonProfilComponent implements OnInit, OnDestroy {
  profileForm: FormGroup;
  passwordForm: FormGroup;

  loading = true;
  submitting = false;

  profileData: EmployeProfil | null = null;
  profil: EmployeProfil | null = null;

  editMode = false;
  passwordEditMode = false;

  hideOld = true;
  hideNew = true;
  hideConfirm = true;

  selectedFile: File | null = null;
  uploadProgress = false;
  photoPreview: string | ArrayBuffer | null = null;
  currentPhotoUrl: string | null = null;

  private pictureSubscription: Subscription | null = null;

  constructor(
    private fb: FormBuilder,
    private profilService: EmployeeProfileService,
    private snackBar: MatSnackBar,
    private pictureService: PictureService,
    private keycloakService: KeycloakService
  ) {
    this.profileForm = this.fb.group({
      telephone: ['', [Validators.pattern(/^[0-9+\s()\-]{6,20}$/)]],
      adresse: ['']
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(url => {
      this.currentPhotoUrl = this.normalizePhotoUrl(url);
    });

    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
  }

  private normalizePhotoUrl(url?: string | null): string | null {
    if (!url) {
      return null;
    }

    let cleanUrl = url.trim();

    if (!cleanUrl) {
      return null;
    }

    // Si PictureService a déjà ajouté ?t=...
    cleanUrl = cleanUrl.split('?')[0];

    // URL absolue
    if (cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    // URL relative backend : /api/uploads/...
    if (cleanUrl.startsWith('/api/')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    // URL sans slash : api/uploads/...
    if (cleanUrl.startsWith('api/')) {
      return `/${cleanUrl}?t=${Date.now()}`;
    }

    // Nom de fichier seulement
    return `/api/uploads/profile-photos/${cleanUrl}?t=${Date.now()}`;
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPass = group.get('newPassword')?.value;
    const confirmPass = group.get('confirmPassword')?.value;

    return newPass === confirmPass ? null : { mismatch: true };
  }

  loadProfile(): void {
    this.loading = true;

    this.profilService.getMonProfil()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res: EmployeProfilResponse) => {
          if (res.success && res.data) {
            this.profileData = res.data;
            this.profil = res.data;

            this.profileForm.patchValue({
              telephone: res.data.telephone || '',
              adresse: res.data.adresse || ''
            });

            const normalizedUrl = this.normalizePhotoUrl(res.data.photoUrl || null);
            this.currentPhotoUrl = normalizedUrl;
            this.pictureService.setPicture(normalizedUrl);
          } else {
            this.snackBar.open('Impossible de charger le profil', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err: any) => {
          console.error('Erreur chargement profil employé:', err);

          this.snackBar.open('Erreur de chargement du profil', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  onImageError(): void {
    console.warn('Erreur chargement image:', this.currentPhotoUrl);
    this.currentPhotoUrl = null;
    this.pictureService.setPicture(null);
  }

  toggleEditMode(): void {
    this.editMode = !this.editMode;

    if (!this.editMode && this.profileData) {
      this.profileForm.patchValue({
        telephone: this.profileData.telephone || '',
        adresse: this.profileData.adresse || ''
      });
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();

      this.snackBar.open('Veuillez corriger les erreurs', 'Fermer', {
        duration: 4000
      });

      return;
    }

    this.submitting = true;

    const formValue = this.profileForm.value;

    const updateData: UpdateProfilRequest = {
      telephone: formValue.telephone || '',
      adresse: formValue.adresse || ''
    };

    this.profilService.updateMonProfil(updateData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (res: EmployeProfilResponse) => {
          if (res.success) {
            this.profileData = res.data;
            this.profil = res.data;

            this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
              duration: 3000
            });

            this.editMode = false;
          } else {
            this.snackBar.open(res.message || 'Erreur lors de la mise à jour', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err: any) => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  togglePasswordEdit(): void {
    this.passwordEditMode = !this.passwordEditMode;

    if (!this.passwordEditMode) {
      this.passwordForm.reset();
    }
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();

      if (this.passwordForm.hasError('mismatch')) {
        this.snackBar.open('Les mots de passe ne correspondent pas', 'Fermer', {
          duration: 4000
        });
      } else {
        this.snackBar.open('Veuillez remplir tous les champs', 'Fermer', {
          duration: 4000
        });
      }

      return;
    }

    this.submitting = true;

    const formValue = this.passwordForm.value;

    const passwordData: ChangePasswordData = {
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword,
      confirmPassword: formValue.confirmPassword
    };

    this.profilService.changePassword(passwordData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: async (res: EmployeProfilResponse) => {
          if (res.success) {
            this.snackBar.open('Mot de passe modifié, reconnexion requise', 'Fermer', {
              duration: 3000
            });

            this.passwordEditMode = false;
            this.passwordForm.reset();

            setTimeout(async () => {
              await this.keycloakService.logout(window.location.origin);
            }, 1500);
          } else {
            this.snackBar.open(res.message || 'Erreur', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err: any) => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
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

    this.profilService.uploadPhoto(this.selectedFile)
      .pipe(finalize(() => this.uploadProgress = false))
      .subscribe({
        next: (res) => {
          if (res.success && res.data?.photoUrl) {
            const normalizedUrl = this.normalizePhotoUrl(res.data.photoUrl);

            if (this.profileData) {
              this.profileData.photoUrl = res.data.photoUrl;
            }

            this.currentPhotoUrl = normalizedUrl;
            this.pictureService.setPicture(normalizedUrl);

            this.selectedFile = null;
            this.photoPreview = null;

            this.snackBar.open('Photo mise à jour', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur lors de la mise à jour de la photo', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: (err: any) => {
          console.error("Erreur lors de l'upload:", err);

          this.snackBar.open("Erreur lors de l'upload", 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  deletePhoto(): void {
    this.profilService.deletePhoto().subscribe({
      next: (res) => {
        if (this.profileData) {
          this.profileData.photoUrl = null;
        }

        this.currentPhotoUrl = null;
        this.selectedFile = null;
        this.photoPreview = null;

        this.pictureService.setPicture(null);

        this.snackBar.open(res.message || 'Photo supprimée', 'Fermer', {
          duration: 3000
        });
      },
      error: (err: any) => {
        console.error('Erreur suppression photo:', err);

        this.snackBar.open('Erreur suppression', 'Fermer', {
          duration: 5000
        });
      }
    });
  }

  getUserInitials(): string {
    if (!this.profileData?.prenom || !this.profileData?.nom) {
      return 'U';
    }

    return `${this.profileData.prenom.charAt(0)}${this.profileData.nom.charAt(0)}`.toUpperCase();
  }

  calculerAnciennete(): string {
    if (!this.profileData?.dateEmbauche) {
      return 'Non renseignée';
    }

    const aujourdHui = new Date();
    const embauche = new Date(this.profileData.dateEmbauche);

    let annees = aujourdHui.getFullYear() - embauche.getFullYear();
    let mois = aujourdHui.getMonth() - embauche.getMonth();

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

  get statutClass(): string {
    if (!this.profileData) {
      return '';
    }

    const statut = this.profileData.statut || '';

    if (statut.includes('Verrouillé')) return 'status-locked';
    if (statut.includes('Inactif')) return 'status-inactive';
    if (statut === 'INACTIF') return 'status-inactive';

    return 'status-active';
  }
}