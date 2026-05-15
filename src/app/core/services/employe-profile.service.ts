import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

import {
  EmployeProfil,
  EmployeProfilResponse,
  UpdateProfilRequest
} from '../models/employe-profil.model';

export interface ChangePasswordData {
  oldPassword: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface UploadPhotoResponse {
  success: boolean;
  data?: {
    photoUrl: string;
  };
  message?: string;
}

export interface DeletePhotoResponse {
  success: boolean;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeProfileService {
  private apiUrl = `${environment.apiUrl}/employes`;

  constructor(private http: HttpClient) {}

  getMonProfil(): Observable<EmployeProfilResponse> {
    return this.http.get<EmployeProfilResponse>(`${this.apiUrl}/mon-profil`);
  }

  getProfile(): Observable<EmployeProfilResponse> {
    return this.getMonProfil();
  }

  updateMonProfil(data: UpdateProfilRequest): Observable<EmployeProfilResponse> {
    return this.http.put<EmployeProfilResponse>(`${this.apiUrl}/mon-profil`, data);
  }

  updateProfile(data: UpdateProfilRequest): Observable<EmployeProfilResponse> {
    return this.updateMonProfil(data);
  }

  changePassword(data: ChangePasswordData): Observable<EmployeProfilResponse> {
    return this.http.post<EmployeProfilResponse>(`${this.apiUrl}/change-password`, data);
  }

uploadPhoto(file: File): Observable<{ success: boolean; data?: { photoUrl: string }; message?: string }> {
  const formData = new FormData();
  formData.append('file', file);

  return this.http.post<{ success: boolean; data?: { photoUrl: string }; message?: string }>(
    `${this.apiUrl}/mon-profil/photo`,
    formData
  );
}

deletePhoto(): Observable<{ success: boolean; message?: string }> {
  return this.http.delete<{ success: boolean; message?: string }>(
    `${this.apiUrl}/mon-profil/photo`
  );
}
  getMonSoldeConges(): Observable<EmployeProfilResponse> {
    return this.http.get<EmployeProfilResponse>(`${this.apiUrl}/mon-solde-conges`);
  }

  getHistoriqueConges(): Observable<EmployeProfilResponse> {
    return this.http.get<EmployeProfilResponse>(`${this.apiUrl}/mon-historique-conges`);
  }

  getMesCompetences(): Observable<EmployeProfilResponse> {
    return this.http.get<EmployeProfilResponse>(`${this.apiUrl}/mes-competences`);
  }

  getMesFormations(): Observable<EmployeProfilResponse> {
    return this.http.get<EmployeProfilResponse>(`${this.apiUrl}/mes-formations`);
  }

  changeEmail(newEmail: string): Observable<EmployeProfilResponse> {
    return this.http.patch<EmployeProfilResponse>(
      `${this.apiUrl}/change-email?newEmail=${encodeURIComponent(newEmail)}`,
      {}
    );
  }
}