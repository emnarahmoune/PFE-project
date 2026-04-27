// src/app/features/admin/pages/employe-form/employe-form.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder, FormGroup, Validators,
  ReactiveFormsModule, AbstractControl, ValidationErrors
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { EmployeService } from '../../services/employe.service';
import { Employe } from '../../models/employe.model';

function noWhitespaceValidator(c: AbstractControl): ValidationErrors | null {
  return c.value && c.value !== c.value.trim() ? { whitespace: true } : null;
}
function phoneValidator(c: AbstractControl): ValidationErrors | null {
  if (!c.value) return null;
  const clean = c.value.replace(/[\s\-().+]/g, '');
  return /^\d{7,15}$/.test(clean) ? null : { invalidPhone: true };
}

@Component({
  selector: 'app-employe-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, MatSnackBarModule],
  templateUrl: './employe-form.component.html',
  styleUrls: ['./employe-form.component.css']
})
export class EmployeFormComponent implements OnInit {
  employeForm!: FormGroup;
  isEditMode = false;
  employeId?: number;
  loading = false;
  submitting = false;

  departements = ['RH','Technique','Commercial','Finance','Marketing','Direction','Logistique'];
  postes = ['Développeur Full Stack','Développeur Backend','Développeur Frontend',
            'Chef de projet','Analyste','Commercial','Comptable','Responsable RH','Directeur','Manager'];
  rolesList = [
    { value: 'user', label: 'Employé' },
    { value: 'manager', label: 'Manager' },
    { value: 'admin_rh', label: 'Admin RH' }
  ];

  managersList: Employe[] = [];

  constructor(
    private fb: FormBuilder,
    private employeService: EmployeService,
    private route: ActivatedRoute,
    private router: Router,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.loadManagers();
    const idParam = this.route.snapshot.params['id'];
    this.employeId = idParam ? +idParam : undefined;
    this.isEditMode = !!this.employeId;
    if (this.isEditMode) this.loadEmploye();
  }

  private buildForm(): void {
    this.employeForm = this.fb.group({
      matricule:   ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20), noWhitespaceValidator]],
      nom:         ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50),
                         noWhitespaceValidator, Validators.pattern(/^[a-zA-ZÀ-ÿ\s\-']+$/)]],
      prenom:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50),
                         noWhitespaceValidator, Validators.pattern(/^[a-zA-ZÀ-ÿ\s\-']+$/)]],
      email:       ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      telephone:   ['', [phoneValidator]],
      dateEmbauche:['', [Validators.required]],
      poste:       ['', [Validators.required]],
      salaire:     [null, [Validators.required, Validators.min(0), Validators.max(1_000_000)]],
      departement: ['', [Validators.required]],
      statut:      ['ACTIF'],
      soldeConges: [25, [Validators.required, Validators.min(0), Validators.max(365)]],
      serviceId:   [null],
      managerId:   [null],
      role:        ['user', [Validators.required]]
    });
  }

  loadManagers(): void {
    this.employeService.getAllManagers().subscribe({
      next: (res) => {
        this.managersList = res.success && Array.isArray(res.data) ? res.data : [];
      },
      error: (err) => {
        console.error('Erreur chargement managers', err);
        this.managersList = [];
      }
    });
  }

  loadEmploye(): void {
    this.loading = true;
    this.employeService.getById(this.employeId!)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            const e: Employe = res.data as Employe;
            this.employeForm.patchValue({
              matricule:    e.matricule    ?? '',
              nom:          e.nom          ?? '',
              prenom:       e.prenom       ?? '',
              email:        e.email        ?? '',
              telephone:    e.telephone    ?? '',
              dateEmbauche: e.dateEmbauche ? e.dateEmbauche.split('T')[0] : '',
              poste:        e.poste        ?? '',
              salaire:      e.salaire      ?? null,
              departement:  e.departement  ?? '',
              statut:       e.statut       ?? 'ACTIF',
              soldeConges:  e.soldeConges  ?? 25,
              serviceId:    e.serviceId    ?? null,
              managerId:    e.managerId    ?? null,
              role:         e.role         ?? 'user'
            });
          } else {
            this.toast(res.message || 'Employé introuvable', 'error');
            this.router.navigate(['/admin/employes']);
          }
        },
        error: () => {
          this.toast('Erreur de connexion au serveur', 'error');
          this.router.navigate(['/admin/employes']);
        }
      });
  }

  onSubmit(): void {
    if (this.employeForm.invalid) {
      this.markAllTouched();
      this.toast('Veuillez corriger les erreurs avant de soumettre', 'warn');
      return;
    }

    this.submitting = true;
    const payload = this.toPayload();
    const obs$ = this.isEditMode
      ? this.employeService.update(this.employeId!, payload)
      : this.employeService.create(payload);

    obs$.pipe(finalize(() => this.submitting = false))
      .subscribe({
        next: (res) => {
          if (res.success) {
            this.toast(
              this.isEditMode ? 'Employé modifié avec succès' : 'Employé créé avec succès',
              'success'
            );
            this.isEditMode
              ? this.router.navigate(['/admin/employes', this.employeId])
              : this.router.navigate(['/admin/employes']);
          } else {
            this.toast(res.message || 'Une erreur est survenue', 'error');
          }
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error?.errors?.join(', ')
                   || (this.isEditMode ? 'Erreur modification' : 'Erreur création');
          this.toast(msg, 'error');
        }
      });
  }

  private toPayload(): Employe {
    const v = this.employeForm.value;
    return {
      matricule:    v.matricule.trim(),
      nom:          v.nom.trim(),
      prenom:       v.prenom.trim(),
      email:        v.email.trim().toLowerCase(),
      telephone:    v.telephone?.trim() || undefined,
      dateEmbauche: new Date(v.dateEmbauche).toISOString().split('T')[0],
      poste:        v.poste,
      salaire:      Number(v.salaire),
      statut:       v.statut || 'ACTIF',
      departement:  v.departement,
      soldeConges:  Number(v.soldeConges) || 25,
      role:         v.role,
      serviceId:    v.serviceId || undefined,
      managerId:    v.managerId || undefined
    };
  }

  setStatut(s: string): void {
    this.employeForm.get('statut')?.setValue(s);
  }

  isStepTwoVisible(): boolean {
    const v = this.employeForm.value;
    return !!(v.matricule?.trim() && v.nom?.trim() && v.prenom?.trim());
  }

  isStepThreeVisible(): boolean {
    const v = this.employeForm.value;
    return this.isStepTwoVisible() && !!(v.email && v.poste && v.departement);
  }

  getErr(field: string): string {
    const ctrl = this.employeForm.get(field);
    if (!ctrl?.errors || !ctrl.touched) return '';
    const e = ctrl.errors;
    if (e['required'])     return 'Champ obligatoire';
    if (e['email'])        return 'Email invalide';
    if (e['minlength'])    return `Min. ${e['minlength'].requiredLength} caractères`;
    if (e['maxlength'])    return `Max. ${e['maxlength'].requiredLength} caractères`;
    if (e['min'])          return `Min. ${e['min'].min}`;
    if (e['max'])          return `Max. ${e['max'].max}`;
    if (e['whitespace'])   return 'Pas d\'espaces en début ou fin';
    if (e['pattern'])      return 'Caractères non autorisés (lettres uniquement)';
    if (e['invalidPhone']) return 'Numéro de téléphone invalide';
    return 'Valeur incorrecte';
  }

  hasErr(field: string): boolean {
    const c = this.employeForm.get(field);
    return !!(c?.invalid && c.touched);
  }

  onCancel(): void {
    this.isEditMode
      ? this.router.navigate(['/admin/employes', this.employeId])
      : this.router.navigate(['/admin/employes']);
  }

  private markAllTouched(): void {
    Object.values(this.employeForm.controls).forEach(c => c.markAsTouched());
  }

  private toast(msg: string, type: 'success'|'error'|'warn'): void {
    this.snack.open(msg, '×', {
      duration: 4500,
      panelClass: [`snack-${type}`],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  get f() { return this.employeForm.controls; }
}