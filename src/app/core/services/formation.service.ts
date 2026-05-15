import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Formation } from '../models/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {
  private readonly apiUrl = 'http://localhost:8082/api/formations';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.apiUrl);
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  create(formation: Formation): Observable<any> {
    return this.http.post<any>(this.apiUrl, formation);
  }

  update(id: number, formation: Formation): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, formation);
  }

  delete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  activer(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/activer`, {});
  }

  desactiver(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/desactiver`, {});
  }

  getParticipants(id: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${id}/participants`);
  }

  retirerParticipant(formationId: number, employeId: number): Observable<any> {
    return this.http.delete<any>(
      `${this.apiUrl}/${formationId}/participants/${employeId}`
    );
  }

  uploadPdf(formData: FormData): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/upload-pdf`, formData);
  }

  getMyFormations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/mes-formations`);
  }

  inscrireFormation(formationId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${formationId}/inscrire`, {});
  }

  getVideos(formationId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${formationId}/videos`);
  }

  getCompletedVideos(formationId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/${formationId}/completed-videos`);
  }

  completeVideo(videoId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/videos/${videoId}/complete`, {});
  }

  resetFormationProgress(formationId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${formationId}/reset-progress`, {});
  }

  generateCertificate(formationId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${formationId}/certificate`, {
      responseType: 'blob'
    });
  }

  getRecommendations(): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/recommendations`);
}


}