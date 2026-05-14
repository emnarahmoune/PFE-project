import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';

import { ManagerService, EmployeScoreDetail } from '../../../../../core/services/manager.service';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-employe-score-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, EmployeeAvatarComponent],
  templateUrl: './employe-score-detail.component.html',
  styleUrls: ['./employe-score-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmployeScoreDetailComponent implements OnInit {
  detail: EmployeScoreDetail | null = null;
  loading = true;
  error = false;
  activeTab: 'overview' | 'factors' | 'history' | 'actions' | 'params' = 'overview';

  niveauxRisque = [
    { niveau: 'FAIBLE',   min: 0,  max: 20,  couleur: '#10b981' },
    { niveau: 'MOYEN',    min: 20, max: 40,  couleur: '#f59e0b' },
    { niveau: 'ÉLEVÉ',   min: 40, max: 70,  couleur: '#ef4444' },
    { niveau: 'CRITIQUE', min: 70, max: 100, couleur: '#7f1d1d' }
  ];

  constructor(
    private route: ActivatedRoute,
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadDetail(+id);
    } else {
      this.error   = true;
      this.loading = false;
    }
  }

  loadDetail(employeId: number): void {
    this.loading = true;
    this.error   = false;

    // getEmployeScoreDetail() retourne Observable<EmployeScoreDetail>
    // Le map() dans le service unwrap déjà ApiResponse<T> → T,
    // donc on reçoit directement EmployeScoreDetail ici.
    this.managerService.getEmployeScoreDetail(employeId)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (detail: EmployeScoreDetail) => {
          if (detail) {
            this.detail = detail;
          } else {
            this.error = true;
          }
          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error('Erreur chargement détail score', err);
          this.error = true;
          this.cdr.detectChanges();
        }
      });
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  getNiveauClass(niveau: string): string {
    const map: Record<string, string> = {
      FAIBLE:   'niveau-faible',
      MOYEN:    'niveau-moyen',
      ELEVE:    'niveau-eleve',
      CRITIQUE: 'niveau-critique'
    };
    return map[niveau] ?? '';
  }

  getNiveauIcon(niveau: string): string {
    const map: Record<string, string> = {
      FAIBLE:   '🟢',
      MOYEN:    '🟠',
      ELEVE:    '🔴',
      CRITIQUE: '⛔'
    };
    return map[niveau] ?? '⚪';
  }

  getScoreBarClass(score: number): string {
    if (score < 20) return 'score-bar-faible';
    if (score < 40) return 'score-bar-moyen';
    if (score < 70) return 'score-bar-eleve';
    return 'score-bar-critique';
  }

  getImpactClass(impact: string): string {
    switch (impact) {
      case 'Élevé': return 'impact-eleve';
      case 'Moyen': return 'impact-moyen';
      default:      return 'impact-faible';
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

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day:   '2-digit',
      month: 'long',
      year:  'numeric'
    });
  }

  retry(): void {
    const id = this.route.snapshot.params['id'];
    if (id) this.loadDetail(+id);
  }

  goBack(): void {
    window.history.back();
  }
}