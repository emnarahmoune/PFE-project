import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../../core/services/api.service';
import { Competence, CompetenceResponse } from '../models/competence.model';

@Injectable({
  providedIn: 'root'
})
export class CompetenceService {
  private endpoint = 'competences';

  constructor(private api: ApiService) {}

  // ===== CRUD OPERATIONS =====

  getAll(): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(this.endpoint);
  }

  getAllPaged(page: number = 0, size: number = 10, sortBy: string = 'nom', direction: string = 'asc'): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/paged`, { page, size, sortBy, direction });
  }

  getById(id: number): Observable<CompetenceResponse> {
    return this.api.getById<CompetenceResponse>(this.endpoint, id);
  }

  getByNom(nom: string): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/nom/${nom}`);
  }

  create(competence: Competence): Observable<CompetenceResponse> {
    return this.api.post<CompetenceResponse>(this.endpoint, competence);
  }

  update(id: number, competence: Competence): Observable<CompetenceResponse> {
    return this.api.put<CompetenceResponse>(this.endpoint, id, competence);
  }

  delete(id: number): Observable<CompetenceResponse> {
    return this.api.delete<CompetenceResponse>(this.endpoint, id);
  }

  // ===== RECHERCHES SPÉCIFIQUES =====

  getByCategorie(categorie: string): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/categorie/${categorie}`);
  }

  getCategories(): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/categories`);
  }

  getTopCompetences(limit: number = 10): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/top/${limit}`);
  }

  getCompetencesNonAttribuees(): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/non-attribuees`);
  }

  // ===== STATISTIQUES =====

  getStats(): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/stats/categories`);
  }

  // ===== RECHERCHE =====

  search(keyword: string): Observable<CompetenceResponse> {
    return this.api.get<CompetenceResponse>(`${this.endpoint}/search`, { keyword });
  }
}