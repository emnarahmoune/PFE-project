// src/app/features/admin/services/employe.service.ts
import { Injectable } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { ApiService } from '../../../../core/services/api.service';
import { Employe, EmployeResponse } from '../models/employe.model';

export interface EmployeStats {
  totalEmployes: number;
  employesActifs: number;
  employesInactifs: number;
  employesEnConge: number;
  salaireMoyen: number;
  masseSalariale: number;
  soldeCongesMoyen: number;
}

@Injectable({ providedIn: 'root' })
export class EmployeService {
  private endpoint = 'employes';

  constructor(private api: ApiService) {}

  getAll(): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(this.endpoint).pipe(catchError(this.handleError('getAll', { success: false, data: [] })));
  }

  getById(id: number): Observable<EmployeResponse> {
    return this.api.getById<EmployeResponse>(this.endpoint, id).pipe(catchError(this.handleError('getById', { success: false, data: {} })));
  }

  create(employe: Employe): Observable<EmployeResponse> {
    return this.api.post<EmployeResponse>(this.endpoint, employe).pipe(catchError(this.handleError('create', { success: false, data: {} })));
  }

  update(id: number, employe: Employe): Observable<EmployeResponse> {
    return this.api.put<EmployeResponse>(this.endpoint, id, employe).pipe(catchError(this.handleError('update', { success: false, data: {} })));
  }

  delete(id: number): Observable<EmployeResponse> {
    return this.api.delete<EmployeResponse>(this.endpoint, id).pipe(catchError(this.handleError('delete', { success: false, data: {} })));
  }

  getAllManagers(): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/managers`).pipe(
      catchError(this.handleError('getAllManagers', { success: false, data: [] }))
    );
  }

  assignManager(employeId: number, managerId: number): Observable<EmployeResponse> {
    return this.api.put<EmployeResponse>(`${this.endpoint}/${employeId}/manager`, managerId, {}).pipe(
      catchError(this.handleError('assignManager', { success: false, data: {} }))
    );
  }

  // Alias pour assignManager
  updateManager(employeId: number, managerId: number): Observable<EmployeResponse> {
    return this.assignManager(employeId, managerId);
  }

  // ✅ NOUVEAU : Récupérer l'équipe d'un manager
  getEquipeByManagerId(managerId: number): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/manager/${managerId}/equipe`).pipe(
      catchError(this.handleError('getEquipeByManagerId', { success: false, data: [] }))
    );
  }

  // ✅ NOUVEAU : Récupérer les congés d'un employé
  getEmployeConges(employeId: number): Observable<any> {
    // Utilise l'endpoint des congés (à adapter selon votre API)
    return this.api.get<any>(`conges/employe/${employeId}`).pipe(
      catchError(this.handleError('getEmployeConges', { success: false, data: [] }))
    );
  }

  findByDepartement(departement: string): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/departement/${departement}`).pipe(catchError(this.handleError('findByDepartement', { success: false, data: [] })));
  }

  findActifs(): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/actifs`).pipe(catchError(this.handleError('findActifs', { success: false, data: [] })));
  }

  private handleError<T>(operation: string, fallback: T) {
    return (error: any): Observable<T> => {
      console.error(`❌ ${operation}`, error);
      return of(fallback);
    };
  }
}