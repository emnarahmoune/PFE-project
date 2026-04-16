import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AppNotification {
  id: number;
  message: string;
  type: string;      // 'SUCCESS', 'ERROR', 'INFO', 'WARNING'
  dateCreation: string;
  lu: boolean;
  relatedDemandeId?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private apiUrl = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<ApiResponse<AppNotification[]>> {
    return this.http.get<ApiResponse<AppNotification[]>>(this.apiUrl);
  }

  getUnreadNotifications(): Observable<ApiResponse<AppNotification[]>> {
    return this.http.get<ApiResponse<AppNotification[]>>(`${this.apiUrl}/unread`);
  }

  markAsRead(id: number): Observable<ApiResponse<null>> {
    return this.http.put<ApiResponse<null>>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<ApiResponse<null>> {
    return this.http.put<ApiResponse<null>>(`${this.apiUrl}/read-all`, {});
  }

  createNotification(message: string, type: string, relatedDemandeId?: number): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(this.apiUrl, { message, type, relatedDemandeId });
  }

  deleteNotification(id: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.apiUrl}/${id}`);
  }

  deleteAllNotifications(): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.apiUrl}/all`);
  }
}