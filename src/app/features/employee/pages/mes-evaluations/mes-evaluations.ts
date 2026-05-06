import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Evaluation, EvaluationStats } from '../../../../core/models/evaluation.model';
import { EvaluationService } from '../../../../core/services/evaluation.service';

@Component({
  selector: 'app-mes-evaluations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mes-evaluations.html',
  styleUrls: ['./mes-evaluations.scss']
})
export class MesEvaluationsComponent implements OnInit {
  evaluations: Evaluation[] = [];
  stats: EvaluationStats | any = null;

  loading = false;
  errorMessage = '';

  constructor(private evaluationService: EvaluationService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.evaluationService.getMyStats().subscribe({
      next: (stats: any) => {
        console.log('STATS EMPLOYÉ REÇUES:', stats);
        this.stats = stats;
      },
      error: error => {
        console.error('Erreur stats employé:', error);
      }
    });

    this.evaluationService.getMyEvaluations().subscribe({
      next: (data: Evaluation[]) => {
        console.log('ÉVALUATIONS EMPLOYÉ REÇUES:', data);
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
  // STATS
  // =========================

  getStatsTotal(): number {
    return this.evaluations.length || Number(this.stats?.totalEvaluations || 0);
  }


getStatsMoyenne(): number {
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

  const moyenneBackend = Number(
    this.stats?.moyenneGlobale ||
    this.stats?.moyenneNote ||
    this.stats?.moyenne ||
    this.stats?.noteMoyenne ||
    0
  );

  return this.normalizeNote(moyenneBackend);
}




getGlobalNote(evaluation: any): number {
  return Number(
    evaluation.noteGlobale ||
    evaluation.note ||
    evaluation.score ||
    0
  );
}


getEvaluateurLabel(evaluation: any): string {
  const role =
    evaluation.evaluateurRole ||
    evaluation.managerRole ||
    evaluation.roleEvaluateur ||
    '';

  const roleNormalized = role.toString().toUpperCase();

  if (
    roleNormalized === 'ADMIN_RH' ||
    roleNormalized === 'ADMIN' ||
    roleNormalized.includes('ADMIN')
  ) {
    return 'Admin RH';
  }

  return 'Manager';
}

getEvaluateurName(evaluation: any): string {
  const prenom =
    evaluation.evaluateurPrenom ||
    evaluation.managerPrenom ||
    '';

  const nom =
    evaluation.evaluateurNom ||
    evaluation.managerNom ||
    '';

  return `${prenom} ${nom}`.trim()
    || evaluation.evaluateurEmail
    || evaluation.managerEmail
    || 'Évaluateur';
}

  getStatsProgression(): number {
    return Number(
      this.stats?.progression ??
      this.stats?.moyenneObjectifs ??
      this.stats?.objectifsAtteints ??
      0
    );
  }



  // =========================
  // INFOS ÉVALUATION
  // =========================

  getPeriode(evaluation: any): string {
    return evaluation.periode ??
      evaluation.period ??
      evaluation.trimestre ??
      evaluation.semestre ??
      'Évaluation';
  }

  // =========================
  // NOTES
  // =========================

  getRawNote(evaluation: any): number {
  return Number(
    evaluation.noteGlobale ||
    evaluation.note ||
    evaluation.score ||
    0
  );
}

getNoteSur5(evaluation: any): number {
  return this.normalizeNote(this.getRawNote(evaluation));
}

getNoteTechnique(evaluation: any): number {
  const note = Number(
    evaluation.noteTechnique ||
    evaluation.technique ||
    evaluation.competenceTechnique ||
    0
  );

  return note > 0 ? this.normalizeNote(note) : this.getNoteSur5(evaluation);
}

getNoteCommunication(evaluation: any): number {
  const note = Number(
    evaluation.noteCommunication ||
    evaluation.communication ||
    0
  );

  return note > 0 ? this.normalizeNote(note) : this.getNoteSur5(evaluation);
}

getNoteLeadership(evaluation: any): number {
  const note = Number(
    evaluation.noteLeadership ||
    evaluation.leadership ||
    0
  );

  return note > 0 ? this.normalizeNote(note) : this.getNoteSur5(evaluation);
}

getNoteProductivite(evaluation: any): number {
  const note = Number(
    evaluation.noteProductivite ||
    evaluation.productivite ||
    evaluation.productivité ||
    0
  );

  return note > 0 ? this.normalizeNote(note) : this.getNoteSur5(evaluation);
}

private normalizeNote(note: number): number {
  if (!note || Number.isNaN(note)) {
    return 0;
  }

  return Number(note.toFixed(1));
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



  // =========================
  // TEXTES
  // =========================

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
  // UI
  // =========================

  getScoreClass(note?: number): string {
    if (!note && note !== 0) return 'score-neutral';
    if (note >= 4) return 'score-good';
    if (note >= 3) return 'score-medium';
    return 'score-low';
  }

  private convertNoteTo5(note: number): number {
    if (!note || Number.isNaN(note)) {
      return 0;
    }

    // Backend note sur 10 => affichage sur 5.
    if (note > 5) {
      return Number((note / 2).toFixed(1));
    }

    return Number(note.toFixed(1));
  }
}