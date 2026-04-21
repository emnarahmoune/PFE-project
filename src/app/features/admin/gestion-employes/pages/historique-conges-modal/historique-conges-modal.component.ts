// src/app/features/admin/gestion-employes/pages/historique-conges-modal/historique-conges-modal.component.ts
import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EmployeService } from '../../services/employe.service';
import { DemandeConge } from '../../../../employee/models/conge.model';

@Component({
  selector: 'app-historique-conges-modal',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatSnackBarModule],
  template: `
    <h2 mat-dialog-title>📅 Congés de {{ data.employeNom }}</h2>
    <mat-dialog-content>
      <div *ngIf="loading" class="loading-spinner">Chargement...</div>
      <table *ngIf="!loading && conges.length > 0" class="conges-table">
        <thead>
          <tr><th>Dates</th><th>Type</th><th>Jours</th><th>Statut</th><th>Décision</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let c of conges">
            <td>{{ c.dateDebut | date:'dd/MM/yyyy' }} → {{ c.dateFin | date:'dd/MM/yyyy' }}</td>
            <td>{{ c.type }}</td>
            <td>{{ c.joursOuvres }}</td>
            <td><span class="status-badge" [class]="'status-' + (c.statut?.toLowerCase() ?? '')">{{ c.statut }}</span></td>
            <td>{{ c.dateDecision ? (c.dateDecision | date:'dd/MM/yyyy') : '-' }}</td>
          </tr>
        </tbody>
      </table>
      <div *ngIf="!loading && conges.length === 0" class="empty">Aucune demande de congé.</div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .conges-table { width: 100%; border-collapse: collapse; }
    .conges-table th, .conges-table td { padding: 10px; text-align: left; border-bottom: 1px solid #e9ecef; }
    .status-badge { padding: 4px 8px; border-radius: 20px; font-size: 0.75rem; font-weight: 500; }
    .status-approuve { background: #d4edda; color: #155724; }
    .status-refuse { background: #f8d7da; color: #721c24; }
    .status-attente { background: #fff3cd; color: #856404; }
    .status-annule { background: #e2e3e5; color: #383d41; }
    .loading-spinner { text-align: center; padding: 20px; }
    .empty { text-align: center; padding: 30px; color: #6c757d; }
  `]
})
export class HistoriqueCongesModalComponent implements OnInit {
  conges: DemandeConge[] = [];
  loading = true;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { employeId: number; employeNom: string },
    public dialogRef: MatDialogRef<HistoriqueCongesModalComponent>,
    private employeService: EmployeService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.employeService.getEmployeConges(this.data.employeId).subscribe({
      next: (res) => {
        if (res.success) this.conges = res.data;
        else this.snackBar.open('Erreur chargement des congés', 'Fermer', { duration: 3000 });
        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Erreur technique', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }
}