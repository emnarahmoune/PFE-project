import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

import {
  Candidature,
  DecisionCandidatureRequest,
  TopCandidature
} from '../models/candidature.model';

@Injectable({
  providedIn: 'root'
})
export class CandidatureService {
  private readonly apiUrl = '/api/candidatures';

  constructor(private http: HttpClient) {}

  // =========================
  // EMPLOYÉ
  // =========================

  postuler(
    offreId: number,
    employeId: number,
    motivation: string,
    cv: File
  ): Observable<Candidature | null> {
    const formData = new FormData();

    formData.append('offreId', String(offreId));
    formData.append('employeId', String(employeId));
    formData.append('motivation', motivation || '');
    formData.append('cv', cv);

    return this.http.post<Candidature>(`${this.apiUrl}/postuler`, formData).pipe(
      catchError(error => {
        console.error('Erreur soumission candidature', error);
        return of(null);
      })
    );
  }

  getMesCandidatures(employeId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.apiUrl}/employe/${employeId}`).pipe(
      catchError(error => {
        console.error('Erreur chargement mes candidatures', error);
        return of([]);
      })
    );
  }

  hasAlreadyApplied(offreId: number, employeId: number): Observable<any> {
    const params = new HttpParams()
      .set('offreId', String(offreId))
      .set('employeId', String(employeId));

    return this.http.get<any>(`${this.apiUrl}/exists`, { params }).pipe(
      catchError(error => {
        console.error('Erreur vérification candidature existante', error);
        return of({
          success: false,
          message: 'Erreur vérification candidature.'
        });
      })
    );
  }

  // =========================
  // ADMIN
  // =========================

  getCandidaturesByOffre(offreId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.apiUrl}/offre/${offreId}`).pipe(
      catchError(error => {
        console.error('Erreur chargement candidatures par offre', error);
        return of([]);
      })
    );
  }

  getTopCandidatures(offreId: number, limit = 5): Observable<TopCandidature[]> {
    const params = new HttpParams().set('limit', String(limit));

    return this.http.get<TopCandidature[]>(
      `${this.apiUrl}/offre/${offreId}/top`,
      { params }
    ).pipe(
      catchError(error => {
        console.error('Erreur chargement top candidatures', error);
        return of([]);
      })
    );
  }

  accepterCandidature(
    candidatureId: number,
    payload: DecisionCandidatureRequest = {}
  ): Observable<Candidature | null> {
    return this.http.post<Candidature>(
      `${this.apiUrl}/${candidatureId}/accepter`,
      payload
    ).pipe(
      catchError(error => {
        console.error('Erreur acceptation candidature', error);
        return of(null);
      })
    );
  }

  refuserCandidature(
    candidatureId: number,
    payload: DecisionCandidatureRequest = {}
  ): Observable<Candidature | null> {
    return this.http.post<Candidature>(
      `${this.apiUrl}/${candidatureId}/refuser`,
      payload
    ).pipe(
      catchError(error => {
        console.error('Erreur refus candidature', error);
        return of(null);
      })
    );
  }

  relancerAnalyseIa(candidatureId: number): Observable<Candidature | null> {
    return this.http.post<Candidature>(
      `${this.apiUrl}/${candidatureId}/relancer-analyse`,
      {}
    ).pipe(
      catchError(error => {
        console.error('Erreur relance analyse IA', error);
        return of(null);
      })
    );
  }

  getCvUrl(candidatureId: number): string {
    return `${this.apiUrl}/${candidatureId}/cv`;
  }
}