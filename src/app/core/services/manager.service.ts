import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagerStats {
  employes: number;
  congesEnAttente: number;
  absenteisme: number;
  turnover: number;
}

export interface Manager {
  id: number;
  nom: string;
  prenom: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {

  private apiUrl = 'http://localhost:8082/api/manager';
  private adminUrl = 'http://localhost:8082/api/managers'; // 🔥 endpoint pour lister tous les managers

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

  getAll(): Observable<Manager[]> {
    return this.http.get<Manager[]>(this.adminUrl);
  }
}