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
  commentaire?: string;
  statut: string;
  dateDemande: string;
  processInstanceId?: string;
  taskId?: string;
  currentTaskId?: string;
  motifRefus?: string;
  urgente?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminCongeService {
  // ✅ URL relative – le proxy se chargera de rediriger vers http://localhost:8082
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

  getDemandesAValider(): Observable<DemandeCongeAdmin[]> {
    return this.http.get<DemandeCongeAdmin[]>(
      `${this.apiUrl}/a-valider`,
      { headers: this.getHeaders() }
    );
  }

  getAllDemandes(): Observable<DemandeCongeAdmin[]> {
    return this.http.get<DemandeCongeAdmin[]>(
      `${this.apiUrl}/all`,
      { headers: this.getHeaders() }
    );
  }

  getDemandeById(id: number): Observable<DemandeCongeAdmin> {
    return this.http.get<DemandeCongeAdmin>(
      `${this.apiUrl}/${id}`,
      { headers: this.getHeaders() }
    );
  }

  approuverDemande(id: number, commentaire?: string): Observable<void> {
    const url = commentaire 
      ? `${this.apiUrl}/${id}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/${id}/valider`;
    return this.http.put<void>(url, {}, { headers: this.getHeaders() });
  }

  refuserDemande(id: number, motif: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/${id}/refuser?motif=${encodeURIComponent(motif)}`,
      {},
      { headers: this.getHeaders() }
    );
  }

  getStats(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${this.apiUrl}/stats/statut`,
      { headers: this.getHeaders() }
    );
  }

  getOrphanRequests(): Observable<DemandeCongeAdmin[]> {
    return this.http.get<DemandeCongeAdmin[]>(
      `${this.apiUrl}/orphan-requests`,
      { headers: this.getHeaders() }
    );
  }
}