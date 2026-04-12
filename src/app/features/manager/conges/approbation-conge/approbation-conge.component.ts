// src/app/features/manager/conges/approbation-conge/approbation-conge.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { WorkflowService, Task } from '../../../../core/services/workflow.service';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-approbation-conge',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './approbation-conge.component.html',
  styleUrls: ['./approbation-conge.component.scss']
})
export class ApprobationCongeComponent implements OnInit {
  
  tasks: Task[] = [];
  loading = false;
  isSubmitting = false;
  selectedTask: Task | null = null;
  showModal = false;
  actionType: 'approve' | 'reject' = 'approve';
  commentaire = '';
  motifRefus = '';
  errorMessage = '';

  constructor(
    private workflowService: WorkflowService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    console.log('✅ Composant ApprobationCongeComponent chargé');
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.workflowService.getManagerTasks().subscribe({
      next: (tasks: Task[]) => {
        console.log('📋 Tâches reçues:', tasks);
        this.tasks = tasks || [];
        console.log(`📋 ${this.tasks.length} tâche(s) chargée(s)`);
        this.loading = false;
      },
      error: (error: any) => {
        console.error('❌ Erreur chargement tâches:', error);
        if (error.status === 401 || error.status === 403) {
          this.errorMessage = 'Accès refusé. Vérifiez vos droits.';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de joindre le serveur.';
        } else {
          this.errorMessage = error.error?.message || 'Erreur lors du chargement.';
        }
        this.loading = false;
      }
    });
  }

  openApproveModal(task: Task): void {
    this.selectedTask = task;
    this.actionType = 'approve';
    this.commentaire = '';
    this.showModal = true;
  }

  openRejectModal(task: Task): void {
    this.selectedTask = task;
    this.actionType = 'reject';
    this.motifRefus = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedTask = null;
    this.commentaire = '';
    this.motifRefus = '';
  }

  submitAction(): void {
    if (!this.selectedTask || this.isSubmitting) return;
    this.isSubmitting = true;

    if (this.actionType === 'approve') {
      this.workflowService.approveTask(this.selectedTask.taskId, this.commentaire).subscribe({
        next: (response: any) => {
          console.log('✅ Tâche approuvée:', response);
          this.notificationService.showSuccess('✅ Demande approuvée avec succès');
          this.closeModal();
          this.loadTasks();
          this.isSubmitting = false;
        },
        error: (error: any) => {
          console.error('❌ Erreur approbation:', error);
          const message = error.error?.message || 'Erreur lors de l\'approbation';
          this.notificationService.showError(message);
          this.isSubmitting = false;
        }
      });
    } else {
      if (!this.motifRefus.trim()) {
        this.notificationService.showError('Veuillez saisir un motif de refus');
        this.isSubmitting = false;
        return;
      }
      this.workflowService.rejectTask(this.selectedTask.taskId, this.motifRefus).subscribe({
        next: (response: any) => {
          console.log('✅ Tâche refusée:', response);
          this.notificationService.showSuccess('❌ Demande refusée avec succès');
          this.closeModal();
          this.loadTasks();
          this.isSubmitting = false;
        },
        error: (error: any) => {
          console.error('❌ Erreur rejet:', error);
          const message = error.error?.message || 'Erreur lors du rejet';
          this.notificationService.showError(message);
          this.isSubmitting = false;
        }
      });
    }
  }

  getEmployeInfo(): string {
    if (!this.selectedTask) return '';
    const nom = this.selectedTask.employeNom || '';
    const prenom = this.selectedTask.employePrenom || '';
    const employeId = this.selectedTask.employeId || '';
    return `${prenom} ${nom}`.trim() || `Employé #${employeId}`;
  }

  getNbJours(): number {
    return this.selectedTask?.nbJours || 0;
  }

  getMotif(): string {
    return this.selectedTask?.commentaire || '';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  // ✅ Méthode pour obtenir le libellé du type de congé
  getTypeLabel(type: string | undefined): string {
    switch (type) {
      case 'ANNUEL': return 'Annuel';
      case 'MALADIE': return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE': return 'Maternité';
      case 'PATERNITE': return 'Paternité';
      default: return type || 'Congé';
    }
  }

  // ✅ Méthode pour obtenir la couleur du type de congé
  getTypeColor(type: string | undefined): string {
    switch (type) {
      case 'ANNUEL': return '#1976d2';
      case 'MALADIE': return '#dc3545';
      case 'SANS_SOLDE': return '#ffc107';
      case 'MATERNITE': return '#28a745';
      case 'PATERNITE': return '#28a745';
      default: return '#6c757d';
    }
  }
}