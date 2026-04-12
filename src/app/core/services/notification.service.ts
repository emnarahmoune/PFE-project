// core/services/notification.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface AppNotification {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info' | 'warning';
  date: Date;
  read: boolean;
  relatedDemandeId?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();
  private storageKey = 'app_notifications';

  constructor(private snackBar: MatSnackBar) {
    this.loadFromLocalStorage();
  }

  private loadFromLocalStorage() {
    const stored = localStorage.getItem(this.storageKey);
    if (stored) {
      const parsed = JSON.parse(stored);
      const notifs = parsed.map((n: any) => ({ ...n, date: new Date(n.date) }));
      this.notificationsSubject.next(notifs);
    }
  }

  private saveToLocalStorage(notifications: AppNotification[]) {
    localStorage.setItem(this.storageKey, JSON.stringify(notifications));
  }

  addNotification(message: string, type: AppNotification['type'], demandeId?: number) {
    const newNotif: AppNotification = {
      id: Date.now().toString() + Math.random().toString(36).substr(2, 6),
      message,
      type,
      date: new Date(),
      read: false,
      relatedDemandeId: demandeId
    };
    const current = this.notificationsSubject.value;
    const updated = [newNotif, ...current];
    this.notificationsSubject.next(updated);
    this.saveToLocalStorage(updated);

    // Affiche également une snackbar
    if (type === 'success') this.showSuccess(message);
    else if (type === 'error') this.showError(message);
    else if (type === 'info') this.showInfo(message);
    return newNotif;
  }

  markAsRead(id: string) {
    const current = this.notificationsSubject.value;
    const updated = current.map(n => n.id === id ? { ...n, read: true } : n);
    this.notificationsSubject.next(updated);
    this.saveToLocalStorage(updated);
  }

  markAllAsRead() {
    const current = this.notificationsSubject.value;
    const updated = current.map(n => ({ ...n, read: true }));
    this.notificationsSubject.next(updated);
    this.saveToLocalStorage(updated);
  }

  removeNotification(id: string) {
    const current = this.notificationsSubject.value;
    const updated = current.filter(n => n.id !== id);
    this.notificationsSubject.next(updated);
    this.saveToLocalStorage(updated);
  }

  getUnreadCount(): Observable<number> {
    return new Observable(observer => {
      this.notifications$.subscribe(notifs => {
        observer.next(notifs.filter(n => !n.read).length);
      });
    });
  }

  // Implémentations MatSnackBar
  showSuccess(message: string) {
    this.snackBar.open(message, 'Fermer', { duration: 5000, panelClass: 'snackbar-success' });
  }

  showError(message: string) {
    this.snackBar.open(message, 'Fermer', { duration: 5000, panelClass: 'snackbar-error' });
  }

  showInfo(message: string) {
    this.snackBar.open(message, 'Fermer', { duration: 3000, panelClass: 'snackbar-info' });
  }
   showWarning(message: string): void {  // ✅ Ajouter cette méthode si nécessaire
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: ['warning-snackbar'],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}