import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import {
  Evaluation,
  EvaluationRequest,
  EvaluationStats,
  EmployeEquipeEvaluation
} from '../models/evaluation.model';
import { environment } from '../../../environments/environment';

interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data?: T;
}

@Injectable({
  providedIn: 'root'
})
export class EvaluationService {
  private readonly apiUrl = `${environment.apiUrl}/evaluations`;

  constructor(private http: HttpClient) {}

  // ======================
  // Admin RH
  // ======================

  getAdminEvaluations(): Observable<Evaluation[]> {
    return this.http
      .get<Evaluation[] | ApiResponse<Evaluation[]> | ApiResponse<any>>(
        `${this.apiUrl}/admin`
      )
      .pipe(
        map(response => this.unwrapArray<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  getAdminStats(): Observable<EvaluationStats> {
    return this.http
      .get<EvaluationStats | ApiResponse<EvaluationStats>>(
        `${this.apiUrl}/admin/stats`
      )
      .pipe(
        map(response => this.unwrapObject<EvaluationStats>(response)),
        catchError(this.handleError)
      );
  }

  // ======================
  // Manager
  // ======================

  getEquipe(): Observable<{ success: boolean; data: EmployeEquipeEvaluation[] }> {
    return this.http
      .get<{ success: boolean; data: EmployeEquipeEvaluation[] }>(
        `${environment.apiUrl}/manager/equipe`
      )
      .pipe(catchError(this.handleError));
  }

  getManagerEvaluations(): Observable<Evaluation[]> {
    return this.http
      .get<Evaluation[] | ApiResponse<Evaluation[]> | ApiResponse<any>>(
        `${this.apiUrl}/manager`
      )
      .pipe(
        map(response => this.unwrapArray<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  getManagerStats(): Observable<EvaluationStats> {
    return this.http
      .get<EvaluationStats | ApiResponse<EvaluationStats>>(
        `${this.apiUrl}/manager/stats`
      )
      .pipe(
        map(response => this.unwrapObject<EvaluationStats>(response)),
        catchError(this.handleError)
      );
  }

  createManagerEvaluation(payload: EvaluationRequest): Observable<Evaluation> {
    return this.http
      .post<Evaluation | ApiResponse<Evaluation>>(
        `${this.apiUrl}/manager`,
        payload
      )
      .pipe(
        map(response => this.unwrapObject<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  updateManagerEvaluation(
    id: number,
    payload: EvaluationRequest
  ): Observable<Evaluation> {
    return this.http
      .put<Evaluation | ApiResponse<Evaluation>>(
        `${this.apiUrl}/manager/${id}`,
        payload
      )
      .pipe(
        map(response => this.unwrapObject<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  deleteManagerEvaluation(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/manager/${id}`)
      .pipe(catchError(this.handleError));
  }

  // ======================
  // Employé connecté
  // ======================

  getMyEvaluations(): Observable<Evaluation[]> {
    return this.http
      .get<Evaluation[] | ApiResponse<Evaluation[]> | ApiResponse<any>>(
        `${this.apiUrl}/me`
      )
      .pipe(
        map(response => this.unwrapArray<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  getMyStats(): Observable<EvaluationStats> {
    return this.http
      .get<EvaluationStats | ApiResponse<EvaluationStats>>(
        `${this.apiUrl}/me/stats`
      )
      .pipe(
        map(response => this.unwrapObject<EvaluationStats>(response)),
        catchError(this.handleError)
      );
  }

  // ======================
  // Compatibilité
  // ======================

  getEvaluationById(id: number): Observable<Evaluation> {
    return this.http
      .get<Evaluation | ApiResponse<Evaluation>>(`${this.apiUrl}/${id}`)
      .pipe(
        map(response => this.unwrapObject<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  getEvaluationsByEmploye(employeId: number): Observable<Evaluation[]> {
    return this.http
      .get<Evaluation[] | ApiResponse<Evaluation[]> | ApiResponse<any>>(
        `${this.apiUrl}/employe/${employeId}`
      )
      .pipe(
        map(response => this.unwrapArray<Evaluation>(response)),
        catchError(this.handleError)
      );
  }

  // ======================
  // Helpers
  // ======================

  private unwrapArray<T>(response: any): T[] {
    if (Array.isArray(response)) {
      return response;
    }

    if (Array.isArray(response?.data)) {
      return response.data;
    }

    if (Array.isArray(response?.data?.content)) {
      return response.data.content;
    }

    if (Array.isArray(response?.content)) {
      return response.content;
    }

    return [];
  }

  private unwrapObject<T>(response: any): T {
    if (response?.data !== undefined && response?.data !== null) {
      return response.data as T;
    }

    return response as T;
  }

  private handleError(error: HttpErrorResponse) {
    console.error('Erreur EvaluationService:', error);
    return throwError(() => error);
  }

  createAdminManagerEvaluation(payload: EvaluationRequest): Observable<Evaluation> {
  return this.http
    .post<Evaluation | ApiResponse<Evaluation>>(
      `${this.apiUrl}/admin/managers`,
      payload
    )
    .pipe(
      map(response => this.unwrapObject<Evaluation>(response)),
      catchError(this.handleError)
    );
}



updateAdminEvaluation(id: number, payload: EvaluationRequest): Observable<Evaluation> {
  return this.http
    .put<Evaluation | ApiResponse<Evaluation>>(
      `${this.apiUrl}/${id}`,
      payload
    )
    .pipe(
      map(response => this.unwrapObject<Evaluation>(response)),
      catchError(this.handleError)
    );
}
}