// src/app/features/conges/conges.component.ts

import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';

import { CommonModule, Location } from '@angular/common';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterModule
} from '@angular/router';

import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { filter, forkJoin, of, Subject, takeUntil } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon, MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule, MatTab, MatTabGroup } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { FullCalendarModule, FullCalendarComponent } from '@fullcalendar/angular';
import { EventInput } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import multiMonthPlugin from '@fullcalendar/multimonth';

import { EmployeeCongeService } from '../../core/services/employee-conge.service';
import {
  CongeResponse,
  DemandeConge,
  SoldeConges
} from '../../core/models/conge.model';

import { WorkflowService, Task } from '../../core/services/workflow.service';
import { ManagerService } from '../../core/services/manager.service';
import {
  AdminCongeService,
  DemandeRefusDetails,
  DemandeRefusManager,
  StatsConges,
  TacheRh
} from '../../core/services/admin-conge.service';

import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { KeycloakInitService } from '../../core/services/keycloak-init.service';

import { Employe } from '../../core/models/employe.model';
import { EmployeeAvatarComponent } from '../../shared/layouts/components/employee-avatar/employee-avatar.component';

type CongeMode =
  | 'EMPLOYE_LISTE'
  | 'EMPLOYE_NOUVEAU'
  | 'EMPLOYE_DETAIL'
  | 'EMPLOYE_MODIFIER'
  | 'MANAGER_VALIDATION'
  | 'MANAGER_HISTORIQUE_EMPLOYE'
  | 'ADMIN_RH_VALIDATION';

type ManagerActionType = 'approve' | 'reject';

interface TypeCongeOption {
  value: string;
  label: string;
  icon: string;
  color: string;
}

// =========================================================
// ✅ NOUVEAU : Durées maximales par type (synchronisées avec le backend)
// =========================================================
const MAX_JOURS_PAR_TYPE: Record<string, number> = {
  PATERNITE: 5,
  MATERNITE: 30,
  MALADIE: 6,
  ANNUEL: 30,
  SANS_SOLDE: 365,
  FORMATION: 3,
  URGENCE: 1
};

@Component({
  selector: 'app-conges',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,

    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatIcon,
    MatTableModule,
    MatChipsModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatCheckboxModule,
    MatTabsModule,
    MatTabGroup,
    MatTab,
    MatBadgeModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,

    FullCalendarModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './conges.component.html',
  styleUrls: ['./conges.component.scss']
})
export class CongesComponent implements OnInit, OnDestroy {
  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;

  private destroy$ = new Subject<void>();

  private employeePollingInterval: any;
  private managerCalendarRefreshInterval: any;
  private adminRefreshInterval: any;

  private readonly EMPLOYEE_POLLING_INTERVAL_MS = 10000;
  private readonly CALENDAR_REFRESH_INTERVAL_MS = 15000;
  private readonly ADMIN_REFRESH_INTERVAL_MS = 15000;

  congeMode: CongeMode = 'EMPLOYE_LISTE';

  loading = false;
  errorMessage = '';
  error: string | null = null;
  isSubmitting = false;

  currentYear = new Date().getFullYear();
  currentUserEmail = '';

  // =========================================================
  // EMPLOYÉ
  // =========================================================

  conges: DemandeConge[] = [];
  demande: DemandeConge | null = null;
  demandeId: number | null = null;

  demandeForm: FormGroup;

  soldeTotal = 0;
  soldePris = 0;
  soldeRestant = 0;
  soldeEnAttente = 0;

  soldeConges = 25;
  congesPris = 0;
  congesRestants = 25;

  previousStatuts = new Map<number, string>();

  selectionMode = false;
  selectedDemandes: Set<number> = new Set();
  selectAll = false;
  hiddenDemandeIds: Set<number> = new Set();

  isAdmin = false;
  isEmploye = false;
  canAnnuler = false;

  typesConge: TypeCongeOption[] = [
    {
      value: 'ANNUEL',
      label: 'Congé annuel',
      icon: 'beach_access',
      color: '#1976d2'
    },
    {
      value: 'MALADIE',
      label: 'Congé maladie',
      icon: 'local_hospital',
      color: '#dc3545'
    },
    {
      value: 'SANS_SOLDE',
      label: 'Congé sans solde',
      icon: 'attach_money',
      color: '#ffc107'
    },
    {
      value: 'MATERNITE',
      label: 'Congé maternité',
      icon: 'child_care',
      color: '#28a745'
    },
    {
      value: 'PATERNITE',
      label: 'Congé paternité',
      icon: 'child_friendly',
      color: '#28a745'
    }
  ];

  // =========================================================
  // MANAGER APPROBATION
  // =========================================================

  tasks: any[] = [];
  equipeSize = 0;

  showModal = false;
  actionType: ManagerActionType = 'approve';
  commentaire = '';
  motifRefus = '';

  selectedTaskForApproval: Task | null = null;

  showDetailsModal = false;
  currentTask: Task | null = null;
  employeDetails: Employe | null = null;
  employeSolde: SoldeConges | null = null;
  employeHistorique: DemandeConge[] = [];
  loadingDetails = false;
  detailsError = '';

  // =========================================================
  // MANAGER CALENDRIER
  // =========================================================

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
    plugins: [dayGridPlugin, interactionPlugin, multiMonthPlugin],
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

  // =========================================================
  // MANAGER HISTORIQUE EMPLOYÉ
  // =========================================================

  managerHistoriqueConges: DemandeConge[] = [];
  managerHistoriqueLoading = true;
  managerHistoriqueError = false;

  managerHistoriqueEmployeId!: number;
  employeNom = '';
  employePoste = '';
  employeDepartement = '';

  absenteisme: number | null = null;
  scoreTurnover: number | null = null;
  scoreTurnoverNiveau: string | null = null;

  // =========================================================
  // ADMIN RH
  // =========================================================

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

  showApproveModal = false;
  showRejectModal = false;

  selectedTache: TacheRh | null = null;

  showRhDetailsModal = false;
  selectedDetails: any = null;

  showRefusDetailsModal = false;
  selectedRefusDetails: DemandeRefusDetails | null = null;
  loadingRefusDetails = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,

    private employeeCongeService: EmployeeCongeService,
    private workflowService: WorkflowService,
    private managerService: ManagerService,
    private adminCongeService: AdminCongeService,

    private authService: AuthService,
    private notificationService: NotificationService,
    private keycloakInit: KeycloakInitService
  ) {
    this.demandeForm = this.fb.group(
      {
        type: ['', Validators.required],
        dateDebut: ['', Validators.required],
        dateFin: ['', Validators.required],
        commentaire: [''],
        urgente: [false]
      },
      {
        validators: this.dateRangeValidator
      }
    );
  }

  ngOnInit(): void {
    this.generateYears();
    this.resolveMode();
    this.loadCurrentUserEmail();
    this.initCurrentMode();

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe(() => {
        this.stopEmployeePolling();
        this.stopManagerCalendarRefresh();
        this.stopAdminAutoRefresh();

        this.resolveMode();
        this.initCurrentMode();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();

    this.stopEmployeePolling();
    this.stopManagerCalendarRefresh();
    this.stopAdminAutoRefresh();
  }

  // =========================================================
  // INIT / MODE
  // =========================================================

  private resolveMode(): void {
    this.congeMode =
      this.route.snapshot.data['congeMode'] ||
      this.resolveModeFromUrl();

    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.demandeId = id && !Number.isNaN(id) ? id : null;
  }

  private resolveModeFromUrl(): CongeMode {
    const url = this.router.url;

    if (url.includes('/manager/employe/') && url.includes('/conges')) {
      return 'MANAGER_HISTORIQUE_EMPLOYE';
    }

    if (url.includes('/manager/conges')) {
      return 'MANAGER_VALIDATION';
    }

    if (url.includes('/admin/conges')) {
      return 'ADMIN_RH_VALIDATION';
    }

    if (url.includes('/nouveau')) {
      return 'EMPLOYE_NOUVEAU';
    }

    if (url.includes('/modifier')) {
      return 'EMPLOYE_MODIFIER';
    }

    if (this.route.snapshot.paramMap.get('id')) {
      return 'EMPLOYE_DETAIL';
    }

    return 'EMPLOYE_LISTE';
  }

  private initCurrentMode(): void {
    this.errorMessage = '';
    this.error = null;
    this.loading = false;
    this.isSubmitting = false;

    switch (this.congeMode) {
      case 'EMPLOYE_LISTE':
        this.initEmployeeList();
        break;

      case 'EMPLOYE_NOUVEAU':
        this.initEmployeeNew();
        break;

      case 'EMPLOYE_DETAIL':
        this.initEmployeeDetail();
        break;

      case 'EMPLOYE_MODIFIER':
        this.initEmployeeEdit();
        break;

      case 'MANAGER_VALIDATION':
        this.initManagerValidation();
        break;

      case 'MANAGER_HISTORIQUE_EMPLOYE':
        this.initManagerEmployeeHistory();
        break;

      case 'ADMIN_RH_VALIDATION':
        this.initAdminRh();
        break;
    }
  }

  private loadCurrentUserEmail(): void {
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

  // =========================================================
  // EMPLOYÉ LISTE
  // =========================================================

  private initEmployeeList(): void {
    this.loadHiddenIdsFromStorage();
    this.loadConges();
    this.loadSolde();
    this.startEmployeePolling();

    this.notificationService.loadNotifications();
    this.notificationService.loadUnreadCount();
  }

  loadConges(): void {
    this.loading = true;
    this.errorMessage = '';

    this.employeeCongeService
      .getMesConges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          this.loading = false;

          const toutesDemandes = this.normalizeConges(
            this.extractDemandesFromResponse(response)
          );

          toutesDemandes.forEach(c => {
            if (c.id && c.statut) {
              this.previousStatuts.set(c.id, c.statut);
            }
          });

          this.conges = this.sortCongesRecentFirst(
            this.filterHiddenDemandes(toutesDemandes)
          );

          this.selectedDemandes.clear();
          this.selectAll = false;
        },
        error: (err: any) => {
          this.loading = false;
          console.error('Erreur chargement congés:', err);

          const message = err?.message?.includes('Refresh')
            ? 'Session expirée, veuillez vous reconnecter'
            : 'Erreur lors du chargement des congés';

          this.snackBar.open(message, 'Fermer', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  loadSolde(): void {
    this.employeeCongeService
      .getMonSoldeConges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          if (response?.success !== false) {
            const data = response?.data as any;

            this.soldeTotal = Number(data?.total ?? data?.soldeTotal ?? data?.soldeActuel ?? 25);
            this.soldePris = Number(data?.pris ?? data?.congesPris ?? data?.joursPris ?? 0);
            this.soldeRestant = Number(
              data?.restant ??
              data?.soldeRestant ??
              data?.joursRestants ??
              data?.soldeActuel ??
              this.soldeTotal - this.soldePris
            );
            this.soldeEnAttente = Number(data?.enAttente ?? data?.congesEnAttente ?? data?.demandesEnAttente ?? 0);

            this.soldeConges = this.soldeTotal;
            this.congesPris = this.soldePris;
            this.congesRestants = this.soldeRestant;
          }
        },
        error: (err: any) => {
          console.error('Erreur solde:', err);

          if (!this.soldeTotal) {
            this.soldeTotal = 25;
            this.soldePris = 0;
            this.soldeRestant = 25;
            this.soldeEnAttente = 0;

            this.soldeConges = 25;
            this.congesPris = 0;
            this.congesRestants = 25;
          }
        }
      });
  }

  private startEmployeePolling(): void {
    if (this.employeePollingInterval) {
      return;
    }

    this.employeePollingInterval = setInterval(() => {
      if (this.congeMode === 'EMPLOYE_LISTE') {
        this.checkForEmployeeUpdates();
      }
    }, this.EMPLOYEE_POLLING_INTERVAL_MS);
  }

  private stopEmployeePolling(): void {
    if (this.employeePollingInterval) {
      clearInterval(this.employeePollingInterval);
      this.employeePollingInterval = null;
    }
  }

  checkForEmployeeUpdates(): void {
    this.employeeCongeService
      .getMesConges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          const toutesDemandes = this.normalizeConges(
            this.extractDemandesFromResponse(response)
          );

          if (toutesDemandes.length > 0) {
            this.detectStatutChanges(toutesDemandes);
            this.conges = this.sortCongesRecentFirst(
              this.filterHiddenDemandes(toutesDemandes)
            );
          }
        },
        error: (err: any) => console.error('Polling error:', err)
      });
  }

  private detectStatutChanges(toutesDemandes: DemandeConge[]): void {
    toutesDemandes.forEach(nouvelle => {
      if (!nouvelle.id || !nouvelle.statut) {
        return;
      }

      const ancienStatut = this.previousStatuts.get(nouvelle.id);

      if (ancienStatut && ancienStatut !== nouvelle.statut) {
        if (nouvelle.statut === 'APPROUVE') {
          const msg = `✅ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été approuvée !`;
          this.notificationService.showSuccess(msg);
          this.notificationService.loadNotifications();
          this.loadSolde();
        } else if (nouvelle.statut === 'REFUSE') {
          const motif = nouvelle.motifRefus || 'aucun motif fourni';
          const msg = `❌ Votre demande du ${this.formatDate(nouvelle.dateDebut)} au ${this.formatDate(nouvelle.dateFin)} a été refusée. Motif : ${motif}`;
          this.notificationService.showError(msg);
          this.notificationService.loadNotifications();
        }
      }

      this.previousStatuts.set(nouvelle.id, nouvelle.statut);
    });
  }

  private loadHiddenIdsFromStorage(): void {
    const saved = localStorage.getItem('hiddenDemandeIds');

    if (!saved) {
      return;
    }

    try {
      const arr = JSON.parse(saved);
      this.hiddenDemandeIds = new Set(arr);
    } catch (e) {
      console.error('Erreur chargement hiddenDemandeIds', e);
    }
  }

  private saveHiddenIdsToStorage(): void {
    localStorage.setItem(
      'hiddenDemandeIds',
      JSON.stringify(Array.from(this.hiddenDemandeIds))
    );
  }

  private filterHiddenDemandes(demandes: DemandeConge[]): DemandeConge[] {
    return demandes.filter(d => !d.id || !this.hiddenDemandeIds.has(d.id));
  }

  toggleSelectionMode(): void {
    this.selectionMode = !this.selectionMode;

    if (!this.selectionMode) {
      this.selectedDemandes.clear();
      this.selectAll = false;
    }
  }

  isSelectable(conge: DemandeConge): boolean {
    return conge.statut !== 'EN_ATTENTE';
  }

  toggleSelection(id: number): void {
    const conge = this.conges.find(c => c.id === id);

    if (conge && !this.isSelectable(conge)) {
      this.snackBar.open(
        'Les demandes en attente ne peuvent pas être sélectionnées',
        'Fermer',
        { duration: 3000 }
      );
      return;
    }

    if (this.selectedDemandes.has(id)) {
      this.selectedDemandes.delete(id);
    } else {
      this.selectedDemandes.add(id);
    }

    this.updateSelectAllState();
  }

  updateSelectAllState(): void {
    const selectableCount = this.conges.filter(c => this.isSelectable(c)).length;

    this.selectAll =
      selectableCount > 0 &&
      this.selectedDemandes.size === selectableCount;
  }

  toggleSelectAll(): void {
    if (this.selectAll) {
      this.selectedDemandes.clear();
      this.selectAll = false;
      return;
    }

    this.conges.forEach(c => {
      if (c.id && this.isSelectable(c)) {
        this.selectedDemandes.add(c.id);
      }
    });

    this.selectAll = true;
  }

  isSelected(id: number): boolean {
    return this.selectedDemandes.has(id);
  }

  deleteSelectedDemandes(): void {
    if (this.selectedDemandes.size === 0) {
      this.snackBar.open('Aucune demande sélectionnée', 'Fermer', { duration: 3000 });
      return;
    }

    const count = this.selectedDemandes.size;

    if (!confirm(`Masquer définitivement ${count} demande(s) ? Elles ne réapparaîtront plus dans la liste.`)) {
      return;
    }

    this.selectedDemandes.forEach(id => this.hiddenDemandeIds.add(id));
    this.saveHiddenIdsToStorage();

    this.selectedDemandes.clear();
    this.selectAll = false;
    this.selectionMode = false;

    this.loadConges();

    this.snackBar.open(`${count} demande(s) masquée(s)`, 'Fermer', { duration: 3000 });
  }

  // =========================================================
  // EMPLOYÉ FORMULAIRE
  // =========================================================

  private initEmployeeNew(): void {
    this.demande = null;

    this.demandeForm.reset({
      type: '',
      dateDebut: '',
      dateFin: '',
      commentaire: '',
      urgente: false
    });

    this.loadSolde();
  }

  private initEmployeeEdit(): void {
    this.loadSolde();
    this.loadDemandeForEdit();
  }

  // ✅ NOUVELLE MÉTHODE : Vérification de la durée maximale
  verifierDureeMax(): boolean {
    const type = this.demandeForm.get('type')?.value;
    if (!type) return true;
   
    const maxJours = MAX_JOURS_PAR_TYPE[type];
    if (!maxJours) return true;
   
    const joursDemandes = this.calculerNombreJours();
    return joursDemandes <= maxJours;
  }

  // ✅ NOUVELLE MÉTHODE : Message d'erreur pour durée maximale
  getMessageDureeMax(): string {
    const type = this.demandeForm.get('type')?.value;
    if (!type) return '';
   
    const maxJours = MAX_JOURS_PAR_TYPE[type];
    if (!maxJours) return '';
   
    const jours = this.calculerNombreJours();
    if (jours > maxJours) {
      return `⚠️ La durée sélectionnée (${jours} jours) dépasse le maximum autorisé (${maxJours} jours) pour ce type de congé.`;
    }
    return '';
  }

  onSubmit(): void {
    if (this.demandeForm.invalid) {
      this.demandeForm.markAllAsTouched();
      this.snackBar.open('Veuillez corriger les erreurs du formulaire', 'Fermer', {
        duration: 3000,
        panelClass: 'snackbar-error'
      });
      return;
    }

    // ✅ AJOUT : Vérification de la durée maximale
    if (!this.verifierDureeMax()) {
      this.snackBar.open(this.getMessageDureeMax(), 'Fermer', {
        duration: 5000,
        panelClass: 'snackbar-error'
      });
      return;
    }

    if (this.congeMode === 'EMPLOYE_NOUVEAU') {
      this.submitNewDemande();
      return;
    }

    if (this.congeMode === 'EMPLOYE_MODIFIER') {
      this.submitEditDemande();
      return;
    }
  }

  private submitNewDemande(): void {
    if (this.demandeForm.invalid) {
      this.demandeForm.markAllAsTouched();
      return;
    }

    const formValue = this.demandeForm.getRawValue();
    const urgenteValue = formValue.urgente === true;

    if (!this.verifierSolde() && !urgenteValue) {
      this.snackBar.open(
        `❌ Solde insuffisant. Vous avez ${this.congesRestants} jours restants.`,
        'Fermer',
        { duration: 5000, panelClass: ['snackbar-error'] }
      );
      return;
    }

    this.isSubmitting = true;

    const demande: DemandeConge = {
      type: formValue.type,
      dateDebut: formValue.dateDebut,
      dateFin: formValue.dateFin,
      commentaire: formValue.commentaire || '',
      urgente: urgenteValue,
      urgent: urgenteValue,
      isUrgent: urgenteValue
    } as any;

    this.employeeCongeService
      .soumettreDemande(demande)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          this.isSubmitting = false;

          if (response?.success || response?.statusCode === 200 || response?.statusCode === 201) {
            this.snackBar.open('Demande envoyée avec succès', 'Fermer', {
              duration: 3000,
              panelClass: 'snackbar-success'
            });

            this.notificationService.loadNotifications();
            this.notificationService.loadUnreadCount();

            this.router.navigate(['/employee/mes-conges'], {
              queryParams: { refresh: Date.now() }
            });

            return;
          }

          this.snackBar.open(
            response?.message || response?.error || 'Erreur lors de l’envoi de la demande',
            'Fermer',
            { duration: 6000, panelClass: 'snackbar-error' }
          );
        },
        error: (error: any) => {
          this.isSubmitting = false;

          const message =
            error?.error?.message ||
            error?.error?.error ||
            error?.message ||
            'Erreur lors de l’envoi de la demande';

          this.snackBar.open(message, 'Fermer', {
            duration: 7000,
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  private submitEditDemande(): void {
    if (!this.demandeId) {
      this.snackBar.open('ID de demande invalide', 'Fermer', {
        duration: 3000,
        panelClass: 'snackbar-error'
      });
      return;
    }

    if (this.demandeForm.invalid) {
      this.demandeForm.markAllAsTouched();
      this.snackBar.open('Veuillez corriger les champs du formulaire', 'Fermer', {
        duration: 3000,
        panelClass: 'snackbar-error'
      });
      return;
    }

    const formValue = this.demandeForm.getRawValue();
    const urgenteValue = formValue.urgente === true;

    const demande: DemandeConge = {
      ...(this.demande || {}),
      id: this.demandeId,
      type: formValue.type,
      dateDebut: formValue.dateDebut,
      dateFin: formValue.dateFin,
      commentaire: formValue.commentaire || '',
      urgente: urgenteValue,
      urgent: urgenteValue,
      isUrgent: urgenteValue
    } as any;

    console.log('DEMANDE MODIFICATION COMPONENT = ', demande);

    this.isSubmitting = true;

    this.employeeCongeService
      .modifierDemande(this.demandeId, demande)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          this.isSubmitting = false;

          console.log('RÉPONSE MODIFICATION BACKEND = ', response);

          if (response?.success || response?.statusCode === 200) {
            const updatedFromBackend = this.extractDemandeFromResponse(response);

            this.demande = this.normalizeConge(updatedFromBackend || demande);

            this.snackBar.open('Demande modifiée avec succès', 'Fermer', {
              duration: 3000,
              panelClass: 'snackbar-success'
            });

            this.notificationService.loadNotifications();
            this.notificationService.loadUnreadCount();

            this.router.navigate(['/employee/mes-conges', this.demandeId], {
              queryParams: {
                refresh: Date.now()
              }
            });

            return;
          }

          this.snackBar.open(
            response?.message || response?.error || 'Modification refusée par le serveur',
            'Fermer',
            {
              duration: 6000,
              panelClass: 'snackbar-error'
            }
          );
        },
        error: (error: any) => {
          this.isSubmitting = false;

          console.error('ERREUR MODIFICATION BACKEND = ', error);

          const message =
            error?.error?.message ||
            error?.error?.error ||
            error?.message ||
            'Erreur lors de la modification';

          this.snackBar.open(message, 'Fermer', {
            duration: 7000,
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  dateRangeValidator(form: FormGroup): { [key: string]: boolean } | null {
    const debut = form.get('dateDebut')?.value;
    const fin = form.get('dateFin')?.value;

    if (!debut || !fin) {
      return null;
    }

    const debutDate = new Date(debut);
    const finDate = new Date(fin);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const normalizedDebut = new Date(debutDate);
    normalizedDebut.setHours(0, 0, 0, 0);

    if (debutDate > finDate) {
      return { dateInvalide: true };
    }

    if (normalizedDebut < today) {
      return { datePassee: true };
    }

    return null;
  }

  calculerNombreJours(): number {
    const debut = this.demandeForm.get('dateDebut')?.value;
    const fin = this.demandeForm.get('dateFin')?.value;

    if (!debut || !fin) {
      return 0;
    }

    const debutDate = new Date(debut);
    const finDate = new Date(fin);

    if (debutDate > finDate) {
      return 0;
    }

    const diffTime = Math.abs(finDate.getTime() - debutDate.getTime());

    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  }

  verifierSolde(): boolean {
    const type = this.demandeForm.get('type')?.value;

    if (type !== 'ANNUEL') {
      return true;
    }

    return this.calculerNombreJours() <= this.soldeRestant;
  }

  // =========================================================
  // EMPLOYÉ DÉTAIL / MODIFICATION
  // =========================================================

  private initEmployeeDetail(): void {
    this.checkUserRole();
    this.loadDemande();
  }

  private checkUserRole(): void {
    const role = (this.authService as any).getUserRole?.();

    this.isAdmin = role === 'ADMIN_RH' || role === 'ADMIN';
    this.isEmploye = role === 'EMPLOYE' || role === 'EMPLOYEE' || !this.isAdmin;
  }

  private loadDemande(): void {
    const id = this.demandeId;

    if (!id) {
      this.loading = false;
      this.errorMessage = 'ID de demande invalide.';
      this.error = this.errorMessage;
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.error = null;
    this.demande = null;

    if (this.isAdmin) {
      this.loadDemandeForAdmin(id);
    } else {
      this.loadDemandeForEmploye(id);
    }
  }

  private loadDemandeForAdmin(id: number): void {
    this.employeeCongeService
      .getAllDemandesAdmin()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          const demandes = this.normalizeConges(this.extractDemandesFromResponse(response));
          const demande = demandes.find(d => Number(d.id) === Number(id));

          if (demande) {
            this.demande = demande;
            this.checkCanAnnuler();
          } else {
            this.errorMessage = 'Demande non trouvée.';
            this.error = this.errorMessage;
          }

          this.loading = false;
        },
        error: (err: any) => {
          console.error('Erreur chargement demande admin:', err);
          this.errorMessage = 'Erreur lors du chargement de la demande.';
          this.error = this.errorMessage;
          this.loading = false;
        }
      });
  }

  private loadDemandeForEmploye(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.error = null;
    this.demande = null;

    this.employeeCongeService
      .getMesConges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          const demandes = this.normalizeConges(this.extractDemandesFromResponse(response));
          const found = demandes.find(d => Number(d.id) === Number(id));

          if (found) {
            this.demande = this.normalizeConge(found);
            this.checkCanAnnuler();
            this.loading = false;
            return;
          }

          this.loadDemandeByIdFallback(id);
        },
        error: (err: any) => {
          console.error('Erreur getMesConges détail:', err);
          this.loadDemandeByIdFallback(id);
        }
      });
  }

  private loadDemandeByIdFallback(id: number): void {
    this.employeeCongeService
      .getCongeById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (detailResponse: CongeResponse) => {
          const detail = this.extractDemandeFromResponse(detailResponse);

          if (detail) {
            this.demande = this.normalizeConge(detail);
            this.checkCanAnnuler();
          } else {
            this.errorMessage = 'Demande non trouvée.';
            this.error = this.errorMessage;
          }

          this.loading = false;
        },
        error: (err: any) => {
          console.error('Erreur getCongeById détail:', err);
          this.errorMessage = 'Demande non trouvée.';
          this.error = this.errorMessage;
          this.loading = false;
        }
      });
  }

  private loadDemandeForEdit(): void {
    const id = this.demandeId;

    if (!id) {
      this.loading = false;
      this.errorMessage = 'ID de demande invalide.';
      this.error = this.errorMessage;
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.error = null;

    this.employeeCongeService
      .getCongeById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          const detail = this.extractDemandeFromResponse(response);

          if (!detail) {
            this.loadDemandeForEditFallback(id);
            return;
          }

          this.setDemandeForEdit(detail);
        },
        error: (err: any) => {
          console.error('Erreur getCongeById modification:', err);
          this.loadDemandeForEditFallback(id);
        }
      });
  }

  private loadDemandeForEditFallback(id: number): void {
    this.employeeCongeService
      .getMesConges()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          const demandes = this.normalizeConges(this.extractDemandesFromResponse(response));
          const found = demandes.find(d => Number(d.id) === Number(id));

          if (!found) {
            this.errorMessage = 'Demande non trouvée.';
            this.error = this.errorMessage;
            this.loading = false;
            return;
          }

          this.setDemandeForEdit(found);
        },
        error: (err: any) => {
          console.error('Erreur fallback modification:', err);
          this.errorMessage = 'Erreur lors du chargement de la demande.';
          this.error = this.errorMessage;
          this.loading = false;
        }
      });
  }

  private setDemandeForEdit(demande: DemandeConge): void {
    const normalized = this.normalizeConge(demande);

    if (normalized.statut !== 'EN_ATTENTE') {
      this.snackBar.open(
        'Seules les demandes en attente peuvent être modifiées',
        'Fermer',
        { duration: 4000, panelClass: 'snackbar-error' }
      );

      this.router.navigate(['/employee/mes-conges']);
      return;
    }

    this.demande = normalized;
    this.patchFormForEdit(normalized);
    this.loading = false;
  }

  private patchFormForEdit(demande: DemandeConge): void {
    this.demandeForm.patchValue({
      type: demande.type || '',
      dateDebut: demande.dateDebut ? new Date(demande.dateDebut) : '',
      dateFin: demande.dateFin ? new Date(demande.dateFin) : '',
      commentaire: demande.commentaire || '',
      urgente: this.getUrgenceFromObject(demande)
    });
  }

  private checkCanAnnuler(): void {
    this.canAnnuler =
      !!this.demande &&
      this.isEmploye &&
      this.demande.statut === 'EN_ATTENTE';
  }

  annulerDemande(target: DemandeConge | null = this.demande): void {
    if (!target?.id) {
      return;
    }

    const targetId = target.id;

    const confirmed = confirm('Êtes-vous sûr de vouloir annuler cette demande de congé ?');

    if (!confirmed) {
      return;
    }

    this.loading = true;
    this.error = null;

    this.employeeCongeService
      .annulerConge(targetId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: CongeResponse) => {
          this.loading = false;

          if (response?.success !== false) {
            const updated = this.extractDemandeFromResponse(response);

            if (this.demande) {
              this.demande = this.normalizeConge({
                ...this.demande,
                ...(updated || {}),
                statut: 'ANNULE'
              });
            }

            this.conges = this.sortCongesRecentFirst(
              this.conges.map(c =>
                c.id === targetId
                  ? this.normalizeConge({
                      ...c,
                      ...(updated || {}),
                      statut: 'ANNULE'
                    })
                  : c
              )
            );

            this.previousStatuts.set(targetId, 'ANNULE');
            this.canAnnuler = false;

            this.notificationService.loadNotifications();
            this.notificationService.loadUnreadCount();
            this.loadSolde();

            this.snackBar.open('Demande annulée avec succès', 'Fermer', {
              duration: 3000,
              panelClass: 'snackbar-success'
            });

            this.router.navigate(['/employee/mes-conges'], {
              queryParams: { refresh: Date.now() }
            });

            setTimeout(() => {
              this.loadConges();
            }, 300);

            return;
          }

          this.snackBar.open(
            response?.message || 'Erreur lors de l’annulation',
            'Fermer',
            { duration: 5000, panelClass: 'snackbar-error' }
          );
        },
        error: (err: any) => {
          this.loading = false;

          this.error =
            err?.error?.error ||
            err?.error?.message ||
            'Erreur lors de l’annulation de la demande.';

          this.snackBar.open(this.error || 'Erreur', 'Fermer', {
            duration: 5000,
            panelClass: 'snackbar-error'
          });
        }
      });
  }

  peutModifier(conge: DemandeConge | null): boolean {
    return !!conge?.id && conge.statut === 'EN_ATTENTE';
  }

  peutAnnuler(conge: DemandeConge | null): boolean {
    return !!conge?.id && conge.statut === 'EN_ATTENTE';
  }

  // =========================================================
  // MANAGER VALIDATION
  // =========================================================

  private initManagerValidation(): void {
    this.loadPageData();
    this.loadCalendarEvents('manager');
    this.startManagerCalendarRefresh();
  }

  loadPageData(): void {
    this.loadManagerTasks();
    this.loadEquipeSize();
  }

  private loadManagerTasks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.workflowService
      .getManagerTasks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tasks: Task[]) => {
          this.tasks = Array.isArray(tasks)
            ? tasks.map(task => this.normalizeTask(task))
            : [];

          this.loading = false;
        },
        error: (error: any) => {
          console.error('Erreur chargement tâches manager:', error);
          this.errorMessage = 'Impossible de charger les demandes.';
          this.tasks = [];
          this.loading = false;
        }
      });
  }

  private loadEquipeSize(): void {
    this.managerService
      .getEquipe()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const equipe = this.unwrapResponse<Employe[]>(res, []);
          this.equipeSize = Array.isArray(equipe) ? equipe.length : 0;
        },
        error: (err: any) => {
          console.error('Erreur chargement équipe:', err);
          this.equipeSize = 0;
        }
      });
  }

  getUrgentTasksCount(): number {
    return this.tasks.filter(t => this.isTaskUrgent(t)).length;
  }

  isOwnRequest(task: any): boolean {
    const employeEmail = String(task?.employeEmail || '').trim().toLowerCase();
    const currentEmail = String(this.currentUserEmail || '').trim().toLowerCase();

    return !!employeEmail && !!currentEmail && employeEmail === currentEmail;
  }

  canTreatManager(task: any): boolean {
    return !this.isOwnRequest(task);
  }

  openApproveModal(task: Task): void {
    if (this.isOwnRequest(task)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande.');
      return;
    }

    this.selectedTaskForApproval = task;
    this.actionType = 'approve';
    this.commentaire = '';
    this.showModal = true;
  }

  openRejectModal(task: Task): void {
    if (this.isOwnRequest(task)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande.');
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
    this.isSubmitting = false;
  }

  submitManagerAction(): void {
    if (!this.selectedTaskForApproval || this.isSubmitting) {
      return;
    }

    if (this.isOwnRequest(this.selectedTaskForApproval)) {
      this.notificationService.showError('Vous ne pouvez pas traiter votre propre demande.');
      this.closeModal();
      return;
    }

    this.isSubmitting = true;

    if (this.actionType === 'approve') {
      this.workflowService
        .approveTask(this.selectedTaskForApproval.taskId, this.commentaire)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.notificationService.showSuccess('Demande approuvée avec succès.');
            this.closeModal();
            this.loadPageData();
          },
          error: (error: any) => {
            this.isSubmitting = false;
            this.notificationService.showError(
              error.error?.message || 'Erreur lors de l’approbation.'
            );
          }
        });

      return;
    }

    if (!this.motifRefus.trim()) {
      this.isSubmitting = false;
      this.notificationService.showError('Veuillez saisir un motif de refus.');
      return;
    }

    this.workflowService
      .rejectTask(this.selectedTaskForApproval.taskId, this.motifRefus)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('Demande refusée avec succès.');
          this.closeModal();
          this.loadPageData();
        },
        error: (error: any) => {
          this.isSubmitting = false;
          this.notificationService.showError(
            error.error?.message || 'Erreur lors du refus.'
          );
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

    if (!employeId) {
      this.detailsError = 'Impossible d’identifier l’employé associé à cette demande.';
      this.loadingDetails = false;
      return;
    }

    forkJoin({
      details: this.managerService.getEmployeDetails(Number(employeId)).pipe(
        catchError(err => {
          console.error('Erreur détails employé:', err);
          return of(null);
        })
      ),
      solde: this.managerService.getEmployeSoldeConges(Number(employeId)).pipe(
        catchError(err => {
          console.error('Erreur solde congés:', err);
          return of(null);
        })
      ),
      historique: this.managerService.getEmployeConges(Number(employeId)).pipe(
        catchError(err => {
          console.error('Erreur historique via getEmployeConges:', err);

          return this.managerService.getEmployeHistoriqueConges(Number(employeId)).pipe(
            catchError(err2 => {
              console.error('Erreur historique via getEmployeHistoriqueConges:', err2);
              return of(null);
            })
          );
        })
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ details, solde, historique }) => {
          this.employeDetails = this.unwrapResponse<Employe | null>(details, null);
          this.employeSolde = this.unwrapResponse<SoldeConges | null>(solde, null);

          const historiqueData = this.unwrapResponse<DemandeConge[]>(historique, []);
          this.employeHistorique = Array.isArray(historiqueData)
            ? this.sortCongesRecentFirst(this.normalizeConges(historiqueData))
            : [];

          this.loadingDetails = false;
        },
        error: (err: any) => {
          console.error('Erreur globale détails:', err);
          this.detailsError = 'Erreur lors du chargement des informations.';
          this.loadingDetails = false;
        }
      });
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.currentTask = null;
    this.employeDetails = null;
    this.employeSolde = null;
    this.employeHistorique = [];
    this.detailsError = '';
    this.loadingDetails = false;
  }

  // =========================================================
  // MANAGER CALENDRIER
  // =========================================================

  loadCalendarEvents(source: 'manager' | 'admin' = 'manager'): void {
    const request$ =
      source === 'admin'
        ? this.adminCongeService.getAllCongesForCalendar()
        : this.managerService.getCalendarEvents();

    request$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
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
        }
      });
  }

  refreshEvents(): void {
    if (this.congeMode === 'ADMIN_RH_VALIDATION') {
      this.loadCalendarEvents('admin');
      return;
    }

    this.loadCalendarEvents('manager');
  }

  private startManagerCalendarRefresh(): void {
    if (this.managerCalendarRefreshInterval) {
      return;
    }

    this.managerCalendarRefreshInterval = setInterval(() => {
      if (this.congeMode === 'MANAGER_VALIDATION') {
        this.loadCalendarEvents('manager');
      }
    }, this.CALENDAR_REFRESH_INTERVAL_MS);
  }

  private stopManagerCalendarRefresh(): void {
    if (this.managerCalendarRefreshInterval) {
      clearInterval(this.managerCalendarRefreshInterval);
      this.managerCalendarRefreshInterval = null;
    }
  }

  changeYearMonth(): void {
    if (!this.calendarComponent) {
      return;
    }

    const calendarApi = this.calendarComponent.getApi();
    const date = new Date(this.selectedYear, this.selectedMonth, 1);

    calendarApi.gotoDate(date);
  }

  generateYears(): void {
    const currentYear = new Date().getFullYear();

    this.years = [];

    for (let i = currentYear - 3; i <= currentYear + 3; i++) {
      this.years.push(i);
    }
  }

  // =========================================================
  // MANAGER HISTORIQUE EMPLOYÉ
  // =========================================================

  private initManagerEmployeeHistory(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.managerHistoriqueEmployeId = id;

    if (!id) {
      this.managerHistoriqueLoading = false;
      this.managerHistoriqueError = true;
      return;
    }

    this.loadManagerEmployeeInfo();
    this.loadManagerEmployeeConges();
    this.loadManagerEmployeeIndicateurs();
  }

  private loadManagerEmployeeInfo(): void {
    this.managerService
      .getEmployeDetails(this.managerHistoriqueEmployeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const emp = this.unwrapResponse<any>(res, null);

          if (emp) {
            this.employeNom = `${emp.prenom || ''} ${emp.nom || ''}`.trim();
            this.employePoste = emp.poste || '';
            this.employeDepartement = emp.departement || '';
          }
        },
        error: () => {
          console.error('Erreur chargement employé');
        }
      });
  }

  private loadManagerEmployeeConges(): void {
    this.managerHistoriqueLoading = true;
    this.managerHistoriqueError = false;

    this.managerService
      .getEmployeConges(this.managerHistoriqueEmployeId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const data = this.unwrapResponse<DemandeConge[]>(res, []);

          this.managerHistoriqueConges = Array.isArray(data)
            ? this.sortCongesRecentFirst(this.normalizeConges(data))
            : [];

          this.managerHistoriqueLoading = false;
        },
        error: (err: any) => {
          console.error('Erreur congés employé manager:', err);
          this.managerHistoriqueLoading = false;
          this.managerHistoriqueError = true;
        }
      });
  }

  private loadManagerEmployeeIndicateurs(): void {
    forkJoin({
      absenteisme: this.managerService.getDernierAbsenteisme(this.managerHistoriqueEmployeId).pipe(
        catchError(err => {
          console.error('Erreur absentéisme:', err);
          return of(null);
        })
      ),
      turnover: this.managerService.getDernierScoreTurnover(this.managerHistoriqueEmployeId).pipe(
        catchError(err => {
          console.error('Erreur score turnover:', err);
          return of(null);
        })
      )
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ absenteisme, turnover }) => {
        const absData = this.unwrapResponse<any>(absenteisme, null);
        const turnoverData = this.unwrapResponse<any>(turnover, null);

        this.absenteisme =
          absData?.valeur !== undefined
            ? Number(absData.valeur)
            : null;

        this.scoreTurnover =
          turnoverData?.score !== undefined
            ? Number(turnoverData.score)
            : null;

        this.scoreTurnoverNiveau =
          turnoverData?.niveauRisque ||
          turnoverData?.niveau ||
          null;
      });
  }

  // =========================================================
  // ADMIN RH
  // =========================================================

  private initAdminRh(): void {
    this.loadAdminAllData();
    this.startAdminAutoRefresh();
  }

  loadAdminAllData(): void {
    this.loadRhTaches();
    this.loadRefusManager();
    this.loadRhStats();
    this.loadCalendarEvents('admin');
  }

  refreshAllData(): void {
    if (this.congeMode === 'ADMIN_RH_VALIDATION') {
      this.loadAdminAllData();
      return;
    }

    if (this.congeMode === 'MANAGER_VALIDATION') {
      this.loadPageData();
      this.loadCalendarEvents('manager');
      return;
    }

    this.loadConges();
    this.loadSolde();
  }

  private startAdminAutoRefresh(): void {
    if (this.adminRefreshInterval) {
      return;
    }

    this.adminRefreshInterval = setInterval(() => {
      if (this.congeMode === 'ADMIN_RH_VALIDATION') {
        this.loadAdminAllData();
      }
    }, this.ADMIN_REFRESH_INTERVAL_MS);
  }

  private stopAdminAutoRefresh(): void {
    if (this.adminRefreshInterval) {
      clearInterval(this.adminRefreshInterval);
      this.adminRefreshInterval = null;
    }
  }

  loadRhTaches(showLoading = true): void {
    if (showLoading) {
      this.loadingTaches = true;
    }

    this.adminCongeService
      .getDemandesAValider()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: TacheRh[]) => {
          const allDemandes = (data || []).map(t => this.normalizeTask(t));

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
          this.showSnackbar('Erreur chargement demandes RH.', 'error');
        }
      });
  }

  loadRefusManager(): void {
    this.loadingRefus = true;

    this.adminCongeService
      .getRefusManager()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: DemandeRefusManager[]) => {
          this.refusManager = (data || []).map(r => this.normalizeTask(r));
          this.loadingRefus = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.loadingRefus = false;
          this.showSnackbar('Erreur chargement refus manager.', 'error');
        }
      });
  }

  loadRhStats(): void {
    this.adminCongeService
      .getStats()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: StatsConges) => {
          if (data) {
            this.stats = data;
          }

          this.cdr.detectChanges();
        },
        error: () => console.error('Erreur stats RH')
      });
  }

  canTreatRh(tache: TacheRh): boolean {
    return this.isPendingRh(tache) && !this.isOwnRequest(tache);
  }

  isPendingRh(tache: any): boolean {
    const statut = String(tache?.statut || '').toUpperCase();

    return (
      statut === 'EN_ATTENTE_RH' ||
      statut === 'EN_ATTENTE_ADMIN' ||
      statut === 'EN_ATTENTE'
    );
  }

  openApproveRhModal(tache: TacheRh): void {
    if (this.isOwnRequest(tache)) {
      this.showSnackbar('Vous ne pouvez pas traiter votre propre demande.', 'error');
      return;
    }

    this.selectedTache = tache;
    this.commentaire = '';
    this.showApproveModal = true;
  }

  openRejectRhModal(tache: TacheRh): void {
    if (this.isOwnRequest(tache)) {
      this.showSnackbar('Vous ne pouvez pas traiter votre propre demande.', 'error');
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
      this.showSnackbar('Vous ne pouvez pas traiter votre propre demande.', 'error');
      this.closeModals();
      return;
    }

    if (!this.selectedTache.demandeId) {
      this.showSnackbar('Identifiant de demande manquant.', 'error');
      return;
    }

    this.isSubmitting = true;

    this.adminCongeService
      .approuverDemande(this.selectedTache.demandeId, this.commentaire)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.showSnackbar('Demande approuvée.', 'success');
          this.closeModals();
          this.loadAdminAllData();
        },
        error: (err: any) => {
          console.error('Erreur approbation RH:', err);
          this.isSubmitting = false;
          this.showSnackbar('Erreur approbation.', 'error');
        }
      });
  }

  confirmReject(): void {
    if (!this.selectedTache || this.isSubmitting) {
      return;
    }

    if (this.isOwnRequest(this.selectedTache)) {
      this.showSnackbar('Vous ne pouvez pas traiter votre propre demande.', 'error');
      this.closeModals();
      return;
    }

    if (!this.selectedTache.demandeId) {
      this.showSnackbar('Identifiant de demande manquant.', 'error');
      return;
    }

    if (!this.motifRefus.trim()) {
      this.showSnackbar('Motif obligatoire.', 'error');
      return;
    }

    this.isSubmitting = true;

    this.adminCongeService
      .refuserDemande(this.selectedTache.demandeId, this.motifRefus)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.showSnackbar('Demande refusée.', 'success');
          this.closeModals();
          this.loadAdminAllData();
        },
        error: (err: any) => {
          console.error('Erreur refus RH:', err);
          this.isSubmitting = false;
          this.showSnackbar('Erreur refus.', 'error');
        }
      });
  }

  openRhDetails(tache: any): void {
    this.selectedDetails = tache;
    this.showRhDetailsModal = true;
  }

  closeRhDetails(): void {
    this.selectedDetails = null;
    this.showRhDetailsModal = false;
  }

  openRefusDetails(refus: DemandeRefusManager): void {
    if (!refus?.id) {
      this.showSnackbar('Identifiant de demande manquant.', 'error');
      return;
    }

    this.loadingRefusDetails = true;
    this.showRefusDetailsModal = true;
    this.selectedRefusDetails = null;

    this.adminCongeService
      .getDemandeRefusDetails(refus.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (details: DemandeRefusDetails) => {
          this.selectedRefusDetails = details;
          this.loadingRefusDetails = false;
        },
        error: (err: any) => {
          console.error('Erreur détails refus:', err);
          this.showSnackbar('Impossible de charger les détails.', 'error');
          this.loadingRefusDetails = false;
          this.closeRefusDetails();
        }
      });
  }

  closeRefusDetails(): void {
    this.showRefusDetailsModal = false;
    this.selectedRefusDetails = null;
    this.loadingRefusDetails = false;
  }

  // =========================================================
  // HELPERS
  // =========================================================

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    if (this.isAdmin) {
      this.router.navigate(['/admin/conges']);
    } else {
      this.router.navigate(['/employee/mes-conges']);
    }
  }

  getSelectedManagerTaskPeriod(): string {
    const task = this.selectedTaskForApproval as any;

    if (!task) {
      return '-';
    }

    return `${this.formatDate(task.dateDebut)} → ${this.formatDate(task.dateFin)}`;
  }

  getEmployeInfo(task: any): string {
    const prenom = task?.employePrenom || task?.prenom || task?.employeePrenom || '';
    const nom = task?.employeNom || task?.nom || task?.employeeNom || '';
    const fullName = `${prenom} ${nom}`.trim();

    return fullName || task?.employeEmail || task?.employeeEmail || `Employé #${task?.employeId || task?.employeeId || ''}`;
  }

  getTaskDays(task: any): number {
    return Number(task?.nbJours || task?.joursOuvres || task?.nombreJours || task?.jours_ouvres || 0);
  }

  getNbJours(task: any): number {
    return this.getTaskDays(task);
  }

  getTaskType(task: any): string {
    return task?.typeConge || task?.type || task?.typeDemande || task?.categorie || 'Congé';
  }

  getTaskRequestDate(task: any): string | undefined {
    return task?.createTime || task?.dateDemande || task?.dateSoumission || task?.createdAt;
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

  private unwrapResponse<T>(response: any, fallback: T): T {
    if (!response) {
      return fallback;
    }

    if (response.data !== undefined) {
      return response.data as T;
    }

    return response as T;
  }

  private extractDemandesFromResponse(response: any): DemandeConge[] {
    if (!response) return [];

    if (Array.isArray(response)) return response;
    if (Array.isArray(response.data)) return response.data;
    if (Array.isArray(response.content)) return response.content;
    if (Array.isArray(response.data?.content)) return response.data.content;
    if (Array.isArray(response.data?.demandes)) return response.data.demandes;
    if (Array.isArray(response.demandes)) return response.demandes;
    if (Array.isArray(response.data?.items)) return response.data.items;
    if (Array.isArray(response.items)) return response.items;

    return [];
  }

  private extractDemandeFromResponse(response: any): DemandeConge | null {
    if (!response) return null;

    if (response.id) return response as DemandeConge;
    if (response.data?.id) return response.data as DemandeConge;
    if (response.data?.demande?.id) return response.data.demande as DemandeConge;
    if (response.demande?.id) return response.demande as DemandeConge;
    if (response.data?.conge?.id) return response.data.conge as DemandeConge;
    if (response.conge?.id) return response.conge as DemandeConge;

    return null;
  }

  private sortCongesRecentFirst(demandes: DemandeConge[]): DemandeConge[] {
    return [...(demandes || [])].sort((a, b) => {
      const idA = Number(a.id || 0);
      const idB = Number(b.id || 0);

      if (idA !== idB) {
        return idB - idA;
      }

      const dateA = this.getCongeSortTime(a);
      const dateB = this.getCongeSortTime(b);

      return dateB - dateA;
    });
  }

  private getCongeSortTime(conge: DemandeConge): number {
    const rawDate =
      conge.dateDemande ||
      conge.dateDecision ||
      conge.dateDebut ||
      '';

    if (!rawDate) {
      return 0;
    }

    const time = new Date(rawDate).getTime();
    return Number.isNaN(time) ? 0 : time;
  }

  getSoldeProgress(): number {
    if (!this.soldeTotal || this.soldeTotal <= 0) {
      return 0;
    }

    return Math.min(100, Math.round((this.soldePris / this.soldeTotal) * 100));
  }

  getEmployeSoldeRestant(): string | number {
    const solde = this.employeSolde as any;

    if (!solde) return '-';

    return (
      solde.restant ??
      solde.soldeRestant ??
      solde.soldeActuel ??
      solde.joursRestants ??
      '-'
    );
  }

  getEmployeSoldeEnAttente(): string | number {
    const solde = this.employeSolde as any;

    if (!solde) return '-';

    return (
      solde.enAttente ??
      solde.demandesEnAttente ??
      solde.congesEnAttente ??
      0
    );
  }

  getEmployeSoldeTotal(): string | number {
    const solde = this.employeSolde as any;

    if (!solde) return '-';

    return (
      solde.total ??
      solde.soldeTotal ??
      solde.soldeInitial ??
      25
    );
  }

  getEmployeCongesPris(): string | number {
    const solde = this.employeSolde as any;

    if (!solde) return '-';

    return (
      solde.pris ??
      solde.congesPris ??
      solde.joursPris ??
      0
    );
  }

  getDemandeEmployeNom(): string {
    const d = this.demande as any;

    if (!d) return '-';

    const fullName = `${d.employePrenom || d.prenom || ''} ${d.employeNom || d.nom || ''}`.trim();

    return (
      fullName ||
      d.employeMatricule ||
      d.employeeName ||
      d.employeEmail ||
      'Moi'
    );
  }

  getDemandeEmployeEmail(): string {
    const d = this.demande as any;

    if (!d) return '-';

    return (
      d.employeEmail ||
      d.employeeEmail ||
      d.email ||
      this.currentUserEmail ||
      '-'
    );
  }

  getTypeLabel(type?: string): string {
    switch (type || this.demande?.type) {
      case 'ANNUEL':
        return 'Congés annuels';
      case 'MALADIE':
        return 'Maladie';
      case 'SANS_SOLDE':
        return 'Sans solde';
      case 'MATERNITE':
        return 'Congé maternité';
      case 'PATERNITE':
        return 'Congé paternité';
      default:
        return type || this.demande?.type || 'Non défini';
    }
  }

  getTypeIcon(type?: string): string {
    return this.typesConge.find(t => t.value === type)?.icon || 'event';
  }

  getTypeColor(type?: string): string {
    return this.typesConge.find(t => t.value === type)?.color || '#6c757d';
  }

  getTypeClass(type?: string): string {
    switch (type) {
      case 'ANNUEL': return 'type-annuel';
      case 'MALADIE': return 'type-maladie';
      case 'SANS_SOLDE': return 'type-sans-solde';
      case 'MATERNITE': return 'type-maternite';
      case 'PATERNITE': return 'type-paternite';
      default: return 'type-autre';
    }
  }

  getStatutLabel(statut?: string): string {
    const value = statut || this.demande?.statut;

    switch (value) {
      case 'EN_ATTENTE': return 'En attente';
      case 'EN_ATTENTE_RH':
      case 'EN_ATTENTE_ADMIN': return 'En attente RH';
      case 'APPROUVE': return 'Approuvé';
      case 'REFUSE': return 'Refusé';
      case 'ANNULE': return 'Annulé';
      default: return value || 'Non défini';
    }
  }

  getStatutClass(statut?: string): string {
    const value = statut || this.demande?.statut;

    switch (value) {
      case 'EN_ATTENTE':
      case 'EN_ATTENTE_RH':
      case 'EN_ATTENTE_ADMIN':
        return 'en-attente status-en_attente';
      case 'APPROUVE':
        return 'approuve status-approuve';
      case 'REFUSE':
        return 'refuse status-refuse';
      case 'ANNULE':
        return 'annule status-annule';
      default:
        return 'annule status-annule';
    }
  }

  getStatusClass(statut?: string): string {
    return this.getStatutClass(statut);
  }

  getFormattedDate(dateValue?: string | Date | null): string {
    return this.formatDate(dateValue);
  }

  formatDate(dateValue?: string | Date | null): string {
    if (!dateValue) return '-';

    const date = dateValue instanceof Date ? dateValue : new Date(dateValue);

    if (Number.isNaN(date.getTime())) return '-';

    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatFullDate(dateValue?: string | Date | null): string {
    if (!dateValue) return '';

    const date = dateValue instanceof Date ? dateValue : new Date(dateValue);

    if (Number.isNaN(date.getTime())) return '';

    return date.toLocaleString('fr-FR');
  }

  getDuree(demande: DemandeConge | null = this.demande): string {
    if (!demande?.dateDebut || !demande?.dateFin) {
      return '-';
    }

    const debut = new Date(demande.dateDebut);
    const fin = new Date(demande.dateFin);

    if (Number.isNaN(debut.getTime()) || Number.isNaN(fin.getTime())) {
      return '-';
    }

    const diffTime = Math.abs(fin.getTime() - debut.getTime());
    const totalDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    const joursOuvres = demande.joursOuvres || demande.nombreJours || totalDays;

    return `${totalDays} jour(s) (${joursOuvres} ouvré(s))`;
  }

  getMotif(): string {
    const commentaire = this.demande?.commentaire?.trim();
    return commentaire || 'Aucun commentaire fourni.';
  }

  getAbsenteismeClass(value: number | null): string {
    if (value === null || value === undefined) {
      return 'neutral';
    }

    if (value >= 10) {
      return 'danger';
    }

    if (value >= 5) {
      return 'warning';
    }

    return 'success';
  }

  getScoreClass(value: number | null): string {
    if (value === null || value === undefined) {
      return 'neutral';
    }

    if (value >= 70) {
      return 'danger';
    }

    if (value >= 40) {
      return 'warning';
    }

    return 'success';
  }

  trackById(_: number, item: DemandeConge): number | undefined {
    return item.id;
  }

  trackByCongeId(_: number, item: DemandeConge): number | undefined {
    return item.id;
  }

  trackByTaskId(index: number, item: any): string | number {
    return item?.taskId || item?.demandeId || item?.id || index;
  }

  trackByRefusId(index: number, item: DemandeRefusManager): number {
    return item?.id || index;
  }

  private showSnackbar(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass:
        type === 'success'
          ? 'snackbar-success'
          : type === 'error'
            ? 'snackbar-error'
            : 'snackbar-info'
    });
  }

  isUrgenteValue(value: any): boolean {
    return value === true || String(value).toLowerCase() === 'true';
  }

  getUrgenceFromObject(obj: any): boolean {
    if (!obj) {
      return false;
    }

    return (
      this.isUrgenteValue(obj.urgente) ||
      this.isUrgenteValue(obj.urgent) ||
      this.isUrgenteValue(obj.isUrgent)
    );
  }

  private normalizeConge(conge: DemandeConge): DemandeConge {
    const urgenteValue = this.getUrgenceFromObject(conge as any);

    return {
      ...conge,
      urgente: urgenteValue,
      urgent: urgenteValue,
      isUrgent: urgenteValue
    } as any;
  }

  private normalizeConges(conges: DemandeConge[]): DemandeConge[] {
    return (conges || []).map(c => this.normalizeConge(c));
  }

  private normalizeTask(task: any): any {
    const urgenteValue = this.getUrgenceFromObject(task);

    return {
      ...task,
      urgente: urgenteValue,
      urgent: urgenteValue,
      isUrgent: urgenteValue
    };
  }

  isTaskUrgent(task: any): boolean {
    return this.getUrgenceFromObject(task);
  }
}