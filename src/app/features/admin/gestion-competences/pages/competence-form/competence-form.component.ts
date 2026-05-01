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
import { CompetenceService } from '../../../../../core/services/competence.service';
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

  // 🔥 NORMALISATION CATEGORIE
  normalizeCategorie(categorie: string): string {
    if (!categorie) return '';

    const cat = categorie.toUpperCase().replace(' ', '_');

    if (cat.includes('SOFT')) return 'SOFT_SKILL';
    if (cat.includes('TECH')) return 'TECHNIQUE';
    if (cat.includes('MANAGE')) return 'MANAGEMENT';
    if (cat.includes('LANG')) return 'LINGUISTIQUE';

    return cat;
  }

  loadCompetence(): void {
    if (!this.competenceId) return;

    this.loading = true;

    this.competenceService.getById(this.competenceId).subscribe({
      next: (response: any) => {
        const competence = response.data as Competence;

        console.log('DATA BACKEND:', competence); // debug

        this.competenceForm.patchValue({
          nom: competence.nom,
          description: competence.description,
          categorie: this.normalizeCategorie(competence.categorie)
        });

        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur chargement compétence:', error);
        this.snackBar.open('Erreur chargement', 'Fermer', { duration: 3000 });
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

    const request = this.isEditMode
      ? this.competenceService.update(this.competenceId!, this.competenceForm.value)
      : this.competenceService.create(this.competenceForm.value);

    request.subscribe({
      next: () => {
        this.submitting = false;
        this.snackBar.open('Succès', 'Fermer', { duration: 3000 });
        this.router.navigate(['/admin/competences']);
      },
      error: (error: any) => {
        this.submitting = false;
        console.error(error);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/admin/competences']);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
    });
  }

getNomErrorMessage(): string {
  if (this.nom?.hasError('required')) {
    return 'Le nom est requis';
  }
  if (this.nom?.hasError('minlength')) {
    return 'Minimum 2 caractères';
  }
  if (this.nom?.hasError('maxlength')) {
    return 'Maximum 100 caractères';
  }
  return '';
}



getDescriptionErrorMessage(): string {
  if (this.description?.hasError('required')) {
    return 'La description est requise';
  }
  if (this.description?.hasError('minlength')) {
    return 'Minimum 10 caractères';
  }
  if (this.description?.hasError('maxlength')) {
    return 'Maximum 500 caractères';
  }
  return '';
}
  
  getCategorieLabel(value: string): string {
    const cat = this.categories.find(c => c.value === value);
    return cat ? cat.label : value;
  }

  get nom() {
  return this.competenceForm.get('nom');
}

get description() {
  return this.competenceForm.get('description');
}

get categorie() {
  return this.competenceForm.get('categorie');
}
goBack(): void {
  this.router.navigate(['/admin/competences']);
}
}