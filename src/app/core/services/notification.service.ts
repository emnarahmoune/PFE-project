import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationApiService, AppNotification, ApiResponse } from './notification-api.service';
import { tap, catchError } from 'rxjs/operators';

// ✅ Réexportation pour que les composants puissent l’utiliser
export { AppNotification } from './notification-api.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(
    private api: NotificationApiService,
    private snackBar: MatSnackBar
  ) {}

  loadNotifications(): void {
    this.api.getNotifications().pipe(
      tap(response => {
        if (response.success) {
          this.notificationsSubject.next(response.data);
          this.updateUnreadCount(response.data);
        }
      }),
      catchError(err => {
        console.error('Erreur chargement notifications', err);
        this.showError('Impossible de charger les notifications');
        return of(null);
      })
    ).subscribe();
  }

  loadUnreadCount(): void {
    this.api.getUnreadNotifications().pipe(
      tap(response => {
        if (response.success) {
          this.unreadCountSubject.next(response.data.length);
        }
      }),
      catchError(err => {
        console.error('Erreur compteur non lues', err);
        return of(null);
      })
    ).subscribe();
  }

  markAsRead(id: number): void {
    this.api.markAsRead(id).subscribe({
      next: (response) => {
        if (response.success) {
          const current = this.notificationsSubject.value;
          const updated = current.map(n => n.id === id ? { ...n, lu: true } : n);
          this.notificationsSubject.next(updated);
          this.updateUnreadCount(updated);
        }
      },
      error: (err) => console.error('Erreur markAsRead', err)
    });
  }

  markAllAsRead(): void {
    this.api.markAllAsRead().subscribe({
      next: (response) => {
        if (response.success) {
          const current = this.notificationsSubject.value;
          const updated = current.map(n => ({ ...n, lu: true }));
          this.notificationsSubject.next(updated);
          this.unreadCountSubject.next(0);
        }
      },
      error: (err) => console.error('Erreur markAllAsRead', err)
    });
  }

  createNotification(message: string, type: string, demandeId?: number): void {
    this.api.createNotification(message, type, demandeId).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadNotifications();
          this.loadUnreadCount();
          this.showInfo('Notification envoyée');
        }
      },
      error: (err) => console.error('Erreur création notification', err)
    });
  }

  deleteNotification(id: number): void {
    this.api.deleteNotification(id).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadNotifications();
          this.loadUnreadCount();
          this.showSuccess('Notification supprimée');
        }
      },
      error: (err) => console.error('Erreur suppression notification', err)
    });
  }

  deleteAllNotifications(): void {
    this.api.deleteAllNotifications().subscribe({
      next: (response) => {
        if (response.success) {
          this.loadNotifications();
          this.loadUnreadCount();
          this.showSuccess('Toutes les notifications ont été supprimées');
        }
      },
      error: (err) => console.error('Erreur suppression toutes notifications', err)
    });
  }

  // Méthodes d'affichage Snackbar
  showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 5000, panelClass: 'snackbar-success' });
  }

  showError(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 5000, panelClass: 'snackbar-error' });
  }

  showInfo(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 3000, panelClass: 'snackbar-info' });
  }

  showWarning(message: string): void {
    this.snackBar.open(message, 'Fermer', { duration: 4000, panelClass: 'snackbar-warning' });
  }

  private updateUnreadCount(notifications: AppNotification[]): void {
    const count = notifications.filter(n => !n.lu).length;
    this.unreadCountSubject.next(count);
  }
}