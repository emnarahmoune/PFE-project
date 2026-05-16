import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormationRecommendation } from '../models/formation-recommendation.model';
import { environment } from '../../../environments/environment';
@Injectable({
  providedIn: 'root'
})
export class FormationRecommendationService {
private readonly apiUrl = `${environment.apiUrl}/formations/recommendations`;
  constructor(private http: HttpClient) {}

  getMyRecommendations(): Observable<FormationRecommendation[]> {
    return this.http.get<FormationRecommendation[]>(
      `${this.apiUrl}/me`
    );
  }

  generateMyRecommendations(): Observable<FormationRecommendation[]> {
    return this.http.get<FormationRecommendation[]>(
      `${this.apiUrl}/me/auto`
    );
  }

  inscrireRecommendation(recommendationId: number): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/${recommendationId}/inscrire`,
      {}
    );
  }
}