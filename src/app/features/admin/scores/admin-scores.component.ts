import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ManagerService } from '../../../core/services/manager.service';
import { ScoreTurnover } from './models/score-turnover.model';
import { EmployeeAvatarComponent } from '../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-admin-scores',
  standalone: true,
  imports: [CommonModule, FormsModule, EmployeeAvatarComponent],
  templateUrl: './admin-scores.component.html',
  styleUrls: ['./admin-scores.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminScoresComponent implements OnInit, OnDestroy {
  scores: ScoreTurnover[] = [];
  filteredScores: ScoreTurnover[] = [];

  loading = true;
  recalculating = false;
  error = false;

  niveauFilter = 'TOUS';
  departementFilter = '';
  searchText = '';

  niveaux = ['TOUS', 'FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE'];
  departements: string[] = [];

  scoreMoyen = 0;
  totalEmployes = 0;
  nbCritiques = 0;
  viewMode: 'table' | 'cards' = 'table';

  autoRefresh = false;
  private destroy$ = new Subject<void>();

  constructor(
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadScores();

    if (this.autoRefresh) {
      interval(300000)
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => this.loadScores(false));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  trackByScoreId(_: number, score: ScoreTurnover): number {
    return score.employeId;
  }

  trackByDept(_: number, dept: string): string {
    return dept;
  }

  normalizeScore(value: any): number | null {
    const n = Number(value);
    if (isNaN(n) || n === null || n === undefined) return null;
    return Math.max(0, Math.min(100, Math.round(n)));
  }

  normalizeNiveau(value: any): string {
    const v = String(value ?? '').trim().toUpperCase();

    switch (v) {
      case 'FAIBLE':
        return 'FAIBLE';
      case 'MOYEN':
        return 'MOYEN';
      case 'ELEVE':
      case 'ÉLEVÉ':
      case 'ELEVÉ':
      case 'ELEVEE':
      case 'ÉLEVÉE':
        return 'ELEVE';
      case 'CRITIQUE':
        return 'CRITIQUE';
      default:
        return 'MOYEN';
    }
  }

  private normalizeText(value: any): string {
    return String(value ?? '').trim();
  }

  loadScores(showFullLoader = true): void {
    if (showFullLoader) {
      this.loading = true;
    }
    this.error = false;

    this.managerService.getDerniersScores().subscribe({
      next: (scoresData: ScoreTurnover[]) => {
        this.scores = (scoresData ?? []).map((s: any) => ({
          ...s,
          score: this.normalizeScore(s.score),
          niveauRisque: this.normalizeNiveau(s.niveauRisque),
          employeDepartement: this.normalizeText(s.employeDepartement),
          employeNom: this.normalizeText(s.employeNom),
          employePrenom: this.normalizeText(s.employePrenom),
          employeMatricule: this.normalizeText(s.employeMatricule),
          employeEmail: this.normalizeText(s.employeEmail)
        }));

        this.departements = [
          ...new Set(
            this.scores
              .map(s => s.employeDepartement)
              .filter((d): d is string => !!d && d.trim() !== '')
          )
        ].sort((a, b) => a.localeCompare(b, 'fr'));

        this.calculerStats();
        this.applyFilters();

        this.loading = false;
        this.recalculating = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement scores', err);
        this.error = true;
        this.loading = false;
        this.recalculating = false;
        this.cdr.detectChanges();
      }
    });
  }

   calculerStats(): void {
    this.totalEmployes = this.scores.length;
    if (this.totalEmployes === 0) {
      this.scoreMoyen = 0;
      this.nbCritiques = 0;
      return;
    }

    const scoresValides = this.scores
      .map(s => s.score)
      .filter((s): s is number => s !== null);

    if (scoresValides.length === 0) {
      this.scoreMoyen = 0;
    } else {
      this.scoreMoyen = Math.round(
        scoresValides.reduce((acc, s) => acc + s, 0) / scoresValides.length
      );
    }

    this.nbCritiques = this.scores.filter(s => s.niveauRisque === 'CRITIQUE').length;
  }

  applyFilters(): void {
    const search = this.searchText.trim().toLowerCase();

    this.filteredScores = this.scores.filter(score => {
      const niveau = this.normalizeNiveau(score.niveauRisque);
      const dept = this.normalizeText(score.employeDepartement);
      const fullName = `${score.employePrenom ?? ''} ${score.employeNom ?? ''}`.toLowerCase();
      const matricule = String(score.employeMatricule ?? '').toLowerCase();

      if (this.niveauFilter !== 'TOUS' && niveau !== this.niveauFilter) return false;
      if (this.departementFilter && dept !== this.departementFilter) return false;

      if (search) {
        if (!fullName.includes(search) && !matricule.includes(search)) return false;
      }

      return true;
    });

    this.cdr.detectChanges();
  }

  resetFilters(): void {
    this.niveauFilter = 'TOUS';
    this.departementFilter = '';
    this.searchText = '';
    this.applyFilters();
  }

  getCountNiveau(niveau: string): number {
    const n = this.normalizeNiveau(niveau);
    return this.scores.filter(s => this.normalizeNiveau(s.niveauRisque) === n).length;
  }

  getPourcentageNiveau(niveau: string): number {
    if (this.totalEmployes === 0) return 0;
    return (this.getCountNiveau(niveau) / this.totalEmployes) * 100;
  }

  getNiveauClass(niveau: string): string {
    const n = this.normalizeNiveau(niveau);
    const map: Record<string, string> = {
      FAIBLE: 'niveau-faible',
      MOYEN: 'niveau-moyen',
      ELEVE: 'niveau-eleve',
      CRITIQUE: 'niveau-critique'
    };
    return map[n] ?? '';
  }

  getNiveauLabel(niveau: string): string {
    const n = this.normalizeNiveau(niveau);
    const map: Record<string, string> = {
      FAIBLE: 'FAIBLE',
      MOYEN: 'MOYEN',
      ELEVE: 'ÉLEVÉ',
      CRITIQUE: 'CRITIQUE'
    };
    return map[n] ?? n;
  }

  getNiveauIcon(niveau: string): string {
    const n = this.normalizeNiveau(niveau);
    const map: Record<string, string> = {
      TOUS: '🎯',
      FAIBLE: '🟢',
      MOYEN: '🟠',
      ELEVE: '🔴',
      CRITIQUE: '⛔'
    };
    return map[n] ?? '⚪';
  }

   getScoreBarClass(score: number | null): string {
    const s = score ?? 0;
    if (s < 25) return 'score-bar-faible';
    if (s < 50) return 'score-bar-moyen';
    if (s < 75) return 'score-bar-eleve';
    return 'score-bar-critique';
  }

  getAvatarColor(dept: string | undefined): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6',
      Technique: '#0891b2',
      Commercial: '#d97706',
      Finance: '#059669',
      Marketing: '#db2777',
      Direction: '#7c3aed',
      Logistique: '#4f46e5'
    };
    return colors[dept ?? ''] ?? '#6366f1';
  }

  toEmployeeAvatar(score: ScoreTurnover): any {
    const photo = score.employePhotoUrl ?? score.photoUrl ?? null;
    return {
      id: score.employeId,
      prenom: score.employePrenom,
      nom: score.employeNom,
      employePrenom: score.employePrenom,
      employeNom: score.employeNom,
      email: score.employeEmail,
      employeEmail: score.employeEmail,
      departement: score.employeDepartement,
      employeDepartement: score.employeDepartement,
      matricule: score.employeMatricule,
      employeMatricule: score.employeMatricule,
      photoUrl: photo,
      employePhotoUrl: photo,
      employePhotoProfil: photo,
      photoProfil: photo
    };
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('fr-FR');
  }

  recalculerScore(employeId: number): void {
    if (!confirm('Recalculer le score de risque pour cet employé ?')) return;

    this.managerService.recalculerScoreTurnover(employeId).subscribe({
      next: () => this.loadScores(false),
      error: (err: any) => console.error('Erreur recalcul', err)
    });
  }

  recalculerTousScores(): void {
    if (!confirm('Recalculer les scores pour tous les employés ?\nCette opération peut prendre quelques secondes.')) return;

    this.recalculating = true;
    this.error = false;
    this.cdr.detectChanges();

    this.managerService.recalculerTousScores().subscribe({
      next: () => this.loadScores(false),
      error: (err: any) => {
        console.error('Erreur recalcul global', err);
        this.recalculating = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  voirDetail(employeId: number): void {
    this.router.navigate(['/admin/scores', employeId, 'detail']);
  }

  retry(): void {
    this.loadScores();
  }

  exportCSV(): void {
    const headers = ['Matricule', 'Nom', 'Prénom', 'Département', 'Score', 'Niveau', 'Date calcul'];

    const rows = this.filteredScores.map(s => [
      s.employeMatricule ?? '',
      s.employeNom ?? '',
      s.employePrenom ?? '',
      s.employeDepartement ?? '',
      String(this.normalizeScore(s.score)),
      this.getNiveauLabel(s.niveauRisque),
      this.formatDate(s.datePrediction)
    ]);

    const csvContent = [headers, ...rows].map(row => row.join(';')).join('\n');
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = Object.assign(document.createElement('a'), {
      href: url,
      download: `scores_turnover_${new Date().toISOString().slice(0, 10)}.csv`,
      style: 'visibility:hidden'
    });

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}