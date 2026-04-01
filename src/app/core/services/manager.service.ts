import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagerStats {
  employes: number;
  congesEnAttente: number;
  absenteisme: number;
  turnover: number;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {

  private apiUrl = 'http://localhost:8082/api/manager'; // 🔥 ton backend

  constructor(private http: HttpClient) {}

  getStats(): Observable<ManagerStats> {
    return this.http.get<ManagerStats>(`${this.apiUrl}/stats`);
  }

  getEquipe(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/equipe`);
  }

  getConges(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/conges`);
  }
  getAlertes(): Observable<string[]> {
  return this.http.get<string[]>(`${this.apiUrl}/alertes`);
}
}