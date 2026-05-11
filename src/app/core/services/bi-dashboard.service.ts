import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardAdminBi {
  effectifTotal: number;
  employesActifs: number;
  employesInactifs: number;
  totalManagers: number;
  totalDepartements: number;

  masseSalariale: number;
  salaireMoyen: number;

  totalDemandesConge: number;
  congesEnAttente: number;
  congesApprouves: number;
  joursAbsence: number;

  totalEvaluations: number;
  noteMoyenneGlobale: number;

  totalFormations: number;
  totalCompetences: number;

  scoreRisqueMoyen: number;
  employesRisqueEleve: number;
}


export interface TopCompetenceBi {
  nom: string;
  count: number;
  pourcentage: number;
}

export interface RecentEmployeeBi {
  id: number;
  nom: string;
  prenom: string;
  poste: string;
  departement: string;
  dateEmbauche: string;
}

export interface DashboardAdminBi {
  effectifTotal: number;
  employesActifs: number;
  employesInactifs: number;
  totalManagers: number;
  totalDepartements: number;

  masseSalariale: number;
  salaireMoyen: number;

  totalDemandesConge: number;
  congesEnAttente: number;
  congesApprouves: number;
  joursAbsence: number;

  totalEvaluations: number;
  noteMoyenneGlobale: number;

  totalFormations: number;
  totalCompetences: number;

  scoreRisqueMoyen: number;
  employesRisqueEleve: number;

  parDepartement: Record<string, number>;
  repartitionStatut: Record<string, number>;
  topCompetences: TopCompetenceBi[];
  recentEmployees: RecentEmployeeBi[];
}


export interface DashboardManagerBi {
  totalEmployes: number;
  employesActifs: number;
  congesEnAttente: number;
  joursAbsence: number;
  tauxPresence: number;
  scoreRisqueMoyen: number;
  employesRisqueEleve: number;
  topCompetences: TopCompetenceBi[];
}
@Injectable({
  providedIn: 'root'
})
export class BiDashboardService {
  private readonly apiUrl = `${environment.apiUrl}/bi`;

  constructor(private http: HttpClient) {}

  getDashboardAdminBi(): Observable<DashboardAdminBi> {
    return this.http.get<DashboardAdminBi>(`${this.apiUrl}/dashboard-admin`);
  }


  getDashboardManagerBi(managerId: number): Observable<DashboardManagerBi> {
  return this.http.get<DashboardManagerBi>(
    `${this.apiUrl}/dashboard-manager/${managerId}`
  );
}
}