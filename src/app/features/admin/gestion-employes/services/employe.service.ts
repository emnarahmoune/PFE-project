import { Injectable } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';
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

@Injectable({
  providedIn: 'root'
})
export class EmployeService {
  private endpoint = 'employes';

  constructor(private api: ApiService) {
    console.log('📦 EmployeService connecté au backend');
  }

  // ===== CRUD =====

  getAll(): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(this.endpoint).pipe(
      catchError(this.handleError<EmployeResponse>('getAll', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  getAllPaged(page: number = 0, size: number = 10, sortBy: string = 'id', direction: string = 'asc'): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/paged`, { page, size, sortBy, direction }).pipe(
      catchError(this.handleError<EmployeResponse>('getAllPaged', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  getById(id: number): Observable<EmployeResponse> {
    return this.api.getById<EmployeResponse>(this.endpoint, id).pipe(
      catchError(this.handleError<EmployeResponse>('getById', {
        success: false,
        message: 'Employé non trouvé',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 404
      }))
    );
  }

  getByMatricule(matricule: string): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/matricule/${matricule}`).pipe(
      catchError(this.handleError<EmployeResponse>('getByMatricule', {
        success: false,
        message: 'Employé non trouvé',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 404
      }))
    );
  }

  create(employe: Employe): Observable<EmployeResponse> {
    return this.api.post<EmployeResponse>(this.endpoint, employe).pipe(
      catchError(this.handleError<EmployeResponse>('create', {
        success: false,
        message: 'Erreur de création',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  update(id: number, employe: Employe): Observable<EmployeResponse> {
    return this.api.put<EmployeResponse>(this.endpoint, id, employe).pipe(
      catchError(this.handleError<EmployeResponse>('update', {
        success: false,
        message: 'Erreur de modification',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  delete(id: number): Observable<EmployeResponse> {
    return this.api.delete<EmployeResponse>(this.endpoint, id).pipe(
      catchError(this.handleError<EmployeResponse>('delete', {
        success: false,
        message: 'Erreur de suppression',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  // ===== NOUVELLE MÉTHODE : MISE À JOUR DU MANAGER =====
  // Version CORRIGÉE : utilise la même signature que update (endpoint, id, data)

  /**
   * Met à jour le manager d'un employé
   * @param employeId ID de l'employé
   * @param managerId ID du nouveau manager (peut être null pour supprimer)
   */
  updateManager(employeId: number, managerId: number | null): Observable<EmployeResponse> {
    const payload = { managerId: managerId };
    // Utilise la même signature que update : (endpoint, id, data)
    return this.api.put<EmployeResponse>(`${this.endpoint}/${employeId}/manager`, employeId, payload).pipe(
      map(response => {
        if (response && response.success) {
          console.log(`✅ Manager mis à jour pour l'employé ${employeId}`);
        }
        return response;
      }),
      catchError(this.handleError<EmployeResponse>('updateManager', {
        success: false,
        message: 'Erreur lors de la mise à jour du manager',
        data: {} as Employe,
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  // ===== RECHERCHES SPÉCIFIQUES =====

  findByDepartement(departement: string): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/departement/${departement}`).pipe(
      catchError(this.handleError<EmployeResponse>('findByDepartement', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  findByStatut(statut: string): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/statut/${statut}`).pipe(
      catchError(this.handleError<EmployeResponse>('findByStatut', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  findActifs(): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/actifs`).pipe(
      catchError(this.handleError<EmployeResponse>('findActifs', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  findByManager(managerId: number): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/manager/${managerId}`).pipe(
      catchError(this.handleError<EmployeResponse>('findByManager', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  findByService(serviceId: number): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/service/${serviceId}`).pipe(
      catchError(this.handleError<EmployeResponse>('findByService', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  findBySoldeCongesFaible(seuil: number): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/solde-conges-faible/${seuil}`).pipe(
      catchError(this.handleError<EmployeResponse>('findBySoldeCongesFaible', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  // ===== STATISTIQUES =====

  getStats(): Observable<EmployeStats> {
    return this.api.get<any>(`${this.endpoint}/stats/tableau-bord`).pipe(
      map(response => {
        if (response && response.data) {
          return response.data as EmployeStats;
        }
        return response as EmployeStats;
      }),
      catchError(this.handleError<EmployeStats>('getStats', {
        totalEmployes: 0,
        employesActifs: 0,
        employesInactifs: 0,
        employesEnConge: 0,
        salaireMoyen: 0,
        masseSalariale: 0,
        soldeCongesMoyen: 0
      }))
    );
  }

  getStatsByDepartement(): Observable<any> {
    return this.api.get<any>(`${this.endpoint}/stats/departement`).pipe(
      map(response => response.data || []),
      catchError(this.handleError<any>('getStatsByDepartement', []))
    );
  }

  getStatsByStatut(): Observable<any> {
    return this.api.get<any>(`${this.endpoint}/stats/statut`).pipe(
      map(response => response.data || {}),
      catchError(this.handleError<any>('getStatsByStatut', {}))
    );
  }

  getMasseSalariale(): Observable<number> {
    return this.api.get<any>(`${this.endpoint}/stats/masse-salariale`).pipe(
      map(response => {
        if (response && response.data !== undefined) {
          return Number(response.data);
        }
        return 0;
      }),
      catchError(this.handleError<number>('getMasseSalariale', 0))
    );
  }

  getSalaireMoyen(): Observable<number> {
    return this.api.get<any>(`${this.endpoint}/stats/salaire-moyen`).pipe(
      map(response => {
        if (response && response.data !== undefined) {
          return Number(response.data);
        }
        return 0;
      }),
      catchError(this.handleError<number>('getSalaireMoyen', 0))
    );
  }

  // ===== RECHERCHE =====

  search(keyword: string): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/search`, { keyword }).pipe(
      catchError(this.handleError<EmployeResponse>('search', {
        success: false,
        message: 'Erreur de recherche',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  getRecents(limit: number = 10): Observable<EmployeResponse> {
    return this.api.get<EmployeResponse>(`${this.endpoint}/recents`, { limit }).pipe(
      catchError(this.handleError<EmployeResponse>('getRecents', {
        success: false,
        message: 'Erreur de chargement',
        data: [],
        timestamp: new Date().toISOString(),
        statusCode: 500
      }))
    );
  }

  // ===== GESTION DES ERREURS =====

  private handleError<T>(operation = 'operation', fallback: T) {
    return (error: any): Observable<T> => {
      console.error(`❌ Erreur ${operation}:`, error);
      
      if (error.status === 0) {
        console.error('🔌 Backend non accessible - Vérifiez que le serveur Spring Boot est lancé');
      } else if (error.status === 401) {
        console.error('🔒 Non authentifié - Vérifiez votre token');
      } else if (error.status === 403) {
        console.error('🚫 Accès interdit - Vérifiez vos droits');
      } else if (error.status === 404) {
        console.error('🔍 Ressource non trouvée');
      }
      
      return of(fallback);
    };
  }
}