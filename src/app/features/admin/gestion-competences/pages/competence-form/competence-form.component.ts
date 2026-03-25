import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { CompetenceService } from '../../services/competence.service';
import { Competence } from '../../models/competence.model';

@Component({
  selector: 'app-competence-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './competence-form.component.html',
  styleUrls: ['./competence-form.component.css']
})
export class CompetenceFormComponent implements OnInit {
  competenceForm: FormGroup;
  isEditMode = false;
  competenceId?: number;
  loading = false;
  submitting = false;

  categories = [
    { value: 'TECHNIQUE', label: 'Technique', icon: 'code' },
    { value: 'SOFT_SKILL', label: 'Soft Skill', icon: 'people' },
    { value: 'LINGUISTIQUE', label: 'Linguistique', icon: 'language' },
    { value: 'MANAGEMENT', label: 'Management', icon: 'business' }
  ];

  constructor(
    private fb: FormBuilder,
    private competenceService: CompetenceService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.competenceForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      categorie: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.competenceId = +id;
      this.loadCompetence();
    }
  }

  loadCompetence(): void {
    if (!this.competenceId) return;

    this.loading = true;
    this.competenceService.getById(this.competenceId).subscribe({
      next: (response) => {
        const competence = response.data as Competence;
        this.competenceForm.patchValue({
          nom: competence.nom,
          description: competence.description,
          categorie: competence.categorie
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement compétence:', error);
        this.snackBar.open('Erreur lors du chargement de la compétence', 'Fermer', { duration: 3000 });
        this.loading = false;
        this.router.navigate(['/admin/competences']);
      }
    });
  }

  onSubmit(): void {
    if (this.competenceForm.invalid) {
      this.markFormGroupTouched(this.competenceForm);
      return;
    }

    this.submitting = true;
    const competenceData: Competence = this.competenceForm.value;

    const request = this.isEditMode && this.competenceId
      ? this.competenceService.update(this.competenceId, competenceData)
      : this.competenceService.create(competenceData);

    request.subscribe({
      next: (response) => {
        this.submitting = false;
        this.snackBar.open(
          this.isEditMode ? 'Compétence modifiée avec succès' : 'Compétence créée avec succès',
          'Fermer',
          { duration: 3000 }
        );
        this.router.navigate(['/admin/competences']);
      },
      error: (error) => {
        this.submitting = false;
        console.error('Erreur sauvegarde:', error);
        
        let errorMessage = 'Erreur lors de la sauvegarde';
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.status === 409) {
          errorMessage = 'Une compétence avec ce nom existe déjà';
        }

        this.snackBar.open(errorMessage, 'Fermer', { duration: 5000 });
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/admin/competences']);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // Getters pour accéder facilement aux contrôles
  get nom() { return this.competenceForm.get('nom'); }
  get description() { return this.competenceForm.get('description'); }
  get categorie() { return this.competenceForm.get('categorie'); }
 // Méthodes utilitaires pour l'aperçu
getCategorieColor(categorie: string): string {
  switch(categorie) {
    case 'TECHNIQUE': return 'primary';
    case 'SOFT_SKILL': return 'accent';
    case 'LINGUISTIQUE': return 'warn';
    case 'MANAGEMENT': return 'info';
    default: return '';
  }
}

getCategorieLabel(categorie: string): string {
  const cat = this.categories.find(c => c.value === categorie);
  return cat ? cat.label : categorie;
}
  // Messages d'erreur personnalisés
  getNomErrorMessage(): string {
    if (this.nom?.hasError('required')) return 'Le nom est requis';
    if (this.nom?.hasError('minlength')) return 'Minimum 2 caractères';
    if (this.nom?.hasError('maxlength')) return 'Maximum 100 caractères';
    return '';
  }

  getDescriptionErrorMessage(): string {
    if (this.description?.hasError('required')) return 'La description est requise';
    if (this.description?.hasError('minlength')) return 'Minimum 10 caractères';
    if (this.description?.hasError('maxlength')) return 'Maximum 500 caractères';
    return '';
  }
}