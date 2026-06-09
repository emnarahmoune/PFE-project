// src/app/features/employes/employes.component.ts

import { CommonModule, Location } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { finalize, forkJoin, Subject, takeUntil } from 'rxjs';

import { EmployeService } from '../../core/services/employe.service';
import { ManagerService, Manager } from '../../core/services/manager.service';
import { AuthService } from '../../core/services/auth.service';

import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';
import { ConfirmationDialogComponent } from '../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

import { Employe } from '../../core/models/employe.model';

type EmployeMode =
  | 'ADMIN_LISTE'
  | 'ADMIN_NOUVEAU'
  | 'ADMIN_DETAIL'
  | 'ADMIN_MODIFIER'
  | 'MANAGER_EQUIPE'
  | 'MANAGER_DETAIL';

interface EmployeExtended extends Employe {}

function noWhitespaceValidator(c: AbstractControl): ValidationErrors | null {
  return c.value && c.value !== c.value.trim() ? { whitespace: true } : null;
}

function phoneValidator(c: AbstractControl): ValidationErrors | null {
  if (!c.value) return null;
  const clean = c.value.replace(/[\s\-().+]/g, '');
  return /^\d{7,15}$/.test(clean) ? null : { invalidPhone: true };
}

@Component({
  selector: 'app-employes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressBarModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './employes.component.html',
  styleUrls: ['./employes.component.scss']
})
export class EmployesComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  mode: EmployeMode = 'ADMIN_LISTE';

  // =========================
  // COMMON
  // =========================

  loading = false;
  error = false;
  Math = Math;

  employeId?: number;
  employe?: Employe;
  managerEmploye: Employe | null = null;

  managersList: any[] = [];

  readonly avatarColors: Record<string, string> = {
    RH: '#8b5cf6',
    Technique: '#0891b2',
    Commercial: '#d97706',
    Finance: '#059669',
    Marketing: '#db2777',
    Direction: '#7c3aed',
    Logistique: '#4f46e5'
  };

  // =========================
  // ADMIN LISTE
  // =========================

  employes: Employe[] = [];
  searchText = '';
  selectedStatut = 'TOUS';
  selectedDepartement = 'TOUS';
  currentPage = 0;
  readonly pageSize = 12;

  userNom = '';
  userPrenom = '';
  userRole = '';

  statuts = ['TOUS', 'ACTIF', 'INACTIF', 'CONGE'];

  departements = [
    'TOUS',
    'RH',
    'Technique',
    'Commercial',
    'Finance',
    'Marketing',
    'Direction',
    'Logistique'
  ];

  showAssignModal = false;
  selectedEmploye: Employe | null = null;
  selectedManagerId: number | null = null;

  // =========================
  // ADMIN FORM
  // =========================

  employeForm!: FormGroup;
  isEditMode = false;
  submitting = false;

  formDepartements = ['RH', 'Technique', 'Commercial', 'Finance', 'Marketing', 'Direction', 'Logistique'];

  postes = [
    'Développeur Full Stack',
    'Développeur Backend',
    'Développeur Frontend',
    'Chef de projet',
    'Analyste',
    'Commercial',
    'Comptable',
    'Responsable RH',
    'Directeur',
    'Manager'
  ];

  rolesList = [
    { value: 'user', label: 'Employé' },
    { value: 'manager', label: 'Manager' },
    { value: 'admin_rh', label: 'Admin RH' }
  ];

  // =========================
  // ADMIN DETAIL
  // =========================

  activeTab = 'competences';

  editManagerMode = false;
  managerForm!: FormGroup;
  updatingManager = false;

  tabs = [
    { id: 'competences', icon: '🧠', label: 'Compétences' },
    { id: 'formations', icon: '📚', label: 'Formations' },
    { id: 'conges', icon: '🏖️', label: 'Congés' },
    { id: 'evaluations', icon: '⭐', label: 'Évaluations' }
  ];

  // =========================
  // MANAGER EQUIPE
  // =========================

  equipe: EmployeExtended[] = [];
  filteredEmployes: EmployeExtended[] = [];

  loadingIndicators = false;

  searchTerm = '';
  sortBy = 'nom';

sortOptions = [
  { value: 'nom', label: 'Nom A-Z' },
  { value: 'poste', label: 'Poste A-Z' },
  { value: 'departement', label: 'Département A-Z' },
  { value: 'statut', label: 'Statut' },
  { value: 'dateEmbaucheDesc', label: 'Embauche récente' }
];

  // =========================
  // MANAGER DETAIL
  // =========================


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private fb: FormBuilder,
    private snack: MatSnackBar,
    private dialog: MatDialog,
    private employeService: EmployeService,
    private managerService: ManagerService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    this.buildForm();
    this.initManagerForm();
    this.watchRoleChanges();

    this.route.data
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.mode = data['employeMode'] || 'ADMIN_LISTE';

        this.route.paramMap
          .pipe(takeUntil(this.destroy$))
          .subscribe(params => {
            const id = params.get('id');
            this.employeId = id ? Number(id) : undefined;
            this.initializeMode();
          });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeMode(): void {
    this.error = false;
    this.isEditMode = this.mode === 'ADMIN_MODIFIER';

    switch (this.mode) {
      case 'ADMIN_LISTE':
        this.loadUserInfo();
        this.loadEmployes();
        this.loadManagers();
        break;

      case 'ADMIN_NOUVEAU':
        this.resetFormForCreate();
        this.loadManagers();
        break;

      case 'ADMIN_MODIFIER':
        this.resetFormForCreate();
        this.loadManagers();

        if (this.employeId) {
          this.loadEmployeForForm();
        } else {
          this.router.navigate(['/admin/employes']);
        }
        break;

      case 'ADMIN_DETAIL':
        this.loadManagers();

        if (this.employeId) {
          this.loadAdminEmploye(this.employeId);
        } else {
          this.router.navigate(['/admin/employes']);
        }
        break;

      case 'MANAGER_EQUIPE':
        this.loadEquipe();
        break;

      case 'MANAGER_DETAIL':
        if (this.employeId) {
          this.loadManagerEmploye(this.employeId);
          this.loadManagers();
        } else {
          this.loading = false;
          this.error = true;
        }
        break;
    }
  }

  // =========================================================
  // HELPERS RESPONSE
  // =========================================================

  private unwrapResponse<T>(response: any, fallback: T): T {
    if (!response) {
      return fallback;
    }

    if (response.data !== undefined) {
      return response.data as T;
    }

    return response as T;
  }

  private toast(msg: string, type: 'success' | 'error' | 'warn'): void {
    this.snack.open(msg, '×', {
      duration: 4000,
      panelClass: [`snack-${type}`],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  private normalizeText(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  // =========================================================
  // ADMIN LISTE
  // =========================================================

  loadUserInfo(): void {
    const user = this.auth.getCurrentUser();

    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userRole = user.role || user.typeUtilisateur || 'EMPLOYE';
    }
  }

  loadEmployes(): void {
    this.loading = true;

    this.employeService.getAll()
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.employes = Array.isArray(res.data) ? res.data : [];
            this.currentPage = 0;
          } else {
            this.toast(res?.message || 'Erreur de chargement', 'error');
          }
        },
        error: () => {
          this.toast('Erreur de connexion au serveur', 'error');
        }
      });
  }

  loadManagers(): void {
    this.employeService.getAllManagers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.managersList = res?.success && Array.isArray(res.data) ? res.data : [];
        },
        error: err => {
          console.error('Erreur chargement managers', err);
          this.managersList = [];
        }
      });
  }

  getFilteredData(): Employe[] {
    const search = this.searchText.trim().toLowerCase();

    return this.employes.filter(e => {
      const matchSearch = !search || [
        e.nom,
        e.prenom,
        e.matricule,
        e.email,
        e.poste,
        e.departement
      ].some(v => v?.toLowerCase().includes(search));

      const matchStatut =
        this.selectedStatut === 'TOUS' ||
        e.statut === this.selectedStatut;

      const matchDept =
        this.selectedDepartement === 'TOUS' ||
        e.departement === this.selectedDepartement;

      return matchSearch && matchStatut && matchDept;
    });
  }

  getPagedData(): Employe[] {
    const filtered = this.getFilteredData();

    return filtered.slice(
      this.currentPage * this.pageSize,
      (this.currentPage + 1) * this.pageSize
    );
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void {
    this.currentPage = 0;
  }

  setListStatut(statut: string): void {
    this.selectedStatut = statut;
    this.currentPage = 0;
  }

  getCount(statut: string): number {
    return this.employes.filter(e => e.statut === statut).length;
  }

  getMasseSalariale(): number {
    return this.employes
      .filter(e => e.statut === 'ACTIF')
      .reduce((sum, e) => sum + (e.salaire ?? 0), 0);
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedStatut = 'TOUS';
    this.selectedDepartement = 'TOUS';
    this.applyFilter();
  }

  viewDetail(id: number): void {
    this.router.navigate(['/admin/employes', id]);
  }

  goToCreate(): void {
    this.router.navigate(['/admin/employes/nouveau']);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/admin/employes', id, 'edit']);
  }

  isManager(emp: Employe | null | undefined): boolean {
    if (!emp) {
      return false;
    }

    const role = String(
      emp.role ||
      (emp as any).typeUtilisateur ||
      (emp as any).typeEmploye ||
      ''
    ).toUpperCase();

    return role === 'MANAGER';
  }

  openAssignModal(employe: Employe): void {
    if (this.isManager(employe)) {
      this.toast('Un manager ne peut pas avoir de manager assigné', 'error');
      return;
    }

    this.selectedEmploye = employe;
    this.selectedManagerId = employe.managerId ?? null;
    this.showAssignModal = true;
  }

  closeAssignModal(): void {
    this.showAssignModal = false;
    this.selectedEmploye = null;
    this.selectedManagerId = null;
  }

  assignManager(): void {
    if (!this.selectedEmploye || !this.selectedManagerId) {
      this.toast('Veuillez sélectionner un manager', 'error');
      return;
    }

    this.loading = true;

    this.employeService.assignManager(this.selectedEmploye.id!, this.selectedManagerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.loading = false;

          if (res?.success) {
            this.toast(
              `Manager assigné à ${this.selectedEmploye!.prenom} ${this.selectedEmploye!.nom}`,
              'success'
            );

            this.closeAssignModal();
            this.loadEmployes();
          } else {
            this.toast(res?.message || 'Erreur lors de l\'assignation', 'error');
          }
        },
        error: (err: any) => {
          this.loading = false;
          this.toast(err?.error?.message || 'Erreur lors de l\'assignation', 'error');
        }
      });
  }

  unassignManager(): void {
    if (!this.selectedEmploye?.id) {
      return;
    }

    const employeName = `${this.selectedEmploye.prenom || ''} ${this.selectedEmploye.nom || ''}`.trim();

    if (!confirm(`Désassigner le manager de ${employeName} ?`)) {
      return;
    }

    this.loading = true;

    this.employeService.unassignManager(this.selectedEmploye.id)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.toast(`Manager désassigné de ${employeName}`, 'success');
            this.closeAssignModal();
            this.loadEmployes();
          } else {
            this.toast(res?.message || 'Erreur lors de la désassignation', 'error');
          }
        },
        error: (err: any) => {
          this.toast(err?.error?.message || 'Erreur lors de la désassignation', 'error');
        }
      });
  }

  hasCurrentManager(): boolean {
    return !!this.selectedEmploye?.managerId;
  }

  getCurrentManagerName(): string {
    if (!this.selectedEmploye?.managerId) {
      return 'Aucun manager';
    }

    const manager = this.managersList.find(m => m.id === this.selectedEmploye?.managerId);

    if (!manager) {
      return 'Manager actuel';
    }

    return `${manager.prenom || ''} ${manager.nom || ''}`.trim();
  }

  getManagersForModal(): Employe[] {
    if (!this.selectedEmploye?.id) {
      return this.managersList;
    }

    return this.managersList.filter(m => m.id !== this.selectedEmploye?.id);
  }

  deleteEmployeFromList(id: number, nom: string): void {
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title: 'Supprimer définitivement',
        message: `Êtes-vous sûr de vouloir supprimer définitivement ${nom} ?\n\nCette action est irréversible.`,
        confirmText: 'Supprimer définitivement',
        cancelText: 'Annuler'
      }
    });

    ref.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe(confirmed => {
        if (!confirmed) {
          return;
        }

        this.loading = true;

        this.employeService.delete(id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (res: any) => {
              if (res?.success) {
                this.employes = this.employes.filter(e => e.id !== id);
                this.currentPage = 0;

                this.toast(`Employé ${nom} supprimé avec succès`, 'success');
              } else {
                this.toast(res?.message || 'Erreur de suppression', 'error');
              }

              this.loading = false;
            },
            error: (err: any) => {
              this.loading = false;

              const serverMsg =
                err?.error?.message ||
                err?.error?.error ||
                `Erreur serveur (${err?.status ?? 'inconnu'})`;

              this.toast(serverMsg, 'error');
            }
          });
      });
  }

  // =========================================================
  // ADMIN FORM
  // =========================================================

  private buildForm(): void {
    this.employeForm = this.fb.group({
      matricule: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(20),
          noWhitespaceValidator
        ]
      ],
      nom: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(50),
          noWhitespaceValidator,
          Validators.pattern(/^[a-zA-ZÀ-ÿ\s\-']+$/)
        ]
      ],
      prenom: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(50),
          noWhitespaceValidator,
          Validators.pattern(/^[a-zA-ZÀ-ÿ\s\-']+$/)
        ]
      ],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      telephone: ['', [phoneValidator]],
      dateEmbauche: ['', [Validators.required]],
      poste: ['', [Validators.required]],
      salaire: [null, [Validators.required, Validators.min(0), Validators.max(1_000_000)]],
      departement: ['', [Validators.required]],
      statut: ['ACTIF'],
      soldeConges: [25, [Validators.required, Validators.min(0), Validators.max(365)]],
      serviceId: [null],
      managerId: [null],
      role: [null, [Validators.required]]
    });
  }

  private resetFormForCreate(): void {
    this.loading = false;
    this.submitting = false;

    this.employeForm.reset({
      matricule: '',
      nom: '',
      prenom: '',
      email: '',
      telephone: '',
      dateEmbauche: '',
      poste: '',
      salaire: null,
      departement: '',
      statut: 'ACTIF',
      soldeConges: 25,
      serviceId: null,
      managerId: null,
      role: null
    });

    this.employeForm.get('managerId')?.enable({ emitEvent: false });
  }

  private watchRoleChanges(): void {
    this.employeForm.get('role')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((role: string) => {
        if (this.isManagerRole(role)) {
          this.employeForm.get('managerId')?.setValue(null, { emitEvent: false });
          this.employeForm.get('managerId')?.disable({ emitEvent: false });
        } else {
          this.employeForm.get('managerId')?.enable({ emitEvent: false });
        }
      });
  }

  private isManagerRole(role: string | null | undefined): boolean {
    const r = String(role || '').trim().toLowerCase();
    return r === 'manager';
  }

  get isManagerSelected(): boolean {
    return this.isManagerRole(this.employeForm.get('role')?.value);
  }

  get filteredManagersList(): Employe[] {
    return this.managersList.filter((manager: Employe) => manager.id !== this.employeId);
  }

  loadEmployeForForm(): void {
    this.loading = true;

    this.employeService.getById(this.employeId!)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success && res.data) {
            const e: Employe = res.data as Employe;
            const role = this.normalizeRoleForForm(e.role);

            this.employeForm.patchValue({
              matricule: e.matricule ?? '',
              nom: e.nom ?? '',
              prenom: e.prenom ?? '',
              email: e.email ?? '',
              telephone: e.telephone ?? '',
              dateEmbauche: e.dateEmbauche ? e.dateEmbauche.split('T')[0] : '',
              poste: e.poste ?? '',
              salaire: e.salaire ?? null,
              departement: e.departement ?? '',
              statut: e.statut ?? 'ACTIF',
              soldeConges: e.soldeConges ?? 25,
              serviceId: e.serviceId ?? null,
              managerId: this.isManagerRole(role) ? null : (e.managerId ?? null),
              role
            });

            if (this.isManagerRole(role)) {
              this.employeForm.get('managerId')?.disable({ emitEvent: false });
            } else {
              this.employeForm.get('managerId')?.enable({ emitEvent: false });
            }
          } else {
            this.toast(res?.message || 'Employé introuvable', 'error');
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

    obs$
      .pipe(
        finalize(() => this.submitting = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.toast(
              this.isEditMode ? 'Employé modifié avec succès' : 'Employé créé avec succès',
              'success'
            );

            this.isEditMode
              ? this.router.navigate(['/admin/employes', this.employeId])
              : this.router.navigate(['/admin/employes']);
          } else {
            this.toast(res?.message || 'Une erreur est survenue', 'error');
          }
        },
        error: (err: any) => {
          const msg =
            err?.error?.message ||
            err?.error?.errors?.join(', ') ||
            (this.isEditMode ? 'Erreur modification' : 'Erreur création');

          this.toast(msg, 'error');
        }
      });
  }

  private toPayload(): Employe {
    const v = this.employeForm.getRawValue();

    const role = v.role;
    const managerId =
      this.isManagerRole(role) ||
      v.managerId === '' ||
      v.managerId === undefined
        ? null
        : v.managerId;

    return {
      matricule: v.matricule.trim(),
      nom: v.nom.trim(),
      prenom: v.prenom.trim(),
      email: v.email.trim().toLowerCase(),
      telephone: v.telephone?.trim() || undefined,
      dateEmbauche: new Date(v.dateEmbauche).toISOString().split('T')[0],
      poste: v.poste,
      salaire: Number(v.salaire),
      statut: v.statut || 'ACTIF',
      departement: v.departement,
      soldeConges: Number(v.soldeConges) || 25,
      role: v.role,
      serviceId: v.serviceId || undefined,
      managerId
    };
  }

  setFormStatut(s: string): void {
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

    if (e['required']) return 'Champ obligatoire';
    if (e['email']) return 'Email invalide';
    if (e['minlength']) return `Min. ${e['minlength'].requiredLength} caractères`;
    if (e['maxlength']) return `Max. ${e['maxlength'].requiredLength} caractères`;
    if (e['min']) return `Min. ${e['min'].min}`;
    if (e['max']) return `Max. ${e['max'].max}`;
    if (e['whitespace']) return 'Pas d\'espaces en début ou fin';
    if (e['pattern']) return 'Caractères non autorisés (lettres uniquement)';
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

  private normalizeRoleForForm(role: string | null | undefined): string | null {
    const r = String(role || '').trim().toUpperCase();

    if (r === 'USER' || r === 'EMPLOYE' || r === 'EMPLOYEE' || r === 'ROLE_USER') {
      return 'user';
    }

    if (r === 'MANAGER' || r === 'ROLE_MANAGER') {
      return 'manager';
    }

    if (r === 'ADMIN_RH' || r === 'ADMIN' || r === 'ROLE_ADMIN_RH' || r === 'ROLE_ADMIN') {
      return 'admin_rh';
    }

    return null;
  }

  get f() {
    return this.employeForm.controls;
  }

  // =========================================================
  // ADMIN DETAIL
  // =========================================================

  initManagerForm(): void {
    this.managerForm = this.fb.group({
      managerId: [null]
    });
  }

  loadAdminEmploye(id: number): void {
    this.loading = true;

    this.employeService.getById(id)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success && res.data) {
            this.employe = res.data as Employe;

            this.managerForm.patchValue({
              managerId: this.employe.managerId
            });
          } else {
            this.toast(res?.message || 'Employé introuvable', 'error');
            this.router.navigate(['/admin/employes']);
          }
        },
        error: () => {
          this.toast('Erreur de connexion au serveur', 'error');
          this.router.navigate(['/admin/employes']);
        }
      });
  }

  toggleStatut(): void {
    if (!this.employe?.id) {
      return;
    }

    const newStatut = this.employe.statut === 'ACTIF' ? 'INACTIF' : 'ACTIF';

    this.employeService.changeStatut(this.employe.id, newStatut)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (!this.employe) {
            return;
          }

          this.employe.statut = newStatut;
          this.toast(
            newStatut === 'ACTIF' ? 'Employé activé avec succès' : 'Employé désactivé avec succès',
            'success'
          );
        },
        error: err => {
          console.error(err);
          this.toast('Erreur lors du changement de statut', 'error');
        }
      });
  }

  updateManager(): void {
    if (!this.employe?.id) {
      return;
    }

    this.updatingManager = true;
    const newManagerId = this.managerForm.get('managerId')?.value;

    this.employeService.updateManager(this.employe.id, newManagerId)
      .pipe(
        finalize(() => this.updatingManager = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.toast('Manager mis à jour avec succès', 'success');
            this.editManagerMode = false;

            if (this.employe?.id) {
              this.loadAdminEmploye(this.employe.id);
            }
          } else {
            this.toast(res?.message || 'Erreur lors de la mise à jour', 'error');
          }
        },
        error: (err: any) => {
          const msg = err?.error?.message || 'Erreur serveur';
          this.toast(msg, 'error');
        }
      });
  }

  cancelEditManager(): void {
    this.editManagerMode = false;

    this.managerForm.patchValue({
      managerId: this.employe?.managerId
    });
  }

  deleteEmployeFromDetail(): void {
    if (!this.employe?.id) {
      return;
    }

    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title: 'Désactiver l\'employé',
        message: `Êtes-vous sûr de vouloir désactiver ${this.employe.prenom} ${this.employe.nom} ?`,
        confirmText: 'Désactiver',
        cancelText: 'Annuler'
      }
    });

    ref.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (!confirmed) {
          return;
        }

        this.employeService.delete(this.employe!.id!)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (res: any) => {
              if (res?.success) {
                this.toast('Employé désactivé avec succès', 'success');
                this.router.navigate(['/admin/employes']);
              } else {
                this.toast(res?.message || 'Erreur de suppression', 'error');
              }
            },
            error: (err: any) => {
              const serverMsg =
                err?.error?.message ||
                err?.error?.error ||
                (typeof err?.error === 'string' ? err.error : null) ||
                `Erreur serveur (${err?.status ?? 'inconnu'})`;

              console.error('❌ Erreur suppression:', err);
              this.toast(serverMsg, 'error');
            }
          });
      });
  }

  editEmploye(): void {
    if (!this.employe?.id) {
      return;
    }

    this.router.navigate(['/admin/employes', this.employe.id, 'edit']);
  }

  getAdminAvatarColor(): string {
    return this.avatarColors[this.employe?.departement ?? ''] ?? '#6366f1';
  }

  getInitials(): string {
    return (
      (this.employe?.prenom?.charAt(0) ?? '') +
      (this.employe?.nom?.charAt(0) ?? '')
    ).toUpperCase();
  }

  getSalaireFormate(): string {
    if (!this.employe?.salaire) {
      return '—';
    }

    const formatted = new Intl.NumberFormat('fr-FR', {
      maximumFractionDigits: 0
    }).format(this.employe.salaire);

    return `${formatted} DT`;
  }

  getDateEmbaucheFormatee(): string {
    if (!this.employe?.dateEmbauche) {
      return '—';
    }

    return new Date(this.employe.dateEmbauche).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  getAnciennete(): string {
    if (!this.employe?.dateEmbauche && !this.managerEmploye?.dateEmbauche) {
      return '—';
    }

    const rawDate = this.employe?.dateEmbauche || this.managerEmploye?.dateEmbauche;
    const ms = Date.now() - new Date(rawDate!).getTime();
    const yrs = Math.floor(ms / (365.25 * 24 * 3600 * 1000));
    const mths = Math.floor(
      (ms % (365.25 * 24 * 3600 * 1000)) /
      (30.44 * 24 * 3600 * 1000)
    );

    if (yrs === 0) {
      return `${mths} mois`;
    }

    if (mths === 0) {
      return `${yrs} an${yrs > 1 ? 's' : ''}`;
    }

    return `${yrs} an${yrs > 1 ? 's' : ''} ${mths} mois`;
  }

  getRoleLabel(role?: string): string {
    switch (role) {
      case 'admin_rh':
        return 'Administrateur RH';
      case 'manager':
        return 'Manager';
      default:
        return 'Employé';
    }
  }

  getManagerNom(managerId?: number | null): string {
    if (!managerId) {
      return '—';
    }

    const manager = this.managersList.find(m => m.id === managerId);
    return manager ? `${manager.prenom} ${manager.nom}` : `Manager #${managerId}`;
  }

  get profileCompletion(): number {
    if (!this.employe) {
      return 0;
    }

    const fields = [
      this.employe.nom,
      this.employe.prenom,
      this.employe.email,
      this.employe.telephone,
      this.employe.poste,
      this.employe.departement,
      this.employe.matricule,
      this.employe.dateEmbauche
    ];

    const filled = fields.filter(f => f && f.toString().trim() !== '').length;
    const total = 10;

    return Math.min(100, Math.round((filled / total) * 100));
  }

  get ancienneteMois(): number {
    if (!this.employe?.dateEmbauche) {
      return 0;
    }

    const diff = Date.now() - new Date(this.employe.dateEmbauche).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24 * 30.44));
  }

  get salaryTrend(): 'up' | 'down' | 'stable' {
    const mois = this.ancienneteMois;

    if (mois > 24) {
      return 'up';
    }

    if (mois < 12) {
      return 'down';
    }

    return 'stable';
  }

  get managerColor(): string {
    if (!this.employe?.managerNom) {
      return '#6c757d';
    }

    let hash = 0;

    for (let i = 0; i < this.employe.managerNom.length; i++) {
      hash = this.employe.managerNom.charCodeAt(i) + ((hash << 5) - hash);
    }

    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 65%, 55%)`;
  }

  goBack(): void {
    if (this.mode === 'ADMIN_DETAIL' || this.mode === 'ADMIN_MODIFIER') {
      this.router.navigate(['/admin/employes']);
      return;
    }

    this.location.back();
  }

  // =========================================================
  // MANAGER EQUIPE
  // =========================================================

  loadEquipe(): void {
    this.loading = true;

    this.managerService.getEquipe()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const data = this.unwrapResponse<EmployeExtended[]>(res, []);
          this.equipe = Array.isArray(data) ? data : [];

          this.applyFiltersAndSort();
          this.loading = false;

          
        },
        error: err => {
          console.error('Erreur chargement équipe:', err);
          this.equipe = [];
          this.filteredEmployes = [];
          this.loading = false;
        }
      });
  }

 
  filterEmployes(): void {
    this.applyFiltersAndSort();
  }

  sortEmployes(): void {
    this.applyFiltersAndSort();
  }

  applyFiltersAndSort(): void {
    const term = this.normalizeText(this.searchTerm);

    let result = [...this.equipe];

    if (term) {
      result = result.filter(emp => {
        const fullName = this.normalizeText(`${emp.prenom || ''} ${emp.nom || ''}`);
        const reverseName = this.normalizeText(`${emp.nom || ''} ${emp.prenom || ''}`);
        const poste = this.normalizeText(emp.poste || '');
        const departement = this.normalizeText(emp.departement || '');
        const email = this.normalizeText(emp.email || '');

        return (
          fullName.includes(term) ||
          reverseName.includes(term) ||
          poste.includes(term) ||
          departement.includes(term) ||
          email.includes(term)
        );
      });
    }

    result.sort((a, b) => this.compareEmployes(a, b));

    this.filteredEmployes = result;
  }

  private compareEmployes(a: EmployeExtended, b: EmployeExtended): number {
    switch (this.sortBy) {
      case 'poste':
        return this.compareText(a.poste, b.poste) || this.compareText(a.nom, b.nom);

      case 'departement':
        return this.compareText(a.departement, b.departement) || this.compareText(a.nom, b.nom);

      case 'statut':
        return this.compareText(a.statut, b.statut) || this.compareText(a.nom, b.nom);

      case 'absenteismeDesc':
        return this.compareNumberDesc(a.absenteisme, b.absenteisme) || this.compareText(a.nom, b.nom);

      case 'scoreDesc':
        return this.compareNumberDesc(a.scoreTurnover, b.scoreTurnover) || this.compareText(a.nom, b.nom);

      case 'dateEmbaucheDesc':
        return this.compareDateDesc(a.dateEmbauche, b.dateEmbauche) || this.compareText(a.nom, b.nom);

      case 'nom':
      default:
        return this.compareText(
          `${a.nom || ''} ${a.prenom || ''}`,
          `${b.nom || ''} ${b.prenom || ''}`
        );
    }
  }

  private compareText(a?: string | null, b?: string | null): number {
    return this.normalizeText(a || '').localeCompare(
      this.normalizeText(b || ''),
      'fr',
      { sensitivity: 'base' }
    );
  }

  private compareNumberDesc(a?: number | null, b?: number | null): number {
    const valueA = a ?? -1;
    const valueB = b ?? -1;

    return valueB - valueA;
  }

  private compareDateDesc(a?: string | Date | null, b?: string | Date | null): number {
    const dateA = a ? new Date(a).getTime() : 0;
    const dateB = b ? new Date(b).getTime() : 0;

    return dateB - dateA;
  }

  getFullName(emp?: EmployeExtended | Employe | null): string {
    const target = emp || this.managerEmploye || this.employe;

    if (!target) {
      return 'Employé';
    }

    return `${target.prenom || ''} ${target.nom || ''}`.trim() || 'Employé';
  }

  getStatutClass(statut?: string | null): string {
    const normalized = this.normalizeText(statut || '');

    if (normalized.includes('actif')) {
      return 'actif';
    }

    if (normalized.includes('conge')) {
      return 'conge';
    }

    if (normalized.includes('inactif')) {
      return 'inactif';
    }

    return 'default';
  }

 

 
  trackByEmployeId(index: number, emp: EmployeExtended): number | string {
    return emp.id || index;
  }

  goToManagerDetail(id: number): void {
    this.router.navigate(['/manager/employe', id]);
  }

  // =========================================================
  // MANAGER DETAIL
  // =========================================================

  loadManagerEmploye(id: number): void {
    this.loading = true;
    this.error = false;

    this.employeService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.loading = false;

          if (res?.success && res?.data) {
            this.managerEmploye = res.data;
          } else {
            this.error = true;
          }
        },
        error: err => {
          console.error('Erreur chargement employé:', err);
          this.loading = false;
          this.error = true;
        }
      });
  }



  updateManagerFromManagerDetail(newManagerId: number): void {
    if (!this.managerEmploye?.id) {
      return;
    }

    this.employeService.updateManager(this.managerEmploye.id, newManagerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.managerEmploye = res.data;
          }
        }
      });
  }


  getAvatarColor(departement?: string | null): string {
  return this.avatarColors[departement || ''] || '#6366f1';
}
  getManagerStatusClass(statut?: string | null): string {
    const normalized = this.normalizeText(statut || '');

    if (normalized.includes('actif')) {
      return 'status-actif';
    }

    if (normalized.includes('conge')) {
      return 'status-conge';
    }

    if (normalized.includes('inactif')) {
      return 'status-inactif';
    }

    return 'status-default';
  }

  formatTnd(value: number | null | undefined): string {
    const amount = Number(value || 0);

    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);

    return `${formatted} DT`;
  }
}