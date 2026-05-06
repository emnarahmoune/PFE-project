import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
@Injectable({
  providedIn: 'root'
})
export class EmployeService {

  private endpoint = 'employes';

  constructor(private api: ApiService, 
    private http: HttpClient) {}

  // =========================
  // ===== CRUD ==============
  // =========================

  getAll(): Observable<any> {
    return this.api.get(this.endpoint)
      .pipe(catchError(this.handleError('getAll', [])));
  }

  getById(id: number): Observable<any> {
    return this.api.get(`${this.endpoint}/${id}`)
      .pipe(catchError(this.handleError('getById', {})));
  }

  create(data: any): Observable<any> {
    return this.api.post(this.endpoint, data)
      .pipe(catchError(this.handleError('create', {})));
  }


  update(id: number, data: any): Observable<any> {
    return this.api.put(this.endpoint, id, data)
      .pipe(catchError(this.handleError('update', {})));
  }

  delete(id: number): Observable<any> {
    return this.api.delete(this.endpoint, id)
      .pipe(catchError(this.handleError('delete', {})));
  }

  // =========================
  // ===== PROFIL CONNECTÉ ===
  // =========================

  getMonProfil(): Observable<any> {
    return this.api.get(`${this.endpoint}/mon-profil`)
      .pipe(catchError(this.handleError('getMonProfil', {})));
  }

  updateMonProfil(data: any): Observable<any> {
    return this.api.patch(`${this.endpoint}/mon-profil`, data)
      .pipe(catchError(this.handleError('updateMonProfil', {})));
  }

  getMonSoldeConges(): Observable<any> {
    return this.api.get(`${this.endpoint}/mon-solde-conges`)
      .pipe(catchError(this.handleError('getMonSoldeConges', {})));
  }

  getMesCompetences(): Observable<any> {
    return this.api.get(`${this.endpoint}/mes-competences`)
      .pipe(catchError(this.handleError('getMesCompetences', [])));
  }

  getMesFormations(): Observable<any> {
    return this.api.get(`${this.endpoint}/mes-formations`)
      .pipe(catchError(this.handleError('getMesFormations', [])));
  }

  getMonHistoriqueConges(): Observable<any> {
    return this.api.get(`${this.endpoint}/mon-historique-conges`)
      .pipe(catchError(this.handleError('getMonHistoriqueConges', [])));
  }

  changePassword(data: any): Observable<any> {
    return this.api.post(`${this.endpoint}/change-password`, data)
      .pipe(catchError(this.handleError('changePassword', {})));
  }

  changeEmail(newEmail: string): Observable<any> {
    return this.api.patch(`${this.endpoint}/change-email?newEmail=${encodeURIComponent(newEmail)}`, {})
      .pipe(catchError(this.handleError('changeEmail', {})));
  }

  // =========================
  // ===== MANAGERS ==========
  // =========================

  getAllManagers(): Observable<any> {
    return this.api.get(`${this.endpoint}/managers`)
      .pipe(catchError(this.handleError('getAllManagers', [])));
  }

  updateManager(employeId: number, managerId: number): Observable<any> {
    return this.api.put(`${this.endpoint}/manager`, employeId, {
      managerId: managerId
    }).pipe(catchError(this.handleError('updateManager', {})));
  }

  assignManager(employeId: number, managerId: number): Observable<any> {
    return this.updateManager(employeId, managerId);
  }

  unassignManager(employeId: number): Observable<any> {
  return this.http.delete<any>(
    `${environment.apiUrl}/employes/${employeId}/manager`
  ).pipe(
    catchError(this.handleError('unassignManager', {}))
  );
}

  getEquipeByManagerId(managerId: number): Observable<any> {
    return this.api.get(`${this.endpoint}/manager/${managerId}/equipe`)
      .pipe(catchError(this.handleError('getEquipeByManagerId', [])));
  }

  getMyEquipe(): Observable<any> {
    return this.api.get(`${this.endpoint}/equipe`)
      .pipe(catchError(this.handleError('getMyEquipe', [])));
  }

  getEmployeForManager(employeId: number): Observable<any> {
    return this.api.get(`${this.endpoint}/manager/employe/${employeId}`)
      .pipe(catchError(this.handleError('getEmployeForManager', {})));
  }

  // =========================
  // ===== STATUT ============
  // =========================

  changeStatut(id: number, statut: string): Observable<any> {
    return this.api.put(`${this.endpoint}/statut`, id, { statut })
      .pipe(catchError(this.handleError('changeStatut', {})));
  }

  // =========================
  // ===== COMPÉTENCES =======
  // =========================

  getCompetences(userId: number): Observable<any> {
    return this.api.get(`${this.endpoint}/${userId}/competences`)
      .pipe(catchError(this.handleError('getCompetences', [])));
  }

  addCompetence(userId: number, data: any): Observable<any> {
    return this.api.post(`${this.endpoint}/${userId}/competences`, data)
      .pipe(catchError(this.handleError('addCompetence', {})));
  }

  updateCompetences(userId: number, data: any): Observable<any> {
    return this.api.put(`${this.endpoint}/competences`, userId, data)
      .pipe(catchError(this.handleError('updateCompetences', {})));
  }

  getMeCompetences(): Observable<any> {
    return this.api.get(`${this.endpoint}/me/competences`)
      .pipe(catchError(this.handleError('getMeCompetences', [])));
  }

  addMeCompetence(data: any): Observable<any> {
    return this.api.post(`${this.endpoint}/me/competences`, data)
      .pipe(catchError(this.handleError('addMeCompetence', {})));
  }

  updateMeCompetences(data: any): Observable<any> {
    return this.api.putSimple(`${this.endpoint}/me/competences`, data)
      .pipe(catchError(this.handleError('updateMeCompetences', {})));
  }

  deleteMeCompetence(id: number): Observable<any> {
    return this.api.delete(`${this.endpoint}/me/competences`, id)
      .pipe(catchError(this.handleError('deleteMeCompetence', {})));
  }

  // =========================
  // ===== CONGÉS ============
  // =========================

  getEmployeConges(employeId: number): Observable<any> {
    return this.api.get(`conges/employe/${employeId}`)
      .pipe(catchError(this.handleError('getEmployeConges', [])));
  }

  // =========================
  // ===== FILTRES ===========
  // =========================

  findByDepartement(departement: string): Observable<any> {
    return this.api.get(`${this.endpoint}/departement/${departement}`)
      .pipe(catchError(this.handleError('findByDepartement', [])));
  }

  findActifs(): Observable<any> {
    return this.api.get(`${this.endpoint}/actifs`)
      .pipe(catchError(this.handleError('findActifs', [])));
  }

  // =========================
  // ===== ERROR HANDLER =====
  // =========================

  private handleError(operation: string, result: any) {
    return (error: any): Observable<any> => {
      console.error(`❌ ${operation}`, error);
      return of(result);
    };
  }
}