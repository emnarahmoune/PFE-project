import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import {
  Evaluation,
  EvaluationRequest,
  EvaluationStats,
  EmployeEquipeEvaluation
} from '../../core/models/evaluation.model';

import { EvaluationService } from '../../core/services/evaluation.service';
import { EmployeService } from '../../core/services/employe.service';
import { ManagerService } from '../../core/services/manager.service';

import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

type EvaluationMode =
  | 'ADMIN_LISTE'
  | 'MANAGER_LISTE'
  | 'EMPLOYE_MES_EVALUATIONS';

type EvaluationStatutForm = 'BROUILLON' | 'PUBLIEE';

@Component({
  selector: 'app-evaluations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './evaluations.component.html',
  styleUrls: ['./evaluations.component.scss']
})
export class EvaluationsComponent implements OnInit {
  mode: EvaluationMode = 'ADMIN_LISTE';

  evaluations: Evaluation[] = [];
  stats: EvaluationStats | any = null;

  managers: any[] = [];
  equipe: EmployeEquipeEvaluation[] = [];

  loading = false;
  loadingEquipe = false;
  saving = false;

  errorMessage = '';
  successMessage = '';

  showEvaluationModal = false;
  showForm = false;

  editingEvaluationId: number | null = null;

  newEvaluation: any = this.getEmptyAdminForm();
  form: EvaluationRequest = this.getEmptyManagerForm();

  constructor(
    private route: ActivatedRoute,
    private evaluationService: EvaluationService,
    private employeService: EmployeService,
    private managerService: ManagerService
  ) {}

  ngOnInit(): void {
    this.mode = this.route.snapshot.data['evaluationMode'] || 'ADMIN_LISTE';

    this.loadData();

    if (this.isAdminMode()) {
      this.loadManagers();
    }

    if (this.isManagerMode()) {
      this.loadEquipe();
    }
  }

  // =========================
  // MODES
  // =========================

  isAdminMode(): boolean {
    return this.mode === 'ADMIN_LISTE';
  }

  isManagerMode(): boolean {
    return this.mode === 'MANAGER_LISTE';
  }

  isEmployeeMode(): boolean {
    return this.mode === 'EMPLOYE_MES_EVALUATIONS';
  }

  // =========================
  // LOAD DATA
  // =========================

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    if (this.isAdminMode()) {
      this.loadAdminData();
      return;
    }

    if (this.isManagerMode()) {
      this.loadManagerData();
      return;
    }

    this.loadEmployeeData();
  }

  private loadAdminData(): void {
    this.evaluationService.getAdminStats().subscribe({
      next: stats => {
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats admin:', error);
      }
    });

    this.evaluationService.getAdminEvaluations().subscribe({
      next: data => {
        this.evaluations = data || [];
        this.loading = false;
      },
      error: error => {
        console.error('Erreur évaluations admin:', error);
        this.errorMessage = 'Impossible de charger les évaluations.';
        this.loading = false;
      }
    });
  }

  private loadManagerData(): void {
    this.evaluationService.getManagerStats().subscribe({
      next: stats => {
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats manager:', error);
      }
    });

    this.evaluationService.getManagerEvaluations().subscribe({
      next: data => {
        this.evaluations = data || [];
        this.loading = false;
      },
      error: error => {
        console.error('Erreur liste évaluations manager:', error);
        this.errorMessage = 'Impossible de charger les évaluations manager.';
        this.loading = false;
      }
    });
  }

  private loadEmployeeData(): void {
    this.evaluationService.getMyStats().subscribe({
      next: stats => {
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats employé:', error);
      }
    });

    this.evaluationService.getMyEvaluations().subscribe({
      next: data => {
        this.evaluations = data || [];
        this.loading = false;
      },
      error: error => {
        console.error('Erreur évaluations employé:', error);
        this.errorMessage = 'Impossible de charger vos évaluations.';
        this.loading = false;
      }
    });
  }

  // =========================
  // ADMIN RH
  // =========================

  loadManagers(): void {
    this.employeService.getAllManagers().subscribe({
      next: (res: any) => {
        this.managers =
          Array.isArray(res?.data) ? res.data :
          Array.isArray(res?.data?.content) ? res.data.content :
          Array.isArray(res?.content) ? res.content :
          Array.isArray(res) ? res :
          [];
      },
      error: err => {
        console.error('Erreur chargement managers:', err);
        this.managers = [];
      }
    });
  }

  openEvaluationModal(): void {
    this.editingEvaluationId = null;
    this.errorMessage = '';
    this.successMessage = '';
    this.resetForm();
    this.showEvaluationModal = true;
  }

  closeEvaluationModal(): void {
    if (this.saving) return;

    this.showEvaluationModal = false;
    this.errorMessage = '';
  }

  continueEvaluation(evaluation: Evaluation | any): void {
    if (!evaluation.id) {
      this.errorMessage = 'Impossible de continuer cette évaluation : ID introuvable.';
      return;
    }

    this.editingEvaluationId = evaluation.id;
    this.errorMessage = '';
    this.successMessage = '';

    this.newEvaluation = {
      employeId: evaluation.employeId ?? null,
      periode: evaluation.periode ?? '',
      dateEvaluation: this.toInputDate(evaluation.dateEvaluation),

      note: Number(evaluation.note ?? evaluation.noteGlobale ?? 0),
      noteGlobale: Number(evaluation.noteGlobale ?? evaluation.note ?? 0),
      noteTechnique: Number(evaluation.noteTechnique ?? 0),
      noteCommunication: Number(evaluation.noteCommunication ?? 0),
      noteLeadership: Number(evaluation.noteLeadership ?? 0),
      notePonctualite: Number(evaluation.notePonctualite ?? 0),
      noteProductivite: Number(evaluation.noteProductivite ?? 0),

      objectifsAtteints: Number(evaluation.objectifsAtteints ?? evaluation.objectifs_atteints ?? 0),
      objectifs: evaluation.objectifs ?? '',

      pointsForts: evaluation.pointsForts ?? '',
      axesAmelioration: evaluation.axesAmelioration ?? '',
      commentaire: evaluation.commentaire ?? '',
      commentaireManager: evaluation.commentaireManager ?? evaluation.commentaire ?? '',

      statut: evaluation.statut ?? 'BROUILLON'
    };

    this.showEvaluationModal = true;
  }

  saveEvaluation(statut: EvaluationStatutForm = 'PUBLIEE'): void {
    if (this.isManagerMode()) {
      this.saveManagerEvaluation(statut);
      return;
    }

    this.saveAdminEvaluation(statut);
  }

  private saveAdminEvaluation(statut: EvaluationStatutForm): void {
    this.errorMessage = '';
    this.successMessage = '';

    const managerId = Number(this.newEvaluation.employeId);
    const note = Number(this.newEvaluation.note);

    if (!managerId || managerId <= 0) {
      this.errorMessage = 'Veuillez sélectionner un manager.';
      return;
    }

    if (!this.newEvaluation.periode || this.newEvaluation.periode.trim() === '') {
      this.errorMessage = 'Veuillez saisir la période.';
      return;
    }

    if (Number.isNaN(note) || note <= 0 || note > 10) {
      this.errorMessage = 'La note globale est obligatoire et doit être comprise entre 1 et 10.';
      return;
    }

    const payload: any = {
      employeId: managerId,
      periode: this.newEvaluation.periode.trim(),
      dateEvaluation: this.newEvaluation.dateEvaluation || new Date().toISOString().substring(0, 10),

      note,
      noteGlobale: note,
      noteTechnique: Number(this.newEvaluation.noteTechnique || 0),
      noteCommunication: Number(this.newEvaluation.noteCommunication || 0),
      noteLeadership: Number(this.newEvaluation.noteLeadership || 0),
      notePonctualite: Number(this.newEvaluation.notePonctualite || 0),
      noteProductivite: Number(this.newEvaluation.noteProductivite || 0),

      objectifsAtteints: Number(this.newEvaluation.objectifsAtteints || 0),
      objectifs: this.newEvaluation.objectifs || '',

      pointsForts: this.newEvaluation.pointsForts || '',
      axesAmelioration: this.newEvaluation.axesAmelioration || '',
      commentaire: this.newEvaluation.commentaire || '',
      commentaireManager: this.newEvaluation.commentaireManager || this.newEvaluation.commentaire || '',

      statut
    };

    this.saving = true;

    const request$ = this.editingEvaluationId
      ? this.evaluationService.updateAdminEvaluation(this.editingEvaluationId, payload)
      : this.evaluationService.createAdminManagerEvaluation(payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = statut === 'BROUILLON'
          ? 'Évaluation enregistrée en brouillon.'
          : 'Évaluation publiée avec succès.';

        this.closeEvaluationModal();
        this.editingEvaluationId = null;
        this.resetForm();
        this.loadData();
      },
      error: err => {
        console.error('Erreur enregistrement évaluation:', err);
        this.saving = false;
        this.errorMessage =
          err?.error?.message ||
          err?.error?.error ||
          err?.error?.data?.message ||
          'Impossible d’enregistrer l’évaluation.';
      }
    });
  }

  resetForm(): void {
    this.newEvaluation = this.getEmptyAdminForm();
  }

  private getEmptyAdminForm(): any {
    return {
      employeId: null,
      periode: '',
      dateEvaluation: new Date().toISOString().substring(0, 10),

      note: 0,
      noteGlobale: 0,
      noteTechnique: 0,
      noteCommunication: 0,
      noteLeadership: 0,
      notePonctualite: 0,
      noteProductivite: 0,

      objectifsAtteints: 0,
      objectifs: '',

      pointsForts: '',
      axesAmelioration: '',
      commentaire: '',
      commentaireManager: '',

      statut: 'PUBLIEE'
    };
  }

  // =========================
  // MANAGER
  // =========================

  loadEquipe(): void {
    this.loadingEquipe = true;
    this.errorMessage = '';

    this.managerService.getEquipe().subscribe({
      next: (response: any) => {
        if (Array.isArray(response)) {
          this.equipe = response;
        } else if (Array.isArray(response?.data)) {
          this.equipe = response.data;
        } else if (Array.isArray(response?.content)) {
          this.equipe = response.content;
        } else {
          this.equipe = [];
        }

        this.loadingEquipe = false;
      },
      error: error => {
        console.error('Erreur chargement équipe = ', error);
        this.loadingEquipe = false;
        this.errorMessage = 'Impossible de charger la liste des employés de votre équipe.';
      }
    });
  }

  openCreateForm(): void {
    this.editingEvaluationId = null;
    this.form = this.getEmptyManagerForm();
    this.showForm = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  editEvaluation(evaluation: Evaluation): void {
    if (this.getStatut(evaluation) === 'PUBLIEE') {
      this.errorMessage = 'Une évaluation publiée ne peut plus être modifiée.';
      return;
    }

    if (!evaluation.id) {
      this.errorMessage = 'Impossible de modifier cette évaluation : ID introuvable.';
      return;
    }

    this.editingEvaluationId = evaluation.id;
    this.errorMessage = '';
    this.successMessage = '';

    this.evaluationService.getEvaluationById(evaluation.id).subscribe({
      next: response => {
        const fullEvaluation = this.extractEvaluationPayload(response) || evaluation;
        this.fillFormForEdit(fullEvaluation);
        this.showForm = true;
      },
      error: error => {
        console.error('Erreur chargement détail évaluation:', error);
        this.fillFormForEdit(evaluation);
        this.showForm = true;
      }
    });
  }

  private saveManagerEvaluation(statut: EvaluationStatutForm = 'PUBLIEE'): void {
    this.errorMessage = '';
    this.successMessage = '';

    const employeId = Number(this.form.employeId);
    const noteGlobale = Number(this.form.noteGlobale);

    if (!employeId || employeId <= 0) {
      this.errorMessage = 'Veuillez sélectionner un employé à évaluer.';
      return;
    }

    if (!this.form.periode || this.form.periode.trim() === '') {
      this.errorMessage = 'Veuillez saisir la période de l’évaluation.';
      return;
    }

    if (Number.isNaN(noteGlobale) || noteGlobale <= 0 || noteGlobale > 10) {
      this.errorMessage = 'La note globale est obligatoire et doit être comprise entre 1 et 10.';
      return;
    }

    const payload: EvaluationRequest = {
      employeId,
      periode: this.form.periode.trim(),
      dateEvaluation: this.form.dateEvaluation || new Date().toISOString().substring(0, 10),

      note: noteGlobale,
      noteGlobale,

      noteTechnique: Number(this.form.noteTechnique || 0),
      noteCommunication: Number(this.form.noteCommunication || 0),
      noteLeadership: Number(this.form.noteLeadership || 0),
      notePonctualite: Number(this.form.notePonctualite || 0),
      noteProductivite: Number(this.form.noteProductivite || 0),

      objectifsAtteints: Number(this.form.objectifsAtteints || 0),
      objectifs: this.form.objectifs || '',

      pointsForts: this.form.pointsForts || '',
      axesAmelioration: this.form.axesAmelioration || '',
      commentaireManager: this.form.commentaireManager || '',

      statut
    };

    this.saving = true;

    const request$ = this.editingEvaluationId
      ? this.evaluationService.updateManagerEvaluation(this.editingEvaluationId, payload)
      : this.evaluationService.createManagerEvaluation(payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = statut === 'BROUILLON'
          ? 'Évaluation enregistrée en brouillon.'
          : 'Évaluation publiée avec succès.';

        this.showForm = false;
        this.editingEvaluationId = null;
        this.form = this.getEmptyManagerForm();

        this.loadData();
      },
      error: error => {
        console.error('Erreur backend:', error);
        this.saving = false;

        this.errorMessage =
          error?.error?.message ||
          error?.error?.error ||
          'Erreur lors de l’enregistrement de l’évaluation.';
      }
    });
  }

  deleteEvaluation(evaluation: Evaluation): void {
    if (!evaluation.id) return;

    const confirmed = confirm('Voulez-vous vraiment supprimer cette évaluation ?');
    if (!confirmed) return;

    this.evaluationService.deleteManagerEvaluation(evaluation.id).subscribe({
      next: () => {
        this.successMessage = 'Évaluation supprimée.';
        this.loadData();
      },
      error: error => {
        this.errorMessage =
          error?.error?.message ||
          'Impossible de supprimer cette évaluation.';
      }
    });
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingEvaluationId = null;
    this.form = this.getEmptyManagerForm();
    this.errorMessage = '';
  }

  private getEmptyManagerForm(): EvaluationRequest {
    return {
      employeId: 0,
      periode: '',
      dateEvaluation: new Date().toISOString().substring(0, 10),

      note: 0,
      noteGlobale: 0,

      noteTechnique: 0,
      noteCommunication: 0,
      noteLeadership: 0,
      notePonctualite: 0,
      noteProductivite: 0,

      objectifsAtteints: 0,
      objectifs: '',

      pointsForts: '',
      axesAmelioration: '',
      commentaireManager: '',

      statut: 'PUBLIEE'
    };
  }

  private fillFormForEdit(evaluation: any): void {
    const noteGlobale = this.toNumber(
      evaluation.noteGlobale ??
      evaluation.note ??
      evaluation.score ??
      evaluation.moyenne ??
      0
    );

    this.form = {
      employeId: Number(
        evaluation.employeId ??
        evaluation.employeeId ??
        evaluation.idEmploye ??
        evaluation.employe?.id ??
        0
      ),

      periode:
        evaluation.periode ??
        evaluation.period ??
        '',

      dateEvaluation: this.toInputDate(
        evaluation.dateEvaluation ??
        evaluation.date ??
        evaluation.dateCreation ??
        evaluation.createdAt
      ),

      note: noteGlobale,
      noteGlobale,

      noteTechnique: this.toNumber(evaluation.noteTechnique ?? evaluation.technique ?? noteGlobale),
      noteCommunication: this.toNumber(evaluation.noteCommunication ?? evaluation.communication ?? noteGlobale),
      noteLeadership: this.toNumber(evaluation.noteLeadership ?? evaluation.leadership ?? noteGlobale),
      notePonctualite: this.toNumber(evaluation.notePonctualite ?? evaluation.ponctualite ?? noteGlobale),
      noteProductivite: this.toNumber(evaluation.noteProductivite ?? evaluation.productivite ?? noteGlobale),

      objectifsAtteints: this.toNumber(
        evaluation.objectifsAtteints ??
        evaluation.objectifs_atteints ??
        0
      ),
      objectifs: evaluation.objectifs ?? '',

      pointsForts: evaluation.pointsForts ?? evaluation.points_forts ?? '',
      axesAmelioration: evaluation.axesAmelioration ?? evaluation.axes_amelioration ?? '',
      commentaireManager: evaluation.commentaireManager ?? evaluation.commentaire_manager ?? '',

      statut: evaluation.statut ?? evaluation.status ?? 'PUBLIEE'
    };
  }

  private extractEvaluationPayload(response: any): any {
    if (!response) return null;

    if (
      response.id !== undefined ||
      response.pointsForts !== undefined ||
      response.axesAmelioration !== undefined ||
      response.commentaireManager !== undefined ||
      response.objectifs !== undefined
    ) {
      return response;
    }

    if (response.data) return this.extractEvaluationPayload(response.data);
    if (response.result) return this.extractEvaluationPayload(response.result);
    if (response.evaluation) return this.extractEvaluationPayload(response.evaluation);
    if (response.body) return this.extractEvaluationPayload(response.body);

    return response;
  }

  // =========================
  // STATS
  // =========================

  getStatsTotal(): number {
    if (this.isEmployeeMode()) {
      return this.evaluations.length || Number(this.stats?.totalEvaluations || 0);
    }

    const s: any = this.stats || {};

    return Number(
      s.totalEvaluations ??
      s.total ??
      s.nombreEvaluations ??
      this.evaluations.length ??
      0
    );
  }

  getStatsMoyenne(): number {
    if (this.isEmployeeMode()) {
      const notes: number[] = [];

      for (const evaluation of this.evaluations as any[]) {
        const noteGlobale = this.getGlobalNote(evaluation);

        if (noteGlobale > 0) {
          notes.push(noteGlobale);
          continue;
        }

        const details = [
          this.getNoteTechnique(evaluation),
          this.getNoteCommunication(evaluation),
          this.getNoteLeadership(evaluation),
          this.getNoteProductivite(evaluation)
        ].filter(note => note > 0);

        if (details.length > 0) {
          const moyenneDetails =
            details.reduce((sum, note) => sum + note, 0) / details.length;

          notes.push(moyenneDetails);
        }
      }

      if (notes.length > 0) {
        const moyenne =
          notes.reduce((sum, note) => sum + note, 0) / notes.length;

        return this.normalizeNote(moyenne);
      }
    }

    const s: any = this.stats || {};

    const moyenne = Number(
      s.moyenneGlobale ??
      s.moyenneNote ??
      s.moyenne ??
      s.noteMoyenne ??
      s.averageScore ??
      0
    );

    return Number(moyenne.toFixed ? moyenne.toFixed(1) : moyenne);
  }

  getStatsMeilleureNote(): number {
    const s: any = this.stats || {};

    return Number(
      s.meilleureNote ??
      s.maxNote ??
      0
    );
  }

  getStatsProgression(): number {
    const s: any = this.stats || {};

    return Number(
      s.progression ??
      s.moyenneObjectifs ??
      s.objectifsAtteints ??
      0
    );
  }

  getStatsPubliees(): number {
    if (!this.evaluations || this.evaluations.length === 0) {
      return 0;
    }

    return this.evaluations.filter((evaluation: any) => {
      const statut = this.getStatut(evaluation);
      return statut === 'PUBLIEE' || statut === 'VALIDEE';
    }).length;
  }

  // =========================
  // GETTERS ÉVALUATION
  // =========================

  getGlobalNote(evaluation: any): number {
    return Number(
      evaluation.noteGlobale ||
      evaluation.note ||
      evaluation.score ||
      0
    );
  }

  getRawNote(evaluation: any): number {
    return this.getGlobalNote(evaluation);
  }

  getNote(evaluation: Evaluation | any): number {
    return Number(
      evaluation.noteGlobale ??
      evaluation.note ??
      evaluation.score ??
      0
    );
  }

  getNoteGlobale(evaluation: Evaluation | any): number {
    const e: any = evaluation;

    return Number(
      e.noteGlobale ??
      e.note ??
      e.noteGenerale ??
      e.score ??
      e.moyenne ??
      0
    );
  }

  getNoteSur5(evaluation: any): number {
    return this.convertNoteTo5(this.getRawNote(evaluation));
  }

  getNoteTechnique(evaluation: Evaluation | any): number {
    const e: any = evaluation;

    const note = Number(
      e.noteTechnique ??
      e.technique ??
      e.competenceTechnique ??
      e.note ??
      0
    );

    if (this.isEmployeeMode()) {
      return note > 0 ? this.convertNoteTo5(note) : this.getNoteSur5(evaluation);
    }

    return note;
  }

  getNoteCommunication(evaluation: Evaluation | any): number {
    const e: any = evaluation;

    const note = Number(
      e.noteCommunication ??
      e.communication ??
      e.note ??
      0
    );

    if (this.isEmployeeMode()) {
      return note > 0 ? this.convertNoteTo5(note) : this.getNoteSur5(evaluation);
    }

    return note;
  }

  getNoteLeadership(evaluation: Evaluation | any): number {
    const e: any = evaluation;

    const note = Number(
      e.noteLeadership ??
      e.leadership ??
      e.note ??
      0
    );

    if (this.isEmployeeMode()) {
      return note > 0 ? this.convertNoteTo5(note) : this.getNoteSur5(evaluation);
    }

    return note;
  }

  getNoteProductivite(evaluation: Evaluation | any): number {
    const e: any = evaluation;

    const note = Number(
      e.noteProductivite ??
      e.productivite ??
      e.productivité ??
      e.note ??
      0
    );

    if (this.isEmployeeMode()) {
      return note > 0 ? this.convertNoteTo5(note) : this.getNoteSur5(evaluation);
    }

    return note;
  }

  getObjectifsAtteints(evaluation: Evaluation | any): number {
    return Number(
      evaluation.objectifsAtteints ??
      evaluation.objectifs_atteints ??
      evaluation.progression ??
      evaluation.moyenneObjectifs ??
      0
    );
  }

  getEmployeName(evaluation: Evaluation | any): string {
    const prenom = evaluation.employePrenom || '';
    const nom = evaluation.employeNom || '';
    return `${prenom} ${nom}`.trim() || evaluation.employeEmail || 'Employé';
  }

  getEquipeEmployeName(employe: EmployeEquipeEvaluation): string {
    const prenom = employe.prenom || '';
    const nom = employe.nom || '';
    return `${prenom} ${nom}`.trim() || employe.email || `Employé #${employe.id}`;
  }

  getEvaluateurName(evaluation: Evaluation | any): string {
    const prenom =
      evaluation.evaluateurPrenom ||
      evaluation.managerPrenom ||
      '';

    const nom =
      evaluation.evaluateurNom ||
      evaluation.managerNom ||
      '';

    return `${prenom} ${nom}`.trim() ||
      evaluation.evaluateurEmail ||
      evaluation.managerEmail ||
      'Évaluateur';
  }

  getEvaluateurLabel(evaluation: Evaluation | any): string {
    const role = String(
      evaluation.evaluateurRole ||
      evaluation.managerRole ||
      evaluation.roleEvaluateur ||
      ''
    ).toUpperCase();

    return role.includes('ADMIN') ? 'Admin RH' : 'Manager';
  }

  getManagerName(evaluation: any): string {
    const prenom =
      evaluation.managerPrenom ??
      evaluation.evaluateurPrenom ??
      evaluation.manager?.prenom ??
      evaluation.evaluateur?.prenom ??
      '';

    const nom =
      evaluation.managerNom ??
      evaluation.evaluateurNom ??
      evaluation.manager?.nom ??
      evaluation.evaluateur?.nom ??
      '';

    return `${prenom} ${nom}`.trim() ||
      evaluation.managerEmail ||
      evaluation.evaluateurEmail ||
      evaluation.manager?.email ||
      evaluation.evaluateur?.email ||
      'Manager';
  }

  getPeriode(evaluation: Evaluation | any): string {
    return evaluation.periode ??
      evaluation.period ??
      evaluation.trimestre ??
      evaluation.semestre ??
      'Évaluation';
  }

  getStatut(evaluation: Evaluation | any): string {
    return String(evaluation.statut || evaluation.status || 'PUBLIEE').toUpperCase();
  }

  getStatutClass(evaluation: Evaluation | any): string {
    const statut = this.getStatut(evaluation);

    if (statut === 'BROUILLON') return 'status-draft';
    if (statut === 'PUBLIEE') return 'status-published';
    if (statut === 'VALIDEE') return 'status-valid';
    if (statut === 'ARCHIVEE') return 'status-archived';

    return 'status';
  }

  getScoreClass(note?: number): string {
    if (!note && note !== 0) return 'score-neutral';

    if (this.isEmployeeMode()) {
      if (note >= 4) return 'score-good';
      if (note >= 3) return 'score-medium';
      return 'score-low';
    }

    if (note >= 8) return 'score-good';
    if (note >= 5) return 'score-medium';
    return 'score-low';
  }

  getAvatarEmployee(evaluation: Evaluation | any): any {
    return {
      id: evaluation.employeId,
      nom: evaluation.employeNom,
      prenom: evaluation.employePrenom,
      email: evaluation.employeEmail,

      photoUrl:
        evaluation.employePhotoUrl ||
        evaluation.employePhotoProfil ||
        evaluation.photoUrl ||
        evaluation.photoProfil ||
        null,

      photoProfil:
        evaluation.employePhotoProfil ||
        evaluation.employePhotoUrl ||
        evaluation.photoProfil ||
        evaluation.photoUrl ||
        null
    };
  }

  getPointsForts(evaluation: any): string {
    return evaluation.pointsForts ??
      evaluation.points_forts ??
      '';
  }

  getAxesAmelioration(evaluation: any): string {
    return evaluation.axesAmelioration ??
      evaluation.axes_amelioration ??
      '';
  }

  getObjectifs(evaluation: any): string {
    return evaluation.objectifs ?? '';
  }

  getCommentaireManager(evaluation: any): string {
    return evaluation.commentaireManager ??
      evaluation.commentaire_manager ??
      evaluation.commentaire ??
      '';
  }

  // =========================
  // UTILS
  // =========================

  private toInputDate(value?: string): string {
    if (!value) return new Date().toISOString().substring(0, 10);
    return value.substring(0, 10);
  }

  private toNumber(value: any): number {
    if (value === null || value === undefined || value === '') {
      return 0;
    }

    const numericValue = Number(value);
    return Number.isNaN(numericValue) ? 0 : numericValue;
  }

  private normalizeNote(note: number): number {
    if (!note || Number.isNaN(note)) {
      return 0;
    }

    return Number(note.toFixed(1));
  }

  private convertNoteTo5(note: number): number {
    if (!note || Number.isNaN(note)) {
      return 0;
    }

    if (note > 5) {
      return Number((note / 2).toFixed(1));
    }

    return Number(note.toFixed(1));
  }
}