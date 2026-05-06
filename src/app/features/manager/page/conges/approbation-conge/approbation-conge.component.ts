import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabGroup, MatTab } from '@angular/material/tabs';
import { MatIcon } from '@angular/material/icon';

import { WorkflowService, Task } from '../../../../../core/services/workflow.service';
import { ManagerService } from '../../../../../core/services/manager.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { NotificationService } from '../../../../../core/services/notification.service';
import { KeycloakInitService } from '../../../../../core/services/keycloak-init.service';

import { Employe } from '../../../../admin/gestion-employes/models/employe.model';
import { DemandeConge, SoldeConges } from '../../../../employee/models/conge.model';
import { ManagerCalendarComponent } from '../../manager-calendar/manager-calendar.component';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-approbation-conge',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatTabGroup,
    MatTab,
    MatIcon,
    ManagerCalendarComponent,
    EmployeeAvatarComponent
  ],
  templateUrl: './approbation-conge.component.html',
  styleUrls: ['./approbation-conge.component.scss']
})
export class ApprobationCongeComponent implements OnInit {

  loading = false;
  isSubmitting = false;
  errorMessage = '';

  tasks: any[] = [];
  equipeSize = 0;

  showModal = false;
  actionType: 'approve' | 'reject' = 'approve';
  commentaire = '';
  motifRefus = '';

  showDetailsModal = false;
  currentTask: Task | null = null;
  employeDetails: Employe | null = null;
  employeSolde: SoldeConges | null = null;
  employeHistorique: DemandeConge[] = [];
  loadingDetails = false;
  detailsError = '';

  currentUserEmail = '';

  selectedTaskForApproval: Task | null = null;

  constructor(
    private workflowService: WorkflowService,
    private managerService: ManagerService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private keycloakInit: KeycloakInitService
  ) {}

  ngOnInit(): void {
    this.loadPageData();
    this.loadCurrentUserEmail();
  }

  loadCurrentUserEmail(): void {
    const user = this.keycloakInit.getUser();

    this.currentUserEmail =
      user?.email ||
      user?.preferred_username ||
      user?.username ||
      '';

    if (!this.currentUserEmail) {
      const authAny = this.authService as any;

      const authUser =
        authAny.getCurrentUser?.() ||
        authAny.getUser?.() ||
        authAny.currentUserValue ||
        authAny.currentUser ||
        authAny.user ||
        null;

      this.currentUserEmail =
        authUser?.email ||
        authUser?.preferred_username ||
        authUser?.username ||
        '';
    }

    if (!this.currentUserEmail) {
      const token =
        localStorage.getItem('token') ||
        localStorage.getItem('access_token') ||
        localStorage.getItem('kc_token') ||
        sessionStorage.getItem('token') ||
        sessionStorage.getItem('access_token') ||
        '';

      this.currentUserEmail = this.extractEmailFromToken(token);
    }

    console.log('EMAIL MANAGER CONNECTÉ = ', this.currentUserEmail);
  }

  private extractEmailFromToken(token: string): string {
    try {
      if (!token || !token.includes('.')) {
        return '';
      }

      const payload = token.split('.')[1];

      const decoded = JSON.parse(
        atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
      );

      return decoded?.email || decoded?.preferred_username || '';
    } catch {
      return '';
    }
  }

  isOwnRequest(tache: any): boolean {
    const employeEmail = String(tache?.employeEmail || '').trim().toLowerCase();
    const currentEmail = String(this.currentUserEmail || '').trim().toLowerCase();

    return !!employeEmail && !!currentEmail && employeEmail === currentEmail;
  }

  canTreatManager(tache: any): boolean {
    return !this.isOwnRequest(tache);
  }

  loadPageData(): void {
    this.loadTasks();
    this.loadEquipeSize();
  }

  loadTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.workflowService.getManagerTasks().subscribe({
      next: (res: any) => {
        const data = this.unwrapResponse<any[]>(res, []);
        this.tasks = Array.isArray(data) ? data : [];

        this.tasks = this.tasks.map((task: any) => ({
          ...task,
          urgente: this.isTaskUrgent(task)
        }));

        console.log('TASKS MANAGER = ', this.tasks);

        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur chargement tâches:', error);
        this.errorMessage = 'Impossible de charger les demandes. Veuillez réessayer.';
        this.tasks = [];
        this.loading = false;
      }
    });
  }

  loadEquipeSize(): void {
    this.managerService.getEquipe().subscribe({
      next: (res: any) => {
        const equipe = this.unwrapResponse<any[]>(res, []);

        this.equipeSize = Array.isArray(equipe) ? equipe.length : 0;

        console.log('EQUIPE APPROBATION = ', equipe);
        console.log('NB MEMBRES EQUIPE = ', this.equipeSize);
      },
      error: (err: any) => {
        console.error('Erreur chargement équipe approbation:', err);
        this.equipeSize = 0;
      }
    });
  }

  private unwrapResponse<T>(response: any, fallback: T): T {
    if (!response) {
      return fallback;
    }

    if (response.data !== undefined) {
      return response.data as T;
    }

    return response as T;
  }

  getUrgentTasksCount(): number {
    return this.tasks?.filter((task: any) => this.isTaskUrgent(task)).length || 0;
  }

  isTaskUrgent(task: any): boolean {
    if (!task) {
      return false;
    }

    if (task.urgente === true || task.urgent === true || task.isUrgent === true) {
      return true;
    }

    if (
      String(task.urgente).toLowerCase() === 'true' ||
      String(task.urgent).toLowerCase() === 'true'
    ) {
      return true;
    }

    if (task.dateDebut) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      const dateDebut = new Date(task.dateDebut);
      dateDebut.setHours(0, 0, 0, 0);

      const diffMs = dateDebut.getTime() - today.getTime();
      const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

      return diffDays >= 0 && diffDays < 7;
    }

    return false;
  }

  getTotalApproved(): number {
    return this.tasks?.filter((task: any) => {
      const statut = String(task.statut || task.status || '').toUpperCase();
      return statut === 'APPROUVE' || statut === 'APPROUVÉ' || statut === 'APPROVED';
    }).length || 0;
  }

  openApproveModal(task: Task): void {
    if (this.isOwnRequest(task)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande de congé.');
      return;
    }

    this.selectedTaskForApproval = task;
    this.actionType = 'approve';
    this.commentaire = '';
    this.showModal = true;
  }

  openRejectModal(task: Task): void {
    if (this.isOwnRequest(task)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande de congé.');
      return;
    }

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
    if (!this.selectedTaskForApproval || this.isSubmitting) {
      return;
    }

    if (this.isOwnRequest(this.selectedTaskForApproval)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande de congé.');
      this.closeModal();
      return;
    }

    this.isSubmitting = true;

    if (this.actionType === 'approve') {
      this.workflowService.approveTask(
        this.selectedTaskForApproval.taskId,
        this.commentaire
      ).subscribe({
        next: () => {
          this.notificationService.showSuccess('Demande approuvée avec succès');
          this.closeModal();
          this.loadPageData();
          this.isSubmitting = false;
        },
        error: (error: any) => {
          this.notificationService.showError(error.error?.message || 'Erreur lors de l’approbation');
          this.isSubmitting = false;
        }
      });

      return;
    }

    if (!this.motifRefus.trim()) {
      this.notificationService.showError('Veuillez saisir un motif de refus');
      this.isSubmitting = false;
      return;
    }

    this.workflowService.rejectTask(
      this.selectedTaskForApproval.taskId,
      this.motifRefus
    ).subscribe({
      next: () => {
        this.notificationService.showSuccess('Demande refusée avec succès');
        this.closeModal();
        this.loadPageData();
        this.isSubmitting = false;
      },
      error: (error: any) => {
        this.notificationService.showError(error.error?.message || 'Erreur lors du refus');
        this.isSubmitting = false;
      }
    });
  }

  openDetailsModal(task: Task): void {
    this.currentTask = task;
    this.showDetailsModal = true;
    this.loadingDetails = true;
    this.detailsError = '';

    this.employeDetails = null;
    this.employeSolde = null;
    this.employeHistorique = [];

    const taskAny = task as any;

    const employeId =
      taskAny.employeId ||
      taskAny.employeeId ||
      taskAny.idEmploye ||
      taskAny.employe?.id ||
      null;

    console.log('DETAIL TASK = ', taskAny);
    console.log('DETAIL EMPLOYE ID = ', employeId);

    if (!employeId) {
      this.detailsError = 'Impossible d’identifier l’employé associé à cette demande.';
      this.loadingDetails = false;
      return;
    }

    const detailsPromise = this.managerService
      .getEmployeDetails(Number(employeId))
      .toPromise()
      .then((res: any) => {
        this.employeDetails = this.unwrapResponse<Employe | null>(res, null);
      })
      .catch((err: any) => {
        console.error('Erreur détails employé:', err);
        this.employeDetails = null;
      });

    const soldePromise = this.managerService
      .getEmployeSoldeConges(Number(employeId))
      .toPromise()
      .then((res: any) => {
        this.employeSolde = this.unwrapResponse<SoldeConges | null>(res, null);
      })
      .catch((err: any) => {
        console.error('Erreur solde congés:', err);
        this.employeSolde = null;
      });

    const historiquePromise = this.managerService
      .getEmployeHistoriqueConges(Number(employeId))
      .toPromise()
      .then((res: any) => {
        this.employeHistorique = this.unwrapResponse<DemandeConge[]>(res, []);
      })
      .catch((err: any) => {
        console.error('Erreur historique congés:', err);
        this.employeHistorique = [];
      });

    Promise.all([detailsPromise, soldePromise, historiquePromise])
      .then(() => {
        this.loadingDetails = false;
      })
      .catch((err: any) => {
        console.error('Erreur globale détails:', err);
        this.detailsError = 'Erreur lors du chargement des informations.';
        this.loadingDetails = false;
      });
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.currentTask = null;
  }

  getEmployeInfo(task: Task): string {
    const nom = task.employeNom || '';
    const prenom = task.employePrenom || '';
    return `${prenom} ${nom}`.trim() || `Employé #${task.employeId}`;
  }

  getNbJours(task: Task): number {
    const t = task as any;
    return t.nbJours || t.joursOuvres || t.jours_ouvres || 0;
  }

  getMotif(task: Task): string {
    return task.commentaire || '';
  }

  getTaskType(task: any): string {
    return task?.typeConge || task?.type || task?.typeDemande || task?.categorie || 'Congé';
  }

  getTaskRequestDate(task: any): string | undefined {
    return task?.createTime || task?.dateDemande || task?.dateSoumission || task?.createdAt;
  }

  getSoldeDisponible(solde: any): number {
    return solde?.soldeActuel || solde?.soldeRestant || solde?.soldeDisponible || solde?.joursRestants || 0;
  }

  getCongeType(conge: any): string {
    return conge?.typeConge || conge?.type || conge?.typeDemande || 'Congé';
  }

  getCongeStatut(conge: any): string {
    return conge?.statut || conge?.status || 'EN_ATTENTE';
  }

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) {
      return '';
    }

    const date = new Date(dateStr);
    return date.toLocaleDateString('fr-FR');
  }

  getTypeLabel(type: string | undefined): string {
    switch (type) {
      case 'ANNUEL':
        return 'Annuel';
      case 'MALADIE':
        return 'Maladie';
      case 'SANS_SOLDE':
        return 'Sans solde';
      case 'MATERNITE':
        return 'Maternité';
      case 'PATERNITE':
        return 'Paternité';
      default:
        return type || 'Congé';
    }
  }

  getTypeColor(type: string | undefined): string {
    switch (type) {
      case 'ANNUEL':
        return '#1976d2';
      case 'MALADIE':
        return '#dc3545';
      case 'SANS_SOLDE':
        return '#ffc107';
      case 'MATERNITE':
      case 'PATERNITE':
        return '#28a745';
      default:
        return '#6c757d';
    }
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'APPROUVE':
        return 'approved';
      case 'REFUSE':
        return 'rejected';
      case 'EN_ATTENTE':
        return 'pending';
      case 'ANNULE':
        return 'cancelled';
      default:
        return '';
    }
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'APPROUVE':
        return 'Approuvé';
      case 'REFUSE':
        return 'Refusé';
      case 'EN_ATTENTE':
        return 'En attente';
      case 'ANNULE':
        return 'Annulé';
      default:
        return statut;
    }
  }
}