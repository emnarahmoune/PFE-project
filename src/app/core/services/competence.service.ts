import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
@Injectable({
  providedIn: 'root'
})
export class CompetenceService {

private apiUrl = `${environment.apiUrl}/competences`;
  constructor(private http: HttpClient) {}

  // ===============================
  // 📥 GET ALL
  // ===============================
  getAll(): Observable<any> {
     return this.http.get(`${this.apiUrl}?page=0&size=20`);
  }


// 📥 GET BY ID (simple)
getById(id: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/${id}`);
}

// 📥 GET DETAILS (avec niveau + employés)
getDetails(id: number): Observable<any> {
  return this.http.get(`${this.apiUrl}/${id}/details`);
} // ===============================
  // ➕ CREATE
  // ===============================
  create(competence: any): Observable<any> {
    return this.http.post(this.apiUrl, competence);
  }

  // ===============================
  // ✏️ UPDATE
  // ===============================
  update(id: number, competence: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, competence);
  }

  // ===============================
  // 🗑️ DELETE
  // ===============================
  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // ===============================
  // 📊 STATS (optionnel)
  // ===============================
  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }

  // ===============================
  // 👥 EMPLOYES PAR COMPETENCE
  // ===============================
  getEmployesByCompetence(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/employes`);
  }

  // ===============================
  // 🔗 ASSOCIER EMPLOYE
  // ===============================
  addEmploye(competenceId: number, employeId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${competenceId}/employes/${employeId}`, {});
  }

  // ===============================
  // ❌ RETIRER EMPLOYE
  // ===============================
  removeEmploye(competenceId: number, employeId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${competenceId}/employes/${employeId}`);
  }
}