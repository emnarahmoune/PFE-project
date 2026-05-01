// core/services/manager-profile.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ManagerProfile, UpdateProfileData, ChangePasswordData } from '../../features/manager/models/manager-profile.model';

@Injectable({ providedIn: 'root' })
export class ManagerProfileService {
  changePassword(data: ChangePasswordData): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${this.apiUrl}/change-password`, data);
  }
  private apiUrl = `${environment.apiUrl}/manager/profile`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<{ success: boolean; data: ManagerProfile; message: string }> {
    return this.http.get<{ success: boolean; data: ManagerProfile; message: string }>(this.apiUrl);
  }
  updateProfile(data: UpdateProfileData): Observable<{ success: boolean; data: ManagerProfile; message: string }> {
    return this.http.put<{ success: boolean; data: ManagerProfile; message: string }>(this.apiUrl, data);
  }
    uploadPhoto(file: File): Observable<{ success: boolean; data: { photoUrl: string }; message: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ success: boolean; data: { photoUrl: string }; message: string }>(
      `${this.apiUrl}/photo`, formData
    );
  }

  deletePhoto(): Observable<{ success: boolean; message: string }> {
    return this.http.delete<{ success: boolean; message: string }>(`${this.apiUrl}/photo`);
  }
}