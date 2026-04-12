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
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';

@Component({
  selector: 'app-demande-conge',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatNativeDateModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDividerModule,
    MatChipsModule, MatProgressBarModule
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
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.demandeForm = this.fb.group({
      type: ['', Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      commentaire: ['']
    }, { validators: this.dateRangeValidator });
  }

  ngOnInit(): void {
    this.loadSoldeConges();
  }

  loadSoldeConges(): void {
    this.congeService.getMonSoldeConges().subscribe({
      next: (response: CongeResponse) => {
        if (response.success) {
          const data = response.data as any;
          this.soldeConges = data.total || 25;
          this.congesPris = data.pris || 0;
          this.congesRestants = data.restant || 25;
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
    
    const debutDate = new Date(debut);
    const finDate = new Date(fin);
    const diffTime = Math.abs(finDate.getTime() - debutDate.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    
    return diffDays;
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

  formatDate(date: Date): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  getTypeIcon(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.icon || 'event';
  }

  getTypeColor(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.color || '#6c757d';
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (this.demandeForm.invalid) {
      this.markFormGroupTouched(this.demandeForm);
      
      if (this.demandeForm.get('type')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner un type de congé', 'Fermer', { duration: 3000 });
      } else if (this.demandeForm.get('dateDebut')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner une date de début', 'Fermer', { duration: 3000 });
      } else if (this.demandeForm.get('dateFin')?.hasError('required')) {
        this.snackBar.open('Veuillez sélectionner une date de fin', 'Fermer', { duration: 3000 });
      } else if (this.demandeForm.hasError('dateInvalide')) {
        this.snackBar.open('La date de fin doit être postérieure à la date de début', 'Fermer', { duration: 3000 });
      } else if (this.demandeForm.hasError('datePassee')) {
        this.snackBar.open('La date de début ne peut pas être dans le passé', 'Fermer', { duration: 3000 });
      }
      return;
    }

    if (!this.verifierSolde()) {
      this.snackBar.open(
        `❌ Solde insuffisant. Vous avez ${this.congesRestants} jours restants.`,
        'Fermer',
        { duration: 5000, panelClass: ['error-snackbar'] }
      );
      return;
    }

    this.submitting = true;
    
    const demande = {
      dateDebut: this.demandeForm.value.dateDebut,
      dateFin: this.demandeForm.value.dateFin,
      type: this.demandeForm.value.type,
      commentaire: this.demandeForm.value.commentaire || ''
    };

    console.log('📤 Envoi de la demande:', demande);

    this.congeService.soumettreDemande(demande).subscribe({
      next: (response: any) => {
        this.submitting = false;
        console.log('📥 Réponse du serveur:', response);
        
        if (response.success || response.statusCode === 200 || response.statusCode === 201) {
          const message = response.message || '✅ Demande de congé soumise avec succès';
          this.snackBar.open(message, 'Fermer', { duration: 3000, panelClass: ['success-snackbar'] });
          
          // ✅ Redirection après succès
          setTimeout(() => {
            this.router.navigate(['/employee/mes-conges']);
          }, 1500);
        } else {
          const errorMsg = response.message || response.error || 'Erreur lors de la soumission';
          this.errorMessage = errorMsg;
          this.snackBar.open(errorMsg, 'Fermer', { duration: 5000, panelClass: ['error-snackbar'] });
        }
      },
      error: (error: any) => {
        this.submitting = false;
        console.error('❌ Erreur complète:', error);
        
        let errorMessage = 'Erreur lors de la soumission de la demande';
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.error?.error) {
          errorMessage = error.error.error;
        } else if (error.message) {
          errorMessage = error.message;
        }
        
        this.errorMessage = errorMessage;
        this.snackBar.open(errorMessage, 'Fermer', { duration: 5000, panelClass: ['error-snackbar'] });
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup) {
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