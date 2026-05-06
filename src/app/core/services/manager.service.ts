// src/app/core/services/manager.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { EventInput } from '@fullcalendar/core';

import { environment } from '../../../environments/environment';
import { Employe } from '../../features/admin/gestion-employes/models/employe.model';
import { DemandeConge, SoldeConges } from '../../features/employee/models/conge.model';
import { ScoreTurnover } from '../../features/admin/scores/models/score-turnover.model';

export interface ManagerStats {
  employes?: number;
  congesEnAttente?: number;
  absenteisme?: number;
  turnover?: number;
  totalEmployes?: number;
  employesActifs?: number;
  masseSalariale?: number;
  salaireMoyen?: number;
  parDepartement?: Record<string, number>;
  tauxPresence?: number;
}

export interface Manager {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  departement?: string;
}

export interface TacheManager {
  taskId: string;
  taskName?: string;
  createTime?: string;
  processInstanceId?: string;

  demandeId?: number;
  employeId?: string | number;

  nbJours?: number;
  joursOuvres?: number;

  dateDebut?: string;
  dateFin?: string;
  type?: string;
  commentaire?: string;
  statut?: string;

  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;

  // ✅ IMPORTANT POUR PHOTO AVATAR
  photoUrl?: string;
  employePhotoProfil?: string;
  employePhotoUrl?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface ManagerCalendarEvent {
  id?: string | number;
  title?: string;
  start?: string;
  end?: string;
  color?: string;
  backgroundColor?: string;
  borderColor?: string;
  textColor?: string;
  extendedProps?: any;

  demandeId?: number;
  statut?: string;
  type?: string;
  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
  dateDebut?: string;
  dateFin?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {

  private apiUrl = `${environment.apiUrl}/employes`;
  private managerApiUrl = `${environment.apiUrl}/manager`;

  constructor(private http: HttpClient) {}

  getEquipe(): Observable<{ success: boolean; data: Employe[] }> {
    return this.http.get<{ success: boolean; data: Employe[] }>(
      `${this.apiUrl}/equipe`
    );
  }

  getEmployeDetails(employeId: number): Observable<{ success: boolean; data: Employe }> {
    return this.http.get<{ success: boolean; data: Employe }>(
      `${this.apiUrl}/manager/employe/${employeId}`
    );
  }

  getEmployeConges(employeId: number): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(
      `${environment.apiUrl}/conges/employe/${employeId}`
    );
  }

  getAllManagers(): Observable<{ success: boolean; data: Manager[] }> {
    return this.http.get<{ success: boolean; data: Manager[] }>(
      `${this.apiUrl}/managers`
    );
  }

  getAll(): Observable<{ success: boolean; data: Manager[] }> {
    return this.getAllManagers();
  }

  /**
   * Ancien endpoint manager direct.
   * Peut être utilisé ailleurs.
   */
  getTachesManager(): Observable<{ success: boolean; data: TacheManager[] }> {
    return this.http.get<{ success: boolean; data: TacheManager[] }>(
      `${this.managerApiUrl}/conges`
    );
  }

  approuverDemande(
    taskId: string,
    commentaire?: string
  ): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/approuver-demande`,
      { taskId, commentaire }
    );
  }

  refuserDemande(
    taskId: string,
    motif: string
  ): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/refuser-demande`,
      { taskId, motif }
    );
  }

  getEmployeSoldeConges(employeId: number): Observable<{ success: boolean; data: SoldeConges }> {
    return this.http.get<{ success: boolean; data: SoldeConges }>(
      `${environment.apiUrl}/employes/${employeId}/solde-conges`
    );
  }

  getEmployeHistoriqueConges(employeId: number): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(
      `${environment.apiUrl}/conges/employe/${employeId}/historique`
    );
  }

  getStats(): Observable<{ success: boolean; data: ManagerStats }> {
    return this.http.get<{ success: boolean; data: ManagerStats }>(
      `${this.managerApiUrl}/stats`
    );
  }

  getConges(): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(
      `${this.managerApiUrl}/conges`
    );
  }

  getDernierScoreTurnover(
    employeId: number
  ): Observable<{ success: boolean; data: { score: number; niveauRisque: string } }> {
    return this.http.get<{ success: boolean; data: any }>(
      `${environment.apiUrl}/scores-turnover/employe/${employeId}/dernier`
    );
  }

  getDerniersScores(): Observable<{ success: boolean; data: ScoreTurnover[] }> {
    return this.http.get<{ success: boolean; data: ScoreTurnover[] }>(
      `${environment.apiUrl}/scores-turnover/derniers`
    );
  }

  recalculerScoreTurnover(employeId: number): Observable<any> {
    return this.http.post(
      `${environment.apiUrl}/scores-turnover/calculer/employe/${employeId}?systemeBIId=1`,
      {}
    );
  }

  getDernierAbsenteisme(
    employeId: number
  ): Observable<{ success: boolean; data: { valeur: number; dateCalcul: string } }> {
    return this.http.get<{ success: boolean; data: any }>(
      `${environment.apiUrl}/indicateurs/type/ABSENTEISME?employeId=${employeId}`
    );
  }

  // ======================================================
  // CALENDRIER MANAGER
  // Backend :
  // GET /api/manager/calendar-events
  // ======================================================

  getCalendarEvents(): Observable<EventInput[]> {
    return this.http.get<ApiResponse<ManagerCalendarEvent[]> | ManagerCalendarEvent[]>(
      `${this.managerApiUrl}/calendar-events`
    ).pipe(
      map((res: ApiResponse<ManagerCalendarEvent[]> | ManagerCalendarEvent[]) => {
        const events = Array.isArray(res)
          ? res
          : (res?.data || []);

        return events.map((event: ManagerCalendarEvent): EventInput => {
          const statut = String(event.statut || event.extendedProps?.statut || '').toUpperCase();

          return {
            id: String(event.id ?? event.demandeId ?? ''),
            title: event.title || this.buildCalendarTitle(event),
            start: event.start || event.dateDebut,
            end: event.end || this.normalizeEndDate(event.end || event.dateFin),
            color: event.color || event.backgroundColor || this.resolveColorByStatut(statut),
            backgroundColor: event.backgroundColor || event.color || this.resolveColorByStatut(statut),
            borderColor: event.borderColor || event.color || this.resolveColorByStatut(statut),
            textColor: event.textColor || '#ffffff',
            extendedProps: {
              ...(event.extendedProps || {}),
              demandeId: event.demandeId ?? event.extendedProps?.demandeId,
              statut: event.statut ?? event.extendedProps?.statut,
              type: event.type ?? event.extendedProps?.type,
              employeNom: event.employeNom ?? event.extendedProps?.employeNom,
              employePrenom: event.employePrenom ?? event.extendedProps?.employePrenom,
              employeEmail: event.employeEmail ?? event.extendedProps?.employeEmail
            }
          };
        });
      })
    );
  }

  private buildCalendarTitle(event: ManagerCalendarEvent): string {
    const prenom = event.employePrenom || event.extendedProps?.employePrenom || '';
    const nom = event.employeNom || event.extendedProps?.employeNom || '';
    const type = event.type || event.extendedProps?.type || 'Congé';

    const fullName = `${prenom} ${nom}`.trim();

    return fullName ? `${fullName} - ${type}` : type;
  }

  private resolveColorByStatut(statut: string): string {
    switch (statut) {
      case 'APPROUVE':
      case 'APPROUVEE':
      case 'APPROUVÉ':
      case 'APPROUVÉE':
        return '#10b981';

      case 'REFUSE':
      case 'REFUSEE':
      case 'REFUSÉ':
      case 'REFUSÉE':
      case 'REFUSE_MANAGER':
      case 'REFUSE_PAR_MANAGER':
        return '#ef4444';

      case 'EN_ATTENTE':
      case 'EN_ATTENTE_MANAGER':
      case 'EN_ATTENTE_RH':
      case 'EN_ATTENTE_ADMIN':
        return '#f59e0b';

      default:
        return '#4361ee';
    }
  }

  private normalizeEndDate(end?: string): string | undefined {
    if (!end) {
      return undefined;
    }

    return end;
  }
}