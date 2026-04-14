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
}

export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
  status?: number;
}

@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  // ✅ URL relative – le proxy se chargera de la redirection
  private apiUrl = '/api';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // ==================== MANAGER ====================

  getManagerTasks(): Observable<Task[]> {
    return this.http.get<ApiResponse<Task[]>>(
      `${this.apiUrl}/workflow/manager/tasks`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => {
        if (Array.isArray(response)) {
          return response as unknown as Task[];
        }
        return response?.data ?? [];
      })
    );
  }

  approveTask(taskId: string, commentaire: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/workflow/manager/decide`,
      { taskId, approve: true, comment: commentaire },
      { headers: this.getHeaders() }
    );
  }

  rejectTask(taskId: string, motif: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/workflow/manager/decide`,
      { taskId, approve: false, comment: motif },
      { headers: this.getHeaders() }
    );
  }

  // ==================== RH ====================

  getRHTasks(): Observable<Task[]> {
    return this.http.get<ApiResponse<Task[]>>(
      `${this.apiUrl}/workflow/rh/tasks`,
      { headers: this.getHeaders() }
    ).pipe(
      map(response => {
        if (Array.isArray(response)) {
          return response as unknown as Task[];
        }
        return response?.data ?? [];
      })
    );
  }

  approveRHTask(taskId: string, commentaire: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/workflow/rh/decide`,
      { taskId, approve: true, comment: commentaire },
      { headers: this.getHeaders() }
    );
  }

  rejectRHTask(taskId: string, motif: string): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(
      `${this.apiUrl}/workflow/rh/decide`,
      { taskId, approve: false, comment: motif },
      { headers: this.getHeaders() }
    );
  }

  // ==================== ADMIN ====================

  getOrphanRequests(): Observable<any> {
    return this.http.get(`${this.apiUrl}/workflow/orphan-requests`, { headers: this.getHeaders() });
  }

  getProcessStatus(processInstanceId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/workflow/instance/${processInstanceId}`, { headers: this.getHeaders() });
  }
}