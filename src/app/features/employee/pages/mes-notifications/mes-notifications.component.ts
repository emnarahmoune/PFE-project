// src/app/features/employee/pages/mes-notifications/mes-notifications.component.ts

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
import { NotificationApiService, AppNotification } from '../../../../core/services/notification-api.service';

@Component({
  selector: 'app-mes-notifications',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatProgressSpinnerModule,
    MatBadgeModule, MatDividerModule
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

  filters = [
    { value: 'all', label: 'Toutes', icon: '📋' },
    { value: 'SUCCESS', label: 'Succès', icon: '✅' },
    { value: 'ERROR', label: 'Erreurs', icon: '❌' },
    { value: 'INFO', label: 'Informations', icon: 'ℹ️' },
    { value: 'unread', label: 'Non lues', icon: '🔴' }
  ];

  stats = {
    total: 0,
    unread: 0,
    success: 0,
    error: 0,
    info: 0
  };

  constructor(
    private notificationService: NotificationApiService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => {
      this.loadNotifications(false);
    }, this.REFRESH_INTERVAL_MS);
  }

  loadNotifications(showLoading = true): void {
    if (showLoading) this.loading = true;

    this.notificationService.getNotifications().subscribe({
      next: (response: any) => {
        this.loading = false;
        // Gérer les deux formats possibles : ApiResponse ou tableau direct
        let data = response;
        if (response && response.success) {
          data = response.data;
        }
        this.notifications = Array.isArray(data) ? data : [];
        this.updateStats();
        this.applyFilter();
      },
      error: (error: any) => {
        this.loading = false;
        console.error('Erreur chargement notifications:', error);
        this.showToast('Erreur de connexion au serveur', 'error');
      }
    });
  }

  updateStats(): void {
    this.stats.total = this.notifications.length;
    this.stats.unread = this.notifications.filter(n => !n.lu).length;
    this.stats.success = this.notifications.filter(n => n.type === 'SUCCESS').length;
    this.stats.error = this.notifications.filter(n => n.type === 'ERROR').length;
    this.stats.info = this.notifications.filter(n => n.type === 'INFO').length;
  }

  applyFilter(): void {
    if (this.selectedFilter === 'all') {
      this.filteredNotifications = [...this.notifications];
    } else if (this.selectedFilter === 'unread') {
      this.filteredNotifications = this.notifications.filter(n => !n.lu);
    } else {
      this.filteredNotifications = this.notifications.filter(n => n.type === this.selectedFilter);
    }
    // Trier par date décroissante
    this.filteredNotifications.sort((a, b) => 
      new Date(b.dateCreation).getTime() - new Date(a.dateCreation).getTime()
    );
  }

  changeFilter(filter: string): void {
    this.selectedFilter = filter;
    this.applyFilter();
  }

  markAsRead(notification: AppNotification): void {
    if (notification.lu) return;

    this.notificationService.markAsRead(notification.id).subscribe({
      next: (response: any) => {
        if (response.success || response.statusCode === 200) {
          notification.lu = true;
          this.updateStats();
          this.applyFilter();
          this.showToast('Notification marquée comme lue', 'success');
        }
      },
      error: (error: any) => {
        console.error('Erreur marquage lecture:', error);
        this.showToast('Erreur lors du marquage', 'error');
      }
    });
  }

  markAllAsRead(): void {
    if (this.stats.unread === 0) {
      this.showToast('Aucune notification non lue', 'info');
      return;
    }

    this.notificationService.markAllAsRead().subscribe({
      next: (response: any) => {
        if (response.success || response.statusCode === 200) {
          this.notifications.forEach(n => n.lu = true);
          this.updateStats();
          this.applyFilter();
          this.showToast('Toutes les notifications ont été marquées comme lues', 'success');
        }
      },
      error: (error: any) => {
        console.error('Erreur marquage tout lu:', error);
        this.showToast('Erreur lors du marquage', 'error');
      }
    });
  }

  deleteNotification(notification: AppNotification, event: Event): void {
    event.stopPropagation();
    
    if (confirm('Supprimer cette notification ?')) {
      this.notificationService.deleteNotification(notification.id).subscribe({
        next: (response: any) => {
          if (response.success || response.statusCode === 200) {
            this.notifications = this.notifications.filter(n => n.id !== notification.id);
            this.updateStats();
            this.applyFilter();
            this.showToast('Notification supprimée', 'success');
          }
        },
        error: (error: any) => {
          console.error('Erreur suppression:', error);
          this.showToast('Erreur lors de la suppression', 'error');
        }
      });
    }
  }

  deleteAllNotifications(): void {
    if (this.stats.total === 0) {
      this.showToast('Aucune notification à supprimer', 'info');
      return;
    }

    if (confirm('Supprimer toutes les notifications ? Cette action est irréversible.')) {
      this.notificationService.deleteAllNotifications().subscribe({
        next: (response: any) => {
          if (response.success || response.statusCode === 200) {
            this.notifications = [];
            this.updateStats();
            this.applyFilter();
            this.showToast('Toutes les notifications ont été supprimées', 'success');
          }
        },
        error: (error: any) => {
          console.error('Erreur suppression tout:', error);
          this.showToast('Erreur lors de la suppression', 'error');
        }
      });
    }
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'SUCCESS': return '✅';
      case 'ERROR': return '❌';
      case 'INFO': return 'ℹ️';
      default: return '📢';
    }
  }

  getNotificationColor(type: string): string {
    switch (type) {
      case 'SUCCESS': return '#28a745';
      case 'ERROR': return '#dc3545';
      case 'INFO': return '#17a2b8';
      default: return '#6c757d';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} minute${diffMins > 1 ? 's' : ''}`;
    if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours > 1 ? 's' : ''}`;
    if (diffDays < 7) return `Il y a ${diffDays} jour${diffDays > 1 ? 's' : ''}`;
    return date.toLocaleDateString('fr-FR');
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass: type === 'success' ? 'snackbar-success' : 
                  type === 'error' ? 'snackbar-error' : 'snackbar-info',
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  refresh(): void {
    this.loadNotifications(true);
  }
}