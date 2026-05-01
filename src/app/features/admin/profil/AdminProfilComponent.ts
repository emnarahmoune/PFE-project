// features/admin/profil/admin-profil.component.ts
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
import { finalize, Subscription } from 'rxjs';
import { AdminProfileService, AdminProfile, UpdateProfileData, ChangePasswordData } from '../../../core/services/AdminProfileService';
import { PictureService } from '../../../core/services/picture.service';

@Component({
  selector: 'app-admin-profil',
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
    MatTooltipModule
  ],
  templateUrl: './AdminProfilComponent.html',
  styleUrls: ['./AdminProfilComponent.scss']
})
export class AdminProfilComponent implements OnInit, OnDestroy {
  profileForm: FormGroup;
  passwordForm: FormGroup;
  loading = true;
  submitting = false;
  profileData: AdminProfile | null = null;
  editMode = false;
  passwordEditMode = false;

  hideOld = true;
  hideNew = true;
  hideConfirm = true;

  // Gestion photo
  selectedFile: File | null = null;
  uploadProgress = false;
  photoPreview: string | ArrayBuffer | null = null;
  currentPhotoUrl: string | null = null;

  private pictureSubscription: Subscription | null = null;

  constructor(
    private fb: FormBuilder,
    private profileService: AdminProfileService,
    private snackBar: MatSnackBar,
    private pictureService: PictureService
  ) {
    this.profileForm = this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      prenom: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      telephone: ['', [Validators.pattern(/^[0-9+\s()\-]{6,20}$/)]],
      poste: ['', [Validators.maxLength(100)]],
      departement: ['']
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadProfile();
    this.pictureSubscription = this.pictureService.picture$.subscribe(url => {
      this.currentPhotoUrl = url;
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
        next: (res) => {
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
          } else {
            this.snackBar.open('Impossible de charger le profil', 'Fermer', { duration: 5000 });
          }
        },
        error: () => this.snackBar.open('Erreur de chargement du profil', 'Fermer', { duration: 5000 })
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
      this.snackBar.open('Veuillez corriger les erreurs', 'Fermer', { duration: 4000 });
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
        next: (res) => {
          if (res.success) {
            this.profileData = res.data;
            this.snackBar.open('Profil mis à jour avec succès', 'Fermer', { duration: 3000 });
            this.editMode = false;
          } else {
            this.snackBar.open(res.message || 'Erreur lors de la mise à jour', 'Fermer', { duration: 5000 });
          }
        },
        error: (err) => this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', { duration: 5000 })
      });
  }

  togglePasswordEdit(): void {
    this.passwordEditMode = !this.passwordEditMode;
    if (!this.passwordEditMode) this.passwordForm.reset();
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      if (this.passwordForm.hasError('mismatch')) {
        this.snackBar.open('Les mots de passe ne correspondent pas', 'Fermer', { duration: 4000 });
      } else {
        this.snackBar.open('Veuillez remplir tous les champs', 'Fermer', { duration: 4000 });
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

    this.profileService.changePassword(passwordData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (res) => {
          if (res.success) {
            this.snackBar.open('Mot de passe modifié avec succès', 'Fermer', { duration: 3000 });
            this.passwordEditMode = false;
            this.passwordForm.reset();
          } else {
            this.snackBar.open(res.message || 'Erreur', 'Fermer', { duration: 5000 });
          }
        },
        error: (err) => this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', { duration: 5000 })
      });
  }

  // ========== PHOTO METHODS ==========
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
      const reader = new FileReader();
      reader.onload = () => { this.photoPreview = reader.result; };
      reader.readAsDataURL(this.selectedFile);
    }
  }

  uploadPhoto(): void {
    if (!this.selectedFile) return;
    this.uploadProgress = true;
    this.profileService.uploadPhoto(this.selectedFile).subscribe({
      next: (res) => {
        if (res.success && res.data?.photoUrl) {
          if (this.profileData) this.profileData.photoUrl = res.data.photoUrl;
          this.snackBar.open('Photo mise à jour', 'Fermer', { duration: 3000 });
          this.selectedFile = null;
          this.photoPreview = null;
          this.pictureService.setPicture(res.data.photoUrl);
        } else {
          this.snackBar.open(res.message || 'Erreur', 'Fermer', { duration: 5000 });
        }
        this.uploadProgress = false;
      },
      error: () => {
        this.snackBar.open("Erreur lors de l'upload", 'Fermer', { duration: 5000 });
        this.uploadProgress = false;
      }
    });
  }

  deletePhoto(): void {
    this.profileService.deletePhoto().subscribe({
      next: () => {
        if (this.profileData) this.profileData.photoUrl = undefined;
        this.snackBar.open('Photo supprimée', 'Fermer', { duration: 3000 });
        this.pictureService.setPicture(null);
      },
      error: () => this.snackBar.open('Erreur suppression', 'Fermer', { duration: 5000 })
    });
  }

  getUserInitials(): string {
    if (!this.profileData) return 'AD';
    return `${this.profileData.prenom.charAt(0)}${this.profileData.nom.charAt(0)}`.toUpperCase();
  }

  get statutClass(): string {
    if (!this.profileData) return '';
    const statut = this.profileData.statutCompte;
    if (statut.includes('Verrouillé')) return 'status-locked';
    if (statut.includes('Inactif')) return 'status-inactive';
    return 'status-active';
  }
}