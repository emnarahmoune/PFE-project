// src/app/features/admin/pages/manager-detail/manager-detail.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { EmployeService } from '../../services/employe.service';
import { Employe } from '../../models/employe.model';
import { DemandeConge } from '../../../../employee/models/conge.model';
import { HistoriqueCongesModalComponent } from '../historique-conges-modal/historique-conges-modal.component';

@Component({
  selector: 'app-manager-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatSnackBarModule, MatDialogModule],
  template: `
    <div class="manager-detail">
      <div class="header" *ngIf="manager">
        <button class="back-btn" routerLink="/admin/managers">← Retour</button>
        <h1>{{ manager.prenom }} {{ manager.nom }}</h1>
        <p>{{ manager.poste }} · {{ manager.departement }}</p>
      </div>

      <div class="loading" *ngIf="loading">Chargement...</div>

      <div class="equipe-section" *ngIf="!loading && equipe.length">
        <h2>👥 Équipe ({{ equipe.length }})</h2>
        <div class="employe-list">
          <div class="employe-card" *ngFor="let emp of equipe">
            <div class="employe-avatar" [style.background]="getAvatarColor(emp.departement ?? '')">
              {{ (emp.prenom?.charAt(0) ?? '') + (emp.nom?.charAt(0) ?? '') }}
            </div>
            <div class="employe-info">
              <strong>{{ emp.prenom }} {{ emp.nom }}</strong>
              <small>{{ emp.poste }}</small>
            </div>
            <button class="btn-conges" (click)="voirHistorique(emp)">📅 Congés</button>
          </div>
        </div>
      </div>

      <div class="empty" *ngIf="!loading && equipe.length === 0">Aucun employé dans cette équipe.</div>
    </div>
  `,
  styles: [`
    .manager-detail { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .header { margin-bottom: 32px; }
    .back-btn { background: none; border: none; color: #667eea; cursor: pointer; font-size: 0.9rem; margin-bottom: 16px; }
    h1 { margin: 0 0 4px; }
    .employe-list { display: flex; flex-direction: column; gap: 12px; }
    .employe-card { display: flex; align-items: center; gap: 16px; padding: 12px; background: white; border-radius: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .employe-avatar { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; }
    .employe-info { flex: 1; display: flex; flex-direction: column; }
    .btn-conges { background: none; border: 1px solid #dee2e6; padding: 6px 12px; border-radius: 20px; cursor: pointer; }
    .loading, .empty { text-align: center; padding: 40px; color: #6c757d; }
  `]
})
export class ManagerDetailComponent implements OnInit {
  manager: Employe | null = null;
  equipe: Employe[] = [];
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private employeService: EmployeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) this.loadManager(+id);
  }

  loadManager(id: number): void {
    this.employeService.getById(id).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.manager = res.data;
          this.loadEquipe(id);
        } else {
          this.snackBar.open('Manager introuvable', 'Fermer', { duration: 3000 });
          this.loading = false;
        }
      },
      error: () => {
        this.snackBar.open('Erreur chargement manager', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadEquipe(managerId: number): void {
    this.employeService.getEquipeByManagerId(managerId).subscribe({
      next: (res) => {
        if (res.success) this.equipe = res.data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Erreur chargement équipe', 'Fermer', { duration: 3000 });
      }
    });
  }

  voirHistorique(employe: Employe): void {
    this.dialog.open(HistoriqueCongesModalComponent, {
      width: '800px',
      data: { employeId: employe.id, employeNom: `${employe.prenom} ${employe.nom}` }
    });
  }

  getAvatarColor(dept: string): string {
    const colors: Record<string, string> = {
      'RH': '#8b5cf6', 'Technique': '#0891b2', 'Commercial': '#d97706',
      'Finance': '#059669', 'Marketing': '#db2777', 'Direction': '#7c3aed', 'Logistique': '#4f46e5'
    };
    return colors[dept] || '#6366f1';
  }
}