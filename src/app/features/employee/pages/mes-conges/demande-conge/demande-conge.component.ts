// src/app/features/employee/pages/demande-conge/demande-conge.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { CongeResponse } from '../../../models/conge.model';
import { NotificationService } from '../../../../../core/services/notification.service';

type TypeCongeValue = 'ANNUEL' | 'MALADIE' | 'SANS_SOLDE' | 'MATERNITE' | 'PATERNITE';

interface TypeCongeOption {
  value: TypeCongeValue;
  label: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-demande-conge',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressBarModule,
    MatCheckboxModule
  ],
  templateUrl: './demande-conge.component.html',
  styleUrls: ['./demande-conge.component.css']
})
export class DemandeCongeComponent implements OnInit {
  demandeForm: FormGroup;

  loading = false;
  submitting = false;

  soldeConges = 25;
  congesPris = 0;
  congesRestants = 25;

  errorMessage = '';

  typesConge: TypeCongeOption[] = [
    {
      value: 'ANNUEL',
      label: 'Congé annuel',
      icon: 'beach_access',
      color: '#1976d2'
    },
    {
      value: 'MALADIE',
      label: 'Congé maladie',
      icon: 'local_hospital',
      color: '#dc3545'
    },
    {
      value: 'SANS_SOLDE',
      label: 'Congé sans solde',
      icon: 'attach_money',
      color: '#ffc107'
    },
    {
      value: 'MATERNITE',
      label: 'Congé maternité',
      icon: 'child_care',
      color: '#28a745'
    },
    {
      value: 'PATERNITE',
      label: 'Congé paternité',
      icon: 'child_friendly',
      color: '#28a745'
    }
  ];

  constructor(
    private fb: FormBuilder,
    private congeService: EmployeeCongeService,
    private router: Router,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService
  ) {
    this.demandeForm = this.fb.group(
      {
        type: ['', Validators.required],
        dateDebut: ['', Validators.required],
        dateFin: ['', Validators.required],
        commentaire: [''],
        urgente: [false]
      },
      {
        validators: this.dateRangeValidator
      }
    );
  }

  ngOnInit(): void {
    this.loadSoldeConges();
  }

  loadSoldeConges(): void {
    this.congeService.getMonSoldeConges().subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          const data = response.data as any;

          this.soldeConges = data.total ?? 25;
          this.congesPris = data.pris ?? 0;
          this.congesRestants = data.restant ?? (this.soldeConges - this.congesPris);
        }
      },
      error: (error: any) => {
        console.error('Erreur chargement solde:', error);

        this.soldeConges = 25;
        this.congesPris = 0;
        this.congesRestants = 25;
      }
    });
  }

  dateRangeValidator(form: FormGroup): { [key: string]: boolean } | null {
    const debut = form.get('dateDebut')?.value;
    const fin = form.get('dateFin')?.value;

    if (!debut || !fin) {
      return null;
    }

    const debutDate = new Date(debut);
    const finDate = new Date(fin);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const normalizedDebut = new Date(debutDate);
    normalizedDebut.setHours(0, 0, 0, 0);

    if (debutDate > finDate) {
      return { dateInvalide: true };
    }

    if (normalizedDebut < today) {
      return { datePassee: true };
    }

    return null;
  }

  calculerNombreJours(): number {
    const debut = this.demandeForm.get('dateDebut')?.value;
    const fin = this.demandeForm.get('dateFin')?.value;

    if (!debut || !fin) {
      return 0;
    }

    const debutDate = new Date(debut);
    const finDate = new Date(fin);

    if (debutDate > finDate) {
      return 0;
    }

    const diffTime = Math.abs(finDate.getTime() - debutDate.getTime());

    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  }

  verifierSolde(): boolean {
    const type = this.demandeForm.get('type')?.value;

    if (type !== 'ANNUEL') {
      return true;
    }

    const joursDemandes = this.calculerNombreJours();

    return joursDemandes <= this.congesRestants;
  }

  getTypeLabel(type: string | null | undefined): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.label || '';
  }

  getTypeIcon(type: string | null | undefined): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.icon || 'event';
  }

  getTypeColor(type: string | null | undefined): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.color || '#6c757d';
  }

  formatDate(date: Date | string | null | undefined): string {
    if (!date) {
      return '';
    }

    return new Date(date).toLocaleDateString('fr-FR');
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.demandeForm.invalid) {
      this.markFormGroupTouched(this.demandeForm);

      if (this.demandeForm.get('type')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner un type de congé', 'Fermer', {
          duration: 3000
        });
      } else if (this.demandeForm.get('dateDebut')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner une date de début', 'Fermer', {
          duration: 3000
        });
      } else if (this.demandeForm.get('dateFin')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner une date de fin', 'Fermer', {
          duration: 3000
        });
      } else if (this.demandeForm.hasError('dateInvalide')) {
        this.snackBar.open('La date de fin doit être postérieure à la date de début', 'Fermer', {
          duration: 3000
        });
      } else if (this.demandeForm.hasError('datePassee')) {
        this.snackBar.open('La date de début ne peut pas être dans le passé', 'Fermer', {
          duration: 3000
        });
      }

      return;
    }

    const urgente = Boolean(this.demandeForm.value.urgente);

    if (!this.verifierSolde() && !urgente) {
      this.snackBar.open(
        `❌ Solde insuffisant. Vous avez ${this.congesRestants} jours restants.`,
        'Fermer',
        {
          duration: 5000,
          panelClass: ['error-snackbar']
        }
      );

      return;
    }

    this.submitting = true;

    const demande = {
      dateDebut: this.demandeForm.value.dateDebut,
      dateFin: this.demandeForm.value.dateFin,
      type: this.demandeForm.value.type,
      commentaire: this.demandeForm.value.commentaire || '',
      urgente
    };

    this.congeService.soumettreDemande(demande).subscribe({
      next: (response: any) => {
        this.submitting = false;

        if (response.success || response.statusCode === 200 || response.statusCode === 201) {
          this.snackBar.open('✅ Demande de congé soumise avec succès', 'Fermer', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });

          this.notificationService.loadNotifications();
          this.notificationService.loadUnreadCount();

          setTimeout(() => {
            this.router.navigate(['/employee/mes-conges']);
          }, 1500);

          return;
        }

        const errorMsg = response.message || response.error || 'Erreur lors de la soumission';

        this.errorMessage = errorMsg;

        this.snackBar.open(errorMsg, 'Fermer', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });

        this.notificationService.loadNotifications();
        this.notificationService.loadUnreadCount();
      },
      error: (error: any) => {
        this.submitting = false;

        let errorMessage = 'Erreur lors de la soumission de la demande';

        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.error?.error) {
          errorMessage = error.error.error;
        } else if (error.message) {
          errorMessage = error.message;
        }

        this.errorMessage = errorMessage;

        this.snackBar.open(errorMessage, 'Fermer', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });

        this.notificationService.loadNotifications();
        this.notificationService.loadUnreadCount();
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  annuler(): void {
    this.router.navigate(['/employee/mes-conges']);
  }
}