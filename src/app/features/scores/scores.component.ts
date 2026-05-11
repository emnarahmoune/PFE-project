import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ManagerService } from '../../core/services/manager.service';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

export interface ScoreTurnover {
  id: number;
  employeId: number;
  employeNom: string;
  employePrenom: string;
  employeMatricule?: string;
  employeDepartement?: string;
  score: number;
  niveauRisque: string;
  datePrediction: string;
  facteursPrincipaux?: string;
  actionRecommandee?: string;

  photoUrl?: string;
  employePhotoUrl?: string;
  employePhotoProfil?: string;
  photoProfil?: string;
  employeEmail?: string;
}

@Component({
  selector: 'app-scores',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './scores.component.html',
  styleUrls: ['./scores.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ScoresComponent implements OnInit {
  scores: ScoreTurnover[] = [];
  filteredScores: ScoreTurnover[] = [];

  loading = true;
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

  constructor(
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadScores();
  }

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
      next: (res: any) => {
        if (res?.success && Array.isArray(res.data)) {
          this.scores = res.data;

          this.departements = [
            ...new Set(
              this.scores
                .map(score => score.employeDepartement)
                .filter((dept): dept is string => !!dept && dept.trim() !== '')
            )
          ].sort((a, b) => a.localeCompare(b));

          this.calculerStats();
          this.applyFilters();
        } else {
          this.scores = [];
          this.filteredScores = [];
          this.error = true;
        }

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Erreur chargement scores turnover:', err);

        this.scores = [];
        this.filteredScores = [];

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

    this.scoreMoyen = Math.round(
      this.scores.reduce((acc, score) => acc + Number(score.score || 0), 0) /
        this.totalEmployes
    );

    this.nbCritiques = this.scores.filter(
      score => this.normalizeNiveau(score.niveauRisque) === 'CRITIQUE'
    ).length;
  }

  applyFilters(): void {
    this.filteredScores = this.scores.filter(score => {
      const niveau = this.normalizeNiveau(score.niveauRisque);

      if (this.niveauFilter !== 'TOUS' && niveau !== this.niveauFilter) {
        return false;
      }

      if (
        this.departementFilter &&
        score.employeDepartement !== this.departementFilter
      ) {
        return false;
      }

      if (this.searchText.trim()) {
        const search = this.searchText.toLowerCase().trim();

        const fullName = `${score.employePrenom || ''} ${score.employeNom || ''}`.toLowerCase();
        const reverseName = `${score.employeNom || ''} ${score.employePrenom || ''}`.toLowerCase();
        const matricule = (score.employeMatricule || '').toLowerCase();
        const departement = (score.employeDepartement || '').toLowerCase();

        if (
          !fullName.includes(search) &&
          !reverseName.includes(search) &&
          !matricule.includes(search) &&
          !departement.includes(search)
        ) {
          return false;
        }
      }

      return true;
    });

    this.filteredScores.sort((a, b) => Number(b.score || 0) - Number(a.score || 0));

    this.cdr.detectChanges();
  }

  resetFilters(): void {
    this.niveauFilter = 'TOUS';
    this.departementFilter = '';
    this.searchText = '';
    this.applyFilters();
  }

  getCountNiveau(niveau: string): number {
    return this.scores.filter(
      score => this.normalizeNiveau(score.niveauRisque) === niveau
    ).length;
  }

  getPourcentageNiveau(niveau: string): number {
    if (this.totalEmployes === 0) {
      return 0;
    }

    return Math.round((this.getCountNiveau(niveau) / this.totalEmployes) * 100);
  }

  getNiveauClass(niveau: string): string {
    switch (this.normalizeNiveau(niveau)) {
      case 'FAIBLE':
        return 'niveau-faible';

      case 'MOYEN':
        return 'niveau-moyen';

      case 'ELEVE':
        return 'niveau-eleve';

      case 'CRITIQUE':
        return 'niveau-critique';

      default:
        return '';
    }
  }

  getNiveauLabel(niveau: string): string {
    switch (this.normalizeNiveau(niveau)) {
      case 'FAIBLE':
        return 'FAIBLE';

      case 'MOYEN':
        return 'MOYEN';

      case 'ELEVE':
        return 'ÉLEVÉ';

      case 'CRITIQUE':
        return 'CRITIQUE';

      default:
        return niveau || '-';
    }
  }

  getNiveauIcon(niveau: string): string {
    switch (this.normalizeNiveau(niveau)) {
      case 'TOUS':
        return '🎯';

      case 'FAIBLE':
        return '🟢';

      case 'MOYEN':
        return '🟠';

      case 'ELEVE':
        return '🔴';

      case 'CRITIQUE':
        return '⛔';

      default:
        return '⚪';
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

  toEmployeeAvatar(score: ScoreTurnover): any {
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

      photoUrl: score.photoUrl,
      employePhotoUrl: score.employePhotoUrl,
      employePhotoProfil: score.employePhotoProfil,
      photoProfil: score.photoProfil
    };
  }

  formatDate(dateStr: string): string {
    if (!dateStr) {
      return '-';
    }

    const date = new Date(dateStr);

    if (Number.isNaN(date.getTime())) {
      return '-';
    }

    return date.toLocaleDateString('fr-FR');
  }

  recalculerScore(employeId: number): void {
    const confirmed = confirm('Recalculer le score de risque pour cet employé ?');

    if (!confirmed) {
      return;
    }

    this.managerService.recalculerScoreTurnover(employeId).subscribe({
      next: () => {
        this.loadScores();
      },
      error: err => {
        console.error('Erreur recalcul score turnover:', err);
      }
    });
  }

  retry(): void {
    this.loadScores();
  }

  exportCSV(): void {
    const headers = [
      'Matricule',
      'Nom',
      'Prénom',
      'Département',
      'Score',
      'Niveau',
      'Date calcul'
    ];

    const rows = this.filteredScores.map(score => [
      score.employeMatricule || '',
      score.employeNom || '',
      score.employePrenom || '',
      score.employeDepartement || '',
      String(score.score ?? ''),
      score.niveauRisque || '',
      this.formatDate(score.datePrediction)
    ]);

    const csvContent = [headers, ...rows]
      .map(row => row.map(value => `"${String(value).replace(/"/g, '""')}"`).join(';'))
      .join('\n');

    const blob = new Blob([`\uFEFF${csvContent}`], {
      type: 'text/csv;charset=utf-8;'
    });

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

  private normalizeNiveau(niveau: string): string {
    const value = String(niveau || '')
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    if (value === 'ELEVE' || value === 'ÉLEVÉ') {
      return 'ELEVE';
    }

    return value;
  }
}