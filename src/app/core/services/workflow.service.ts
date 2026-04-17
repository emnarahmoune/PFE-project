// src/app/core/services/workflow.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AuthService } from './auth.service';

export interface Task {
  taskId: string;
  taskName: string;
  createTime: string;
  processInstanceId: string;
  employeId?: string;
  nbJours?: number;
  demandeId?: number;
  dateDebut?: string;
  dateFin?: string;
  type?: string;
  commentaire?: string;
  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
}

export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
  statusCode?: number;
  timestamp?: string;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private apiUrl = '/api';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }

  // ==================== MANAGER ====================
  // ✅ Récupère les tâches du manager (demandes en attente)
  getManagerTasks(): Observable<Task[]> {
    return this.http.get<ApiResponse<Task[]>>(
      `${this.apiUrl}/manager/conges`,
      { headers: this.getHeaders() }
    ).pipe(map(res => res.data || []));
  }

  // ✅ Approbation par le manager
  approveTask(taskId: string, commentaire: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/manager/approuver-demande`,
      { taskId, commentaire },
      { headers: this.getHeaders() }
    );
  }

  // ✅ Refus par le manager
  rejectTask(taskId: string, motif: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/manager/refuser-demande`,
      { taskId, motif },
      { headers: this.getHeaders() }
    );
  }

  // ==================== ADMIN RH ====================
  // ✅ Récupère les tâches RH
  getRHTasks(): Observable<Task[]> {
    return this.http.get<ApiResponse<Task[]>>(
      `${this.apiUrl}/admin/conges/a-valider`,
      { headers: this.getHeaders() }
    ).pipe(map(res => res.data || []));
  }

  // ✅ Approbation RH (PUT avec demandeId, pas taskId)
  approveRHTask(demandeId: number, commentaire?: string): Observable<void> {
    const url = commentaire
      ? `${this.apiUrl}/admin/conges/${demandeId}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/admin/conges/${demandeId}/valider`;
    return this.http.put<void>(url, {}, { headers: this.getHeaders() });
  }

  // ✅ Refus RH
  rejectRHTask(demandeId: number, motif: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/admin/conges/${demandeId}/refuser?motif=${encodeURIComponent(motif)}`,
      {},
      { headers: this.getHeaders() }
    );
  }

  // ==================== AUTRES ====================
  getOrphanRequests(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/conges/orphan-requests`, { headers: this.getHeaders() });
  }

  getProcessStatus(processInstanceId: string): Observable<any> {
    // Si vous avez un endpoint pour ça, sinon à créer
    return this.http.get(`${this.apiUrl}/workflow/instance/${processInstanceId}`, { headers: this.getHeaders() });
  }
}