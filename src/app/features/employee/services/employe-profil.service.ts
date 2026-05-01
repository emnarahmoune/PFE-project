import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { EmployeProfilResponse, UpdateProfilRequest } from '../models/employe-profil.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeProfilService {

  private endpoint = 'employes';

  constructor(private api: ApiService) {}

  getMonProfil(): Observable<EmployeProfilResponse> {
    return this.api.get<EmployeProfilResponse>(`${this.endpoint}/mon-profil`);
  }

  updateMonProfil(data: any): Observable<any> {
  return this.api.putCustom('employes/mon-profil', data);
}

  updateInformationsPersonnelles(data: UpdateProfilRequest): Observable<EmployeProfilResponse> {
    return this.updateMonProfil(data);
  }

  getMonSoldeConges(): Observable<EmployeProfilResponse> {
    return this.api.get<EmployeProfilResponse>(`${this.endpoint}/mon-solde-conges`);
  }

  getHistoriqueConges(): Observable<EmployeProfilResponse> {
    return this.api.get<EmployeProfilResponse>(`${this.endpoint}/mon-historique-conges`);
  }

  getMesCompetences(): Observable<EmployeProfilResponse> {
    return this.api.get<EmployeProfilResponse>(`${this.endpoint}/mes-competences`);
  }

  getMesFormations(): Observable<EmployeProfilResponse> {
    return this.api.get<EmployeProfilResponse>(`${this.endpoint}/mes-formations`);
  }

  changePassword(oldPassword: string, newPassword: string): Observable<EmployeProfilResponse> {
    return this.api.post<EmployeProfilResponse>(`${this.endpoint}/change-password`, {
      oldPassword,
      newPassword
    });
  }

  changeEmail(newEmail: string): Observable<EmployeProfilResponse> {
    return this.api.patch(`${this.endpoint}/change-email?newEmail=${encodeURIComponent(newEmail)}`, {});
  }
}