import { Component, OnInit, OnDestroy } from '@angular/core';
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
import { NotificationService, AppNotification } from '../../../../core/services/notification.service';
import { Subscription } from 'rxjs';
import { KeycloakInitService } from '../../../../core/services/keycloak-init.service';

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
export class EmployeeLayoutComponent implements OnInit, OnDestroy {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  userNom = '';
  userPrenom = '';
  userRole = '';
  userEmail = '';
  notificationCount = 0;
  recentNotifications: AppNotification[] = [];

  // ✅ Ajout de la propriété menuItems utilisée dans le template
  menuItems = [
    { path: '/employee/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { path: '/employee/mon-profil', icon: 'person', label: 'Mon profil' },
    { path: '/employee/mes-conges', icon: 'event', label: 'Mes congés' },
    { path: '/employee/mes-formations', icon: 'school', label: 'Mes formations' },
    { path: '/employee/competences', icon: 'psychology', label: 'Mes compétences' },
  
  ];

  private unreadCountSub?: Subscription;
  private notificationsSub?: Subscription;

  constructor(
    private authService: AuthService,
    public router: Router,
    private notificationService: NotificationService,
    private keycloakInit: KeycloakInitService,
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.notificationService.loadNotifications();
    this.notificationService.loadUnreadCount();

    this.unreadCountSub = this.notificationService.unreadCount$.subscribe(count => {
      this.notificationCount = count;
    });

    this.notificationsSub = this.notificationService.notifications$.subscribe(notifications => {
      this.recentNotifications = notifications.slice(0, 5);
    });
  }

  ngOnDestroy(): void {
    this.unreadCountSub?.unsubscribe();
    this.notificationsSub?.unsubscribe();
  }

loadUserInfo(): void {
  const user = this.keycloakInit.getUser();

  if (user) {
    this.userNom = user.nom || '';
    this.userPrenom = user.prenom || '';
    this.userEmail = user.email || '';
    this.userRole = user.role || user.typeUtilisateur || 'EMPLOYE';
  } else {
    this.userNom = '';
    this.userPrenom = '';
  }
}

  markNotificationRead(id: number): void {
    this.notificationService.markAsRead(id);
  }

  markAllNotificationsRead(): void {
    this.notificationService.markAllAsRead();
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout(): void {
    this.authService.logout();
  }

  getUserName(): string {
    return `${this.userPrenom} ${this.userNom}`.trim() || this.userEmail || 'Utilisateur';  }

  getUserInitials(): string {
    return this.userPrenom && this.userNom ? `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase() : 'U';
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
    if (path.includes('/employee/competences')) return 'Mes compétences';
    return 'Espace employé';
  }
}