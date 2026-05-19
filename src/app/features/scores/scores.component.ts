import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize, Subject, takeUntil } from 'rxjs';

import { ManagerService, EmployeScoreDetail } from '../../core/services/manager.service';
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
    RouterModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './scores.component.html',
  styleUrls: ['./scores.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ScoresComponent implements OnInit, OnDestroy {
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
  showDetail = false;
  
  detail: EmployeScoreDetail | null = null;
  detailLoading = false;
  detailError = false;
  activeTab: 'overview' | 'factors' | 'history' | 'actions' | 'params' = 'overview';
  
  niveauxRisque = [
    { niveau: 'FAIBLE',   min: 0,  max: 20,  couleur: '#10b981' },
    { niveau: 'MOYEN',    min: 20, max: 40,  couleur: '#f59e0b' },
    { niveau: 'ÉLEVÉ',    min: 40, max: 70,  couleur: '#ef4444' },
    { niveau: 'CRITIQUE', min: 70, max: 100, couleur: '#7f1d1d' }
  ];

  private destroy$ = new Subject<void>();
  private isLoadingScores = false;

  constructor(
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // CORRECTION : Appeler loadScores() immédiatement au démarrage
    this.loadScores();
    
    // Puis écouter les changements de route pour le détail
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params['id'];
        if (id) {
          this.loadDetail(+id);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ==================== LISTE OPTIMISÉE ====================
  
  loadScores(): void {
    // Éviter les chargements multiples simultanés
    if (this.isLoadingScores) {
      console.log('Chargement déjà en cours...');
      return;
    }
    
    console.log('Début chargement des scores...');
    this.isLoadingScores = true;
    this.loading = true;
    this.error = false;
    this.showDetail = false;

    this.managerService.getDerniersScores()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          console.log('Réponse reçue:', response);
          
          const scoresData = this.extractScoresData(response);
          
          if (!scoresData || scoresData.length === 0) {
            console.log('Aucune donnée reçue');
            this.handleEmptyResponse();
            return;
          }
          
          console.log(`Données extraites: ${scoresData.length} employés`);
          
          // Mapping optimisé
          this.scores = this.mapScoresData(scoresData);
          this.updateDepartements();
          this.calculerStats();
          this.applyFilters();
          
          console.log(`Scores chargés: ${this.scores.length} employés`);
          
          this.loading = false;
          this.isLoadingScores = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Erreur chargement scores:', err);
          this.handleErrorResponse();
        }
      });
  }

  private extractScoresData(response: any): any[] {
    if (!response) {
      console.log('Réponse vide');
      return [];
    }
    
    if (response && response.success && Array.isArray(response.data)) {
      console.log('Format: { success, data }');
      return response.data;
    }
    if (response && Array.isArray(response)) {
      console.log('Format: Array direct');
      return response;
    }
    if (response && response.data && Array.isArray(response.data)) {
      console.log('Format: { data }');
      return response.data;
    }
    if (response && response.items && Array.isArray(response.items)) {
      console.log('Format: { items }');
      return response.items;
    }
    
    console.log('Format non reconnu, tentative de récupération...');
    // Dernière tentative: chercher un tableau dans la réponse
    for (const key in response) {
      if (Array.isArray(response[key])) {
        console.log(`Tableau trouvé dans la clé '${key}'`);
        return response[key];
      }
    }
    
    return [];
  }

  private mapScoresData(scoresData: any[]): ScoreTurnover[] {
    return scoresData.map((item: any, index: number) => {
      const scoreValue = this.extractScoreValue(item);
      const niveauRisque = this.extractNiveauRisque(item);
      
      return {
        id: item.id || item.employeId || index,
        employeId: item.employeId || item.id || index,
        employeNom: item.employeNom || item.nom || item.lastName || item.name || '',
        employePrenom: item.employePrenom || item.prenom || item.firstName || '',
        employeMatricule: item.employeMatricule || item.matricule || '',
        employeDepartement: item.employeDepartement || item.departement || '',
        score: scoreValue,
        niveauRisque: niveauRisque,
        datePrediction: item.datePrediction || item.dateCalcul || item.date || new Date().toISOString(),
        facteursPrincipaux: item.facteursPrincipaux || item.facteurs,
        actionRecommandee: item.actionRecommandee || item.action,
        photoUrl: item.photoUrl || item.employePhotoUrl || '',
        employeEmail: item.employeEmail || item.email || ''
      };
    });
  }

  private extractScoreValue(item: any): number {
    // Ordre de priorité pour trouver le score
    if (typeof item.score === 'number') return item.score;
    if (typeof item.score === 'string') return parseFloat(item.score) || 0;
    if (typeof item.valeur === 'number') return item.valeur;
    if (typeof item.valeur === 'string') return parseFloat(item.valeur) || 0;
    if (item.scoreActuel?.score) return item.scoreActuel.score;
    if (typeof item.scoreGlobal === 'number') return item.scoreGlobal;
    if (typeof item.riskScore === 'number') return item.riskScore;
    if (typeof item.prediction === 'number') return item.prediction;
    if (typeof item.probabilite === 'number') return item.probabilite;
    if (typeof item.turnoverRisk === 'number') return item.turnoverRisk;
    
    // Recherche automatique dans les propriétés
    for (const key of Object.keys(item)) {
      const val = item[key];
      if (typeof val === 'number' && val >= 0 && val <= 100) {
        console.log(`Score trouvé dans '${key}': ${val}`);
        return val;
      }
      if (typeof val === 'string' && !isNaN(parseFloat(val)) && parseFloat(val) >= 0 && parseFloat(val) <= 100) {
        const numVal = parseFloat(val);
        console.log(`Score (string) trouvé dans '${key}': ${numVal}`);
        return numVal;
      }
    }
    
    return 0;
  }

  private extractNiveauRisque(item: any): string {
    if (item.niveauRisque) return item.niveauRisque;
    if (item.niveau) return item.niveau;
    if (item.riskLevel) return item.riskLevel;
    if (item.categorie) return item.categorie;
    
    // Déterminer par le score
    const score = this.extractScoreValue(item);
    if (score < 20) return 'FAIBLE';
    if (score < 40) return 'MOYEN';
    if (score < 70) return 'ELEVE';
    return 'CRITIQUE';
  }

  private updateDepartements(): void {
    this.departements = [...new Set(
      this.scores
        .map(score => score.employeDepartement)
        .filter((dept): dept is string => !!dept && dept.trim() !== '')
    )].sort((a, b) => a.localeCompare(b));
  }

  private handleEmptyResponse(): void {
    this.scores = [];
    this.filteredScores = [];
    this.error = true;
    this.loading = false;
    this.isLoadingScores = false;
    this.cdr.detectChanges();
  }

  private handleErrorResponse(): void {
    this.scores = [];
    this.filteredScores = [];
    this.error = true;
    this.loading = false;
    this.isLoadingScores = false;
    this.cdr.detectChanges();
  }

  calculerStats(): void {
    this.totalEmployes = this.scores.length;
    if (this.totalEmployes === 0) {
      this.scoreMoyen = 0;
      this.nbCritiques = 0;
      return;
    }
    
    let sum = 0;
    let count = 0;
    let critiques = 0;
    
    for (const score of this.scores) {
      if (typeof score.score === 'number' && !isNaN(score.score) && score.score > 0) {
        sum += score.score;
        count++;
      }
      if (this.normalizeNiveau(score.niveauRisque) === 'CRITIQUE') {
        critiques++;
      }
    }
    
    this.scoreMoyen = count > 0 ? Math.round(sum / count) : 0;
    this.nbCritiques = critiques;
    
    console.log(`Stats: total=${this.totalEmployes}, scoreMoyen=${this.scoreMoyen}, nbCritiques=${this.nbCritiques}`);
  }

  applyFilters(): void {
    const searchLower = this.searchText.toLowerCase().trim();
    const hasSearch = searchLower.length > 0;
    const hasDepartement = !!this.departementFilter;
    const hasNiveau = this.niveauFilter !== 'TOUS';
    
    this.filteredScores = this.scores.filter(score => {
      if (hasNiveau && this.normalizeNiveau(score.niveauRisque) !== this.niveauFilter) return false;
      if (hasDepartement && score.employeDepartement !== this.departementFilter) return false;
      
      if (hasSearch) {
        const fullName = `${score.employePrenom || ''} ${score.employeNom || ''}`.toLowerCase();
        const reverseName = `${score.employeNom || ''} ${score.employePrenom || ''}`.toLowerCase();
        if (!fullName.includes(searchLower) && 
            !reverseName.includes(searchLower) && 
            !(score.employeMatricule || '').toLowerCase().includes(searchLower) && 
            !(score.employeDepartement || '').toLowerCase().includes(searchLower)) {
          return false;
        }
      }
      return true;
    });
    
    this.filteredScores.sort((a, b) => (b.score || 0) - (a.score || 0));
    this.cdr.detectChanges();
  }

  resetFilters(): void {
    this.niveauFilter = 'TOUS';
    this.departementFilter = '';
    this.searchText = '';
    this.applyFilters();
  }

  getCountNiveau(niveau: string): number {
    let count = 0;
    for (const score of this.scores) {
      if (this.normalizeNiveau(score.niveauRisque) === niveau) count++;
    }
    return count;
  }

  getPourcentageNiveau(niveau: string): number {
    if (this.totalEmployes === 0) return 0;
    return Math.round((this.getCountNiveau(niveau) / this.totalEmployes) * 100);
  }

  // ==================== DÉTAIL ====================
  
  viewDetail(employeId: number): void {
    this.router.navigate(['/admin/scores', employeId]);
  }

  loadDetail(employeId: number): void {
    if (this.detailLoading) return;
    
    this.detailLoading = true;
    this.detailError = false;
    this.showDetail = true;
    this.loading = false;

    this.managerService.getEmployeScoreDetail(employeId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.detailLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (detail: EmployeScoreDetail) => {
          this.detail = detail || null;
          this.detailError = !detail;
          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error('Erreur chargement détail', err);
          this.detailError = true;
          this.cdr.detectChanges();
        }
      });
  }

  closeDetail(): void {
    this.router.navigate(['/admin/scores']);
    this.showDetail = false;
    this.detail = null;
    this.activeTab = 'overview';
    // Recharger la liste quand on revient
    this.loadScores();
  }

  recalculerScore(employeId: number): void {
    const confirmed = confirm('Recalculer le score de risque pour cet employé ?');
    if (!confirmed) return;
    
    this.managerService.recalculerScoreTurnover(employeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (this.showDetail) {
            this.loadDetail(employeId);
          }
          this.loadScores();
        },
        error: (err: any) => {
          console.error('Erreur recalcul:', err);
          alert('Erreur lors du recalcul du score');
        }
      });
  }

  retry(): void {
    this.loadScores();
  }

  exportCSV(): void {
    const headers = ['Matricule', 'Nom', 'Prénom', 'Département', 'Score', 'Niveau', 'Date calcul'];
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
    
    const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `scores_turnover_${new Date().toISOString().slice(0, 19)}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  // ==================== HELPERS ====================
  
  trackByScoreId(index: number, score: ScoreTurnover): number {
    return score.employeId;
  }

  trackByDept(index: number, dept: string): string {
    return dept;
  }

  getNiveauClass(niveau: string): string {
    const map: Record<string, string> = {
      FAIBLE: 'niveau-faible',
      MOYEN: 'niveau-moyen',
      ELEVE: 'niveau-eleve',
      CRITIQUE: 'niveau-critique'
    };
    return map[this.normalizeNiveau(niveau)] || '';
  }

  getNiveauLabel(niveau: string): string {
    switch (this.normalizeNiveau(niveau)) {
      case 'FAIBLE': return 'FAIBLE';
      case 'MOYEN': return 'MOYEN';
      case 'ELEVE': return 'ÉLEVÉ';
      case 'CRITIQUE': return 'CRITIQUE';
      default: return niveau || '-';
    }
  }

  getNiveauIcon(niveau: string): string {
    switch (this.normalizeNiveau(niveau)) {
      case 'TOUS': return '🎯';
      case 'FAIBLE': return '🟢';
      case 'MOYEN': return '🟠';
      case 'ELEVE': return '🔴';
      case 'CRITIQUE': return '⛔';
      default: return '⚪';
    }
  }

  getImpactClass(impact: string): string {
    switch (impact) {
      case 'Élevé': return 'impact-eleve';
      case 'Moyen': return 'impact-moyen';
      default: return 'impact-faible';
    }
  }

  getNiveauCouleur(score: number): string {
    if (score < 20) return '#10b981';
    if (score < 40) return '#f59e0b';
    if (score < 70) return '#ef4444';
    return '#7f1d1d';
  }

  getScorePourcentage(): number {
    return Math.min(100, Math.max(0, this.detail?.scoreActuel?.score ?? 0));
  }

  getScoreValue(score: any): number {
    if (typeof score === 'number') return score;
    if (typeof score?.score === 'number') return score.score;
    if (typeof score?.score === 'string') return parseFloat(score.score) || 0;
    if (typeof score?.valeur === 'number') return score.valeur;
    if (typeof score?.valeur === 'string') return parseFloat(score.valeur) || 0;
    if (score && typeof score === 'object') {
      for (const key of Object.keys(score)) {
        const val = score[key];
        if (typeof val === 'number' && val >= 0 && val <= 100) return val;
      }
    }
    return 0;
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '—';
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  }

  toEmployeeAvatar(score: ScoreTurnover): any {
    return {
      id: score.employeId,
      prenom: score.employePrenom,
      nom: score.employeNom,
      email: score.employeEmail,
      departement: score.employeDepartement,
      matricule: score.employeMatricule,
      photoUrl: score.photoUrl
    };
  }

  private normalizeNiveau(niveau: string): string {
    const value = String(niveau || '').trim().toUpperCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (value === 'ELEVE' || value === 'ÉLEVÉ') return 'ELEVE';
    return value;
  }
}