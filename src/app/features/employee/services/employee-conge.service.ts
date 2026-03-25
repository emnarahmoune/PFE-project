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

  getMesConges(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/mes-conges`);
  }

  getCongeById(id: number): Observable<CongeResponse> {
    return this.api.getById<CongeResponse>(this.endpoint, id);
  }

  soumettreDemande(demande: DemandeConge): Observable<CongeResponse> {
    return this.api.post<CongeResponse>(this.endpoint, demande);
  }

  modifierDemande(id: number, demande: DemandeConge): Observable<CongeResponse> {
    return this.api.put<CongeResponse>(this.endpoint, id, demande);
  }

  annulerDemande(id: number): Observable<CongeResponse> {
    return this.api.put<CongeResponse>(this.endpoint, id, { statut: 'ANNULE' });
  }
  getMonSoldeConges(): Observable<CongeResponse> {
    return this.api.get<CongeResponse>(`${this.endpoint}/mon-solde-conges`);
}}