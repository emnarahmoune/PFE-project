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
  private baseUrl = '/api/formations'; // 🔥 simplifié (proxy Angular)

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

  // 🔥 CORRIGÉ → DETAILS COMPLETS
  getById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}/details`);
  }

  create(formation: Formation): Observable<FormationResponse> {
    return this.api.post<FormationResponse>(this.endpoint, formation);
  }

  update(id: number, formation: Formation): Observable<FormationResponse> {
    return this.api.put<FormationResponse>(`${this.endpoint}/id`, id, formation);
  }

  // 🔥 CORRIGÉ (URL BACKEND)
  delete(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/id/${id}`);
  }

  // =========================
  // PARTICIPANTS
  // =========================

  getParticipants(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${id}/participants`);
  }

  retirerParticipant(formationId: number, employeId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${formationId}/participants/${employeId}`);
  }

  // =========================
  // MES FORMATIONS
  // =========================

  getMyFormations(): Observable<any[]> {
    return this.http.get<any[]>(`/api/employe-formations/mes-formations`);
  }

  inscrireFormation(formationId: number) {
    return this.http.post(`/api/employe-formations/inscrire`, {
      formationId
    });
  }

  // =========================
  // RECOMMANDATIONS
  // =========================

  getRecommendations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/recommandations`);
  }

  getRecommendationsSkill(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/recommandations-skill`);
  }

  // =========================
  // VIDEOS
  // =========================

  getVideos(formationId: number): Observable<any[]> {
    return this.http.get<any[]>(`/api/employe-formations/${formationId}/videos`);
  }

  completeVideo(videoId: number) {
    return this.http.post(`/api/employe-formations/complete-video/${videoId}`, {});
  }

  getCompletedVideos(formationId: number) {
    return this.http.get<number[]>(`/api/employe-formations/${formationId}/completed-videos`);
  }

  // =========================
  // UPLOAD
  // =========================

  uploadPdf(formData: FormData) {
    return this.http.post(`${this.baseUrl}/upload-pdf`, formData);
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
 activer(id: number) {
  return this.http.put(`/api/formations/${id}/activer`, {});
}

desactiver(id: number) {
  return this.http.put(`/api/formations/${id}/desactiver`, {});
}
 

 

  // =========================
  // STATS
  // =========================

  getStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/stats`);
  }
}