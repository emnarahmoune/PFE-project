import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

import { CvAnalyse } from '../models/cv-analyse.model';
import { MatchingResult } from '../models/recrutement-score.model';

@Injectable({
  providedIn: 'root'
})
export class CvAnalysisService {
  private readonly apiUrl = '/api/cv-analysis';

  constructor(private http: HttpClient) {}

  analyserCandidature(candidatureId: number): Observable<MatchingResult | null> {
    return this.http.post<MatchingResult>(
      `${this.apiUrl}/candidature/${candidatureId}/analyser`,
      {}
    ).pipe(
      catchError(error => {
        console.error('Erreur analyse IA candidature', error);
        return of(null);
      })
    );
  }

  getAnalyseByCandidature(candidatureId: number): Observable<CvAnalyse | null> {
    return this.http.get<CvAnalyse>(
      `${this.apiUrl}/candidature/${candidatureId}`
    ).pipe(
      catchError(error => {
        console.error('Erreur chargement analyse CV', error);
        return of(null);
      })
    );
  }
}