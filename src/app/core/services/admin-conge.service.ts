import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { AuthService } from './auth.service';
import { EventInput } from '@fullcalendar/core/index.js';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  statusCode: number;
}

export interface TacheRh {
  statut: string;
  nombreJours: any;
  taskId: string;
  taskName: string;
  createTime: string;
  processInstanceId: string;
  employeId: string;
  nbJours: number;
  demandeId: number;
  montantConge?: number;
  dateDebut?: string;
  dateFin?: string;
  type?: string;
  commentaire?: string;
  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
  employeDepartement?: string;
  photoUrl?: string;
employePhotoProfil?: string;
employePhotoUrl?: string;
}

export interface StatsConges {
  EN_ATTENTE: number;
  APPROUVE: number;
  REFUSE: number;
  [key: string]: number;
}

export interface DemandeRefusManager {
  id: number;
  dateDebut: string;
  dateFin: string;
  type: string;
  statut: string;
  motifRefus: string;
  dateDecision: string;
  employeNom: string;
  employePrenom: string;
  employeDepartement: string;
  managerNom: string;
  photoUrl?: string;
employePhotoProfil?: string;
employePhotoUrl?: string;
}

export interface DemandeRefusDetails {
  demandeId: number;
  employeePrenom: string;
  employeeNom: string;
  employeeEmail: string;
  employeeDepartement: string;
  managerNom: string;
  dateDebut: string;
  dateFin: string;
  nbJours: number;
  type: string;
  motifRefus: string;
  dateSoummission: string;
  dateDecisionManager: string;
  commentaireRH?: string;
  piecesJustificatives?: string[];
  
}

@Injectable({ providedIn: 'root' })
export class AdminCongeService {
  private apiUrl = '/api/admin/conges';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  getDemandesAValider(): Observable<TacheRh[]> {
    return this.http.get<ApiResponse<TacheRh[]>>(
      `${this.apiUrl}/a-valider`,
      { headers: this.getHeaders() }
    ).pipe(map(res => res.data || []));
  }

  approuverDemande(demandeId: number, commentaire?: string): Observable<void> {
    const url = commentaire
      ? `${this.apiUrl}/${demandeId}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/${demandeId}/valider`;
    return this.http.put<void>(url, {}, { headers: this.getHeaders() });
  }

  refuserDemande(demandeId: number, motif: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/${demandeId}/refuser?motif=${encodeURIComponent(motif)}`,
      {},
      { headers: this.getHeaders() }
    );
  }


  getAllCongesForCalendar(): Observable<EventInput[]> {
    return this.http.get<ApiResponse<EventInput[]>>(`${this.apiUrl}/calendar-events`, { headers: this.getHeaders() })
      .pipe(map(res => res.data || []));
  }

  getDemandeRefusDetails(demandeId: number): Observable<DemandeRefusDetails> {
    return this.http.get<ApiResponse<any>>(
      `${this.apiUrl}/demandes/${demandeId}/refus-details`,
      { headers: this.getHeaders() }
    ).pipe(
      tap(response => console.log('RAW API REFUS DETAILS:', response)),
      map(response => {
        if (!response?.success || !response?.data) {
          throw new Error('Données non trouvées dans la réponse');
        }
        const data = response.data;
        // Mapping robuste pour accepter les deux orthographes possibles
        return {
          demandeId: data.demandeId ?? 0,
          employeePrenom: data.employeePrenom ?? data.employePrenom ?? '',
          employeeNom: data.employeeNom ?? data.employeNom ?? '',
          employeeEmail: data.employeeEmail ?? data.employeEmail ?? '',
          employeeDepartement: data.employeeDepartement ?? data.employeDepartement ?? '',
          managerNom: data.managerNom ?? '',
          dateDebut: data.dateDebut ?? '',
          dateFin: data.dateFin ?? '',
          nbJours: data.nbJours ?? 0,
          type: data.type ?? '',
          motifRefus: data.motifRefus ?? '',
          dateSoummission: data.dateSoummission ?? data.dateSoumission ?? '',
          dateDecisionManager: data.dateDecisionManager ?? '',
          commentaireRH: data.commentaireRH ?? '',
          piecesJustificatives: data.piecesJustificatives ?? []
        };
      })
    );
  }

  getRefusManager(): Observable<DemandeRefusManager[]> {
    return this.http.get<ApiResponse<DemandeRefusManager[]>>(`${this.apiUrl}/refus-manager`, { headers: this.getHeaders() })
      .pipe(map(res => res.data || []));
  }

  getStats(): Observable<StatsConges> {
    return this.http.get<ApiResponse<StatsConges>>(`${this.apiUrl}/stats/statut`, { headers: this.getHeaders() })
      .pipe(map(res => res.data || { EN_ATTENTE: 0, APPROUVE: 0, REFUSE: 0 }));
  }
}