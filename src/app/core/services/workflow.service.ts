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
  urgente?: boolean
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
 getManagerTasks(): Observable<Task[]> {
  return this.http.get<any>(`${this.apiUrl}/workflow/manager/tasks`, { headers: this.getHeaders() })
      .pipe(map(response => {
        console.log('📦 Réponse brute de /api/manager/conges :', response);
        let tasks: Task[] = [];
        // Cas 1: réponse avec wrapper { data: [...] }
        if (response && response.data && Array.isArray(response.data)) {
          tasks = response.data;
        }
        // Cas 2: réponse directe sous forme de tableau
        else if (Array.isArray(response)) {
          tasks = response;
        }
        // Cas 3: réponse paginée { content: [...] }
        else if (response && response.content && Array.isArray(response.content)) {
          tasks = response.content;
        }
        console.log('✅ Tâches extraites :', tasks);
        return tasks;
      }));
  }

approveTask(taskId: string, commentaire: string = ''): Observable<ApiResponse<string>> {
  return this.http.post<ApiResponse<string>>(
    `${this.apiUrl}/workflow/manager/decide`,
    {
      taskId,
      approve: true,
      comment: commentaire || ''
    },
    { headers: this.getHeaders() }
  );
}

rejectTask(taskId: string, motif: string): Observable<ApiResponse<string>> {
  return this.http.post<ApiResponse<string>>(
    `${this.apiUrl}/workflow/manager/decide`,
    {
      taskId,
      approve: false,
      comment: motif
    },
    { headers: this.getHeaders() }
  );
}

  // ==================== ADMIN RH ====================
  getRHTasks(): Observable<Task[]> {
    return this.http.get<any>(`${this.apiUrl}/admin/conges/a-valider`, { headers: this.getHeaders() })
      .pipe(map(response => {
        console.log('📦 Réponse brute de /api/admin/conges/a-valider :', response);
        let tasks: Task[] = [];
        if (response && response.data && Array.isArray(response.data)) {
          tasks = response.data;
        } else if (Array.isArray(response)) {
          tasks = response;
        } else if (response && response.content && Array.isArray(response.content)) {
          tasks = response.content;
        }
        return tasks;
      }));
  }

  approveRHTask(demandeId: number, commentaire?: string): Observable<void> {
    const url = commentaire
      ? `${this.apiUrl}/admin/conges/${demandeId}/valider?commentaire=${encodeURIComponent(commentaire)}`
      : `${this.apiUrl}/admin/conges/${demandeId}/valider`;
    return this.http.put<void>(url, {}, { headers: this.getHeaders() });
  }

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
    return this.http.get(`${this.apiUrl}/workflow/instance/${processInstanceId}`, { headers: this.getHeaders() });
  }
}