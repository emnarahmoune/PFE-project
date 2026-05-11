// src/app/shared/components/scores-table/scores-table.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScoreTurnover } from '../../../../../core/models/score-turnover.model';

@Component({
  selector: 'app-scores-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="table-wrapper">
      <table>
        <thead>
          <tr><th>Employé</th><th>Département</th><th>Score</th><th>Niveau</th><th>Date</th><th>Actions</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let score of scores">
            <td>{{ score.employePrenom }} {{ score.employeNom }}</td>
            <td>{{ score.employeDepartement || '-' }}</td>
            <td>{{ score.score }}</td>
            <td><span class="badge" [ngClass]="getNiveauClass(score.niveauRisque)">{{ score.niveauRisque }}</span></td>
            <td>{{ score.datePrediction | date:'dd/MM/yyyy' }}</td>
            <td><button (click)="recalculer.emit(score.employeId)">Recalculer</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .table-wrapper { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #e2e8f0; }
    th { background: #f1f5f9; font-weight: 600; }
    .badge { padding: 0.25rem 0.75rem; border-radius: 2rem; font-size: 0.75rem; }
    .niveau-faible { background: #dcfce7; color: #15803d; }
    .niveau-moyen { background: #fef3c7; color: #b45309; }
    .niveau-eleve { background: #fee2e2; color: #b91c1c; }
    .niveau-critique { background: #7f1d1d; color: white; }
    button { background: none; border: 1px solid #6366f1; color: #4f46e5; padding: 0.25rem 0.75rem; border-radius: 2rem; cursor: pointer; }
  `]
})
export class ScoresTableComponent {
  @Input() scores: ScoreTurnover[] = [];
  @Output() recalculer = new EventEmitter<number>();

  getNiveauClass(niveau: string): string {
    switch (niveau) {
      case 'FAIBLE': return 'niveau-faible';
      case 'MOYEN': return 'niveau-moyen';
      case 'ELEVE': return 'niveau-eleve';
      case 'CRITIQUE': return 'niveau-critique';
      default: return '';
    }
  }
}