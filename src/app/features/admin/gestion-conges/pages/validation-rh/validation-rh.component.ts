import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { FullCalendarModule, FullCalendarComponent } from '@fullcalendar/angular';
import { EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import multiMonthPlugin from '@fullcalendar/multimonth';

import {
  AdminCongeService,
  TacheRh,
  StatsConges,
  DemandeRefusManager,
  DemandeRefusDetails
} from '../../../../../core/services/admin-conge.service';

import { KeycloakInitService } from '../../../../../core/services/keycloak-init.service';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-validation-rh',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,

    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatTooltipModule,
    MatTabsModule,
    MatBadgeModule,
    MatFormFieldModule,
    MatSelectModule,

    FullCalendarModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './validation-rh.component.html',
  styleUrls: ['./validation-rh.component.scss']
})
export class ValidationRhComponent implements OnInit, OnDestroy {

  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;

  currentUserEmail = '';

  taches: TacheRh[] = [];
  loadingTaches = false;

  refusManager: DemandeRefusManager[] = [];
  loadingRefus = false;

  stats: StatsConges = {
    EN_ATTENTE: 0,
    APPROUVE: 0,
    REFUSE: 0
  };

  displayedColumns = ['employe', 'periode', 'jours', 'type', 'actions'];
  displayedColumnsRefus = ['employe', 'manager', 'periode', 'motif', 'dateDecision'];

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth();

  years: number[] = [];

  months = [
    'Janvier',
    'Février',
    'Mars',
    'Avril',
    'Mai',
    'Juin',
    'Juillet',
    'Août',
    'Septembre',
    'Octobre',
    'Novembre',
    'Décembre'
  ];

  calendarOptions: any = {
    initialView: 'dayGridMonth',
    locale: 'fr',
    plugins: [
      dayGridPlugin,
      interactionPlugin,
      multiMonthPlugin
    ],
    events: [] as EventInput[],
    height: 'auto',
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,dayGridWeek,multiMonthYear'
    },
    buttonText: {
      today: "Aujourd'hui",
      month: 'Mois',
      week: 'Semaine',
      multiMonthYear: 'Année'
    },
    eventDisplay: 'block',
    eventTimeFormat: {
      hour: '2-digit',
      minute: '2-digit',
      meridiem: false
    }
  };

  showApproveModal = false;
  showRejectModal = false;

  selectedTache: TacheRh | null = null;
  commentaire = '';
  motifRefus = '';
  isSubmitting = false;

  showDetailsModal = false;
  selectedDetails: any = null;

  showRefusDetailsModal = false;
  selectedRefusDetails: DemandeRefusDetails | null = null;
  loadingRefusDetails = false;

  private refreshInterval: any;
  private readonly REFRESH_INTERVAL_MS = 15000;

  constructor(
    private adminCongeService: AdminCongeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    private keycloakInit: KeycloakInitService
  ) {}

  ngOnInit(): void {
    this.generateYears();
    this.loadCurrentUserEmail();
    this.loadAllData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadCurrentUserEmail(): void {
    const user = this.keycloakInit.getUser();

    this.currentUserEmail =
      user?.email ||
      user?.preferred_username ||
      user?.username ||
      '';
  }

  isOwnRequest(tache: any): boolean {
    const employeEmail = String(tache?.employeEmail || '').trim().toLowerCase();
    const currentEmail = String(this.currentUserEmail || '').trim().toLowerCase();

    return !!employeEmail && !!currentEmail && employeEmail === currentEmail;
  }

  canTreatRh(tache: TacheRh): boolean {
    return this.isPendingRh(tache) && !this.isOwnRequest(tache);
  }

  loadAllData(): void {
    this.loadTaches();
    this.loadRefusManager();
    this.loadStats();
    this.loadCalendarEvents();
  }

  refreshAllData(): void {
    this.loadAllData();
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.loadAllData();
    }, this.REFRESH_INTERVAL_MS);
  }

  generateYears(): void {
    const currentYear = new Date().getFullYear();

    this.years = [];

    for (let i = currentYear - 3; i <= currentYear + 3; i++) {
      this.years.push(i);
    }
  }

  changeYearMonth(): void {
    if (this.calendarComponent) {
      const calendarApi = this.calendarComponent.getApi();
      const date = new Date(this.selectedYear, this.selectedMonth, 1);

      calendarApi.gotoDate(date);
    }
  }

  loadTaches(showLoading = true): void {
    if (showLoading) {
      this.loadingTaches = true;
    }

    this.adminCongeService.getDemandesAValider().subscribe({
      next: (data: TacheRh[]) => {
        const allDemandes = data || [];

        this.taches = allDemandes.filter((d: any) =>
          d.statut === 'EN_ATTENTE_RH' ||
          d.statut === 'EN_ATTENTE_ADMIN' ||
          d.statut === 'EN_ATTENTE'
        );

        this.stats.EN_ATTENTE = this.taches.length;
        this.loadingTaches = false;

        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingTaches = false;
        this.showToast('Erreur chargement demandes', 'error');
      }
    });
  }

  loadRefusManager(): void {
    this.loadingRefus = true;

    this.adminCongeService.getRefusManager().subscribe({
      next: (data: DemandeRefusManager[]) => {
        this.refusManager = data || [];
        this.loadingRefus = false;

        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingRefus = false;
        this.showToast('Erreur chargement refus manager', 'error');
      }
    });
  }

  openRefusDetails(refus: DemandeRefusManager): void {
    if (!refus || !refus.id) {
      this.showToast('Identifiant de demande manquant', 'error');
      return;
    }

    this.loadingRefusDetails = true;
    this.showRefusDetailsModal = true;
    this.selectedRefusDetails = null;

    this.cdr.detectChanges();

    this.adminCongeService.getDemandeRefusDetails(refus.id).subscribe({
      next: (details: DemandeRefusDetails) => {
        this.selectedRefusDetails = details;
        this.loadingRefusDetails = false;

        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Erreur détail refus', err);

        this.showToast('Impossible de charger les détails', 'error');
        this.loadingRefusDetails = false;
        this.closeRefusDetails();

        this.cdr.detectChanges();
      }
    });
  }

  closeRefusDetails(): void {
    this.showRefusDetailsModal = false;
    this.selectedRefusDetails = null;
    this.loadingRefusDetails = false;

    this.cdr.detectChanges();
  }

  loadStats(): void {
    this.adminCongeService.getStats().subscribe({
      next: (data: StatsConges) => {
        if (data) {
          this.stats = data;
        }

        this.cdr.detectChanges();
      },
      error: () => {
        console.error('Erreur stats');
      }
    });
  }

  loadCalendarEvents(): void {
    this.adminCongeService.getAllCongesForCalendar().subscribe({
      next: (events: EventInput[]) => {
        const safeEvents = events || [];

        if (this.calendarComponent) {
          const calendarApi = this.calendarComponent.getApi();

          calendarApi.removeAllEventSources();
          calendarApi.addEventSource(safeEvents);
        }

        this.calendarOptions = {
          ...this.calendarOptions,
          events: safeEvents
        };

        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Erreur chargement calendrier:', err);
        this.showToast('Erreur chargement calendrier', 'error');
      }
    });
  }

  openApproveModal(tache: TacheRh): void {
    if (this.isOwnRequest(tache)) {
      this.showToast('Vous ne pouvez pas traiter votre propre demande de congé.', 'error');
      return;
    }

    this.selectedTache = tache;
    this.commentaire = '';
    this.showApproveModal = true;
  }

  openRejectModal(tache: TacheRh): void {
    if (this.isOwnRequest(tache)) {
      this.showToast('Vous ne pouvez pas traiter votre propre demande de congé.', 'error');
      return;
    }

    this.selectedTache = tache;
    this.motifRefus = '';
    this.showRejectModal = true;
  }

  closeModals(): void {
    this.showApproveModal = false;
    this.showRejectModal = false;
    this.selectedTache = null;
    this.commentaire = '';
    this.motifRefus = '';
    this.isSubmitting = false;
  }

  confirmApprove(): void {
    if (!this.selectedTache || this.isSubmitting) {
      return;
    }

    if (this.isOwnRequest(this.selectedTache)) {
      this.showToast('Vous ne pouvez pas traiter votre propre demande de congé.', 'error');
      this.closeModals();
      return;
    }

    if (!this.isPendingRh(this.selectedTache)) {
      this.showToast('Cette demande est déjà traitée', 'error');
      return;
    }

    if (!this.selectedTache.demandeId) {
      console.error('Demande sans demandeId:', this.selectedTache);
      this.showToast('Identifiant de demande manquant', 'error');
      return;
    }

    this.isSubmitting = true;

    this.adminCongeService.approuverDemande(
      this.selectedTache.demandeId,
      this.commentaire
    ).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('✅ Demande approuvée', 'success');
        this.closeModals();
        this.loadAllData();
      },
      error: (err: any) => {
        console.error('Erreur approbation admin:', err);
        this.isSubmitting = false;
        this.showToast('Erreur approbation', 'error');
      }
    });
  }



  getPhotoDebug(item: any): string {
  return (
    item?.photoUrl ||
    item?.employePhotoProfil ||
    item?.employePhotoUrl ||
    item?.employeePhotoProfil ||
    item?.employeePhotoUrl ||
    'AUCUNE PHOTO'
  );
}

  confirmReject(): void {
    if (!this.selectedTache || this.isSubmitting) {
      return;
    }

    if (this.isOwnRequest(this.selectedTache)) {
      this.showToast('Vous ne pouvez pas traiter votre propre demande de congé.', 'error');
      this.closeModals();
      return;
    }

    if (!this.isPendingRh(this.selectedTache)) {
      this.showToast('Cette demande est déjà traitée', 'error');
      return;
    }

    if (!this.selectedTache.demandeId) {
      console.error('Demande sans demandeId:', this.selectedTache);
      this.showToast('Identifiant de demande manquant', 'error');
      return;
    }

    if (!this.motifRefus.trim()) {
      this.showToast('Motif obligatoire', 'error');
      return;
    }

    this.isSubmitting = true;

    this.adminCongeService.refuserDemande(
      this.selectedTache.demandeId,
      this.motifRefus
    ).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('❌ Demande refusée', 'success');
        this.closeModals();
        this.loadAllData();
      },
      error: (err: any) => {
        console.error('Erreur refus admin:', err);
        this.isSubmitting = false;
        this.showToast('Erreur refus', 'error');
      }
    });
  }

  openDetails(tache: any): void {
    this.selectedDetails = tache;
    this.showDetailsModal = true;
  }

  closeDetails(): void {
    this.showDetailsModal = false;
    this.selectedDetails = null;
  }

  isPendingRh(tache: TacheRh): boolean {
    const statut = (tache as any)?.statut || 'EN_ATTENTE_RH';

    return (
      statut === 'EN_ATTENTE_RH' ||
      statut === 'EN_ATTENTE' ||
      statut === 'EN_ATTENTE_ADMIN'
    );
  }

  getStatusClass(statut?: string): string {
    switch (statut) {
      case 'APPROUVE':
      case 'APPROUVEE':
        return 'approved';

      case 'REFUSE':
        return 'refused';

      case 'REFUSE_MANAGER':
      case 'REFUSE_PAR_MANAGER':
        return 'manager-refused';

      case 'EN_ATTENTE_RH':
      case 'EN_ATTENTE':
      case 'EN_ATTENTE_ADMIN':
        return 'pending';

      default:
        return 'pending';
    }
  }

  getStatutLabel(statut?: string): string {
    const map: Record<string, string> = {
      APPROUVE: '✅ Approuvé',
      APPROUVEE: '✅ Approuvée',
      EN_ATTENTE: '⏳ En attente',
      EN_ATTENTE_RH: '⏳ En attente RH',
      EN_ATTENTE_ADMIN: '⏳ En attente RH',
      REFUSE: '❌ Refusé',
      REFUSE_MANAGER: '❌ Refus manager',
      REFUSE_PAR_MANAGER: '❌ Refus manager',
      ANNULE: '🗑️ Annulé'
    };

    return map[statut || ''] || '⏳ En attente RH';
  }

  private parseDate(dateStr: string | undefined): Date | null {
    if (!dateStr) {
      return null;
    }

    let date = new Date(dateStr);

    if (isNaN(date.getTime()) && dateStr.includes(' ')) {
      date = new Date(dateStr.replace(' ', 'T'));
    }

    return isNaN(date.getTime()) ? null : date;
  }

  formatFullDate(dateStr: string | undefined): string {
    const date = this.parseDate(dateStr);

    if (!date) {
      return '';
    }

    return date.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDate(dateStr: string | undefined): string {
    const date = this.parseDate(dateStr);

    if (!date) {
      return '';
    }

    return date.toLocaleDateString('fr-FR');
  }

  getInitials(prenom?: string, nom?: string): string {
    const p = prenom?.trim()?.charAt(0) || '';
    const n = nom?.trim()?.charAt(0) || '';

    return `${p}${n}`.toUpperCase() || 'RH';
  }

  getTypeLabel(type?: string): string {
    const map: Record<string, string> = {
      ANNUEL: 'Annuel',
      MALADIE: 'Maladie',
      SANS_SOLDE: 'Sans solde',
      MATERNITE: 'Maternité',
      PATERNITE: 'Paternité'
    };

    return map[type || ''] || 'Non spécifié';
  }

  getTypeColor(type?: string): string {
    const map: Record<string, string> = {
      ANNUEL: '#1976d2',
      MALADIE: '#dc3545',
      SANS_SOLDE: '#ffc107',
      MATERNITE: '#28a745',
      PATERNITE: '#28a745'
    };

    return map[type || ''] || '#6c757d';
  }

  getAvatarColor(dept?: string): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6',
      Technique: '#0891b2',
      Commercial: '#d97706',
      Finance: '#059669',
      Marketing: '#db2777',
      Direction: '#7c3aed',
      Logistique: '#4f46e5'
    };

    return colors[dept || ''] || '#6366f1';
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'right'
    });
  }

  trackByTaskId(index: number, item: TacheRh): number {
    return item.demandeId ?? index;
  }

  trackByRefusId(index: number, item: DemandeRefusManager): number {
    return item.id ?? index;
  }
}