// src/app/core/services/manager-assignment.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Manager {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  departement: string;
  actif: boolean;
}

export interface AssignManagerDTO {
  employeId: number;
  managerId: number;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerAssignmentService {
  private apiUrl = 'http://localhost:8082/api/admin/manager-assignment';

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

  /**
   * Assigner un manager à un employé
   */
  assignManager(employeId: number, managerId: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/assign`,
      { employeId, managerId },
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupérer tous les employés sans manager
   */
  getEmployesSansManager(): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/employes-sans-manager`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupérer tous les managers
   */
  getAllManagers(): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/managers`,
      { headers: this.getHeaders() }
    );
  }

  /**
   * Récupérer les employés d'un manager
   */
  getEmployesByManager(managerId: number): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/employes/${managerId}`,
      { headers: this.getHeaders() }
    );
  }
}