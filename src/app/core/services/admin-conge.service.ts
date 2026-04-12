// src/app/core/services/admin-conge.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface DemandeCongeAdmin {
  id: number;
  employeNom: string;
  employePrenom: string;
  employeEmail: string;
  employeId: number;
  dateDebut: string;
  dateFin: string;
  joursOuvres: number;
  type: string;
  commentaire: string;
  statut: string;
  dateDemande: string;
  processInstanceId: string;
  taskId: string;
  currentTaskId?: string;
  motifRefus?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  statusCode?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminCongeService {
  private apiUrl = 'http://localhost:8082/api/admin/conges';

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

  /**
   * Récupère les demandes en attente avec plus de 10 jours
   */
  getDemandesAValider(): Observable<ApiResponse<DemandeCongeAdmin[]>> {
    return this.http.get<ApiResponse<DemandeCongeAdmin[]>>(
      `${this.apiUrl}/a-valider`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère toutes les demandes
   */
  getAllDemandes(): Observable<ApiResponse<DemandeCongeAdmin[]>> {
    return this.http.get<ApiResponse<DemandeCongeAdmin[]>>(
      `${this.apiUrl}/all`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère une demande par son ID
   */
  getDemandeById(id: number): Observable<ApiResponse<DemandeCongeAdmin>> {
    return this.http.get<ApiResponse<DemandeCongeAdmin>>(
      `${this.apiUrl}/${id}`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Approuve une demande
   */
  approuverDemande(id: number, commentaire?: string): Observable<ApiResponse<void>> {
    const url = commentaire 
      ? `${this.apiUrl}/${id}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/${id}/valider`;
    return this.http.put<ApiResponse<void>>(url, {}, { headers: this.getHeaders() });
  }

  /**
   * Refuse une demande avec motif
   */
  refuserDemande(id: number, motif: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.apiUrl}/${id}/refuser?motif=${encodeURIComponent(motif)}`,
      {},
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère les statistiques par statut
   */
  getStats(): Observable<ApiResponse<Record<string, number>>> {
    return this.http.get<ApiResponse<Record<string, number>>>(
      `${this.apiUrl}/stats/statut`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupère les demandes orphelines (sans instance Camunda)
   */
  getOrphanRequests(): Observable<ApiResponse<DemandeCongeAdmin[]>> {
    return this.http.get<ApiResponse<DemandeCongeAdmin[]>>(
      `${this.apiUrl}/orphan-requests`,
      { headers: this.getHeaders() }
    );
  }
}