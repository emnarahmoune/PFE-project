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
    MatProgressBarModule  // ✅ AJOUT DE MatProgressBarModule
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

  // ✅ AJOUT DE LA MÉTHODE getTypeLabel
  getTypeLabel(type: string): string {
    const found = this.typesConge.find(t => t.value === type);
    return found?.label || type;
  }

  // ✅ AJOUT DE LA MÉTHODE formatDate
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
    if (this.demandeForm.invalid) {
      this.markFormGroupTouched(this.demandeForm);
      return;
    }

    if (!this.verifierSolde()) {
      this.snackBar.open(
        `Solde insuffisant. Vous avez ${this.congesRestants} jours restants.`,
        'Fermer',
        { duration: 5000, panelClass: ['error-snackbar'] }
      );
      return;
    }

    this.submitting = true;
    const demande: DemandeConge = {
      type: this.demandeForm.value.type,
      dateDebut: this.demandeForm.value.dateDebut,
      dateFin: this.demandeForm.value.dateFin,
      commentaire: this.demandeForm.value.commentaire,
      statut: 'EN_ATTENTE'
    };

    this.congeService.soumettreDemande(demande).subscribe({
      next: (response: CongeResponse) => {
        this.submitting = false;
        if (response.success) {
          this.snackBar.open('Demande de congé soumise avec succès', 'Fermer', { duration: 3000 });
          this.router.navigate(['/employee/mes-conges']);
        } else {
          this.snackBar.open(response.message || 'Erreur lors de la soumission', 'Fermer', { duration: 5000 });
        }
      },
      error: (error: any) => {
        this.submitting = false;
        console.error('Erreur soumission:', error);
        this.snackBar.open(
          error.error?.message || 'Erreur lors de la soumission de la demande',
          'Fermer',
          { duration: 5000 }
        );
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