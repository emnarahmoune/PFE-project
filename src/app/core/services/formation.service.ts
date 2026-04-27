import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { HttpClient } from '@angular/common/http';
import { Formation, FormationResponse } from '../../features/admin/gestion-formations/models/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {

  private endpoint = 'formations';
  private baseUrl = 'http://localhost:8082/api';

  constructor(
    private api: ApiService,
    private http: HttpClient
  ) {}

  // =========================
  // CRUD
  // =========================

  getAll(): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(this.endpoint);
  }

  getAllPaged(page = 0, size = 10): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/paged`, { page, size });
  }

  getById(id: number): Observable<FormationResponse> {
    return this.api.getById<FormationResponse>(this.endpoint, id);
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

  // =========================
  // MES FORMATIONS
  // =========================

  getMyFormations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/employe-formations/mes-formations`);
  }

  // =========================
  // INSCRIPTION
  // =========================

 inscrireFormation(formationId: number) {
  return this.http.post(`${this.baseUrl}/employe-formations/inscrire`, {
  formationId: formationId
});
}

  // =========================
  // RECOMMANDATIONS
  // =========================

 getRecommendations(): Observable<any[]> {
  return this.http.get<any[]>(`${this.baseUrl}/formations/recommandations`);
}


  // =========================
  // VIDEOS
  // =========================

  getVideos(formationId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/employe-formations/${formationId}/videos`);
  }

  // =========================
  // PROGRESSION
  // =========================

  completeVideo(videoId: number) {
    return this.http.post(`${this.baseUrl}/employe-formations/complete-video/${videoId}`, {});
  }


  getCompletedVideos(formationId: number) {
  return this.http.get<number[]>(
    `/api/employe-formations/${formationId}/completed-videos`
  );
}

getRecommendationsSkill(): Observable<any[]> {
  return this.http.get<any[]>(`${this.baseUrl}/formations/recommandations-skill`);
}


  // =========================
  // SEARCH
  // =========================

  search(keyword: string): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/search`, { keyword });
  }

  // =========================
  // ACTIVER / DESACTIVER
  // =========================

  activer(id: number): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(this.endpoint, id, { actif: true });
  }

  desactiver(id: number): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(this.endpoint, id, { actif: false });
  }

  // =========================
  // PARTICIPANTS
  // =========================

  getParticipants(formationId: number): Observable<FormationResponse> {
    return this.api.get<FormationResponse>(`${this.endpoint}/${formationId}/participants`);
  }

  retirerParticipant(formationId: number, employeId: number): Observable<FormationResponse> {
    return this.api.delete<FormationResponse>(`${this.endpoint}/${formationId}/participants`, employeId);
  }

  // =========================
  // STATS
  // =========================

  getStats(): Observable<any> {
    return this.api.get<any>(`${this.endpoint}/stats/tableau-bord`);
  }
}