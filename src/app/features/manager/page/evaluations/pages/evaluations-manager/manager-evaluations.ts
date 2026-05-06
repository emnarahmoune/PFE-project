import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Evaluation,
  EvaluationRequest,
  EvaluationStats,
  EmployeEquipeEvaluation
} from '../../../../../../core/models/evaluation.model';

import { EvaluationService } from '../../../../../../core/services/evaluation.service';
import { EmployeeAvatarComponent } from '../../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';
import { ManagerService } from '../../../../../../core/services/manager.service';

type EvaluationStatutForm = 'BROUILLON' | 'PUBLIEE';

@Component({
  selector: 'app-manager-evaluations',
  standalone: true,
  imports: [CommonModule, FormsModule, EmployeeAvatarComponent],
  templateUrl: './manager-evaluations.html',
  styleUrls: ['./manager-evaluations.scss']
})
export class ManagerEvaluationsComponent implements OnInit {
  evaluations: Evaluation[] = [];
  stats: EvaluationStats | null = null;
  equipe: EmployeEquipeEvaluation[] = [];

  loading = false;
  loadingEquipe = false;
  saving = false;

  errorMessage = '';
  successMessage = '';

  showForm = false;
  editingEvaluationId: number | null = null;

  form: EvaluationRequest = this.getEmptyForm();

  constructor(
    private evaluationService: EvaluationService,
    private managerService: ManagerService
  ) {}

  ngOnInit(): void {
    this.loadData();
    this.loadEquipe();
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.evaluationService.getManagerStats().subscribe({
      next: (stats: EvaluationStats) => {
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats manager:', error);
      }
    });

    this.evaluationService.getManagerEvaluations().subscribe({
      next: (data: Evaluation[]) => {
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
    this.form = this.getEmptyForm();
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

  saveEvaluation(statut: EvaluationStatutForm = 'PUBLIEE'): void {
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
        this.form = this.getEmptyForm();

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
    this.form = this.getEmptyForm();
    this.errorMessage = '';
  }

  getStatsTotal(): number {
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
    const s: any = this.stats || {};

    const value =
      s.moyenneGlobale ??
      s.moyenne ??
      s.noteMoyenne ??
      s.averageScore ??
      s.moyenneNote ??
      0;

    return Number(value || 0);
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

  getNoteGlobale(evaluation: Evaluation): number {
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

  getNoteTechnique(evaluation: Evaluation): number {
    const e: any = evaluation;

    return Number(
      e.noteTechnique ??
      e.technique ??
      e.note ??
      0
    );
  }

  getNoteCommunication(evaluation: Evaluation): number {
    const e: any = evaluation;

    return Number(
      e.noteCommunication ??
      e.communication ??
      e.note ??
      0
    );
  }

  getNoteLeadership(evaluation: Evaluation): number {
    const e: any = evaluation;

    return Number(
      e.noteLeadership ??
      e.leadership ??
      e.note ??
      0
    );
  }

  getNoteProductivite(evaluation: Evaluation): number {
    const e: any = evaluation;

    return Number(
      e.noteProductivite ??
      e.productivite ??
      e.note ??
      0
    );
  }

  getObjectifsAtteints(evaluation: Evaluation | any): number {
    return Number(
      evaluation.objectifsAtteints ??
      evaluation.objectifs_atteints ??
      0
    );
  }

  getEmployeName(evaluation: Evaluation): string {
    const prenom = evaluation.employePrenom || '';
    const nom = evaluation.employeNom || '';
    return `${prenom} ${nom}`.trim() || evaluation.employeEmail || 'Employé';
  }

  getEquipeEmployeName(employe: EmployeEquipeEvaluation): string {
    const prenom = employe.prenom || '';
    const nom = employe.nom || '';
    return `${prenom} ${nom}`.trim() || employe.email || `Employé #${employe.id}`;
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
    if (note >= 8) return 'score-good';
    if (note >= 5) return 'score-medium';
    return 'score-low';
  }

  private getEmptyForm(): EvaluationRequest {
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
}