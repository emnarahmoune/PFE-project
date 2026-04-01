import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { DemandeConge, CongeResponse } from '../models/conge.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeCongeService {
  private endpoint = 'conges';

  constructor(private api: ApiService) {}

  // ===== EMPLOYÉ =====
  
  getMesConges(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/mes-conges`);
  }

  getCongeById(id: number): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/${id}`);
  }

  soumettreDemande(demande: DemandeConge): Observable<CongeResponse> {
    // Formater les dates pour le backend
    const demandeFormatted = {
      ...demande,
      dateDebut: this.formatDateForBackend(demande.dateDebut),
      dateFin: this.formatDateForBackend(demande.dateFin),
      dateDemande: this.formatDateForBackend(demande.dateDemande),
      dateDecision: this.formatDateForBackend(demande.dateDecision)
    };
    return this.api.post<CongeResponse>(this.endpoint, demandeFormatted);
  }

  modifierDemande(id: number, demande: DemandeConge): Observable<CongeResponse> {
    const demandeFormatted = {
      ...demande,
      dateDebut: this.formatDateForBackend(demande.dateDebut),
      dateFin: this.formatDateForBackend(demande.dateFin)
    };
    return this.api.put<CongeResponse>(this.endpoint, id, demandeFormatted);
  }

  annulerConge(id: number): Observable<CongeResponse> {
    return this.api.put<CongeResponse>(`${this.endpoint}/annuler`, id, {});
  }

  getMonSoldeConges(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/mon-solde-conges`);
  }

  // ===== ADMIN =====
  
  getAllDemandesAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/admin/all`);
  }

  getDemandesEnAttenteAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/statut/EN_ATTENTE`);
  }

  getDemandesApprouveesAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/statut/APPROUVE`);
  }

  getDemandesRefuseesAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/statut/REFUSE`);
  }

  getDemandesByEmployeAdmin(employeId: number): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/employe/${employeId}`);
  }

  getStatsAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/stats/statut`);
  }

  // ✅ CORRECTION: delete avec 2 arguments (endpoint, id)
  deleteDemandeAdmin(id: number): Observable<CongeResponse> {
    return this.api.delete<CongeResponse>(this.endpoint, id);
  }

  // ===== UTILITAIRES =====
  
  private formatDateForBackend(date: string | Date | undefined): string | undefined {
    if (!date) return undefined;
    const d = new Date(date);
    return d.toISOString().split('T')[0];
  }
}