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
import { MatDividerModule } from '@angular/material/divider';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation.model';

@Component({
  selector: 'app-formation-form',
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
    MatDividerModule
  ],
  templateUrl: './formation-form.component.html',
  styleUrls: ['./formation-form.component.css']
})
export class FormationFormComponent implements OnInit {
getDomaineLabel(arg0: any) {
throw new Error('Method not implemented.');
}
  formationForm: FormGroup;
  isEditMode = false;
  formationId: number | null = null;
  loading = false;
  submitting = false;

  domaines = [
    { value: 'TECHNIQUE', label: 'Technique', icon: 'code' },
    { value: 'SOFT_SKILLS', label: 'Soft Skills', icon: 'people' },
    { value: 'MANAGEMENT', label: 'Management', icon: 'business' },
    { value: 'LANGUES', label: 'Langues', icon: 'language' },
    { value: 'SECURITE', label: 'Sécurité', icon: 'security' }
  ];

  constructor(
    private fb: FormBuilder,
    private formationService: FormationService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.formationForm = this.fb.group({
      titre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      domaine: ['', Validators.required],
      dureeHeures: ['', [Validators.required, Validators.min(1), Validators.max(500)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
      actif: [true]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.formationId = +params['id'];
        this.loadFormation();
      }
    });
  }

  loadFormation(): void {
    if (!this.formationId) return;
    
    this.loading = true;
    this.formationService.getById(this.formationId).subscribe({
      next: (response) => {
        const formation = response.data as Formation;
        this.formationForm.patchValue({
          titre: formation.titre,
          domaine: formation.domaine,
          dureeHeures: formation.dureeHeures,
          description: formation.description,
          actif: formation.actif
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement formation:', error);
        this.snackBar.open('Erreur lors du chargement de la formation', 'Fermer', { duration: 3000 });
        this.router.navigate(['/admin/formations']);
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.formationForm.invalid) {
      this.markFormGroupTouched(this.formationForm);
      return;
    }

    this.submitting = true;
    const formationData: Formation = this.formationForm.value;

    const request = this.isEditMode && this.formationId
      ? this.formationService.update(this.formationId, formationData)
      : this.formationService.create(formationData);

    request.subscribe({
      next: (response) => {
        this.submitting = false;
        this.snackBar.open(
          this.isEditMode ? 'Formation modifiée avec succès' : 'Formation créée avec succès',
          'Fermer',
          { duration: 3000 }
        );
        this.router.navigate(['/admin/formations']);
      },
      error: (error) => {
        this.submitting = false;
        console.error('Erreur sauvegarde:', error);
        this.snackBar.open(
          error.error?.message || 'Erreur lors de la sauvegarde',
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

  // Getters pour les erreurs
  get titreErrors(): string {
    const control = this.formationForm.get('titre');
    if (control?.hasError('required')) return 'Le titre est obligatoire';
    if (control?.hasError('minlength')) return 'Le titre doit contenir au moins 3 caractères';
    if (control?.hasError('maxlength')) return 'Le titre ne peut pas dépasser 150 caractères';
    return '';
  }

  get domaineErrors(): string {
    const control = this.formationForm.get('domaine');
    if (control?.hasError('required')) return 'Le domaine est obligatoire';
    return '';
  }

  get dureeErrors(): string {
    const control = this.formationForm.get('dureeHeures');
    if (control?.hasError('required')) return 'La durée est obligatoire';
    if (control?.hasError('min')) return 'La durée doit être d\'au moins 1 heure';
    if (control?.hasError('max')) return 'La durée ne peut pas dépasser 500 heures';
    return '';
  }

  get descriptionErrors(): string {
    const control = this.formationForm.get('description');
    if (control?.hasError('required')) return 'La description est obligatoire';
    if (control?.hasError('minlength')) return 'La description doit contenir au moins 10 caractères';
    if (control?.hasError('maxlength')) return 'La description ne peut pas dépasser 2000 caractères';
    return '';
  }

  cancel(): void {
    this.router.navigate(['/admin/formations']);
  }
}