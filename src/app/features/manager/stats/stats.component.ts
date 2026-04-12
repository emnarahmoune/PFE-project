import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkflowService } from '../../../core/services/workflow.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-manager-stats',
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
  styleUrls: ['./stats.component.css']
})
export class StatsComponent implements OnInit {
  loading = false;
  stats = {
    totalDemandes: 0,
    demandesApprouvees: 0,
    demandesRejetees: 0,
    demandesEnAttente: 0,
    tauxApprobation: 0
  };
  
  demandesRecentes: any[] = [];

  constructor(
    private workflowService: WorkflowService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    // Implémente la récupération des stats depuis ton backend
    // Pour l'instant, données mockées
    setTimeout(() => {
      this.stats = {
        totalDemandes: 24,
        demandesApprouvees: 18,
        demandesRejetees: 3,
        demandesEnAttente: 3,
        tauxApprobation: 75
      };
      this.demandesRecentes = [
        { employe: 'Jean Dupont', nbJours: 5, date: '2024-03-25', statut: 'Approuvé' },
        { employe: 'Marie Martin', nbJours: 3, date: '2024-03-24', statut: 'En attente' },
        { employe: 'Pierre Durand', nbJours: 7, date: '2024-03-23', statut: 'Approuvé' },
        { employe: 'Sophie Bernard', nbJours: 2, date: '2024-03-22', statut: 'Rejeté' }
      ];
      this.loading = false;
    }, 500);
  }
}