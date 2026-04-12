// src/app/core/services/notification-api.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface AppNotification {
  id: number;                // ID numérique depuis la base
  message: string;
  type: string;              // 'SUCCESS', 'ERROR', 'INFO', 'WARNING'
  dateCreation: string;      // date de création
  lu: boolean;               // lu (true/false)
  relatedDemandeId?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private apiUrl = `${environment.apiUrl}/notifications`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /** Récupère toutes les notifications de l'employé connecté */
  getNotifications(): Observable<any> {
    return this.http.get(`${this.apiUrl}`, { headers: this.getHeaders() });
  }

  /** Récupère toutes les notifications de l'employé connecté (alias) */
  getMyNotifications(): Observable<any> {
    return this.getNotifications();
  }

  /** Récupère uniquement les notifications non lues */
  getUnreadNotifications(): Observable<any> {
    return this.http.get(`${this.apiUrl}/unread`, { headers: this.getHeaders() });
  }

  /** Marque une notification comme lue */
  markAsRead(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/read`, {}, { headers: this.getHeaders() });
  }

  /** Marque toutes les notifications comme lues */
  markAllAsRead(): Observable<any> {
    return this.http.put(`${this.apiUrl}/read-all`, {}, { headers: this.getHeaders() });
  }

  /** Crée une notification (pour les événements métier) */
  createNotification(message: string, type: string, relatedDemandeId?: number): Observable<any> {
    return this.http.post(`${this.apiUrl}`, 
      { message, type, relatedDemandeId }, 
      { headers: this.getHeaders() }
    );
  }

  /** Supprime une notification */
  deleteNotification(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  /** Supprime toutes les notifications de l'utilisateur */
  deleteAllNotifications(): Observable<any> {
    // Note: Cette méthode peut ne pas être implémentée côté backend
    // Si ce n'est pas le cas, il faudra la créer ou utiliser une alternative
    return this.http.delete(`${this.apiUrl}/all`, { headers: this.getHeaders() });
  }
}