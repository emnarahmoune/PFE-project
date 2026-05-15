import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EmployeeDashboardStats {
  congesRestants: number;
  formationsTerminees: number;
  progressionFormations: number;
  competencesValidees: number;
  noteMoyenne: number;
  certificatsObtenus: number;
  recommandationsIA: number;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeDashboardService {
  private apiUrl = `${environment.apiUrl}/employee-dashboard`;

  constructor(private http: HttpClient) {}

  getMesStats(): Observable<{ success: boolean; data: EmployeeDashboardStats }> {
    return this.http.get<{ success: boolean; data: EmployeeDashboardStats }>(
      `${this.apiUrl}/stats`
    );
  }
}