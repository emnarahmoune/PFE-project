import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';

import { Subscription } from 'rxjs';

import {
  NotificationService,
  AppNotification
} from '../../../../core/services/notification.service';

@Component({
  selector: 'app-mes-notifications',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatDividerModule
  ],
  templateUrl: './mes-notifications.component.html',
  styleUrls: ['./mes-notifications.component.scss']
})
export class MesNotificationsComponent implements OnInit, OnDestroy {
  notifications: AppNotification[] = [];
  filteredNotifications: AppNotification[] = [];

  loading = false;
  selectedFilter = 'all';

  private refreshInterval: any;
  private readonly REFRESH_INTERVAL_MS = 30000;
  private notificationsSub?: Subscription;

  // ✅ Important : empêche une notification supprimée localement
  // de revenir si le service réémet une ancienne liste.
  private deletedNotificationIds = new Set<number>();

  filters = [
    { value: 'all', label: 'Toutes', icon: '📋' },
    { value: 'unread', label: 'Non lues', icon: '🔴' },
    { value: 'SUCCESS', label: 'Succès', icon: '✅' },
    { value: 'ERROR', label: 'Erreurs', icon: '❌' },
    { value: 'INFO', label: 'Informations', icon: 'ℹ️' },
    
  ];

  stats = {
    total: 0,
    unread: 0,
    success: 0,
    error: 0,
    info: 0
  };

  constructor(
    private notificationService: NotificationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.startAutoRefresh();

    this.notificationsSub = this.notificationService.notifications$.subscribe({
      next: (notifications: AppNotification[]) => {
        const safeNotifications = notifications || [];

        // ✅ On retire les notifications déjà supprimées localement
        this.notifications = safeNotifications.filter(
          notification => !this.deletedNotificationIds.has(notification.id)
        );

        this.recalculateView();
      },
      error: (error: any) => {
        console.error('Erreur flux notifications:', error);
        this.showToast('Erreur lors du chargement des notifications', 'error');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }

    this.notificationsSub?.unsubscribe();
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.notificationService.loadNotifications();
    }, this.REFRESH_INTERVAL_MS);
  }

  loadNotifications(): void {
    this.loading = true;

    try {
      this.notificationService.loadNotifications();
    } catch (error) {
      console.error('Erreur chargement notifications:', error);
      this.showToast('Erreur lors du chargement des notifications', 'error');
    } finally {
      setTimeout(() => {
        this.loading = false;
      }, 300);
    }
  }

  refresh(): void {
    this.loadNotifications();
  }

  private recalculateView(): void {
    this.updateStats();
    this.applyFilter();
  }

  updateStats(): void {
    this.stats = {
      total: this.notifications.length,
      unread: this.notifications.filter(n => !n.lu).length,
      success: this.notifications.filter(n => n.type === 'SUCCESS').length,
      error: this.notifications.filter(n => n.type === 'ERROR').length,
      info: this.notifications.filter(n => n.type === 'INFO').length
    };
  }

  applyFilter(): void {
    if (this.selectedFilter === 'all') {
      this.filteredNotifications = [...this.notifications];
    } else if (this.selectedFilter === 'unread') {
      this.filteredNotifications = this.notifications.filter(n => !n.lu);
    } else {
      this.filteredNotifications = this.notifications.filter(
        n => n.type === this.selectedFilter
      );
    }

    this.filteredNotifications.sort((a, b) => {
      return new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime();
    });
  }

  changeFilter(filter: string): void {
    this.selectedFilter = filter;
    this.applyFilter();
  }

  markAsRead(notification: AppNotification): void {
    if (!notification || notification.lu) {
      return;
    }

    this.notifications = this.notifications.map(n =>
      n.id === notification.id ? { ...n, lu: true } : n
    );

    this.recalculateView();

    this.notificationService.markAsRead(notification.id);
    this.notificationService.loadUnreadCount();
  }

  markAllAsRead(): void {
    if (this.stats.unread === 0) {
      this.showToast('Aucune notification non lue', 'info');
      return;
    }

    this.notifications = this.notifications.map(n => ({
      ...n,
      lu: true
    }));

    this.recalculateView();

    this.notificationService.markAllAsRead();
    this.notificationService.loadUnreadCount();

    this.showToast('Toutes les notifications sont marquées comme lues', 'success');
  }

  deleteNotification(notification: AppNotification, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (!notification?.id) {
      this.showToast('Notification introuvable', 'error');
      return;
    }

    const confirmed = confirm('Supprimer cette notification ?');

    if (!confirmed) {
      return;
    }

    const deletedId = notification.id;

    // ✅ Empêche la notification de revenir si le service réémet une ancienne liste
    this.deletedNotificationIds.add(deletedId);

    // ✅ Suppression locale immédiate
    this.notifications = this.notifications.filter(n => n.id !== deletedId);
    this.filteredNotifications = this.filteredNotifications.filter(n => n.id !== deletedId);

    // ✅ Stats mises à jour immédiatement
    this.recalculateView();

    // ✅ Suppression backend/service
    this.notificationService.deleteNotification(deletedId);
    this.notificationService.loadUnreadCount();

    this.showToast('Notification supprimée', 'success');
  }

  deleteAllNotifications(): void {
    if (this.stats.total === 0) {
      this.showToast('Aucune notification à supprimer', 'info');
      return;
    }

    const confirmed = confirm(
      'Supprimer toutes les notifications ? Cette action est irréversible.'
    );

    if (!confirmed) {
      return;
    }

    // ✅ Mémorise les IDs supprimés pour éviter leur retour dans l’affichage
    this.notifications.forEach(notification => {
      this.deletedNotificationIds.add(notification.id);
    });

    // ✅ Suppression locale immédiate
    this.notifications = [];
    this.filteredNotifications = [];

    // ✅ Stats mises à jour immédiatement
    this.recalculateView();

    // ✅ Suppression backend/service
    this.notificationService.deleteAllNotifications();
    this.notificationService.loadUnreadCount();

    this.showToast('Toutes les notifications ont été supprimées', 'success');
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'SUCCESS':
        return '✅';
      case 'ERROR':
        return '❌';
      case 'INFO':
        return 'ℹ️';
      default:
        return '📢';
    }
  }

  getNotificationColor(type: string): string {
    switch (type) {
      case 'SUCCESS':
        return '#28a745';
      case 'ERROR':
        return '#dc3545';
      case 'INFO':
        return '#17a2b8';
      default:
        return '#6c757d';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) {
      return '';
    }

    const date = new Date(dateStr);
    const now = new Date();

    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) {
      return 'À l’instant';
    }

    if (diffMins < 60) {
      return `Il y a ${diffMins} minute${diffMins > 1 ? 's' : ''}`;
    }

    if (diffHours < 24) {
      return `Il y a ${diffHours} heure${diffHours > 1 ? 's' : ''}`;
    }

    if (diffDays < 7) {
      return `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
    }

    return date.toLocaleDateString('fr-FR');
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass:
        type === 'success'
          ? 'snackbar-success'
          : type === 'error'
            ? 'snackbar-error'
            : 'snackbar-info',
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}