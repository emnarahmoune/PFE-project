// src/app/features/formations/formations.component.ts

import { CommonModule, Location } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { finalize, Subject, takeUntil } from 'rxjs';
import { FormationRecommendationService } from '../../core/services/formation-recommendation.service';
import { FormationRecommendation } from '../../core/models/formation-recommendation.model';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatMenuModule } from '@angular/material/menu';

import { FormationService } from '../../core/services/formation.service';
import {
  Formation,
  FormationVideo,
  FormationSupport
} from '../../core/models/formation.model';

import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

type FormationMode =
  | 'ADMIN_LISTE'
  | 'ADMIN_NOUVEAU'
  | 'ADMIN_MODIFIER'
  | 'ADMIN_DETAIL'
  | 'ADMIN_PARTICIPANTS'
  | 'EMPLOYE_MES_FORMATIONS';

type DomaineFormation =
  | 'TECHNIQUE'
  | 'SOFT_SKILLS'
  | 'MANAGEMENT'
  | 'LANGUES'
  | 'SECURITE'
  | 'INFORMATIQUE';

interface DomaineOption {
  value: DomaineFormation;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-formations',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatMenuModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './formations.component.html',
  styleUrls: ['./formations.component.scss']
})
export class FormationsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  mode: FormationMode = 'ADMIN_LISTE';

  // =========================
  // COMMON
  // =========================

  loading = false;
  submitting = false;
  formationId: number | null = null;

  formation: any = null;
  participants: any[] = [];

  // =========================
  // ADMIN LISTE
  // =========================

  formations: Formation[] = [];
  filteredFormations: Formation[] = [];

  searchText = '';
  selectedDomaine = 'TOUS';
  viewMode: 'grid' | 'table' = 'grid';

  stats: any = {
    total: 0,
    actives: 0,
    dureeMoyenne: 0,
    totalParticipants: 0,
    participantsMoyens: 0,
    informatique: 0,
    technique: 0,
    softSkills: 0,
    management: 0,
    langues: 0,
    securite: 0
  };

  domainesFilter = [
    'TOUS',
    'INFORMATIQUE',
    'TECHNIQUE',
    'SOFT_SKILLS',
    'MANAGEMENT',
    'LANGUES',
    'SECURITE'
  ];

  // =========================
  // ADMIN FORM
  // =========================

  formationForm!: FormGroup;
  isEditMode = false;

  domaines: DomaineOption[] = [
    { value: 'TECHNIQUE', label: 'Technique', icon: 'build' },
    { value: 'SOFT_SKILLS', label: 'Soft Skills', icon: 'psychology' },
    { value: 'MANAGEMENT', label: 'Management', icon: 'groups' },
    { value: 'LANGUES', label: 'Langues', icon: 'language' },
    { value: 'SECURITE', label: 'Sécurité', icon: 'security' },
    { value: 'INFORMATIQUE', label: 'Informatique', icon: 'computer' }
  ];

  // =========================
  // EMPLOYEE
  // =========================

formationsSuivies: any[] = [];

recommandations: FormationRecommendation[] = [];
recommandationsPoste: FormationRecommendation[] = [];
recommandationsSkill: FormationRecommendation[] = [];

recommendationsLoading = false;
recommendationsError = '';

  videos: any[] = [];
  completedVideos: number[] = [];

  selectedFormationIndex: number | null = null;
  selectedFormationId: number | null = null;
  revoirMode = false;
  revoirModeFormationId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
private formationService: FormationService,
private formationRecommendationService: FormationRecommendationService,
private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.buildForm();

    this.route.data
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.mode = data['formationMode'] || 'ADMIN_LISTE';

        this.route.paramMap
          .pipe(takeUntil(this.destroy$))
          .subscribe(params => {
            const id = params.get('id');
            this.formationId = id ? Number(id) : null;

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
        this.loadFormations();
        break;

      case 'ADMIN_NOUVEAU':
        this.resetForm();
        break;

      case 'ADMIN_MODIFIER':
        this.resetForm();

        if (this.formationId) {
          this.loadFormationForEdit(this.formationId);
        } else {
          this.router.navigate(['/admin/formations']);
        }
        break;

      case 'ADMIN_DETAIL':
        if (this.formationId) {
          this.loadFormationDetail(this.formationId);
        } else {
          this.router.navigate(['/admin/formations']);
        }
        break;

      case 'ADMIN_PARTICIPANTS':
        if (this.formationId) {
          this.loadParticipants(this.formationId);
        } else {
          this.router.navigate(['/admin/formations']);
        }
        break;

      case 'EMPLOYE_MES_FORMATIONS':
  this.loadMesFormations();
  this.loadRecommendationsIA();
  break;
    }
  }

  // =========================================================
  // HELPERS
  // =========================================================

  private toast(message: string, action = 'Fermer'): void {
    this.snackBar.open(message, action, {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  normalizeText(value: string | null | undefined): string {
    return String(value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  normalizeDomaine(value: string | null | undefined): string {
    const raw = String(value || '')
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();

    const normalized = raw
      .replace(/[-\s]+/g, '_')
      .replace(/_+/g, '_');

    const aliases: Record<string, string> = {
      TOUS: 'TOUS',

      SOFT_SKILL: 'SOFT_SKILLS',
      SOFT_SKILLS: 'SOFT_SKILLS',
      SOFTSKILL: 'SOFT_SKILLS',
      SOFTSKILLS: 'SOFT_SKILLS',

      INFORMATIQUE: 'INFORMATIQUE',
      INFO: 'INFORMATIQUE',

      TECHNIQUE: 'TECHNIQUE',
      TECHNICAL: 'TECHNIQUE',

      MANAGEMENT: 'MANAGEMENT',

      LANGUE: 'LANGUES',
      LANGUES: 'LANGUES',

      SECURITE: 'SECURITE',
      SECURITE_INFORMATIQUE: 'SECURITE',
      SECURITY: 'SECURITE'
    };

    return aliases[raw] || aliases[normalized] || normalized;
  }

  getDomaineLabel(domaine: string | null | undefined): string {
    const d = this.normalizeDomaine(domaine);

    const labels: Record<string, string> = {
      TOUS: 'Tous',
      INFORMATIQUE: 'Informatique',
      TECHNIQUE: 'Technique',
      SOFT_SKILLS: 'Soft Skills',
      MANAGEMENT: 'Management',
      LANGUES: 'Langues',
      SECURITE: 'Sécurité'
    };

    return labels[d] || d || 'Non défini';
  }

  getDomaineColor(domaine: string | null | undefined): string {
    const d = this.normalizeDomaine(domaine);

    const colors: Record<string, string> = {
      INFORMATIQUE: '#4361ee',
      TECHNIQUE: '#0891b2',
      SOFT_SKILLS: '#8b5cf6',
      MANAGEMENT: '#f97316',
      LANGUES: '#10b981',
      SECURITE: '#ef4444'
    };

    return colors[d] || '#4361ee';
  }

  getDomaineMaterialColor(domaine: string): string {
    const map: Record<string, string> = {
      TECHNIQUE: 'primary',
      SOFT_SKILLS: 'accent',
      MANAGEMENT: 'warn',
      LANGUES: 'primary',
      SECURITE: 'warn',
      INFORMATIQUE: 'primary'
    };

    return map[this.normalizeDomaine(domaine)] || 'primary';
  }

  // =========================================================
  // ADMIN LIST
  // =========================================================

  loadFormations(): void {
    this.loading = true;

    this.formationService.getAll()
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          const data = this.extractFormations(res);

          this.formations = data;
          this.calculateStats(data);
          this.applyFilter();
        },
        error: () => {
          this.toast('Erreur chargement');
        }
      });
  }

  extractFormations(res: any): Formation[] {
    if (Array.isArray(res)) {
      return res;
    }

    if (Array.isArray(res?.data)) {
      return res.data;
    }

    if (Array.isArray(res?.content)) {
      return res.content;
    }

    if (Array.isArray(res?.items)) {
      return res.items;
    }

    return [];
  }

  calculateStats(data: Formation[]): void {
    const total = data.length;

    this.stats.total = total;
    this.stats.actives = data.filter((f: any) => !!f.actif).length;

    this.stats.totalParticipants = data.reduce(
      (sum: number, f: any) => sum + Number(f.nombreParticipants || 0),
      0
    );

    const totalDuree = data.reduce(
      (sum: number, f: any) => sum + Number(f.dureeHeures || 0),
      0
    );

    this.stats.dureeMoyenne = total > 0 ? Math.round(totalDuree / total) : 0;

    this.stats.participantsMoyens =
      total > 0 ? Math.round(this.stats.totalParticipants / total) : 0;

    this.stats.informatique = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'INFORMATIQUE'
    ).length;

    this.stats.technique = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'TECHNIQUE'
    ).length;

    this.stats.softSkills = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'SOFT_SKILLS'
    ).length;

    this.stats.management = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'MANAGEMENT'
    ).length;

    this.stats.langues = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'LANGUES'
    ).length;

    this.stats.securite = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'SECURITE'
    ).length;
  }

  applyFilter(): void {
    const search = this.normalizeText(this.searchText);
    const selected = this.normalizeDomaine(this.selectedDomaine);

    let filtered = [...this.formations];

    if (search) {
      filtered = filtered.filter((f: any) => {
        const titre = this.normalizeText(f.titre);
        const description = this.normalizeText(f.description);
        const domaine = this.normalizeText(f.domaine);

        return (
          titre.includes(search) ||
          description.includes(search) ||
          domaine.includes(search)
        );
      });
    }

    if (selected !== 'TOUS') {
      filtered = filtered.filter((f: any) => {
        const formationDomaine = this.normalizeDomaine(f.domaine);
        return formationDomaine === selected;
      });
    }

    this.filteredFormations = filtered;
  }

  filterByDomaine(domaine: string): void {
    this.selectedDomaine = this.normalizeDomaine(domaine);
    this.applyFilter();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedDomaine = 'TOUS';
    this.applyFilter();
  }

  goToCreate(): void {
    this.router.navigate(['/admin/formations/new']);
  }

  goToDetail(id: number | undefined): void {
    if (!id) return;
    this.router.navigate(['/admin/formations', id]);
  }

  goToEdit(id: number | undefined): void {
    if (!id) return;
    this.router.navigate(['/admin/formations', id, 'edit']);
  }

  goToParticipants(id: number | undefined = this.formation?.id): void {
    if (!id) return;
    this.router.navigate(['/admin/formations', id, 'participants']);
  }

  deleteFormationFromList(formation: Formation): void {
    if (!formation?.id) return;

    if (!confirm(`Supprimer la formation "${formation.titre}" ?`)) {
      return;
    }

    this.formationService.delete(formation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toast('Formation supprimée', 'OK');
          this.loadFormations();
        },
       error: (err: any) => {
          console.error(err);
          this.toast('Erreur suppression');
        }
      });
  }

  toggleFormationActifFromList(formation: Formation): void {
    if (!formation?.id) return;

    const call$ = formation.actif
      ? this.formationService.desactiver(formation.id)
      : this.formationService.activer(formation.id);

    call$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toast(
            formation.actif ? 'Formation désactivée' : 'Formation activée',
            'OK'
          );

          this.loadFormations();
        },
        error: (err: any) => {
          console.error(err);
          this.toast('Erreur changement statut');
        }
      });
  }

  // =========================================================
  // ADMIN FORM
  // =========================================================

  private buildForm(): void {
    this.formationForm = this.fb.group({
      titre: ['', Validators.required],
      domaine: ['', Validators.required],
      dureeHeures: [1, [Validators.required, Validators.min(1)]],
      description: ['', Validators.required],
      actif: [true],
      videos: this.fb.array([]),
      supports: this.fb.array([])
    });
  }

  private resetForm(): void {
    this.loading = false;
    this.submitting = false;

    this.formationForm.reset({
      titre: '',
      domaine: '',
      dureeHeures: 1,
      description: '',
      actif: true
    });

    this.videosArray.clear();
this.supportsArray.clear();
  }

  get videosArray(): FormArray {
    return this.formationForm.get('videos') as FormArray;
  }

  get supportsArray(): FormArray {
    return this.formationForm.get('supports') as FormArray;
  }

  createVideoGroup(data?: any): FormGroup {
    return this.fb.group({
      id: [data?.id || null],
      titre: [data?.titre || '', Validators.required],
      urlYoutube: [
        data?.urlYoutube || data?.url_youtube || data?.url || '',
        Validators.required
      ],
      ordre: [data?.ordre || this.videosArray.length + 1]
    });
  }

  createSupportGroup(data?: any): FormGroup {
    return this.fb.group({
      id: [data?.id || null],
      titre: [data?.titre || '', Validators.required],
      fichierUrl: [
        data?.fichierUrl || data?.fichier_url || data?.url || data?.pdfPath || '',
        Validators.required
      ],
      ordre: [data?.ordre || this.supportsArray.length + 1]
    });
  }

  addVideo(): void {
    this.videosArray.push(this.createVideoGroup());
  }

  removeVideo(index: number): void {
    this.videosArray.removeAt(index);
    this.reorderVideos();
  }

  addSupport(): void {
    this.supportsArray.push(this.createSupportGroup());
  }

  removeSupport(index: number): void {
    this.supportsArray.removeAt(index);
    this.reorderSupports();
  }

  reorderVideos(): void {
    this.videosArray.controls.forEach((control, index) => {
      control.patchValue({ ordre: index + 1 });
    });
  }

  reorderSupports(): void {
    this.supportsArray.controls.forEach((control, index) => {
      control.patchValue({ ordre: index + 1 });
    });
  }

  loadFormationForEdit(id: number): void {
    this.loading = true;

    this.formationService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const formation = this.extractFormation(res);

          this.formationForm.patchValue({
            titre: formation?.titre || '',
            domaine: this.normalizeDomaine(formation?.domaine || 'TECHNIQUE'),
            dureeHeures: formation?.dureeHeures || 1,
            description: formation?.description || '',
            actif: formation?.actif ?? true
          });

          this.videosArray.clear();
          this.supportsArray.clear();

          this.extractVideos(formation)
            .sort((a: any, b: any) => (a.ordre || 0) - (b.ordre || 0))
            .forEach((video: any) => {
              this.videosArray.push(this.createVideoGroup(video));
            });

          this.extractSupports(formation)
            .sort((a: any, b: any) => (a.ordre || 0) - (b.ordre || 0))
            .forEach((support: any) => {
              this.supportsArray.push(this.createSupportGroup(support));
            });

          this.loading = false;
        },
       error: (err: any) => {
          console.error('Erreur chargement formation:', err);
          this.loading = false;

          this.toast('Erreur lors du chargement de la formation');
          this.router.navigate(['/admin/formations']);
        }
      });
  }

  private extractFormation(res: any): any {
    if (!res) return {};
    if (res.data) return res.data;
    if (res.formation) return res.formation;
    return res;
  }

  private extractVideos(formation: any): any[] {
    if (!formation) return [];
    if (Array.isArray(formation.videos)) return formation.videos;
    if (Array.isArray(formation.formationVideos)) return formation.formationVideos;
    if (Array.isArray(formation.videoList)) return formation.videoList;
    return [];
  }

  private extractSupports(formation: any): any[] {
    if (!formation) return [];
    if (Array.isArray(formation.supports)) return formation.supports;
    if (Array.isArray(formation.formationSupports)) return formation.formationSupports;
    if (Array.isArray(formation.supportList)) return formation.supportList;
    return [];
  }

  normalizeDomaineForForm(domaine: string): DomaineFormation {
    const value = this.normalizeDomaine(domaine) as DomaineFormation;

    const found = this.domaines.find(d => d.value === value);

    if (found) {
      return found.value;
    }

    return 'TECHNIQUE';
  }

  getSelectedDomaine(): DomaineOption | undefined {
    const value = this.formationForm?.get('domaine')?.value;
    return this.domaines.find(d => d.value === value);
  }

  getSelectedIcon(): string {
    return this.getSelectedDomaine()?.icon || 'help';
  }

  getSelectedLabel(): string {
    return this.getSelectedDomaine()?.label || 'Choisir';
  }

  uploadSupportPdf(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (file.type !== 'application/pdf') {
      this.toast('Veuillez sélectionner un fichier PDF');
      input.value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.formationService.uploadPdf(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const filePath =
            res?.filePath ||
            res?.fichierUrl ||
            res?.url ||
            res?.data?.filePath ||
            res?.data?.fichierUrl ||
            '';

          this.supportsArray.at(index).patchValue({
            fichierUrl: filePath,
            titre: this.supportsArray.at(index).get('titre')?.value || file.name
          });

          this.toast('PDF chargé avec succès', 'OK');
        },
        error: (err: any) => {
          console.error('Erreur upload PDF:', err);
          this.toast('Erreur lors du chargement du PDF');
        }
      });
  }

  onSubmit(): void {
    if (this.formationForm.invalid) {
      this.formationForm.markAllAsTouched();
      this.toast('Veuillez remplir tous les champs obligatoires');
      return;
    }

    this.submitting = true;

    const payload: Formation = {
      titre: this.formationForm.value.titre,
      domaine: this.normalizeDomaineForForm(this.formationForm.value.domaine),
      dureeHeures: Number(this.formationForm.value.dureeHeures),
      description: this.formationForm.value.description,
      actif: this.formationForm.value.actif,

      videos: this.formationForm.value.videos.map((video: any, index: number) => ({
        id: video.id,
        titre: video.titre,
        urlYoutube: video.urlYoutube,
        ordre: index + 1
      })),

      supports: this.formationForm.value.supports.map((support: any, index: number) => ({
        id: support.id,
        titre: support.titre,
        fichierUrl: support.fichierUrl,
        ordre: index + 1
      }))
    };

    const request$ = this.isEditMode && this.formationId
      ? this.formationService.update(this.formationId, payload)
      : this.formationService.create(payload);

    request$
      .pipe(
        finalize(() => this.submitting = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res: any) => {
          const success = res?.success !== false;

          if (success) {
            this.toast(
              this.isEditMode
                ? 'Formation modifiée avec succès'
                : 'Formation créée avec succès',
              'OK'
            );

            this.router.navigate(['/admin/formations']);
          } else {
            this.toast(res?.message || 'Erreur lors de la sauvegarde');
          }
        },
        error: (err: any) => {
          console.error('Erreur sauvegarde formation:', err);
          this.toast('Erreur serveur lors de la sauvegarde');
        }
      });
  }

  cancelForm(): void {
    this.router.navigate(['/admin/formations']);
  }

  // =========================================================
  // ADMIN DETAIL
  // =========================================================

 loadFormationDetail(id: number): void {
  this.loading = true;
  this.participants = [];

  this.formationService.getById(id)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (res: any) => {
        this.formation = this.extractFormation(res);

        this.formationService.getParticipants(id)
          .pipe(finalize(() => this.loading = false), takeUntil(this.destroy$))
          .subscribe({
            next: (participantsData: any) => {
              this.participants = Array.isArray(participantsData)
                ? participantsData
                : participantsData?.data || [];
            },
            error: (err: any) => {
              console.error('Erreur chargement participants détail:', err);
              this.participants = [];
            }
          });
      },
      error: (err: any) => {
        console.error('Erreur chargement formation:', err);
        this.formation = null;
        this.participants = [];
        this.loading = false;
        this.toast('Erreur chargement formation');
      }
    });
}
  goBack(): void {
    this.location.back();
  }

  goToEditCurrent(): void {
    if (!this.formation?.id) return;
    this.router.navigate(['/admin/formations', this.formation.id, 'edit']);
  }

  deleteFormation(): void {
    if (!this.formation?.id) return;

    if (!confirm('Supprimer cette formation ?')) return;

    this.formationService.delete(this.formation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.router.navigate(['/admin/formations']),
        error: (err: any) => {
          console.error(err);
          this.toast('Erreur suppression formation');
        }
      });
  }

  toggleStatut(): void {
    if (!this.formation?.id) return;

    const call$ = this.formation.actif
      ? this.formationService.desactiver(this.formation.id)
      : this.formationService.activer(this.formation.id);

    call$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.loadFormationDetail(this.formation.id),
        error: (err: any) => {
          console.error(err);
          this.toast('Erreur changement statut');
        }
      });
  }

retirerParticipant(p: any): void {
  if (!this.formation?.id) {
    this.toast('Formation invalide');
    return;
  }

  const employeId =
    p?.employeId ||
    p?.employeeId ||
    p?.employe?.id ||
    p?.id;

  if (!employeId) {
    console.error('Participant sans employeId:', p);
    this.toast('Employé introuvable');
    return;
  }

  if (!confirm(`Retirer ${p.prenom || ''} ${p.nom || ''} de cette formation ?`)) {
    return;
  }

  this.formationService.retirerParticipant(this.formation.id, employeId)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        this.toast('Participant retiré avec succès ✅', 'OK');
        this.loadFormationDetail(this.formation.id);
      },
      error: (err: any) => {
        console.error('Erreur retrait participant:', err);
        this.toast('Erreur retrait participant ❌');
      }
    });
}

  getCompletionRate(): number {
    if (!this.participants.length) return 0;
    return Math.min((this.participants.length / 10) * 100, 100);
  }

  getAvgSatisfaction(): number {
    return 4;
  }

  // =========================================================
  // ADMIN PARTICIPANTS
  // =========================================================

  loadParticipants(id: number): void {
    this.loading = true;
    this.participants = [];

    this.formationService.getParticipants(id)
      .pipe(
        finalize(() => this.loading = false),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (data: any) => {
          this.participants = Array.isArray(data)
            ? data
            : data?.data || [];
        },
       error: (err: any) => {
          console.error('Erreur participants:', err);
          this.participants = [];
        }
      });
  }

  // =========================================================
  // EMPLOYEE MES FORMATIONS
  // =========================================================

loadMesFormations(): void {
  this.formationService.getMyFormations()
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (data: any[]) => {
        console.log('MES FORMATIONS REÇUES =', data);
        this.formationsSuivies = Array.isArray(data) ? data : [];
      },
      error: (err: any) => {
        console.error('Erreur chargement mes formations:', err);
        this.formationsSuivies = [];
      }
    });
}
loadRecommendationsIA(): void {
  this.recommendationsLoading = true;
  this.recommendationsError = '';

  this.formationRecommendationService.generateMyRecommendations()
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (data: any[]) => {
        console.log('RECOMMANDATIONS IA CONNECTED USER =', data);

        this.recommandations = Array.isArray(data) ? data : [];

        this.recommandationsPoste = this.recommandations.filter((rec: any) =>
          rec?.type === 'GAP_POSTE'
        );

        this.recommandationsSkill = this.recommandations.filter((rec: any) =>
          rec?.type === 'BOOST_COMPETENCES'
        );

        this.recommendationsLoading = false;
      },
      error: (err: any) => {
        console.error('Erreur recommandations IA:', err);
        this.recommendationsError = 'Impossible de générer les recommandations IA.';
        this.recommendationsLoading = false;
      }
    });
}
private getCurrentEmployeId(): number | null {
  const directKeys = [
    'employeId',
    'employeeId',
    'userId',
    'id'
  ];

  for (const key of directKeys) {
    const value = localStorage.getItem(key);

    if (value && !Number.isNaN(Number(value))) {
      return Number(value);
    }
  }

  const objectKeys = [
    'currentUser',
    'user',
    'authUser',
    'connectedUser'
  ];

  for (const key of objectKeys) {
    const raw = localStorage.getItem(key);

    if (!raw) {
      continue;
    }

    try {
      const user = JSON.parse(raw);
      const id =
        user?.employeId ||
        user?.employeeId ||
        user?.userId ||
        user?.id;

      if (id && !Number.isNaN(Number(id))) {
        return Number(id);
      }
    } catch {
      // ignore invalid localStorage json
    }
  }

  return null;
}

getRecommendationScore(rec: FormationRecommendation): number {
  return Math.round(Number(rec.score || 0) * 100);
}

getRecommendationTypeLabel(type?: string | null): string {
  if (type === 'GAP_POSTE') {
    return 'Objectif poste';
  }

  if (type === 'BOOST_COMPETENCES') {
    return 'Boost compétences';
  }

  return 'Recommandation IA';
}

getRecommendationProvider(rec: FormationRecommendation): string {
  return rec.provider || 'Source externe';
}

trackRecommendation(index: number, rec: FormationRecommendation): number {
  return rec.id || index;
}

handleFormationButton(ef: any, index: number): void {
  if (this.selectedFormationIndex === index) {
    this.selectedFormationIndex = null;
    this.selectedFormationId = null;
    this.videos = [];
    return;
  }

  const formation = ef?.formation || ef;

  if (!formation?.id) {
    this.toast('Formation invalide');
    return;
  }

  this.selectedFormationIndex = index;
  this.voirFormation(formation);
}

voirFormation(formation: any): void {
  if (!formation?.id) {
    this.toast('Formation invalide');
    return;
  }

  this.selectedFormationId = formation.id;
  this.videos = [];
  this.completedVideos = [];

  this.formationService.getVideos(formation.id)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (videosData: any[]) => {
        const videos = Array.isArray(videosData) ? videosData : [];

        this.formationService.getCompletedVideos(formation.id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (ids: number[]) => {
              this.completedVideos = Array.isArray(ids)
                ? ids.map(id => Number(id))
                : [];

              this.videos = videos.map((video: any) => ({
                ...video,
                urlYoutube: video.urlYoutube || video.url_youtube || video.url || '',
                completedView: this.completedVideos.includes(Number(video.id))
              }));

              console.log('VIDÉOS FORMATION =', this.videos);
            },
            error: (err: any) => {
              console.error('Erreur vidéos complétées:', err);

              this.completedVideos = [];

              this.videos = videos.map((video: any) => ({
                ...video,
                urlYoutube: video.urlYoutube || video.url_youtube || video.url || '',
                completedView: false
              }));
            }
          });
      },
      error: (err: any) => {
        console.error('Erreur chargement vidéos:', err);
        this.videos = [];
      }
    });
}




handleFormationMainButton(ef: any, index: number): void {
  if ((ef?.progression || 0) >= 100 && this.selectedFormationIndex !== index) {
    this.restartFormationAndOpen(ef, index);
    return;
  }

  this.handleFormationButton(ef, index);
}




restartFormationAndOpen(ef: any, index: number): void {
  const formation = ef?.formation || ef;
  const formationId = formation?.id;

  if (!formationId) {
    this.toast('Formation invalide');
    return;
  }

  this.formationService.resetFormationProgress(formationId)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        ef.progression = 0;
        ef.statut = 'EN_COURS';

        this.completedVideos = [];
        this.videos = [];
        this.selectedFormationIndex = index;
        this.selectedFormationId = formationId;
        this.revoirModeFormationId = formationId;

        this.formationService.getVideos(formationId)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (videosData: any[]) => {
              this.videos = (Array.isArray(videosData) ? videosData : []).map((v: any) => ({
                ...v,
                urlYoutube: v.urlYoutube || v.url_youtube || v.url || '',
                completedView: false
              }));
            },
            error: (err: any) => {
              console.error('Erreur chargement vidéos après reset:', err);
              this.videos = [];
            }
          });
      },
      error: (err: any) => {
        console.error('Erreur reset formation:', err);
        this.toast('Erreur lors de la réinitialisation ❌');
      }
    });
}
  revoirFormation(ef: any, index: number): void {
    const formationId = ef.formation.id;

    this.formationService.resetFormationProgress(formationId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          ef.progression = 0;

          this.selectedFormationIndex = index;
          this.selectedFormationId = formationId;
          this.revoirModeFormationId = formationId;

          this.completedVideos = [];
          this.videos = [];

          this.formationService.getVideos(formationId)
            .pipe(takeUntil(this.destroy$))
            .subscribe((videosData: any[]) => {
              this.videos = videosData.map((v: any) => ({
                ...v,
                completedView: false
              }));
            });
        },
        error: (err: any) => {
          console.error('Erreur reset formation:', err);
          alert('Erreur lors de la réinitialisation ❌');
        }
      });
  }

  isRevoirMode(): boolean {
    return String(this.selectedFormationId) === String(this.revoirModeFormationId);
  }

  isVideoCompleted(videoId: number): boolean {
    if (this.isRevoirMode()) {
      return false;
    }

    return this.completedVideos.map(id => String(id)).includes(String(videoId));
  }

  completeVideo(video: any, ef: any): void {
    this.formationService.completeVideo(video.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          video.completedView = true;

          if (!this.completedVideos.includes(Number(video.id))) {
            this.completedVideos.push(Number(video.id));
          }

          const totalVideos = this.videos.length;
          const completedCount = this.videos.filter(v => v.completedView).length;

          if (totalVideos > 0) {
            ef.progression = Math.round((completedCount / totalVideos) * 100);
          }
        },
        error: (err: any) => {
          console.error(err);
        }
      });
  }

 inscrire(formation: any): void {
  if (!formation?.id) {
    if (formation?.url) {
      window.open(formation.url, '_blank', 'noopener,noreferrer');
      return;
    }

    this.toast('Formation externe : ouvrez le lien de la formation.');
    return;
  }

  this.formationService.inscrireFormation(formation.id)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        this.toast('Inscription réussie ✅', 'OK');

        this.recommandations = this.recommandations.filter(
          f => f.id !== formation.id
        );

        this.recommandationsPoste = this.recommandationsPoste.filter(
          f => f.id !== formation.id
        );

        this.recommandationsSkill = this.recommandationsSkill.filter(
          f => f.id !== formation.id
        );

        this.loadMesFormations();
        this.loadRecommendationsIA();
      },
      error: (err: any) => {
        console.error(err);

        if (err.status === 200) {
          this.toast('Inscription réussie ✅', 'OK');
          this.loadMesFormations();
          this.loadRecommendationsIA();
        } else {
          this.toast('Erreur inscription ❌');
        }
      }
    });
}

  generateCertificate(formationId: number): void {
    this.formationService.generateCertificate(formationId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');

          a.href = url;
          a.download = 'certificat.pdf';
          a.click();

          window.URL.revokeObjectURL(url);
        },
        error: (err: any) => {
          console.error(err);
          alert('Erreur génération certificat ❌');
        }
      });
  }





 
inscrireRecommendation(rec: FormationRecommendation): void {

  // Cas formation EXTERNE — ouvrir le lien dans un nouvel onglet
  if (!rec?.id) {
    const url = (rec as any)?.url;
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
      this.toast('Formation externe ouverte dans un nouvel onglet 🔗', 'OK');
    } else {
      this.toast('Formation externe — aucun lien disponible.');
    }
    return;
  }

  // Cas formation INTERNE — inscription normale
  this.formationRecommendationService.inscrireRecommendation(rec.id)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: () => {
        this.toast('Formation ajoutée à vos formations ✅', 'OK');
        this.recommandations = this.recommandations.filter(r => r.id !== rec.id);
        this.recommandationsPoste = this.recommandationsPoste.filter(r => r.id !== rec.id);
        this.recommandationsSkill = this.recommandationsSkill.filter(r => r.id !== rec.id);
        this.loadMesFormations();
      },
      error: (err: unknown) => {
        console.error(err);
        this.toast('Erreur inscription recommandation IA ❌');
      }
    });
}


getYoutubeEmbedUrl(url: string): SafeResourceUrl | null {
  if (!url) {
    return null;
  }

  let videoId = '';

  if (url.includes('watch?v=')) {
    videoId = url.split('watch?v=')[1];

    if (videoId.includes('&')) {
      videoId = videoId.split('&')[0];
    }
  } else if (url.includes('youtu.be/')) {
    videoId = url.split('youtu.be/')[1];

    if (videoId.includes('?')) {
      videoId = videoId.split('?')[0];
    }
  } else if (url.includes('/embed/')) {
    videoId = url.split('/embed/')[1];

    if (videoId.includes('?')) {
      videoId = videoId.split('?')[0];
    }
  } else if (url.includes('/shorts/')) {
    videoId = url.split('/shorts/')[1];

    if (videoId.includes('?')) {
      videoId = videoId.split('?')[0];
    }
  }

  if (!videoId) {
    return null;
  }

  return this.sanitizer.bypassSecurityTrustResourceUrl(
    'https://www.youtube.com/embed/' + videoId
  );
}
}