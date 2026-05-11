import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

import { EmployeService } from '../../core/services/employe.service';
import {
  OffreRecrutement,
  StatutOffreRecrutement
} from '../../core/models/offre-recrutement.model';

import {
  Candidature,
  TopCandidature
} from '../../core/models/candidature.model';

import { RecrutementService } from '../../core/services/recrutement.service';
import { CandidatureService } from '../../core/services/candidature.service';

type RecrutementMode = 'ADMIN_RECRUTEMENT' | 'EMPLOYE_RECRUTEMENT';

@Component({
  selector: 'app-recrutement',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './recrutement.component.html',
  styleUrls: ['./recrutement.component.scss']
})
export class RecrutementComponent implements OnInit, OnDestroy {
  recrutementMode: RecrutementMode = 'EMPLOYE_RECRUTEMENT';

  loading = false;
  saving = false;
  analysing = false;

  offres: OffreRecrutement[] = [];
  offresFiltrees: OffreRecrutement[] = [];

  candidatures: Candidature[] = [];
  mesCandidatures: Candidature[] = [];
  topCandidatures: TopCandidature[] = [];

  selectedOffre: OffreRecrutement | null = null;
  selectedCandidature: Candidature | null = null;

  showOffreForm = false;
  editingOffre: OffreRecrutement | null = null;

  showPostulerForm = false;
  selectedCvFile: File | null = null;
  cvFileName = '';

  searchTerm = '';
  statutFilter: 'ALL' | StatutOffreRecrutement = 'ALL';

  offreForm!: FormGroup;
  candidatureForm!: FormGroup;
  decisionForm!: FormGroup;

  currentEmployeId: number | null = null;
  currentUser: any = null;

  submitMode: 'BROUILLON' | 'PUBLIER' = 'BROUILLON';

  postesDisponibles: string[] = [
    'Développeur Full Stack',
    'Développeur Frontend',
    'Développeur Backend',
    'Développeur Angular',
    'Développeur Java / Spring Boot',
    'Ingénieur DevOps',
    'Data Analyst',
    'Data Scientist',
    'Chef de projet',
    'Scrum Master',
    'Product Owner',
    'Responsable RH',
    'Chargé de recrutement',
    'Responsable formation',
    'Manager équipe',
    'Technicien support',
    'Administrateur système',
    'Administrateur réseau',
    'QA Tester',
    'Business Analyst'
  ];

  competencesDisponibles: string[] = [
    'Communication',
    'Leadership',
    'Travail en équipe',
    'Gestion de projet',
    'Analyse fonctionnelle',
    'Résolution de problèmes',
    'Autonomie',
    'Adaptabilité',
    'Organisation',
    'Esprit critique',
    'Gestion du temps',
    'Prise de décision',
    'Créativité',
    'Relation client',
    'Documentation',
    'Encadrement équipe',
    'Méthodes Agile',
    'Scrum',
    'Conception logicielle',
    'Architecture logicielle'
  ];

  technologiesDisponibles: string[] = [
    'Angular',
    'TypeScript',
    'JavaScript',
    'HTML',
    'CSS',
    'SCSS',
    'Java',
    'Spring Boot',
    'Spring Security',
    'Keycloak',
    'Python',
    'FastAPI',
    'Flask',
    'Node.js',
    'Express.js',
    'PostgreSQL',
    'MySQL',
    'MongoDB',
    'Docker',
    'Kubernetes',
    'Git',
    'GitHub',
    'GitLab',
    'CI/CD',
    'REST API',
    'Microservices',
    'Machine Learning',
    'NLP',
    'Power BI'
  ];

  niveauxEtudeDisponibles: string[] = [
    'Bac',
    'Bac+2',
    'Bac+3',
    'Licence',
    'Bac+5',
    'Master',
    'Ingénieur',
    'Doctorat',
    'Certification professionnelle'
  ];

  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private recrutementService: RecrutementService,
    private candidatureService: CandidatureService,
    private employeService: EmployeService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadCurrentUser();

    this.routeSub = this.route.data.subscribe(data => {
      this.recrutementMode =
        data['recrutementMode'] || 'EMPLOYE_RECRUTEMENT';

      this.loadCurrentEmployeAndData();
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  // =========================
  // INIT
  // =========================

  private initForms(): void {
    this.offreForm = this.fb.group({
      titrePoste: ['', [Validators.required]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      departement: [''],
      typeContrat: ['CDI'],
      localisation: [''],

      competencesRequises: [[], Validators.required],
      technologiesRequises: [[], Validators.required],

      experienceMin: [0],
      niveauEtude: [''],
      dateExpiration: [''],
      salairePropose: [0]
    });

  this.candidatureForm = this.fb.group({
  motivation: ['', [Validators.required]]
});

    this.decisionForm = this.fb.group({
      commentaire: ['']
    });
  }

  private loadCurrentEmployeAndData(): void {
    if (this.isAdminMode()) {
      this.loadData();
      return;
    }

    this.loadCurrentEmploye(() => {
      this.loadData();
    });
  }

  private loadData(): void {
    if (this.isAdminMode()) {
      this.loadAdminOffres();
      return;
    }

    this.loadEmployeeOffres();

    if (this.currentEmployeId) {
      this.loadMesCandidatures();
    } else {
      console.warn('Aucun employeId trouvé pour l’utilisateur connecté.');
      this.mesCandidatures = [];
    }
  }

  private loadCurrentUser(): void {
    const possibleKeys = [
      'currentUser',
      'user',
      'authUser',
      'connectedUser',
      'employe',
      'employee',
      'USER',
      'AUTH_USER'
    ];

    for (const key of possibleKeys) {
      const rawValue = localStorage.getItem(key);

      if (!rawValue) {
        continue;
      }

      try {
        const parsedUser = JSON.parse(rawValue);

        const employeId =
          parsedUser?.employeId ||
          parsedUser?.employeeId ||
          parsedUser?.idEmploye ||
          parsedUser?.id_employee ||
          parsedUser?.employe?.id ||
          parsedUser?.employee?.id ||
          parsedUser?.id;

        if (employeId && !isNaN(Number(employeId))) {
          this.currentUser = parsedUser;
          this.currentEmployeId = Number(employeId);
          return;
        }
      } catch (error) {
        console.warn(`Impossible de lire la clé localStorage ${key}`, error);
      }
    }

    const rawEmployeId =
      localStorage.getItem('employeId') ||
      localStorage.getItem('employeeId') ||
      localStorage.getItem('idEmploye');

    if (rawEmployeId && !isNaN(Number(rawEmployeId))) {
      this.currentEmployeId = Number(rawEmployeId);
      return;
    }

    this.currentEmployeId = null;
  }

 private loadCurrentEmploye(callback?: () => void): void {
  this.employeService.getMonProfil().subscribe({
    next: response => {
      console.log('Réponse complète /api/employes/mon-profil:', response);

      const profil = response?.data || response;

      console.log('Profil utilisé:', profil);
      console.log('Clés du profil:', Object.keys(profil || {}));

      const employeId =
        profil?.id ??
        profil?.employeId ??
        profil?.employeeId ??
        profil?.idEmploye ??
        profil?.id_employee ??
        profil?.userId ??
        profil?.utilisateurId ??
        profil?.employe?.id ??
        profil?.employee?.id ??
        profil?.user?.id;

      if (employeId !== null && employeId !== undefined && !isNaN(Number(employeId))) {
        this.currentUser = profil;
        this.currentEmployeId = Number(employeId);

        localStorage.setItem('employeId', String(this.currentEmployeId));
        localStorage.setItem('currentUser', JSON.stringify(profil));

        console.log('ID employé récupéré:', this.currentEmployeId);

        callback?.();
        return;
      }

      this.currentEmployeId = null;

      console.error(
        'ID employé introuvable. Vérifie les clés affichées ci-dessus.',
        profil
      );

      callback?.();
    },
    error: err => {
      console.error('Erreur GET /api/employes/mon-profil:', err);

      this.currentEmployeId = null;

      alert(
        'Impossible de récupérer votre profil employé.\n\n' +
        `Status HTTP: ${err?.status}\n` +
        `Message: ${err?.error?.message || err?.message || 'Erreur inconnue'}`
      );

      callback?.();
    }
  });
}

  // =========================
  // MODE
  // =========================

  isAdminMode(): boolean {
    return this.recrutementMode === 'ADMIN_RECRUTEMENT';
  }

  isEmployeeMode(): boolean {
    return this.recrutementMode === 'EMPLOYE_RECRUTEMENT';
  }

  // =========================
  // ADMIN - OFFRES
  // =========================

  loadAdminOffres(): void {
    this.loading = true;

    this.recrutementService.getAllOffres().subscribe({
      next: offres => {
        this.offres = offres || [];
        this.applyFilters();
        this.loading = false;

        if (this.offres.length > 0 && !this.selectedOffre) {
          this.selectOffre(this.offres[0]);
        }
      },
      error: err => {
        console.error('Erreur offres admin', err);
        this.loading = false;
      }
    });
  }

  openCreateOffreForm(): void {
    this.editingOffre = null;
    this.showOffreForm = true;

    this.offreForm.reset({
      titrePoste: '',
      description: '',
      departement: '',
      typeContrat: 'CDI',
      localisation: '',
      competencesRequises: [],
      technologiesRequises: [],
      experienceMin: 0,
      niveauEtude: '',
      dateExpiration: '',
      salairePropose: 0
    });
  }

  openEditOffreForm(offre: OffreRecrutement): void {
    this.editingOffre = offre;
    this.showOffreForm = true;

    console.log('Offre reçue pour modification:', offre);
    console.log('Salaire proposé reçu:', offre.salairePropose);

    this.offreForm.patchValue({
      titrePoste: offre.titrePoste || '',
      description: offre.description || '',
      departement: offre.departement || '',
      typeContrat: offre.typeContrat || 'CDI',
      localisation: offre.localisation || '',
      competencesRequises: offre.competencesRequises || [],
      technologiesRequises: offre.technologiesRequises || [],
      experienceMin: offre.experienceMin ?? 0,
      niveauEtude: offre.niveauEtude || '',
      dateExpiration: this.toDateInputValue(offre.dateExpiration),
      salairePropose: offre.salairePropose ?? 0
    });

    console.log(
      'Valeur form salaire après patch:',
      this.offreForm.get('salairePropose')?.value
    );
  }

  closeOffreForm(): void {
    this.showOffreForm = false;
    this.editingOffre = null;
    this.offreForm.reset();
  }

  saveOffre(mode: 'BROUILLON' | 'PUBLIER' = 'BROUILLON'): void {
    if (this.offreForm.invalid) {
      this.offreForm.markAllAsTouched();
      return;
    }

    this.saving = true;

    const value = this.offreForm.value;

    const payload = {
      titrePoste: value.titrePoste,
      description: value.description,
      departement: value.departement,
      typeContrat: value.typeContrat,
      localisation: value.localisation,
      competencesRequises: this.normalizeArrayValue(value.competencesRequises),
      technologiesRequises: this.normalizeArrayValue(value.technologiesRequises),
      experienceMin: Number(value.experienceMin || 0),
      niveauEtude: value.niveauEtude,
      dateExpiration: value.dateExpiration || undefined,
      salairePropose: value.salairePropose
    };

    const request$ = this.editingOffre?.id
      ? this.recrutementService.updateOffre(this.editingOffre.id, payload)
      : this.recrutementService.createOffre(payload);

    request$.subscribe({
      next: saved => {
        if (!saved?.id) {
          this.saving = false;
          alert("Erreur : l'offre n'a pas été enregistrée.");
          return;
        }

        const targetStatut: StatutOffreRecrutement =
          mode === 'PUBLIER' ? 'OUVERTE' : 'BROUILLON';

        console.log('Changement statut demandé :', {
          id: saved.id,
          targetStatut
        });

        this.recrutementService.changerStatutOffre(saved.id, targetStatut).subscribe({
          next: updated => {
            console.log('Statut changé avec succès :', updated);

            this.saving = false;
            this.closeOffreForm();
            this.loadAdminOffres();
          },
          error: err => {
            console.error('Erreur changement statut complète :', err);

            this.saving = false;

            alert(
              "L'offre est enregistrée, mais le changement de statut a échoué.\n\n" +
              `Status HTTP: ${err?.status}\n` +
              `Message: ${err?.error?.message || err?.message || 'Erreur inconnue'}`
            );

            this.loadAdminOffres();
          }
        });
      },
      error: err => {
        console.error('Erreur sauvegarde offre:', err);
        this.saving = false;
        alert("Erreur lors de l'enregistrement de l'offre.");
      }
    });
  }

  changerStatut(offre: OffreRecrutement, statut: StatutOffreRecrutement): void {
    if (!offre.id) return;

    this.recrutementService.changerStatutOffre(offre.id, statut).subscribe({
      next: updated => {
        if (updated) {
          offre.statut = updated.statut;
          this.applyFilters();
        }
      },
      error: err => console.error('Erreur changement statut', err)
    });
  }

  deleteOffre(offre: OffreRecrutement): void {
    if (!offre.id) return;

    const confirmed = confirm(
      `Voulez-vous vraiment supprimer l'offre "${offre.titrePoste}" ?`
    );

    if (!confirmed) return;

    this.recrutementService.deleteOffre(offre.id).subscribe({
      next: deleted => {
        if (deleted) {
          this.selectedOffre = null;
          this.loadAdminOffres();
        }
      },
      error: err => console.error('Erreur suppression offre', err)
    });
  }

  // =========================
  // ADMIN - CANDIDATURES
  // =========================

  selectOffre(offre: OffreRecrutement): void {
    this.selectedOffre = offre;
    this.selectedCandidature = null;

    if (this.isAdminMode() && offre.id) {
      this.loadCandidaturesByOffre(offre.id);
      this.loadTopCandidatures(offre.id);
    }
  }

  loadCandidaturesByOffre(offreId: number): void {
    this.candidatureService.getCandidaturesByOffre(offreId).subscribe({
      next: candidatures => {
        this.candidatures = candidatures || [];
      },
      error: err => console.error('Erreur candidatures offre', err)
    });
  }

  loadTopCandidatures(offreId: number): void {
    this.candidatureService.getTopCandidatures(offreId, 5).subscribe({
      next: top => {
        this.topCandidatures = top || [];
      },
      error: err => console.error('Erreur top candidatures', err)
    });
  }

  selectCandidature(candidature: Candidature): void {
  if (this.selectedCandidature?.id === candidature.id) {
    this.selectedCandidature = null;
    this.decisionForm.reset({
      commentaire: ''
    });
    return;
  }

  this.selectedCandidature = candidature;
  this.decisionForm.reset({
    commentaire: ''
  });
}

  accepterCandidature(candidature: Candidature): void {
  if (!candidature.id) {
    return;
  }

  if (!this.canAccepterCandidature(candidature)) {
    alert(this.getDecisionLockMessage(candidature));
    return;
  }

  const commentaire = this.decisionForm.value.commentaire || '';

  this.candidatureService.accepterCandidature(candidature.id, {
    commentaire
  }).subscribe({
    next: updated => {
      if (updated) {
        this.selectedCandidature = updated;

        if (this.selectedOffre) {
          this.selectedOffre.statut = 'FERMEE' as StatutOffreRecrutement;
        }

        if (this.selectedOffre?.id) {
          this.loadCandidaturesByOffre(this.selectedOffre.id);
          this.loadTopCandidatures(this.selectedOffre.id);
        }

        this.loadAdminOffres();
      }
    },
    error: err => {
      console.error('Erreur acceptation candidature', err);
      alert(
        err?.error?.message ||
        err?.error ||
        "Erreur lors de l'acceptation de la candidature."
      );
    }
  });
}
 refuserCandidature(candidature: Candidature): void {
  if (!candidature.id) {
    return;
  }

  if (!this.canRefuserCandidature(candidature)) {
    alert(this.getDecisionLockMessage(candidature));
    return;
  }

  const commentaire = this.decisionForm.value.commentaire || '';

  this.candidatureService.refuserCandidature(candidature.id, {
    commentaire
  }).subscribe({
    next: updated => {
      if (updated) {
        this.selectedCandidature = updated;

        if (this.selectedOffre?.id) {
          this.loadCandidaturesByOffre(this.selectedOffre.id);
          this.loadTopCandidatures(this.selectedOffre.id);
        }
      }
    },
    error: err => {
      console.error('Erreur refus candidature', err);
      alert(
        err?.error?.message ||
        err?.error ||
        'Erreur lors du refus de la candidature.'
      );
    }
  });
}

  relancerAnalyseIa(candidature: Candidature): void {
    if (!candidature.id) return;

    this.analysing = true;

    this.candidatureService.relancerAnalyseIa(candidature.id).subscribe({
      next: updated => {
        this.analysing = false;

        if (updated) {
          this.selectedCandidature = updated;

          if (this.selectedOffre?.id) {
            this.loadCandidaturesByOffre(this.selectedOffre.id);
            this.loadTopCandidatures(this.selectedOffre.id);
          }
        }
      },
      error: err => {
        console.error('Erreur relance IA', err);
        this.analysing = false;
      }
    });
  }

  // =========================
  // EMPLOYÉ
  // =========================

  loadEmployeeOffres(): void {
    this.loading = true;

    this.recrutementService.getOffresOuvertes().subscribe({
      next: offres => {
        this.offres = offres || [];
        this.applyFilters();
        this.loading = false;
      },
      error: err => {
        console.error('Erreur offres employé', err);
        this.loading = false;
      }
    });
  }

  loadMesCandidatures(): void {
    if (!this.currentEmployeId) {
      console.warn('Impossible de charger les candidatures : employeId introuvable.');
      this.mesCandidatures = [];
      return;
    }

    this.candidatureService.getMesCandidatures(this.currentEmployeId).subscribe({
      next: candidatures => {
        this.mesCandidatures = candidatures || [];
      },
      error: err => console.error('Erreur mes candidatures', err)
    });
  }

  openPostulerForm(offre: OffreRecrutement): void {
  this.loadCurrentEmploye(() => {
    if (!this.currentEmployeId) {
      alert('Impossible de récupérer votre identifiant employé. Veuillez vous reconnecter.');
      return;
    }

    this.selectedOffre = offre;
    this.showPostulerForm = true;
    this.selectedCvFile = null;
    this.cvFileName = '';

    this.candidatureForm.reset({
      motivation: ''
    });
  });
}
  closePostulerForm(): void {
    this.showPostulerForm = false;
    this.selectedOffre = null;
    this.selectedCvFile = null;
    this.cvFileName = '';
    this.candidatureForm.reset();
  }

  onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;

    if (!file) {
      this.selectedCvFile = null;
      this.cvFileName = '';
      return;
    }

    const allowedTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/msword'
    ];

    if (!allowedTypes.includes(file.type)) {
      alert('Format non supporté. Merci de choisir un CV PDF, DOC ou DOCX.');
      input.value = '';
      this.selectedCvFile = null;
      this.cvFileName = '';
      return;
    }

    this.selectedCvFile = file;
    this.cvFileName = file.name;
  }

  submitCandidature(): void {
  if (!this.selectedOffre?.id) {
    return;
  }

  this.loadCurrentEmploye(() => {
    if (!this.currentEmployeId) {
      alert('Impossible de récupérer votre identifiant employé. Veuillez vous reconnecter.');
      return;
    }

   if (this.candidatureForm.invalid) {
  this.candidatureForm.markAllAsTouched();

  const motivation = this.candidatureForm.get('motivation')?.value || '';

  if (!motivation.trim()) {
    alert('Veuillez saisir une motivation.');
    return;
  }

  if (motivation.trim().length < 5) {
    alert('La motivation doit contenir au moins 5 caractères.');
    return;
  }

  alert('Veuillez vérifier les champs du formulaire.');
  return;
}

    if (!this.selectedCvFile) {
      alert('Merci de joindre votre CV.');
      return;
    }

    this.saving = true;

    this.candidatureService.postuler(
      this.selectedOffre!.id!,
      this.currentEmployeId,
      this.candidatureForm.value.motivation,
      this.selectedCvFile
    ).subscribe({
      next: candidature => {
        this.saving = false;

        if (candidature) {
          this.closePostulerForm();
          this.loadMesCandidatures();
          alert('Candidature envoyée avec succès.');
        }
      },
      error: err => {
        console.error('Erreur postulation', err);
        this.saving = false;
        alert('Erreur lors de la candidature.');
      }
    });
  });
}
  hasApplied(offreId?: number): boolean {
    if (!offreId) return false;

    return this.mesCandidatures.some(c => c.offreId === offreId);
  }

  getCandidatureForOffre(offreId?: number): Candidature | undefined {
    if (!offreId) return undefined;

    return this.mesCandidatures.find(c => c.offreId === offreId);
  }

  // =========================
  // FILTERS
  // =========================

  applyFilters(): void {
    const search = this.searchTerm.toLowerCase().trim();

    this.offresFiltrees = this.offres.filter(offre => {
      const matchesSearch =
        !search ||
        offre.titrePoste?.toLowerCase().includes(search) ||
        offre.description?.toLowerCase().includes(search) ||
        offre.departement?.toLowerCase().includes(search) ||
        (offre.competencesRequises || []).some(c =>
          c.toLowerCase().includes(search)
        ) ||
        (offre.technologiesRequises || []).some(t =>
          t.toLowerCase().includes(search)
        );

      const matchesStatut =
        this.statutFilter === 'ALL' || offre.statut === this.statutFilter;

      return matchesSearch && matchesStatut;
    });
  }

  onSearchChange(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value || '';
    this.applyFilters();
  }

  onStatutFilterChange(event: Event): void {
    this.statutFilter = ((event.target as HTMLSelectElement).value ||
      'ALL') as any;

    this.applyFilters();
  }

  // =========================
  // HELPERS
  // =========================

  textToArray(value: string): string[] {
    if (!value) return [];

    return value
      .split(',')
      .map(item => item.trim())
      .filter(item => !!item);
  }

  arrayToText(value?: string[]): string {
    return (value || []).join(', ');
  }

  normalizeArrayValue(value: string[] | string | null | undefined): string[] {
    if (!value) return [];

    if (Array.isArray(value)) {
      return value
        .map(item => String(item).trim())
        .filter(item => !!item);
    }

    return this.textToArray(value);
  }

  getScoreColor(score?: number): string {
    const value = score || 0;

    if (value >= 85) return 'score-excellent';
    if (value >= 70) return 'score-good';
    if (value >= 50) return 'score-medium';

    return 'score-low';
  }

  getScoreLabel(score?: number): string {
    const value = score || 0;

    if (value >= 85) return 'Excellent';
    if (value >= 70) return 'Très bon';
    if (value >= 50) return 'Moyen';

    return 'Faible';
  }

  getStatutClass(statut?: string): string {
    switch (statut) {
      case 'OUVERTE':
      case 'ACCEPTEE':
      case 'ANALYSEE':
        return 'status-success';

      case 'EN_ANALYSE':
      case 'SOUMISE':
        return 'status-warning';

      case 'FERMEE':
      case 'REFUSEE':
        return 'status-danger';

      case 'BROUILLON':
        return 'status-muted';

      default:
        return 'status-muted';
    }
  }

  formatDate(date?: string): string {
    if (!date) return '-';

    return new Date(date).toLocaleDateString('fr-FR');
  }

  private toDateInputValue(date?: string): string {
    if (!date) return '';

    return date.substring(0, 10);
  }

  getOffreStatutLabel(statut?: string): string {
    switch (statut) {
      case 'BROUILLON':
        return 'Brouillon';

      case 'OUVERTE':
        return 'Publiée';

      case 'FERMEE':
        return 'Fermée';

      default:
        return statut || '-';
    }
  }

private isStatutFinal(statut?: string): boolean {
  return statut === 'ACCEPTEE' || statut === 'REFUSEE';
}

hasAcceptedCandidatureForSelectedOffre(): boolean {
  return this.candidatures.some(c => c.statut === 'ACCEPTEE');
}

isSelectedOffreClosed(): boolean {
  return this.selectedOffre?.statut === 'FERMEE';
}

canAccepterCandidature(candidature: Candidature): boolean {
  if (!candidature?.id) {
    return false;
  }

  if (this.isStatutFinal(candidature.statut)) {
    return false;
  }

  if (this.isSelectedOffreClosed()) {
    return false;
  }

  if (this.hasAcceptedCandidatureForSelectedOffre()) {
    return false;
  }

  return true;
}

canRefuserCandidature(candidature: Candidature): boolean {
  if (!candidature?.id) {
    return false;
  }

  if (this.isStatutFinal(candidature.statut)) {
    return false;
  }

  if (this.isSelectedOffreClosed()) {
    return false;
  }

  if (this.hasAcceptedCandidatureForSelectedOffre()) {
    return false;
  }

  return true;
}

canRelancerAnalyse(candidature: Candidature): boolean {
  if (!candidature?.id) {
    return false;
  }

  if (this.isStatutFinal(candidature.statut)) {
    return false;
  }

  if (this.isSelectedOffreClosed()) {
    return false;
  }

  return true;
}

getDecisionLockMessage(candidature: Candidature): string {
  if (candidature.statut === 'ACCEPTEE') {
    return 'Cette candidature est déjà acceptée.';
  }

  if (candidature.statut === 'REFUSEE') {
    return 'Cette candidature est déjà refusée.';
  }

  if (this.isSelectedOffreClosed()) {
    return 'Cette offre est fermée. Les décisions ne sont plus modifiables.';
  }

  if (this.hasAcceptedCandidatureForSelectedOffre()) {
    return 'Une candidature est déjà acceptée pour cette offre.';
  }

  return '';
}





}