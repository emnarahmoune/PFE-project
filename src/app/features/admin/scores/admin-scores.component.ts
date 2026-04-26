import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ManagerService } from '../../../core/services/manager.service';
import { ScoreTurnover } from './models/score-turnover.model';

@Component({
  selector: 'app-admin-scores',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-scores.component.html',
  styleUrls: ['./admin-scores.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminScoresComponent implements OnInit {
  scores: ScoreTurnover[] = [];
  filteredScores: ScoreTurnover[] = [];
  loading = true;
  error = false;

  // Filtres
  niveauFilter = 'TOUS';
  departementFilter = '';
  searchText = '';

  niveaux = ['TOUS', 'FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE'];
  departements: string[] = [];

  // Statistiques
  scoreMoyen = 0;
  totalEmployes = 0;
  nbCritiques = 0;

  // Mode d'affichage
  viewMode: 'table' | 'cards' = 'table';

  constructor(
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadScores();
  }

  // TrackBy pour optimiser le re-rendu
  trackByScoreId(index: number, score: ScoreTurnover): number {
    return score.employeId;
  }

  trackByDept(index: number, dept: string): string {
    return dept;
  }

  loadScores(): void {
    this.loading = true;
    this.error = false;
    this.managerService.getDerniersScores().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.scores = res.data;
          this.departements = [...new Set(
            this.scores
              .map(s => s.employeDepartement)
              .filter((dept): dept is string => !!dept && dept.trim() !== '')
          )];
          this.calculerStats();
          this.applyFilters();
        } else {
          this.error = true;
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = true;
        this.loading = false;
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
    this.scoreMoyen = Math.round(this.scores.reduce((acc, s) => acc + s.score, 0) / this.totalEmployes);
    this.nbCritiques = this.scores.filter(s => s.niveauRisque === 'CRITIQUE').length;
  }

  applyFilters(): void {
    this.filteredScores = this.scores.filter(score => {
      if (this.niveauFilter !== 'TOUS' && score.niveauRisque !== this.niveauFilter) return false;
      if (this.departementFilter && score.employeDepartement !== this.departementFilter) return false;
      if (this.searchText) {
        const search = this.searchText.toLowerCase();
        const fullName = `${score.employePrenom} ${score.employeNom}`.toLowerCase();
        const mat = (score.employeMatricule || '').toLowerCase();
        if (!fullName.includes(search) && !mat.includes(search)) return false;
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

  // ========================= STATS DE RÉPARTITION =========================
  getCountNiveau(niveau: string): number {
    return this.scores.filter(s => s.niveauRisque === niveau).length;
  }

  getPourcentageNiveau(niveau: string): number {
    if (this.totalEmployes === 0) return 0;
    return (this.getCountNiveau(niveau) / this.totalEmployes) * 100;
  }

  // ========================= AFFICHAGE (badges, icônes, couleurs) =========================
  getNiveauClass(niveau: string): string {
    switch (niveau) {
      case 'FAIBLE': return 'niveau-faible';
      case 'MOYEN': return 'niveau-moyen';
      case 'ELEVE': return 'niveau-eleve';
      case 'CRITIQUE': return 'niveau-critique';
      default: return '';
    }
  }

  getNiveauLabel(niveau: string): string {
    switch (niveau) {
      case 'FAIBLE': return 'FAIBLE';
      case 'MOYEN': return 'MOYEN';
      case 'ELEVE': return 'ÉLEVÉ';
      case 'CRITIQUE': return 'CRITIQUE';
      default: return niveau;
    }
  }

  getNiveauIcon(niveau: string): string {
    switch (niveau) {
      case 'TOUS': return '🎯';
      case 'FAIBLE': return '🟢';
      case 'MOYEN': return '🟠';
      case 'ELEVE': return '🔴';
      case 'CRITIQUE': return '⛔';
      default: return '⚪';
    }
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
    return colors[dept || ''] || '#6366f1';
  }

  getAvatarInitials(prenom: string, nom: string): string {
    const p = prenom?.charAt(0) || '';
    const n = nom?.charAt(0) || '';
    return (p + n).toUpperCase();
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }

  // ========================= ACTIONS =========================
  recalculerScore(employeId: number): void {
    if (confirm('Recalculer le score de risque pour cet employé ?')) {
      this.managerService.recalculerScoreTurnover(employeId).subscribe({
        next: () => this.loadScores(),
        error: (err) => console.error(err)
      });
    }
  }

  retry(): void {
    this.loadScores();
  }

  // ========================= EXPORT CSV =========================
  exportCSV(): void {
    const headers = ['Matricule', 'Nom', 'Prénom', 'Département', 'Score', 'Niveau', 'Date calcul'];
    const rows = this.filteredScores.map(s => [
      s.employeMatricule || '',
      s.employeNom,
      s.employePrenom,
      s.employeDepartement || '',
      s.score,
      s.niveauRisque,
      this.formatDate(s.datePrediction)
    ]);
    const csvContent = [headers, ...rows].map(row => row.join(';')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', 'scores_turnover.csv');
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}