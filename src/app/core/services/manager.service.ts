// src/app/core/services/manager.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Employe } from '../../features/admin/gestion-employes/models/employe.model';
import { DemandeConge, SoldeConges } from '../../features/employee/models/conge.model';
import { ScoreTurnover } from '../../features/admin/scores/models/score-turnover.model';

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

  getTachesManager(): Observable<{ success: boolean; data: TacheManager[] }> {
    return this.http.get<{ success: boolean; data: TacheManager[] }>(`${this.managerApiUrl}/conges`);
  }

  approuverDemande(taskId: string, commentaire?: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/approuver-demande`,
      { taskId, commentaire }
    );
  }

  getEmployeSoldeConges(employeId: number): Observable<{ success: boolean; data: SoldeConges }> {
    return this.http.get<{ success: boolean; data: SoldeConges }>(
      `${environment.apiUrl}/employes/${employeId}/solde-conges`
    );
  }

  getEmployeHistoriqueConges(employeId: number): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(
      `${environment.apiUrl}/conges/employe/${employeId}/historique`
    );
  }

  refuserDemande(taskId: string, motif: string): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(
      `${this.managerApiUrl}/refuser-demande`,
      { taskId, motif }
    );
  }

  getStats(): Observable<{ success: boolean; data: ManagerStats }> {
    return this.http.get<{ success: boolean; data: ManagerStats }>(`${this.managerApiUrl}/stats`);
  }

  getConges(): Observable<{ success: boolean; data: DemandeConge[] }> {
    return this.http.get<{ success: boolean; data: DemandeConge[] }>(`${this.managerApiUrl}/conges`);
  }

   getDernierScoreTurnover(employeId: number): Observable<{ success: boolean; data: { score: number; niveauRisque: string } }> {
    return this.http.get<{ success: boolean; data: any }>(
      `${environment.apiUrl}/scores-turnover/employe/${employeId}/dernier`
    );
  }
   getDerniersScores(): Observable<{ success: boolean; data: ScoreTurnover[] }> {
    return this.http.get<{ success: boolean; data: ScoreTurnover[] }>(
      `${environment.apiUrl}/scores-turnover/derniers`
    );
  }

    recalculerScoreTurnover(employeId: number): Observable<any> {
    return this.http.post(
      `${environment.apiUrl}/scores-turnover/calculer/employe/${employeId}?systemeBIId=1`,
      {}
    );
  }

 getDernierAbsenteisme(employeId: number): Observable<{ success: boolean; data: { valeur: number; dateCalcul: string } }> {
    return this.http.get<{ success: boolean; data: any }>(
      `${environment.apiUrl}/indicateurs/type/ABSENTEISME?employeId=${employeId}`
    );
  }
  getCalendarEvents(): Observable<any[]> {
  return this.http.get<any[]>(`${environment.apiUrl}/managers/calendar-events`);
}
}