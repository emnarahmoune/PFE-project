// src/app/features/employee/pages/modifier-conge/modifier-conge.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
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
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';
import { NotificationService } from '../../../../../core/services/notification.service';

@Component({
  selector: 'app-modifier-conge',
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
    MatProgressBarModule
  ],
  templateUrl: './modifier-conge.component.html',
  styleUrls: ['./modifier-conge.component.css']
})
export class ModifierCongeComponent implements OnInit {
  demandeForm: FormGroup;
  loading = false;
  submitting = false;
  demandeId!: number;

  // Données de la demande originale
  demandeOriginale?: DemandeConge;

  // Solde (pour l'affichage)
  soldeConges = 0;
  congesPris = 0;
  congesRestants = 0;

  typesConge = [
    { value: 'ANNUEL', label: 'Congé annuel', icon: 'beach_access', color: '#1976d2' },
    { value: 'MALADIE', label: 'Congé maladie', icon: 'local_hospital', color: '#dc3545' },
    { value: 'SANS_SOLDE', label: 'Congé sans solde', icon: 'attach_money', color: '#ffc107' },
    { value: 'MATERNITE', label: 'Congé maternité', icon: 'child_care', color: '#28a745' },
    { value: 'PATERNITE', label: 'Congé paternité', icon: 'child_friendly', color: '#28a745' }
  ];

  constructor(
    private fb: FormBuilder,
    private congeService: EmployeeCongeService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService
  ) {
    this.demandeForm = this.fb.group({
      type: ['', Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      commentaire: ['']
    }, { validators: this.dateRangeValidator });
  }

  ngOnInit(): void {
    this.demandeId = +this.route.snapshot.params['id'];
    if (!this.demandeId) {
      this.snackBar.open('Demande introuvable', 'Fermer', { duration: 3000 });
      this.router.navigate(['/employee/mes-conges']);
      return;
    }
    this.loadDemande();
    this.loadSolde(); // pour afficher le solde restant (optionnel)
  }

  loadDemande(): void {
    this.loading = true;
    this.congeService.getCongeById(this.demandeId).subscribe({
      next: (response: CongeResponse) => {
        this.loading = false;
        if (response.success && response.data) {
          this.demandeOriginale = response.data as DemandeConge;
          // Vérifier que la demande est modifiable
          if (this.demandeOriginale.statut !== 'EN_ATTENTE') {
            this.snackBar.open('Seules les demandes en attente peuvent être modifiées', 'Fermer', { duration: 4000 });
            this.router.navigate(['/employee/mes-conges']);
            return;
          }
          this.demandeForm.patchValue({
            type: this.demandeOriginale.type,
            dateDebut: this.demandeOriginale.dateDebut,
            dateFin: this.demandeOriginale.dateFin,
            commentaire: this.demandeOriginale.commentaire || ''
          });
        } else {
          this.snackBar.open('Impossible de charger la demande', 'Fermer', { duration: 3000 });
          this.router.navigate(['/employee/mes-conges']);
        }
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        this.snackBar.open('Erreur de chargement', 'Fermer', { duration: 3000 });
        this.router.navigate(['/employee/mes-conges']);
      }
    });
  }

  loadSolde(): void {
    this.congeService.getMonSoldeConges().subscribe({
      next: (response: any) => {
        if (response.success) {
          const data = response.data;
          this.soldeConges = data.total || 0;
          this.congesPris = data.pris || 0;
          this.congesRestants = data.restant || 0;
        }
      },
      error: (err) => console.error('Erreur solde', err)
    });
  }

  dateRangeValidator(form: FormGroup): { [key: string]: boolean } | null {
    const debut = form.get('dateDebut')?.value;
    const fin = form.get('dateFin')?.value;
    if (debut && fin && new Date(debut) > new Date(fin)) {
      return { dateInvalide: true };
    }
    if (debut && new Date(debut) < new Date()) {
      return { datePassee: true };
    }
    return null;
  }

  calculerNombreJours(): number {
    const debut = this.demandeForm.get('dateDebut')?.value;
    const fin = this.demandeForm.get('dateFin')?.value;
    if (!debut || !fin) return 0;
    const diffTime = Math.abs(new Date(fin).getTime() - new Date(debut).getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  }

  verifierSolde(): boolean {
    const type = this.demandeForm.get('type')?.value;
    if (type !== 'ANNUEL') return true;
    const joursDemandes = this.calculerNombreJours();
    return joursDemandes <= this.congesRestants;
  }

  getTypeLabel(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.label || type;
  }

  getTypeIcon(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.icon || 'event';
  }

  getTypeColor(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.color || '#6c757d';
  }

  formatDate(date: Date | string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  onSubmit(): void {
    if (this.demandeForm.invalid) {
      this.markFormGroupTouched(this.demandeForm);
      this.snackBar.open('Veuillez corriger les erreurs du formulaire', 'Fermer', { duration: 3000 });
      return;
    }

    if (!this.verifierSolde()) {
      this.snackBar.open(
        `❌ Solde insuffisant. Il vous reste ${this.congesRestants} jours.`,
        'Fermer',
        { duration: 5000, panelClass: ['error-snackbar'] }
      );
      return;
    }

    this.submitting = true;
    const updatedDemande: DemandeConge = {
      ...this.demandeOriginale,
      dateDebut: this.demandeForm.value.dateDebut,
      dateFin: this.demandeForm.value.dateFin,
      type: this.demandeForm.value.type,
      commentaire: this.demandeForm.value.commentaire || ''
    };

    this.congeService.modifierDemande(this.demandeId, updatedDemande).subscribe({
      next: (response: CongeResponse) => {
        this.submitting = false;
        if (response.success) {
          this.snackBar.open('✅ Demande modifiée avec succès', 'Fermer', { duration: 3000 });
          this.notificationService.loadNotifications();
          this.notificationService.loadUnreadCount();
          this.router.navigate(['/employee/mes-conges']);
        } else {
          const msg = response.message || 'Erreur lors de la modification';
          this.snackBar.open(msg, 'Fermer', { duration: 5000 });
        }
      },
      error: (error) => {
        this.submitting = false;
        console.error(error);
        const msg = error.error?.message || 'Erreur technique';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) this.markFormGroupTouched(control);
    });
  }

  annuler(): void {
    this.router.navigate(['/employee/mes-conges']);
  }
}