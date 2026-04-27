// src/app/core/services/admin-conge.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AuthService } from './auth.service';

// Interface de la réponse API (wrapper)
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
  demandeId: number;        // ← c'est l'ID de la demande, utilisé pour les actions
  montantConge?: number;
  dateDebut?: string;
  dateFin?: string;
  type?: string;
  commentaire?: string;
  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
}

export interface StatsConges {
  EN_ATTENTE: number;
  APPROUVE: number;
  REFUSE: number;
  [key: string]: number;
}
export interface DemandeRefusManager {
  demandeId: number;
employeDepartement: string|undefined;
motif: any;
  id: number;
  dateDebut: string;
  dateFin: string;
  type: string;
  statut: string;
  motifRefus: string;
  dateDecision: string;
  employeNom: string;
  employePrenom: string;
  managerNom: string;
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

  // Récupère les tâches RH (désemballe response.data)
  getDemandesAValider(): Observable<TacheRh[]> {
    return this.http.get<ApiResponse<TacheRh[]>>(
      `${this.apiUrl}/a-valider`,
      { headers: this.getHeaders() }
    ).pipe(map(res => res.data || []));
  }

  // ✅ Approbation : PUT /api/admin/conges/{demandeId}/valider
  approuverDemande(demandeId: number, commentaire?: string): Observable<void> {
    const url = commentaire
      ? `${this.apiUrl}/${demandeId}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/${demandeId}/valider`;
    return this.http.put<void>(url, {}, { headers: this.getHeaders() });
  }

  // ✅ Refus : PUT /api/admin/conges/{demandeId}/refuser
  refuserDemande(demandeId: number, motif: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/${demandeId}/refuser?motif=${encodeURIComponent(motif)}`,
      {},
      { headers: this.getHeaders() }
    );
  }
  

  // ✅ NOUVEAU : récupérer les demandes refusées par les managers
  getRefusManager(): Observable<DemandeRefusManager[]> {
    return this.http
      .get<ApiResponse<DemandeRefusManager[]>>(`${this.apiUrl}/refus-manager`, { headers: this.getHeaders() })
      .pipe(map(res => res.data || []));
  }  
  // Statistiques
  getStats(): Observable<StatsConges> {
    return this.http.get<ApiResponse<StatsConges>>(
      `${this.apiUrl}/stats/statut`,
      { headers: this.getHeaders() }
    ).pipe(map(res => res.data || { EN_ATTENTE: 0, APPROUVE: 0, REFUSE: 0 }));
  }
}