// src/app/features/employee/services/employee-conge.service.ts
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { DemandeConge, SoldeConges, CongeResponse } from '../models/conge.model';

@Injectable({ providedIn: 'root' })
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
  const urgenteValue =
    demande.urgente === true ||
    String((demande as any).urgent).toLowerCase() === 'true' ||
    String((demande as any).isUrgent).toLowerCase() === 'true';

  const demandeFormatted = {
    type: demande.type,
    dateDebut: this.formatDateForBackend(demande.dateDebut),
    dateFin: this.formatDateForBackend(demande.dateFin),
    commentaire: demande.commentaire || '',

    urgente: urgenteValue,
    urgent: urgenteValue,
    isUrgent: urgenteValue
  };

  console.log('PAYLOAD CREATION BACKEND = ', demandeFormatted);

  return this.api.post<CongeResponse>(this.endpoint, demandeFormatted);
}

modifierDemande(id: number, demande: DemandeConge): Observable<CongeResponse> {
  const urgenteValue =
    demande.urgente === true ||
    String((demande as any).urgent).toLowerCase() === 'true' ||
    String((demande as any).isUrgent).toLowerCase() === 'true';

  const demandeFormatted = {
    type: demande.type,
    dateDebut: this.formatDateForBackend(demande.dateDebut),
    dateFin: this.formatDateForBackend(demande.dateFin),
    commentaire: demande.commentaire || '',

    // ✅ Important
    urgente: urgenteValue,
    urgent: urgenteValue,
    isUrgent: urgenteValue
  };

  console.log('PAYLOAD MODIFICATION BACKEND = ', demandeFormatted);

  return this.api.put<CongeResponse>(
    this.endpoint,
    id,
    demandeFormatted
  );
}

  annulerConge(id: number): Observable<CongeResponse> {
    return this.api.put<CongeResponse>(`${this.endpoint}/annuler`, id, {});
  }

  // ✅ Solde typé explicitement
  getMonSoldeConges(): Observable<CongeResponse & { data: SoldeConges }> {
    return this.api.get<CongeResponse & { data: SoldeConges }>(`${this.endpoint}/mon-solde-conges`);
  }

  // ===== ADMIN (si besoin) =====
  getAllDemandesAdmin(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/admin/all`);
  }

  getNotifications(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/notifications`);
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

  deleteDemandeAdmin(id: number): Observable<CongeResponse> {
    return this.api.delete<CongeResponse>(this.endpoint, id);
  }

  // ===== UTILITAIRES =====
private formatDateForBackend(date: string | Date | undefined): string | undefined {
  if (!date) return undefined;

  const d = new Date(date);

  if (Number.isNaN(d.getTime())) {
    return undefined;
  }

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
} 
}