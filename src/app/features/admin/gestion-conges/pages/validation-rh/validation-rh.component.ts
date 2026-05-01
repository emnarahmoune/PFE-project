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
import { AdminCongeService, TacheRh, StatsConges, DemandeRefusManager, DemandeRefusDetails } from '../../../../../core/services/admin-conge.service';
import interactionPlugin from '@fullcalendar/interaction';
import dayGridPlugin from '@fullcalendar/daygrid';
import multiMonthPlugin from '@fullcalendar/multimonth';@Component({
  selector: 'app-validation-rh',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatChipsModule, MatSnackBarModule,
    MatProgressSpinnerModule, MatDialogModule,
    MatTooltipModule, MatTabsModule, MatBadgeModule,
    FullCalendarModule, MatFormFieldModule, MatSelectModule
  ],
  templateUrl: './validation-rh.component.html',
  styleUrls: ['./validation-rh.component.scss']
})
export class ValidationRhComponent implements OnInit, OnDestroy {

  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;

  taches: TacheRh[] = [];
  loadingTaches = false;
  refusManager: DemandeRefusManager[] = [];
  loadingRefus = false;
  stats: StatsConges = { EN_ATTENTE: 0, APPROUVE: 0, REFUSE: 0 };

  displayedColumns = ['employe', 'periode', 'jours', 'type', 'actions'];
  displayedColumnsRefus = ['employe', 'manager', 'periode', 'motif', 'dateDecision'];

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth();
  years: number[] = [];
  months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

 calendarOptions: any = {
  initialView: 'dayGridMonth',
  locale: 'fr',
  plugins: [dayGridPlugin, interactionPlugin, multiMonthPlugin],
  events: [],
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
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.generateYears();
    this.loadAllData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  generateYears(): void {
    const currentYear = new Date().getFullYear();
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
    this.refreshInterval = setInterval(() => this.loadAllData(), this.REFRESH_INTERVAL_MS);
  }

  loadTaches(showLoading = true): void {
    if (showLoading) this.loadingTaches = true;
    this.adminCongeService.getDemandesAValider().subscribe({
      next: (data) => {
        this.taches = data || [];
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
      next: (data) => {
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

  loadStats(): void {
    this.adminCongeService.getStats().subscribe({
      next: (data) => { if (data) this.stats = data; this.cdr.detectChanges(); },
      error: () => console.error('Erreur stats')
    });
  }

  loadCalendarEvents(): void {
    this.adminCongeService.getAllCongesForCalendar().subscribe({
      next: (events: EventInput[]) => {
        if (this.calendarComponent) {
          const calendarApi = this.calendarComponent.getApi();
          calendarApi.removeAllEventSources();
          calendarApi.addEventSource(events);
        } else {
          this.calendarOptions = { ...this.calendarOptions, events };
        }
        this.cdr.detectChanges();
      },
      error: () => this.showToast('Erreur chargement calendrier', 'error')
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
    this.cdr.detectChanges();  // force affichage du spinner

    this.adminCongeService.getDemandeRefusDetails(refus.id).subscribe({
      next: (details: DemandeRefusDetails) => {
        this.selectedRefusDetails = details;
        this.loadingRefusDetails = false;
        this.cdr.detectChanges();  // met à jour l'affichage
      },
      error: (err) => {
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

  private parseDate(dateStr: string | undefined): Date | null {
    if (!dateStr) return null;
    let d = new Date(dateStr);
    if (isNaN(d.getTime()) && dateStr.includes(' ')) {
      d = new Date(dateStr.replace(' ', 'T'));
    }
    return isNaN(d.getTime()) ? null : d;
  }

  formatFullDate(dateStr: string | undefined): string {
    const d = this.parseDate(dateStr);
    if (!d) return '';
    return d.toLocaleString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatDate(dateStr: string | undefined): string {
    const d = this.parseDate(dateStr);
    if (!d) return '';
    return d.toLocaleDateString('fr-FR');
  }

  openApproveModal(tache: TacheRh): void {
    this.selectedTache = tache;
    this.commentaire = '';
    this.showApproveModal = true;
  }

  openRejectModal(tache: TacheRh): void {
    this.selectedTache = tache;
    this.motifRefus = '';
    this.showRejectModal = true;
  }

  closeModals(): void {
    this.showApproveModal = false;
    this.showRejectModal = false;
    this.selectedTache = null;
  }

  confirmApprove(): void {
    if (!this.selectedTache || this.isSubmitting) return;
    this.isSubmitting = true;
    this.adminCongeService.approuverDemande(this.selectedTache.demandeId, this.commentaire).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('✅ Demande approuvée', 'success');
        this.closeModals();
        this.loadAllData();
      },
      error: () => {
        this.isSubmitting = false;
        this.showToast('Erreur approbation', 'error');
      }
    });
  }

  confirmReject(): void {
    if (!this.selectedTache || this.isSubmitting) return;
    if (!this.motifRefus.trim()) {
      this.showToast('Motif obligatoire', 'error');
      return;
    }
    this.isSubmitting = true;
    this.adminCongeService.refuserDemande(this.selectedTache.demandeId, this.motifRefus).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showToast('❌ Demande refusée', 'success');
        this.closeModals();
        this.loadAllData();
      },
      error: () => {
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

  getTypeLabel(type?: string): string {
    const map: Record<string, string> = {
      ANNUEL: 'Annuel', MALADIE: 'Maladie', SANS_SOLDE: 'Sans solde',
      MATERNITE: 'Maternité', PATERNITE: 'Paternité'
    };
    return map[type || ''] || 'Non spécifié';
  }

  getTypeColor(type?: string): string {
    const map: Record<string, string> = {
      ANNUEL: '#1976d2', MALADIE: '#dc3545', SANS_SOLDE: '#ffc107',
      MATERNITE: '#28a745', PATERNITE: '#28a745'
    };
    return map[type || ''] || '#6c757d';
  }

  getAvatarColor(dept?: string): string {
    const colors: Record<string, string> = {
      RH: '#8b5cf6', Technique: '#0891b2', Commercial: '#d97706',
      Finance: '#059669', Marketing: '#db2777', Direction: '#7c3aed', Logistique: '#4f46e5'
    };
    return colors[dept || ''] || '#6366f1';
  }

  getStatutLabel(statut: string): string {
    const map: Record<string, string> = {
      APPROUVE: '✅ Approuvé', EN_ATTENTE: '⏳ En attente',
      REFUSE: '❌ Refusé', ANNULE: '🗑️ Annulé'
    };
    return map[statut] || statut;
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: type === 'success' ? 'snackbar-success' : 'snackbar-error',
      horizontalPosition: 'right'
    });
  }

  trackByTaskId(index: number, item: TacheRh): number { return item.demandeId ?? index; }
  trackByRefusId(index: number, item: DemandeRefusManager): number { return item.id ?? index; }
}