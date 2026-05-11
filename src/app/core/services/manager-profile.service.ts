// src/app/core/services/manager-profile.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ManagerProfile,
  UpdateProfileData,
  ChangePasswordData
} from '../models/manager-profile.model';

@Injectable({ providedIn: 'root' })
export class ManagerProfileService {

  // ✅ profil manager
  private profileUrl = `${environment.apiUrl}/manager/profile`;

  // ✅ racine manager backend
  private managerUrl = `${environment.apiUrl}/manager`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<{ success: boolean; data: ManagerProfile; message: string }> {
    return this.http.get<{ success: boolean; data: ManagerProfile; message: string }>(
      this.profileUrl
    );
  }

  updateProfile(data: UpdateProfileData): Observable<{ success: boolean; data: ManagerProfile; message: string }> {
    return this.http.put<{ success: boolean; data: ManagerProfile; message: string }>(
      this.profileUrl,
      data
    );
  }
  
changePassword(data: ChangePasswordData) {
  return this.http.post<any>('/api/manager/change-password', data);
}

  uploadPhoto(file: File): Observable<{ success: boolean; data: { photoUrl: string; message?: string }; message: string }> {
    const formData = new FormData();
    formData.append('file', file);

    // ✅ backend : POST /api/manager/photo
    return this.http.post<{ success: boolean; data: { photoUrl: string; message?: string }; message: string }>(
      `${this.managerUrl}/photo`,
      formData
    );
  }

  deletePhoto(): Observable<{ success: boolean; message: string }> {
    // ✅ backend : DELETE /api/manager/photo
    return this.http.delete<{ success: boolean; message: string }>(
      `${this.managerUrl}/photo`
    );
  }
}