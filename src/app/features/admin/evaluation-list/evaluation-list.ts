import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Evaluation,
  EvaluationStats
} from '../../../core/models/evaluation.model';

import { EvaluationService } from '../../../core/services/evaluation.service';
import { EmployeService } from '../../../core/services/employe.service';
import { EmployeeAvatarComponent } from '../../../shared/layouts/components/employee-avatar/employee-avatar.component';

type EvaluationStatutForm = 'BROUILLON' | 'PUBLIEE';

@Component({
  selector: 'app-evaluation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, EmployeeAvatarComponent],
  templateUrl: './evaluation-list.html',
  styleUrls: ['./evaluation-list.scss']
})
export class EvaluationListComponent implements OnInit {
  evaluations: Evaluation[] = [];
  stats: EvaluationStats | any = null;
  managers: any[] = [];

  loading = false;
  saving = false;

  errorMessage = '';
  successMessage = '';

  showEvaluationModal = false;
  editingEvaluationId: number | null = null;

  newEvaluation: any = this.getEmptyForm();

  constructor(
    private evaluationService: EvaluationService,
    private employeService: EmployeService
  ) {}

  ngOnInit(): void {
    this.loadData();
    this.loadManagers();
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.evaluationService.getAdminStats().subscribe({
      next: (stats: EvaluationStats | any) => {
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats admin:', error);
      }
    });

    this.evaluationService.getAdminEvaluations().subscribe({
      next: (data: Evaluation[]) => {
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

  saveEvaluation(statut: EvaluationStatutForm): void {
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
    this.newEvaluation = this.getEmptyForm();
  }

  private getEmptyForm(): any {
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

  private toInputDate(value?: string): string {
    if (!value) return new Date().toISOString().substring(0, 10);
    return value.substring(0, 10);
  }

  getEmployeName(evaluation: Evaluation | any): string {
    const prenom = evaluation.employePrenom || '';
    const nom = evaluation.employeNom || '';
    return `${prenom} ${nom}`.trim() || evaluation.employeEmail || 'Employé';
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

  getNote(evaluation: Evaluation | any): number {
    return Number(
      evaluation.noteGlobale ??
      evaluation.note ??
      evaluation.score ??
      0
    );
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

    const moyenne = Number(
      s.moyenneGlobale ??
      s.moyenneNote ??
      s.moyenne ??
      s.noteMoyenne ??
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

  getPeriode(evaluation: Evaluation | any): string {
    return evaluation.periode || evaluation.period || '-';
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
}