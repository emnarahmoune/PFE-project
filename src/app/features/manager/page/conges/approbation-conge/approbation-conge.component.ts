import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkflowService, Task } from '../../../../../core/services/workflow.service';
import { ManagerService } from '../../../../../core/services/manager.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { NotificationService } from '../../../../../core/services/notification.service';
import { Employe } from '../../../../admin/gestion-employes/models/employe.model';
import { DemandeConge, SoldeConges } from '../../../../employee/models/conge.model';
import { ManagerCalendarComponent } from '../../manager-calendar/manager-calendar.component';
import { MatTabGroup, MatTab } from "@angular/material/tabs";
import { MatIcon } from "@angular/material/icon";

@Component({
  selector: 'app-approbation-conge',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatProgressSpinnerModule, ManagerCalendarComponent, MatTabGroup, MatTab, MatIcon],
  templateUrl: './approbation-conge.component.html',
  styleUrls: ['./approbation-conge.component.scss']
})
export class ApprobationCongeComponent implements OnInit {
equipeSize: any;
getTotalApproved() {
throw new Error('Method not implemented.');
}
  
  
  loading = false;
  isSubmitting = false;
  
  // Modals
  showModal = false;
  actionType: 'approve' | 'reject' = 'approve';
  commentaire = '';
  motifRefus = '';
  errorMessage = '';
  tasks: any[] = [];
  
  // Détails employé
  showDetailsModal = false;
  currentTask: Task | null = null;
  employeDetails: Employe | null = null;
  employeSolde: SoldeConges | null = null;
  employeHistorique: DemandeConge[] = [];
  loadingDetails = false;
  detailsError = '';
  

  // Pour les modaux d'approbation/refus
  selectedTaskForApproval: Task | null = null;

  constructor(
    private workflowService: WorkflowService,
    private managerService: ManagerService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadTasks();
  }


getUrgentTasksCount(): number {
  return this.tasks?.filter((task: any) => task.urgente).length || 0;
}
  loadTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.workflowService.getManagerTasks().subscribe({
      next: (tasks: Task[]) => {
        this.tasks = tasks || [];
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur chargement tâches:', error);
        this.errorMessage = 'Impossible de charger les demandes. Veuillez réessayer.';
        this.loading = false;
      }
    });
  }

  // ==================== MODAL D'APPROBATION / REFUS ====================
  openApproveModal(task: Task): void {
    this.selectedTaskForApproval = task;
    this.actionType = 'approve';
    this.commentaire = '';
    this.showModal = true;
  }

  openRejectModal(task: Task): void {
    this.selectedTaskForApproval = task;
    this.actionType = 'reject';
    this.motifRefus = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedTaskForApproval = null;
    this.commentaire = '';
    this.motifRefus = '';
  }

  submitAction(): void {
    if (!this.selectedTaskForApproval || this.isSubmitting) return;
    this.isSubmitting = true;

    if (this.actionType === 'approve') {
      this.workflowService.approveTask(this.selectedTaskForApproval.taskId, this.commentaire).subscribe({
        next: () => {
          this.notificationService.showSuccess('Demande approuvée avec succès');
          this.closeModal();
          this.loadTasks();
          this.isSubmitting = false;
        },
        error: (error) => {
          this.notificationService.showError(error.error?.message || 'Erreur lors de l\'approbation');
          this.isSubmitting = false;
        }
      });
    } else {
      if (!this.motifRefus.trim()) {
        this.notificationService.showError('Veuillez saisir un motif de refus');
        this.isSubmitting = false;
        return;
      }
      this.workflowService.rejectTask(this.selectedTaskForApproval.taskId, this.motifRefus).subscribe({
        next: () => {
          this.notificationService.showSuccess('Demande refusée avec succès');
          this.closeModal();
          this.loadTasks();
          this.isSubmitting = false;
        },
        error: (error) => {
          this.notificationService.showError(error.error?.message || 'Erreur lors du refus');
          this.isSubmitting = false;
        }
      });
    }
  }

  // ==================== MODAL DE DÉTAILS ====================
  openDetailsModal(task: Task): void {
    this.currentTask = task;
    this.showDetailsModal = true;
    this.loadingDetails = true;
    this.detailsError = '';
    this.employeDetails = null;
    this.employeSolde = null;
    this.employeHistorique = [];

    const employeId = task.employeId ? Number(task.employeId) : null;
    if (!employeId) {
      this.detailsError = 'Impossible d\'identifier l\'employé.';
      this.loadingDetails = false;
      return;
    }

    Promise.all([
      this.managerService.getEmployeDetails(employeId).toPromise(),
      this.managerService.getEmployeSoldeConges(employeId).toPromise(),
      this.managerService.getEmployeHistoriqueConges(employeId).toPromise()
    ]).then(([empRes, soldeRes, histRes]) => {
      this.employeDetails = empRes?.data || null;
      this.employeSolde = soldeRes?.data || null;
      this.employeHistorique = histRes?.data || [];
      this.loadingDetails = false;
    }).catch(err => {
      console.error(err);
      this.detailsError = 'Erreur lors du chargement des informations.';
      this.loadingDetails = false;
    });
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.currentTask = null;
  }

  // ==================== MÉTHODES UTILITAIRES ====================
  getEmployeInfo(task: Task): string {
    const nom = task.employeNom || '';
    const prenom = task.employePrenom || '';
    return `${prenom} ${nom}`.trim() || `Employé #${task.employeId}`;
  }

  getNbJours(task: Task): number {
    return task.nbJours || 0;
  }

  getMotif(task: Task): string {
    return task.commentaire || '';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

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

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'approved';
      case 'REFUSE': return 'rejected';
      case 'EN_ATTENTE': return 'pending';
      case 'ANNULE': return 'cancelled';
      default: return '';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'Approuvé';
      case 'REFUSE': return 'Refusé';
      case 'EN_ATTENTE': return 'En attente';
      case 'ANNULE': return 'Annulé';
      default: return statut;
    }
  }
  
}