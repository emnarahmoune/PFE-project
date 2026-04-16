// src/app/core/services/manager.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employe } from '../../features/admin/gestion-employes/models/employe.model';
import { DemandeConge } from '../../features/employee/models/conge.model';

export interface ManagerStats {
  employes?: number;
  congesEnAttente?: number;
  absenteisme?: number;
  turnover?: number;
  totalEmployes?: number;
  employesActifs?: number;
  masseSalariale?: number;
  salaireMoyen?: number;
  parDepartement?: Record<string, number>;
  tauxPresence?: number;
}

export interface Manager {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  departement?: string;
}

@Injectable({ providedIn: 'root' })
export class ManagerService {
  private apiUrl = `${environment.apiUrl}/employes`;
  private managerApiUrl = `${environment.apiUrl}/manager`;

  constructor(private http: HttpClient) {}

  getEquipe(): Observable<{ success: boolean; data: Employe[] }> {
    return this.http.get<{ success: boolean; data: Employe[] }>(`${this.apiUrl}/equipe`);
  }

  getEmployeDetails(employeId: number): Observable<{ success: boolean; data: Employe }> {
    return this.http.get<{ success: boolean; data: Employe }>(`${this.apiUrl}/manager/employe/${employeId}`);
  }

  getEmployeConges(employeId: number): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(`${environment.apiUrl}/conges/employe/${employeId}`);
  }

  getAllManagers(): Observable<{ success: boolean; data: Manager[] }> {
    return this.http.get<{ success: boolean; data: Manager[] }>(`${this.apiUrl}/managers`);
  }

  getAll(): Observable<{ success: boolean; data: Manager[] }> {
    return this.getAllManagers();
  }

  getStats(): Observable<{ success: boolean; data: ManagerStats }> {
    return this.http.get<{ success: boolean; data: ManagerStats }>(`${this.managerApiUrl}/stats`);
  }

  getConges(): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(`${this.managerApiUrl}/conges`);
  }
}