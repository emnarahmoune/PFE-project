import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { WorkflowService } from '../../core/services/workflow.service';
import { AuthService } from '../../core/services/auth.service';

interface StatsConges {
  totalDemandes: number;
  demandesApprouvees: number;
  demandesRejetees: number;
  demandesEnAttente: number;
  tauxApprobation: number;
}

interface DemandeRecente {
  employe: string;
  nbJours: number;
  date: string;
  statut: 'Approuvé' | 'Rejeté' | 'En attente' | string;
}

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {
  loading = false;

  stats: StatsConges = {
    totalDemandes: 0,
    demandesApprouvees: 0,
    demandesRejetees: 0,
    demandesEnAttente: 0,
    tauxApprobation: 0
  };

  demandesRecentes: DemandeRecente[] = [];

  constructor(
    private workflowService: WorkflowService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;

    /*
      Logique actuelle conservée :
      - pas d'appel backend réel pour l'instant
      - données mockées comme dans ton ancien composant

      Quand ton backend sera prêt, tu pourras remplacer le setTimeout()
      par un appel workflowService sans changer le HTML ni le style.
    */
    setTimeout(() => {
      this.stats = {
        totalDemandes: 24,
        demandesApprouvees: 18,
        demandesRejetees: 3,
        demandesEnAttente: 3,
        tauxApprobation: 75
      };

      this.demandesRecentes = [
        {
          employe: 'Jean Dupont',
          nbJours: 5,
          date: '2024-03-25',
          statut: 'Approuvé'
        },
        {
          employe: 'Marie Martin',
          nbJours: 3,
          date: '2024-03-24',
          statut: 'En attente'
        },
        {
          employe: 'Pierre Durand',
          nbJours: 7,
          date: '2024-03-23',
          statut: 'Approuvé'
        },
        {
          employe: 'Sophie Bernard',
          nbJours: 2,
          date: '2024-03-22',
          statut: 'Rejeté'
        }
      ];

      this.loading = false;
    }, 500);
  }

  refresh(): void {
    this.loadStats();
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'Approuvé':
        return 'status-approved';

      case 'Rejeté':
        return 'status-rejected';

      case 'En attente':
        return 'status-pending';

      default:
        return '';
    }
  }

  trackByDemande(index: number, demande: DemandeRecente): string {
    return `${demande.employe}-${demande.date}-${index}`;
  }
}