// core/services/admin-profile.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminProfile {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  poste: string;
  dateEmbauche: string; // Date format ISO
  departement: string;
  actif: boolean;
  role: string;
  statutCompte: string;
  anciennete: number;
  photoUrl?: string; 
}

export interface UpdateProfileData {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  poste: string;
  departement: string;
}

export interface ChangePasswordData {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class AdminProfileService {
  private apiUrl = `${environment.apiUrl}/admin/profile`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<{ success: boolean; data: AdminProfile; message: string }> {
    return this.http.get<{ success: boolean; data: AdminProfile; message: string }>(this.apiUrl);
  }

  updateProfile(data: UpdateProfileData): Observable<{ success: boolean; data: AdminProfile; message: string }> {
    return this.http.put<{ success: boolean; data: AdminProfile; message: string }>(this.apiUrl, data);
  }

  changePassword(data: ChangePasswordData): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.apiUrl}/change-password`, data);
  }
  
  // ========== PHOTO ==========
  uploadPhoto(file: File): Observable<{ success: boolean; data: { photoUrl: string }; message: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiUrl}/photo`, formData);
  }

  deletePhoto(): Observable<{ success: boolean; message: string }> {
    return this.http.delete<any>(`${this.apiUrl}/photo`);
  }
}