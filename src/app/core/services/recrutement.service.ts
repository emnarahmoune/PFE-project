import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  OffreRecrutement,
  CreateOffreRecrutementRequest,
  UpdateOffreRecrutementRequest,
  StatutOffreRecrutement
} from '../models/offre-recrutement.model';
import { Observable, catchError, map, of, tap } from 'rxjs';
@Injectable({
  providedIn: 'root'
})
export class RecrutementService {
private readonly apiUrl = `${environment.apiUrl}/recrutement`;
private offresCache: OffreRecrutement[] | null = null;
  constructor(private http: HttpClient) {}

 getAllOffres(): Observable<OffreRecrutement[]> {
  if (this.offresCache) {
    return of(this.offresCache);
  }

  return this.http.get<OffreRecrutement[]>(`${this.apiUrl}/offres?page=0&size=20`).pipe(
    tap(data => this.offresCache = data),
    catchError(error => {
      console.error('Erreur chargement offres recrutement', error);
      return of([]);
    })
  );
}
  getOffresOuvertes(): Observable<OffreRecrutement[]> {
    return this.http.get<OffreRecrutement[]>(`${this.apiUrl}/offres/ouvertes`).pipe(
      catchError(error => {
        console.error('Erreur chargement offres ouvertes', error);
        return of([]);
      })
    );
  }

  getOffreById(id: number): Observable<OffreRecrutement | null> {
    return this.http.get<OffreRecrutement>(`${this.apiUrl}/offres/${id}`).pipe(
      catchError(error => {
        console.error('Erreur chargement offre recrutement', error);
        return of(null);
      })
    );
  }

createOffre(payload: CreateOffreRecrutementRequest): Observable<OffreRecrutement | null> {
  this.offresCache = null;
  return this.http.post<OffreRecrutement>(`${this.apiUrl}/offres`, payload).pipe(
    catchError(error => {
      console.error('Erreur création offre recrutement', error);
      return of(null);
    })
  );
}

 updateOffre(id: number, payload: UpdateOffreRecrutementRequest): Observable<OffreRecrutement | null> {
  this.offresCache = null;
  return this.http.put<OffreRecrutement>(`${this.apiUrl}/offres/${id}`, payload).pipe(
    catchError(error => {
      console.error('Erreur modification offre recrutement', error);
      return of(null);
    })
  );
}

  deleteOffre(id: number): Observable<boolean> {
  this.offresCache = null;
  return this.http.delete<any>(`${this.apiUrl}/offres/${id}`).pipe(
    map(() => true),
    catchError(error => {
      console.error('Erreur suppression offre recrutement', error);
      return of(false);
    })
  );
}

changerStatutOffre(id: number, statut: StatutOffreRecrutement): Observable<OffreRecrutement> {
  this.offresCache = null;
  return this.http.put<OffreRecrutement>(
    `${this.apiUrl}/offres/${id}/statut`,
    { statut }
  );
}
}