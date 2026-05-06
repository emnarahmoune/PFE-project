// src/app/features/admin/gestion-employes/pages/historique-conges-modal/historique-conges-modal.component.ts

import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule
} from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { EmployeService } from '../../../../../core/services/employe.service';
import { DemandeConge } from '../../../../employee/models/conge.model';

@Component({
  selector: 'app-historique-conges-modal',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './historique-conges-modal.component.html',
  styleUrls: ['./historique-conges-modal.component.scss']
})
export class HistoriqueCongesModalComponent implements OnInit {
  conges: DemandeConge[] = [];
  loading = true;

  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: {
      employeId: number;
      employeNom: string;
    },
    public dialogRef: MatDialogRef<HistoriqueCongesModalComponent>,
    private employeService: EmployeService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadConges();
  }

  private loadConges(): void {
    this.loading = true;

    this.employeService.getEmployeConges(this.data.employeId).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.conges = res.data || [];
        } else {
          this.snackBar.open('Erreur chargement des congés', 'Fermer', {
            duration: 3000
          });
        }

        this.loading = false;
      },
      error: () => {
        this.snackBar.open('Erreur technique', 'Fermer', {
          duration: 3000
        });

        this.loading = false;
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}