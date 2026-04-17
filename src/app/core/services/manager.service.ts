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

// ✅ Interface pour les tâches du manager (identique à Task dans workflow.service)
export interface TacheManager {
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
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(
      `${environment.apiUrl}/conges/employe/${employeId}`
    );
  }

  getAllManagers(): Observable<{ success: boolean; data: Manager[] }> {
    return this.http.get<{ success: boolean; data: Manager[] }>(`${this.apiUrl}/managers`);
  }

  getAll(): Observable<{ success: boolean; data: Manager[] }> {
    return this.getAllManagers();
  }

  // ✅ Récupère les tâches du manager (demandes en attente)
  getTachesManager(): Observable<{ success: boolean; data: TacheManager[] }> {
    return this.http.get<{ success: boolean; data: TacheManager[] }>(`${this.managerApiUrl}/conges`);
  }

  // ✅ Approbation d'une demande
  approuverDemande(taskId: string, commentaire?: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/approuver-demande`,
      { taskId, commentaire }
    );
  }

  // ✅ Refus d'une demande
  refuserDemande(taskId: string, motif: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/refuser-demande`,
      { taskId, motif }
    );
  }

  // ✅ Statistiques pour le manager
  getStats(): Observable<{ success: boolean; data: ManagerStats }> {
    return this.http.get<{ success: boolean; data: ManagerStats }>(`${this.managerApiUrl}/stats`);
  }

  // ✅ Récupère toutes les demandes de congé (manager)
  getConges(): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(`${this.managerApiUrl}/conges`);
  }
}