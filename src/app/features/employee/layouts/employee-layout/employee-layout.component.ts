import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationApiService, AppNotification } from '../../../../core/services/notification-api.service';

@Component({
  selector: 'app-employee-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatBadgeModule,
    MatTooltipModule
  ],
  templateUrl: './employee-layout.component.html',
  styleUrls: ['./employee-layout.component.scss']
})
export class EmployeeLayoutComponent implements OnInit {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  userNom = '';
  userPrenom = '';
  userRole = '';
  userEmail = '';
  notificationCount = 0;
  recentNotifications: AppNotification[] = [];

  menuItems = [
    { path: '/employee/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { path: '/employee/mon-profil', icon: 'person', label: 'Mon profil' },
    { path: '/employee/mes-conges', icon: 'event', label: 'Mes congés' },
    { path: '/employee/mes-formations', icon: 'school', label: 'Mes formations' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private notifApi: NotificationApiService
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadNotifications();
  }

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userEmail = user.email || '';
      this.userRole = user.role || user.typeUtilisateur || 'EMPLOYE';
    }
  }

  loadNotifications(): void {
    this.notifApi.getMyNotifications().subscribe({
      next: (res) => {
        if (res.success) {
          const allNotifs = res.data as AppNotification[];
          this.recentNotifications = allNotifs.slice(0, 5);
          this.notificationCount = allNotifs.filter(n => !n.lu).length;
        }
      },
      error: (err) => console.error('Erreur chargement notifications', err)
    });
  }

  markNotificationRead(id: number): void {
    this.notifApi.markAsRead(id).subscribe(() => {
      this.loadNotifications(); // recharger après mise à jour
    });
  }

  markAllNotificationsRead(): void {
    this.notifApi.markAllAsRead().subscribe(() => {
      this.loadNotifications();
    });
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout() {
    this.authService.logout();
  }

  getUserName(): string {
    if (this.userPrenom && this.userNom) {
      return `${this.userPrenom} ${this.userNom}`;
    }
    return 'Utilisateur';
  }

  getUserInitials(): string {
    if (this.userPrenom && this.userNom) {
      return `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase();
    }
    return 'U';
  }

  goToProfile(): void {
    this.router.navigate(['/employee/mon-profil']);
  }

  goToNotifications(): void {
    this.router.navigate(['/employee/notifications']);
  }

  getCurrentPageTitle(): string {
    const path = this.router.url;
    if (path.includes('/employee/dashboard')) return 'Tableau de bord';
    if (path.includes('/employee/mon-profil')) return 'Mon profil';
    if (path.includes('/employee/mes-conges')) return 'Mes congés';
    if (path.includes('/employee/mes-formations')) return 'Mes formations';
    return 'Espace employé';
  }
}