// src/app/features/competences/competences.component.ts

import { CommonModule, Location } from '@angular/common';
import { Component, OnDestroy, OnInit, Pipe, PipeTransform } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { Subject, finalize, takeUntil } from 'rxjs';

import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CompetenceService } from '../../core/services/competence.service';
import { ApiService } from '../../core/services/api.service';

import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { Competence } from '../../core/models/competence.model';

type CompetenceMode =
  | 'ADMIN_LISTE'
  | 'ADMIN_NOUVEAU'
  | 'ADMIN_MODIFIER'
  | 'ADMIN_DETAIL'
  | 'EMPLOYE_MES_COMPETENCES';

@Pipe({
  name: 'replace',
  standalone: true
})
export class ReplacePipe implements PipeTransform {
  transform(value: string | null | undefined, from: string, to: string): string {
    if (!value) {
      return '';
    }

    return value.split(from).join(to);
  }
}

@Component({
  selector: 'app-competences',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatMenuModule,
    MatIconModule,
    MatProgressSpinnerModule,
    EmployeeAvatarComponent,
    ReplacePipe,
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
  templateUrl: './competences.component.html',
  styleUrls: ['./competences.component.scss']
})
export class CompetencesComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  mode: CompetenceMode = 'ADMIN_LISTE';

  loading = false;
  submitting = false;

  competenceId?: number;
  competence: any = null;
  employes: any[] = [];

  // ======================================================
  // ADMIN LISTE
  // ======================================================

  allData: Competence[] = [];

  searchText = '';
  selectedCategorie = 'TOUTES';

  currentPage = 0;
  readonly pageSize = 9;

  sortField: keyof Competence | '' = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  stats = {
    total: 0,
    technique: 0,
    softSkill: 0,
    linguistique: 0,
    management: 0
  };

  categories = [
    { value: 'TOUTES', label: 'Toutes', icon: '📌', color: '#6B7280' },
    { value: 'TECHNIQUE', label: 'Technique', icon: '⚙️', color: '#3B82F6' },
    { value: 'SOFT_SKILL', label: 'Soft skill', icon: '🤝', color: '#EC4899' },
    { value: 'LINGUISTIQUE', label: 'Linguistique', icon: '🌐', color: '#10B981' },
    { value: 'MANAGEMENT', label: 'Management', icon: '📊', color: '#F59E0B' }
  ];

  // ======================================================
  // ADMIN FORM
  // ======================================================

  competenceForm!: FormGroup;
  isEditMode = false;

  formCategories = [
    { value: 'TECHNIQUE', label: 'Technique', icon: 'code' },
    { value: 'SOFT_SKILL', label: 'Soft Skill', icon: 'people' },
    { value: 'LINGUISTIQUE', label: 'Linguistique', icon: 'language' },
    { value: 'MANAGEMENT', label: 'Management', icon: 'business' }
  ];

  // ======================================================
  // EMPLOYEE COMPETENCES
  // ======================================================

  competences: any[] = [];
  competencesDisponibles: any[] = [];

  showConfirm = false;
  selectedId: number | null = null;
  selectedName = '';

  newCompetenceId: number | null = null;
  newLevel = 1;

  errorMsg = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private fb: FormBuilder,
    private snack: MatSnackBar,
    private competenceService: CompetenceService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.buildForm();

    this.route.data
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.mode = data['competenceMode'] || 'ADMIN_LISTE';

        this.route.paramMap
          .pipe(takeUntil(this.destroy$))
          .subscribe(params => {
            const id = params.get('id');
            this.competenceId = id ? Number(id) : undefined;
            this.initializeMode();
          });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeMode(): void {
    this.isEditMode = this.mode === 'ADMIN_MODIFIER';

    switch (this.mode) {
      case 'ADMIN_LISTE':
        this.loadCompetences();
        this.loadStats();
        break;

      case 'ADMIN_NOUVEAU':
        this.resetForm();
        break;

      case 'ADMIN_MODIFIER':
        this.resetForm();

        if (this.competenceId) {
          this.loadCompetenceForEdit();
        } else {
          this.router.navigate(['/admin/competences']);
        }
        break;

      case 'ADMIN_DETAIL':
        if (this.competenceId) {
          this.loadCompetenceDetail(this.competenceId);
        } else {
          this.router.navigate(['/admin/competences']);
        }
        break;

      case 'EMPLOYE_MES_COMPETENCES':
        this.loadUserCompetences();
        this.loadAllCompetencesForEmployee();
        break;
    }
  }

  // ======================================================
  // COMMON HELPERS
  // ======================================================

  private toast(message: string): void {
    this.snack.open(message, '×', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  normalizeCategorie(value: string | null | undefined): string {
    return (value || '')
      .toUpperCase()
      .replace(/\s+/g, '_')
      .trim();
  }

  getBadgeClass(categorie: string): string {
    if (!categorie) {
      return 'autre';
    }

    const cat = categorie.toLowerCase();

    if (cat.includes('tech')) {
      return 'technique';
    }

    if (cat.includes('soft')) {
      return 'soft';
    }

    if (cat.includes('manage')) {
      return 'management';
    }

    if (cat.includes('ling')) {
      return 'linguistique';
    }

    return 'autre';
  }

  goBack(): void {
    this.location.back();
  }

  // ======================================================
  // ADMIN LISTE
  // ======================================================

  loadCompetences(): void {
    this.loading = true;

    this.competenceService.getAll()
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          this.allData = Array.isArray(res?.data) ? res.data as Competence[] : [];
          this.calculateStats();
          this.currentPage = 0;
        },
        error: () => {
          this.toast('Erreur lors du chargement des compétences');
        }
      });
  }

  loadStats(): void {
    this.competenceService.getStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const s = res?.data || {};

          this.stats = {
            total: s.total || this.allData.length || 0,
            technique: s.TECHNIQUE || s.technique || 0,
            softSkill: s.SOFT_SKILL || s.softSkill || 0,
            linguistique: s.LINGUISTIQUE || s.linguistique || 0,
            management: s.MANAGEMENT || s.management || 0
          };
        },
        error: () => {
          this.calculateStats();
        }
      });
  }

  calculateStats(): void {
    this.stats.total = this.allData.length;

    this.stats.technique = this.allData.filter(c =>
      this.normalizeCategorie(c.categorie) === 'TECHNIQUE'
    ).length;

    this.stats.softSkill = this.allData.filter(c =>
      this.normalizeCategorie(c.categorie) === 'SOFT_SKILL'
    ).length;

    this.stats.linguistique = this.allData.filter(c =>
      this.normalizeCategorie(c.categorie) === 'LINGUISTIQUE'
    ).length;

    this.stats.management = this.allData.filter(c =>
      this.normalizeCategorie(c.categorie) === 'MANAGEMENT'
    ).length;
  }

  get autresDomaines(): number {
    return Math.max(
      this.stats.total -
      this.stats.technique -
      this.stats.softSkill,
      0
    );
  }

  getFilteredData(): Competence[] {
    const search = this.searchText.trim().toLowerCase();

    let data = this.allData.filter(c => {
      const matchSearch =
        !search ||
        c.nom?.toLowerCase().includes(search) ||
        c.description?.toLowerCase().includes(search);

      const matchCat =
        this.selectedCategorie === 'TOUTES' ||
        this.normalizeCategorie(c.categorie) === this.normalizeCategorie(this.selectedCategorie);

      return matchSearch && matchCat;
    });

    if (this.sortField) {
      const field = this.sortField;
      const dir = this.sortDirection === 'asc' ? 1 : -1;

      data = [...data].sort((a, b) => {
        const av = a[field] ?? '';
        const bv = b[field] ?? '';

        if (typeof av === 'number' && typeof bv === 'number') {
          return (av - bv) * dir;
        }

        return String(av).localeCompare(String(bv)) * dir;
      });
    }

    return data;
  }

  getPagedData(): Competence[] {
    const filtered = this.getFilteredData();
    const start = this.currentPage * this.pageSize;

    return filtered.slice(start, start + this.pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void {
    this.currentPage = 0;
  }

  setCategorie(cat: string): void {
    this.selectedCategorie = cat;
    this.currentPage = 0;
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedCategorie = 'TOUTES';
    this.currentPage = 0;
    this.sortField = '';
    this.sortDirection = 'asc';
  }

  sortBy(field: keyof Competence): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }

    this.currentPage = 0;
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) {
      return '';
    }

    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  getNiveauMoyenPourcentage(niveau?: number): number {
    if (!niveau) {
      return 0;
    }

    return Math.round((niveau / 4) * 100);
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.getTotalPages()) {
      this.currentPage++;
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.getTotalPages()) {
      this.currentPage = page;
    }
  }

  getPages(): number[] {
    const total = this.getTotalPages();
    const current = this.currentPage;
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 0; i < total; i++) {
        pages.push(i);
      }
    } else if (current <= 3) {
      for (let i = 0; i <= 4; i++) {
        pages.push(i);
      }

      pages.push(-1, total - 1);
    } else if (current >= total - 4) {
      pages.push(0, -1);

      for (let i = total - 5; i < total; i++) {
        pages.push(i);
      }
    } else {
      pages.push(0, -1);

      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i);
      }

      pages.push(-1, total - 1);
    }

    return pages;
  }

  viewDetails(id: number): void {
    this.router.navigate(['/admin/competences', id]);
  }

  goToCreate(): void {
    this.router.navigate(['/admin/competences/nouveau']);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/admin/competences', id, 'edit']);
  }

  deleteCompetence(id: number, nom: string): void {
    if (!confirm(`Supprimer "${nom}" ? Cette action est irréversible.`)) {
      return;
    }

    this.competenceService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.allData = this.allData.filter(c => c.id !== id);
          this.calculateStats();
          this.currentPage = 0;
          this.toast('Compétence supprimée');
        },
        error: () => {
          this.toast('Erreur lors de la suppression');
        }
      });
  }

  refresh(): void {
    this.loadCompetences();
    this.loadStats();
  }

  getCategoryColor(categorie?: string): string {
    const normalized = this.normalizeCategorie(categorie);

    switch (normalized) {
      case 'TECHNIQUE':
        return '#3B82F6';
      case 'SOFT_SKILL':
      case 'SOFT_SKILLS':
        return '#EC4899';
      case 'LINGUISTIQUE':
        return '#10B981';
      case 'MANAGEMENT':
        return '#F59E0B';
      default:
        return '#6B7280';
    }
  }

  getCategoryIcon(categorie?: string): string {
    const normalized = this.normalizeCategorie(categorie);

    switch (normalized) {
      case 'TECHNIQUE':
        return '⚙️';
      case 'SOFT_SKILL':
      case 'SOFT_SKILLS':
        return '🤝';
      case 'LINGUISTIQUE':
        return '🌐';
      case 'MANAGEMENT':
        return '📊';
      default:
        return '📌';
    }
  }

  getCategoryLabel(categorie?: string): string {
    const normalized = this.normalizeCategorie(categorie);

    switch (normalized) {
      case 'TECHNIQUE':
        return 'Technique';
      case 'SOFT_SKILL':
      case 'SOFT_SKILLS':
        return 'Soft skill';
      case 'LINGUISTIQUE':
        return 'Linguistique';
      case 'MANAGEMENT':
        return 'Management';
      default:
        return 'Autre';
    }
  }

  // ======================================================
  // ADMIN FORM
  // ======================================================

  private buildForm(): void {
    this.competenceForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      categorie: ['', Validators.required]
    });
  }

  private resetForm(): void {
    this.loading = false;
    this.submitting = false;
    this.competenceForm.reset({
      nom: '',
      description: '',
      categorie: ''
    });
  }

  loadCompetenceForEdit(): void {
    if (!this.competenceId) {
      return;
    }

    this.loading = true;

    this.competenceService.getById(this.competenceId)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (response: any) => {
          const competence = response?.data || response;

          this.competenceForm.patchValue({
            nom: competence.nom,
            description: competence.description,
            categorie: this.normalizeCategorieForForm(competence.categorie)
          });
        },
        error: () => {
          this.toast('Erreur chargement');
          this.router.navigate(['/admin/competences']);
        }
      });
  }

  normalizeCategorieForForm(categorie: string): string {
    if (!categorie) return '';

    const cat = categorie.toUpperCase().replace(' ', '_');

    if (cat.includes('SOFT')) return 'SOFT_SKILL';
    if (cat.includes('TECH')) return 'TECHNIQUE';
    if (cat.includes('MANAGE')) return 'MANAGEMENT';
    if (cat.includes('LANG')) return 'LINGUISTIQUE';

    return cat;
  }

  onSubmit(): void {
    if (this.competenceForm.invalid) {
      this.markFormGroupTouched(this.competenceForm);
      return;
    }

    this.submitting = true;

    const request$ = this.isEditMode && this.competenceId
      ? this.competenceService.update(this.competenceId, this.competenceForm.value)
      : this.competenceService.create(this.competenceForm.value);

    request$
      .pipe(
        finalize(() => this.submitting = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: () => {
          this.toast('Succès');
          this.router.navigate(['/admin/competences']);
        },
        error: error => {
          console.error(error);
          this.toast('Erreur sauvegarde');
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
      return 'Le nom doit contenir au moins 2 caractères';
    }

    if (this.nom?.hasError('maxlength')) {
      return 'Le nom ne doit pas dépasser 100 caractères';
    }

    return '';
  }

  getDescriptionErrorMessage(): string {
    if (this.description?.hasError('required')) {
      return 'La description est requise';
    }

    if (this.description?.hasError('minlength')) {
      return 'La description doit contenir au moins 10 caractères';
    }

    if (this.description?.hasError('maxlength')) {
      return 'La description ne doit pas dépasser 500 caractères';
    }

    return '';
  }

  getCategorieLabel(value: string): string {
    return this.formCategories.find(c => c.value === value)?.label || value;
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

  // ======================================================
  // ADMIN DETAIL
  // ======================================================

  loadCompetenceDetail(id: number): void {
    this.loading = true;

    this.competenceService.getDetails(id)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          this.competence = res?.data || null;

          const rawEmployes = res?.data?.employes || [];
          this.employes = rawEmployes.map((emp: any) => this.normalizeEmployeForAvatar(emp));
        },
        error: err => {
          console.error('Erreur chargement compétence:', err);
          this.competence = null;
          this.employes = [];
        }
      });
  }

  private normalizeEmployeForAvatar(emp: any): any {
    return {
      ...emp,

      id: emp.id || emp.employeId || emp.employeeId,

      prenom:
        emp.prenom ||
        emp.employePrenom ||
        emp.firstName ||
        emp.employeePrenom ||
        '',

      nom:
        emp.nom ||
        emp.employeNom ||
        emp.lastName ||
        emp.employeeNom ||
        '',

      email:
        emp.email ||
        emp.employeEmail ||
        emp.employeeEmail ||
        '',

      poste:
        emp.poste ||
        emp.employePoste ||
        emp.employeePoste ||
        'Employé',

      departement:
        emp.departement ||
        emp.employeDepartement ||
        emp.employeeDepartement ||
        '',

      photoUrl:
        emp.photoUrl ||
        emp.photo_url ||
        emp.photoProfil ||
        emp.photo ||
        emp.imageUrl ||
        emp.avatarUrl ||
        emp.employePhotoUrl ||
        emp.employePhotoProfil ||
        emp.employeePhotoUrl ||
        emp.employeePhotoProfil ||
        emp.profilPhoto ||
        emp.profilePhoto ||
        ''
    };
  }

  getNiveauValue(niveau: string): number {
    if (!niveau) {
      return 0;
    }

    switch (String(niveau).toUpperCase()) {
      case 'DEBUTANT':
        return 1;
      case 'INTERMEDIAIRE':
        return 2;
      case 'AVANCE':
        return 3;
      case 'EXPERT':
        return 4;
      default:
        return Number(niveau) || 0;
    }
  }

  getProgressValue(niveau: string): number {
    return this.getNiveauValue(niveau) * 25;
  }

  goToEmploye(emp: any): void {
    const id = emp?.id || emp?.employeId || emp?.employeeId;

    if (!id) {
      return;
    }

    this.router.navigate(['/admin/employes', id]);
  }

  // ======================================================
  // EMPLOYEE COMPETENCES
  // ======================================================

  loadUserCompetences(): void {
    this.loading = true;

    this.api.get('employes/me/competences')
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data: any) => {
          const list = Array.isArray(data) ? data : data?.data || [];

          this.competences = list.map((c: any) => ({
            ...c,
            niveau: Number(c.niveau)
          }));
        },
        error: err => {
          console.error(err);
          this.errorMsg = 'Erreur chargement compétences';
        }
      });
  }

  loadAllCompetencesForEmployee(): void {
    this.api.get('competences')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          if (Array.isArray(data)) {
            this.competencesDisponibles = data;
          } else if (data?.data) {
            this.competencesDisponibles = data.data;
          } else {
            this.competencesDisponibles = [];
          }
        },
        error: err => {
          console.error('Erreur API compétences', err);
          this.competencesDisponibles = [];
        }
      });
  }

  addCompetence(): void {
    if (!this.newCompetenceId) return;

    const exists = this.competences.some(
      c => c.competenceId == this.newCompetenceId
    );

    if (exists) {
      alert('⚠️ Compétence déjà ajoutée !');
      return;
    }

    const payload = {
      competenceId: this.newCompetenceId,
      niveau: this.newLevel
    };

    this.api.post('employes/me/competences', payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          alert('✔ Compétence ajoutée');

          this.loadUserCompetences();

          this.newCompetenceId = null;
          this.newLevel = 1;
        },
        error: err => {
          console.error('ERROR:', err);

          if (err.status === 200) {
            this.loadUserCompetences();
            return;
          }

          alert('Erreur ajout compétence');
        }
      });
  }

  deleteCompetenceEmployee(id: number, nom: string): void {
    this.selectedId = id;
    this.selectedName = nom;
    this.showConfirm = true;
  }

  confirmDelete(): void {
    if (!this.selectedId) return;

    this.api.delete('employes/me/competences', this.selectedId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadUserCompetences();
          this.showConfirm = false;
        },
        error: () => alert('Erreur suppression')
      });
  }

  cancelDelete(): void {
    this.showConfirm = false;
  }

  save(): void {
    const payload = this.competences.map(c => ({
      competenceId: c.competenceId,
      niveau: c.niveau
    }));

    this.api.putCustom('employes/me/competences', payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => alert('✔ Compétences mises à jour'),
        error: () => alert('Erreur mise à jour')
      });
  }

  getLevelLabel(level: number): string {
    switch (Number(level)) {
      case 1:
        return 'Débutant';
      case 2:
        return 'Intermédiaire';
      case 3:
        return 'Avancé';
      case 4:
        return 'Expert';
      default:
        return '';
    }
  }
}