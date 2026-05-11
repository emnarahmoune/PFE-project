import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { finalize, Subscription } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

import { environment } from '../../../environments/environment';

import {
  AdminProfileService,
  AdminProfile,
  UpdateProfileData as AdminUpdateProfileData,
  ChangePasswordData as AdminChangePasswordData
} from '../../core/services/AdminProfile.service';

import { ManagerProfileService } from '../../core/services/manager-profile.service';

import {
  ManagerProfile,
  UpdateProfileData as ManagerUpdateProfileData,
  ChangePasswordData as ManagerChangePasswordData
} from '../../core/models/manager-profile.model';

import {
  EmployeeProfileService,
  ChangePasswordData as EmployeeChangePasswordData
} from '../../core/services/employe-profile.service';

import {
  EmployeProfil,
  EmployeProfilResponse,
  UpdateProfilRequest
} from '../employee/models/employe-profil.model';

import { PictureService } from '../../core/services/picture.service';

type ProfilMode =
  | 'ADMIN_PROFIL'
  | 'MANAGER_PROFIL'
  | 'EMPLOYE_PROFIL';

type ProfilData = AdminProfile | ManagerProfile | EmployeProfil | any;

@Component({
  selector: 'app-profil',
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
    MatTooltipModule,
    MatChipsModule
  ],
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.scss']
})
export class ProfilComponent implements OnInit, OnDestroy {
  mode: ProfilMode = 'EMPLOYE_PROFIL';

  profileForm: FormGroup;
  passwordForm: FormGroup;

  loading = true;
  submitting = false;

  profileData: ProfilData | null = null;
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

  loadingPassword = false;
  passwordError = '';
  passwordSuccess = '';

  private pictureSubscription: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private adminProfileService: AdminProfileService,
    private managerProfileService: ManagerProfileService,
    private employeeProfileService: EmployeeProfileService,
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
      departement: [''],
      adresse: ['']
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
    this.mode = this.route.snapshot.data['profilMode'] || 'EMPLOYE_PROFIL';

    this.configureFormForMode();

    this.pictureSubscription = this.pictureService.picture$.subscribe(rawUrl => {
      this.currentPhotoUrl = this.normalizePhotoUrl(rawUrl);
    });

    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
  }

  // =========================================================
  // MODES
  // =========================================================

  isAdminMode(): boolean {
    return this.mode === 'ADMIN_PROFIL';
  }

  isManagerMode(): boolean {
    return this.mode === 'MANAGER_PROFIL';
  }

  isEmployeeMode(): boolean {
    return this.mode === 'EMPLOYE_PROFIL';
  }

  private configureFormForMode(): void {
    if (this.isEmployeeMode()) {
      this.profileForm.get('nom')?.clearValidators();
      this.profileForm.get('prenom')?.clearValidators();
      this.profileForm.get('email')?.clearValidators();
      this.profileForm.get('poste')?.clearValidators();
      this.profileForm.get('departement')?.clearValidators();

      this.profileForm.get('telephone')?.setValidators([
        Validators.pattern(/^[0-9+\s()\-]{6,20}$/)
      ]);

      this.profileForm.get('adresse')?.setValidators([]);
    } else {
      this.profileForm.get('nom')?.setValidators([
        Validators.required,
        Validators.maxLength(100)
      ]);
      this.profileForm.get('prenom')?.setValidators([
        Validators.required,
        Validators.maxLength(100)
      ]);
      this.profileForm.get('email')?.setValidators([
        Validators.required,
        Validators.email,
        Validators.maxLength(150)
      ]);
      this.profileForm.get('telephone')?.setValidators([
        Validators.pattern(/^$|^[0-9+\s()\-]{6,20}$/)
      ]);
      this.profileForm.get('poste')?.setValidators([
        Validators.maxLength(100)
      ]);
      this.profileForm.get('departement')?.setValidators([]);
    }

    Object.keys(this.profileForm.controls).forEach(key => {
      this.profileForm.get(key)?.updateValueAndValidity();
    });
  }

  // =========================================================
  // VALIDATION PASSWORD
  // =========================================================

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPass = group.get('newPassword')?.value;
    const confirmPass = group.get('confirmPassword')?.value;

    return newPass === confirmPass ? null : { mismatch: true };
  }

  // =========================================================
  // LOAD PROFILE
  // =========================================================

  loadProfile(): void {
    this.loading = true;

    if (this.isAdminMode()) {
      this.loadAdminProfile();
      return;
    }

    if (this.isManagerMode()) {
      this.loadManagerProfile();
      return;
    }

    this.loadEmployeeProfile();
  }

  private loadAdminProfile(): void {
    this.adminProfileService.getProfile()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: res => {
          if (res.success && res.data) {
            this.profileData = res.data;

            this.profileForm.patchValue({
              nom: res.data.nom,
              prenom: res.data.prenom,
              email: res.data.email,
              telephone: res.data.telephone,
              poste: res.data.poste,
              departement: res.data.departement,
              adresse: ''
            });

            const normalizedPhotoUrl = this.normalizePhotoUrl(res.data.photoUrl);
            this.currentPhotoUrl = normalizedPhotoUrl;
            this.pictureService.setPicture(normalizedPhotoUrl);
          } else {
            this.snackBar.open('Impossible de charger le profil', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: () => {
          this.snackBar.open('Erreur de chargement du profil', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private loadManagerProfile(): void {
    this.managerProfileService.getProfile()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: res => {
          if (res.success && res.data) {
            this.profileData = res.data;

            this.profileForm.patchValue({
              nom: res.data.nom,
              prenom: res.data.prenom,
              email: res.data.email,
              telephone: res.data.telephone,
              poste: res.data.poste,
              departement: res.data.departement,
              adresse: ''
            });

            const normalizedPhotoUrl = this.normalizePhotoUrl(res.data.photoUrl || null);
            this.currentPhotoUrl = normalizedPhotoUrl;
            this.pictureService.setPicture(res.data.photoUrl || null);
          } else {
            this.snackBar.open('Impossible de charger le profil manager', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: () => {
          this.snackBar.open('Erreur chargement profil', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private loadEmployeeProfile(): void {
    this.employeeProfileService.getMonProfil()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res: EmployeProfilResponse) => {
          if (res.success && res.data) {
            this.profileData = res.data;
            this.profil = res.data;

            this.profileForm.patchValue({
              nom: res.data.nom || '',
              prenom: res.data.prenom || '',
              email: res.data.email || '',
              telephone: res.data.telephone || '',
              poste: res.data.poste || '',
              departement: res.data.departement || '',
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
        error: err => {
          console.error('Erreur chargement profil employé:', err);

          this.snackBar.open('Erreur de chargement du profil', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  // =========================================================
  // EDIT PROFILE
  // =========================================================

  toggleEditMode(): void {
    this.editMode = !this.editMode;

    if (!this.editMode && this.profileData) {
      this.resetProfileFormFromData();
    }
  }

  private resetProfileFormFromData(): void {
    if (!this.profileData) return;

    this.profileForm.patchValue({
      nom: this.profileData.nom || '',
      prenom: this.profileData.prenom || '',
      email: this.profileData.email || '',
      telephone: this.profileData.telephone || '',
      poste: this.profileData.poste || '',
      departement: this.profileData.departement || '',
      adresse: this.profileData.adresse || ''
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();

      this.snackBar.open('Veuillez corriger les erreurs', 'Fermer', {
        duration: 4000
      });

      return;
    }

    if (this.isAdminMode()) {
      this.saveAdminProfile();
      return;
    }

    if (this.isManagerMode()) {
      this.saveManagerProfile();
      return;
    }

    this.saveEmployeeProfile();
  }

  private saveAdminProfile(): void {
    this.submitting = true;

    const formValue = this.profileForm.value;

    const updateData: AdminUpdateProfileData = {
      nom: formValue.nom,
      prenom: formValue.prenom,
      email: formValue.email,
      telephone: formValue.telephone || '',
      poste: formValue.poste || '',
      departement: formValue.departement || ''
    };

    this.adminProfileService.updateProfile(updateData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: res => {
          if (res.success) {
            this.profileData = res.data;

            const normalizedPhotoUrl = this.normalizePhotoUrl(res.data?.photoUrl);
            this.currentPhotoUrl = normalizedPhotoUrl;
            this.pictureService.setPicture(normalizedPhotoUrl);

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
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private saveManagerProfile(): void {
    this.submitting = true;

    const formValue = this.profileForm.value;

    const updateData: ManagerUpdateProfileData = {
      nom: formValue.nom,
      prenom: formValue.prenom,
      email: formValue.email,
      telephone: formValue.telephone || '',
      poste: formValue.poste || '',
      departement: formValue.departement || ''
    };

    this.managerProfileService.updateProfile(updateData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: res => {
          if (res.success) {
            this.profileData = res.data;
            this.editMode = false;

            const normalizedPhotoUrl = this.normalizePhotoUrl(res.data?.photoUrl || null);
            this.currentPhotoUrl = normalizedPhotoUrl;
            this.pictureService.setPicture(res.data?.photoUrl || null);

            this.snackBar.open('Profil mis à jour avec succès', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur lors de la mise à jour', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur lors de la mise à jour', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private saveEmployeeProfile(): void {
    this.submitting = true;

    const formValue = this.profileForm.value;

    const updateData: UpdateProfilRequest = {
      telephone: formValue.telephone || '',
      adresse: formValue.adresse || ''
    };

    this.employeeProfileService.updateMonProfil(updateData)
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
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  // =========================================================
  // PASSWORD
  // =========================================================

  togglePasswordEdit(): void {
    this.passwordEditMode = !this.passwordEditMode;

    if (!this.passwordEditMode) {
      this.passwordForm.reset();
      this.hideOld = true;
      this.hideNew = true;
      this.hideConfirm = true;
      this.passwordError = '';
      this.passwordSuccess = '';
    }
  }

  changePassword(): void {
    this.passwordError = '';
    this.passwordSuccess = '';

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();

      if (this.passwordForm.hasError('mismatch')) {
        this.passwordError = 'Les mots de passe ne correspondent pas.';
        this.snackBar.open('Les mots de passe ne correspondent pas', 'Fermer', {
          duration: 4000
        });
      } else {
        this.passwordError = 'Veuillez remplir correctement tous les champs.';
        this.snackBar.open('Veuillez remplir tous les champs', 'Fermer', {
          duration: 4000
        });
      }

      return;
    }

    if (this.isManagerMode()) {
      this.changeManagerPassword();
      return;
    }

    if (this.isAdminMode()) {
      this.changeAdminPassword();
      return;
    }

    this.changeEmployeePassword();
  }

  private changeAdminPassword(): void {
    this.submitting = true;

    const formValue = this.passwordForm.value;

    const passwordData: AdminChangePasswordData = {
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword,
      confirmPassword: formValue.confirmPassword
    };

    this.adminProfileService.changePassword(passwordData)
      .pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: async res => {
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
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private changeManagerPassword(): void {
    const formValue = this.passwordForm.value;

    const passwordData: ManagerChangePasswordData = {
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword,
      confirmPassword: formValue.confirmPassword
    };

    this.loadingPassword = true;

    this.managerProfileService.changePassword(passwordData)
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
        error: error => {
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

  private changeEmployeePassword(): void {
    this.submitting = true;

    const formValue = this.passwordForm.value;

    const passwordData: EmployeeChangePasswordData = {
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword,
      confirmPassword: formValue.confirmPassword
    };

    this.employeeProfileService.changePassword(passwordData)
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
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  // =========================================================
  // PHOTO
  // =========================================================

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

    if (this.isAdminMode()) {
      this.uploadAdminPhoto();
      return;
    }

    if (this.isManagerMode()) {
      this.uploadManagerPhoto();
      return;
    }

    this.uploadEmployeePhoto();
  }

  private uploadAdminPhoto(): void {
    if (!this.selectedFile) return;

    this.uploadProgress = true;

    this.adminProfileService.uploadPhoto(this.selectedFile).subscribe({
      next: res => {
        if (res.success && res.data?.photoUrl) {
          const normalizedPhotoUrl = this.normalizePhotoUrl(res.data.photoUrl);

          if (this.profileData) {
            this.profileData.photoUrl = res.data.photoUrl;
          }

          this.currentPhotoUrl = normalizedPhotoUrl;
          this.pictureService.setPicture(normalizedPhotoUrl);

          this.selectedFile = null;
          this.photoPreview = null;

          this.snackBar.open('Photo mise à jour', 'Fermer', {
            duration: 3000
          });
        } else {
          this.snackBar.open(res.message || 'Erreur', 'Fermer', {
            duration: 5000
          });
        }

        this.uploadProgress = false;
      },
      error: () => {
        this.snackBar.open("Erreur lors de l'upload", 'Fermer', {
          duration: 5000
        });

        this.uploadProgress = false;
      }
    });
  }

  private uploadManagerPhoto(): void {
    if (!this.selectedFile) return;

    this.uploadProgress = true;

    this.managerProfileService.uploadPhoto(this.selectedFile)
      .pipe(finalize(() => this.uploadProgress = false))
      .subscribe({
        next: res => {
          if (res.success && res.data?.photoUrl) {
            if (this.profileData) {
              this.profileData.photoUrl = res.data.photoUrl;
            }

            const displayUrl = this.normalizePhotoUrl(res.data.photoUrl);
            this.currentPhotoUrl = displayUrl;
            this.pictureService.setPicture(res.data.photoUrl);

            this.selectedFile = null;
            this.photoPreview = null;

            this.snackBar.open('Photo mise à jour', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur upload photo', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur upload photo', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  private uploadEmployeePhoto(): void {
    if (!this.selectedFile) return;

    this.uploadProgress = true;

    this.employeeProfileService.uploadPhoto(this.selectedFile)
      .pipe(finalize(() => this.uploadProgress = false))
      .subscribe({
        next: res => {
          if (res.success && res.data?.photoUrl) {
            if (this.profileData) {
              this.profileData.photoUrl = res.data.photoUrl;
            }

            const normalizedUrl = this.normalizePhotoUrl(res.data.photoUrl);
            this.currentPhotoUrl = normalizedUrl;
            this.pictureService.setPicture(normalizedUrl);

            this.selectedFile = null;
            this.photoPreview = null;

            this.snackBar.open('Photo mise à jour', 'Fermer', {
              duration: 3000
            });
          } else {
            this.snackBar.open(res.message || 'Erreur upload photo', 'Fermer', {
              duration: 5000
            });
          }
        },
        error: err => {
          this.snackBar.open(err.error?.message || 'Erreur upload photo', 'Fermer', {
            duration: 5000
          });
        }
      });
  }

  deletePhoto(): void {
    if (this.isAdminMode()) {
      this.deleteAdminPhoto();
      return;
    }

    if (this.isManagerMode()) {
      this.deleteManagerPhoto();
      return;
    }

    this.deleteEmployeePhoto();
  }

  private deleteAdminPhoto(): void {
    this.adminProfileService.deletePhoto().subscribe({
      next: () => {
        if (this.profileData) {
          this.profileData.photoUrl = undefined;
        }

        this.currentPhotoUrl = null;
        this.photoPreview = null;
        this.selectedFile = null;

        this.pictureService.setPicture(null);

        this.snackBar.open('Photo supprimée', 'Fermer', {
          duration: 3000
        });
      },
      error: () => {
        this.snackBar.open('Erreur suppression', 'Fermer', {
          duration: 5000
        });
      }
    });
  }

  private deleteManagerPhoto(): void {
    this.managerProfileService.deletePhoto().subscribe({
      next: () => {
        if (this.profileData) {
          this.profileData.photoUrl = undefined;
        }

        this.currentPhotoUrl = null;
        this.photoPreview = null;
        this.selectedFile = null;

        this.pictureService.setPicture(null);

        this.snackBar.open('Photo supprimée', 'Fermer', {
          duration: 3000
        });
      },
      error: err => {
        this.snackBar.open(err.error?.message || 'Erreur suppression photo', 'Fermer', {
          duration: 5000
        });
      }
    });
  }

  private deleteEmployeePhoto(): void {
    this.employeeProfileService.deletePhoto().subscribe({
      next: () => {
        if (this.profileData) {
          this.profileData.photoUrl = null;
        }

        this.currentPhotoUrl = null;
        this.photoPreview = null;
        this.selectedFile = null;

        this.pictureService.setPicture(null);

        this.snackBar.open('Photo supprimée', 'Fermer', {
          duration: 3000
        });
      },
      error: err => {
        this.snackBar.open(err.error?.message || 'Erreur suppression photo', 'Fermer', {
          duration: 5000
        });
      }
    });
  }

  // =========================================================
  // DISPLAY
  // =========================================================

  getPhotoUrl(photoUrl?: string | null): string {
    return this.normalizePhotoUrl(photoUrl) || '';
  }

  onImageError(): void {
    console.warn('Erreur chargement image :', this.currentPhotoUrl);
    this.currentPhotoUrl = null;

    if (!this.isAdminMode()) {
      this.pictureService.setPicture(null);
    }
  }

  private normalizePhotoUrl(photoUrl?: string | null): string | null {
    if (!photoUrl || String(photoUrl).trim() === '') {
      return null;
    }

    let url = String(photoUrl).trim();
    url = url.split('?')[0];

    if (url.startsWith('data:image')) {
      return url;
    }

    if (url.startsWith('http://') || url.startsWith('https://')) {
      return `${url}?t=${Date.now()}`;
    }

    if (url.startsWith('/api/')) {
      const baseUrl = environment.apiUrl.replace(/\/api$/, '');
      return `${baseUrl}${url}?t=${Date.now()}`;
    }

    if (url.startsWith('api/')) {
      const baseUrl = environment.apiUrl.replace(/\/api$/, '');
      return `${baseUrl}/${url}?t=${Date.now()}`;
    }

    if (url.startsWith('/photos/')) {
      return `${environment.apiUrl}${url}?t=${Date.now()}`;
    }

    if (url.startsWith('/uploads/')) {
      return `${environment.apiUrl}${url}?t=${Date.now()}`;
    }

    if (url.startsWith('uploads/')) {
      return `${environment.apiUrl}/${url}?t=${Date.now()}`;
    }

    if (this.isAdminMode()) {
      return `${environment.apiUrl}/photos/${url}?t=${Date.now()}`;
    }

    return `/api/uploads/profile-photos/${url}?t=${Date.now()}`;
  }

  getUserInitials(): string {
    if (!this.profileData) {
      if (this.isAdminMode()) return 'AD';
      if (this.isManagerMode()) return 'MG';
      return 'EM';
    }

    const prenomInitial = this.profileData.prenom
      ? String(this.profileData.prenom).charAt(0)
      : '';

    const nomInitial = this.profileData.nom
      ? String(this.profileData.nom).charAt(0)
      : '';

    const fallback =
      this.isAdminMode() ? 'AD' :
      this.isManagerMode() ? 'MG' :
      'EM';

    return `${prenomInitial}${nomInitial}`.toUpperCase() || fallback;
  }

  getFullName(): string {
    if (!this.profileData) return '';

    if (this.isManagerMode() && this.profileData.nomComplet) {
      return this.profileData.nomComplet;
    }

    return `${this.profileData.prenom || ''} ${this.profileData.nom || ''}`.trim();
  }

  getRoleLabel(): string {
    if (this.isAdminMode()) return this.profileData?.role || 'ADMIN_RH';
    if (this.isManagerMode()) return 'Manager';

    return this.profileData?.role ||
      this.profileData?.typeUtilisateur ||
      'Employé';
  }

  getPosteLabel(): string {
    if (!this.profileData) return '';

    if (this.isAdminMode()) {
      return this.profileData.poste || 'Administrateur RH';
    }

    if (this.isManagerMode()) {
      return this.profileData.poste || 'Manager';
    }

    return this.profileData.poste || 'Employé';
  }

  get statutClass(): string {
    if (!this.profileData) {
      return '';
    }

    const statutCompte = String(this.profileData.statutCompte || '');
    const statut = String(this.profileData.statut || '');

    if (statutCompte.includes('Verrouillé')) {
      return 'status-locked';
    }

    if (statutCompte.includes('Inactif') || statut === 'INACTIF') {
      return 'status-inactive';
    }

    return 'status-active';
  }

  getStatusText(): string {
    if (!this.profileData) return 'Actif';

    return this.profileData.statutCompte ||
      (this.profileData.statut === 'INACTIF' ? 'Inactif' : 'Actif');
  }

  getDateEmbauche(): any {
    return this.profileData?.dateEmbauche || null;
  }

  getDateNomination(): any {
    return this.profileData?.dateNomination || null;
  }

  getAncienneteLabel(): string {
    if (!this.profileData) return 'Non renseignée';

    if (this.isManagerMode()) {
      return this.profileData.ancienneteManagerLabel ||
        `${this.profileData.ancienneteManager || 0} an(s)`;
    }

    if (this.profileData.anciennete !== undefined && this.profileData.anciennete !== null) {
      return `${this.profileData.anciennete} an(s)`;
    }

    return 'Non renseignée';
  }
}