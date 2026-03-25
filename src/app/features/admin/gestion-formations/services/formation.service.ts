import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../../core/services/api.service';
import { Formation, FormationResponse, InscriptionRequest } from '../models/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {
  private endpoint = 'formations';

  constructor(private api: ApiService) {}

  // ===== CRUD OPERATIONS =====

  getAll(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(this.endpoint);
  }

  getAllPaged(page: number = 0, size: number = 10, sortBy: string = 'titre', direction: string = 'asc'): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/paged`, { page, size, sortBy, direction });
  }

  getById(id: number): Observable<FormationResponse> {
    return this.api.getById<FormationResponse>(this.endpoint, id);
  }

  getByTitre(titre: string): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/titre/${titre}`);
  }

  create(formation: Formation): Observable<FormationResponse> {
    return this.api.post<FormationResponse>(this.endpoint, formation);
  }

  update(id: number, formation: Formation): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(this.endpoint, id, formation);
  }

  delete(id: number): Observable<FormationResponse> {
    return this.api.delete<FormationResponse>(this.endpoint, id);
  }

  // ===== GESTION DES PARTICIPANTS =====

  ajouterParticipant(formationId: number, employeId: number): Observable<FormationResponse> {
    return this.api.post<FormationResponse>(`${this.endpoint}/${formationId}/participants/${employeId}`, {});
  }

  retirerParticipant(formationId: number, employeId: number): Observable<FormationResponse> {
    return this.api.delete<FormationResponse>(`${this.endpoint}/${formationId}/participants`, employeId);
  }

  getParticipants(formationId: number): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/${formationId}/participants`);
  }

  // ===== RECHERCHES SPÉCIFIQUES =====

  getByDomaine(domaine: string): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/domaine/${domaine}`);
  }

  getActives(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/actives`);
  }

  getPopulaires(limit: number = 10): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/populaires`, { limit });
  }

  getFormationsByEmploye(employeId: number): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/employe/${employeId}`);
  }

  getFormationsNonSuivies(employeId: number): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/non-suivies/${employeId}`);
  }

  // ===== STATISTIQUES =====

  getStats(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/stats/tableau-bord`);
  }

  getStatsByDomaine(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/stats/domaine`);
  }

  getDureeMoyenne(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/stats/duree-moyenne`);
  }

  // ===== GESTION DU STATUT =====

  activer(id: number): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(this.endpoint, id, { actif: true });
  }

  desactiver(id: number): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(this.endpoint, id, { actif: false });
  }

  // ===== RECHERCHE =====

  search(keyword: string): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/search`, { keyword });
  }

  getRecentes(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/recentes`);
  }
}