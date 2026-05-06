import { Component, OnInit, OnDestroy, Renderer2, ChangeDetectorRef } from '@angular/core';
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

import { Subscription } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService, AppNotification } from '../../../../core/services/notification.service';
import { KeycloakInitService } from '../../../../core/services/keycloak-init.service';
import { PictureService } from '../../../../core/services/picture.service';
import { EmployeeProfileService } from '../../../../core/services/employe-profile.service';

interface EmployeeMenuItem {
  path: string;
  icon: string;
  label: string;
}

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
  userRole = 'EMPLOYE';
  userEmail = '';
  userPhotoUrl: string | null = null;

  notificationCount = 0;
  recentNotifications: AppNotification[] = [];
  darkMode = false;

  menuItems: EmployeeMenuItem[] = [
    { path: '/employee/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { path: '/employee/mes-conges', icon: 'event', label: 'Mes congés' },
    { path: '/employee/mes-formations', icon: 'school', label: 'Mes formations' },
    { path: '/employee/competences', icon: 'psychology', label: 'Mes compétences' },
    { path: '/employee/mon-profil', icon: 'person', label: 'Mon profil' },
    { path: '/employee/mes-evaluations', icon: 'star', label: 'Mes évaluations' }
  ];

  private unreadCountSub?: Subscription;
  private notificationsSub?: Subscription;
  private pictureSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    public router: Router,
    private notificationService: NotificationService,
    private keycloakInit: KeycloakInitService,
    private renderer: Renderer2,
    private pictureService: PictureService,
    private employeeProfileService: EmployeeProfileService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
    this.loadFullProfile();
    this.loadThemePreference();
    this.injectNotificationMenuStyles();
    this.subscribeToPicture();

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
    this.pictureSubscription?.unsubscribe();
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
      this.userEmail = '';
      this.userRole = 'EMPLOYE';
    }
  }

  private loadFullProfile(): void {
    this.employeeProfileService.getProfile().subscribe({
      next: (response: any) => {
        const data = response?.data || response;

        if (data) {
          this.userNom = data.nom || this.userNom;
          this.userPrenom = data.prenom || this.userPrenom;
          this.userEmail = data.email || this.userEmail;
          this.userRole = data.role || data.typeUtilisateur || this.userRole;

          this.pictureService.setPicture(data.photoUrl || null);
        }
      },
      error: err => {
        console.error('Erreur chargement profil employé complet', err);
      }
    });
  }

  private subscribeToPicture(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(urlWithTimestamp => {
      this.userPhotoUrl = urlWithTimestamp;
      this.cdr.detectChanges();
    });
  }

  getVisibleMenuItems(): EmployeeMenuItem[] {
    if (this.isAdminUser()) {
      return this.menuItems.filter(item => item.path !== '/employee/mes-evaluations');
    }

    return this.menuItems;
  }

  isAdminUser(): boolean {
    const role = String(this.userRole || '').toUpperCase();
    const roles = this.getNormalizedRoles();

    return role === 'ADMIN' ||
      role === 'ADMIN_RH' ||
      role === 'RH' ||
      roles.includes('admin') ||
      roles.includes('admin_rh') ||
      roles.includes('rh');
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

  toggleTheme(): void {
    this.darkMode = !this.darkMode;
    localStorage.setItem('theme', this.darkMode ? 'dark' : 'light');
    this.applyBodyTheme();
  }

  private loadThemePreference(): void {
    const savedTheme = localStorage.getItem('theme');
    this.darkMode = savedTheme === 'dark';
    this.applyBodyTheme();
  }

  private applyBodyTheme(): void {
    if (this.darkMode) {
      this.renderer.addClass(document.body, 'dark-theme');
    } else {
      this.renderer.removeClass(document.body, 'dark-theme');
    }
  }

  private injectNotificationMenuStyles(): void {
    const styleId = 'employee-notification-menu-global-styles';

    let style = document.getElementById(styleId) as HTMLStyleElement | null;

    if (style === null) {
      const createdStyle = this.renderer.createElement('style') as HTMLStyleElement;
      this.renderer.setAttribute(createdStyle, 'id', styleId);
      this.renderer.appendChild(document.head, createdStyle);
      style = createdStyle;
    }

    style.textContent = `
      body.dark-theme .cdk-overlay-pane:has(.notif-header),
      body.dark-theme .cdk-overlay-pane:has(.notif-list),
      body.dark-theme .cdk-overlay-pane:has(.notif-footer) {
        background: transparent !important;
        background-color: transparent !important;
      }

      body.dark-theme .mat-mdc-menu-panel:has(.notif-header),
      body.dark-theme .mat-mdc-menu-panel:has(.notif-list),
      body.dark-theme .mat-mdc-menu-panel.notif-menu-panel,
      body.dark-theme .notif-menu-panel.mat-mdc-menu-panel {
        background: #111827 !important;
        background-color: #111827 !important;
        border-color: #263244 !important;
        color: #f8fafc !important;
        overflow: hidden !important;
        box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55) !important;

        --mat-menu-container-color: #111827 !important;
        --mat-menu-item-label-text-color: #f8fafc !important;
        --mdc-theme-surface: #111827 !important;
        --mat-app-surface: #111827 !important;
        --mat-app-surface-container: #111827 !important;
        --mat-app-surface-container-high: #111827 !important;
        --mat-app-surface-container-highest: #111827 !important;
      }

      body.dark-theme .mat-mdc-menu-panel:has(.notif-header) .mat-mdc-menu-content,
      body.dark-theme .mat-mdc-menu-panel:has(.notif-list) .mat-mdc-menu-content,
      body.dark-theme .notif-menu-panel .mat-mdc-menu-content {
        background: #111827 !important;
        background-color: #111827 !important;
        padding: 0 !important;
        margin: 0 !important;
        color: #f8fafc !important;
      }

      body.dark-theme .notif-header {
        background: #111827 !important;
        background-color: #111827 !important;
        border-bottom: 1px solid #263244 !important;
        padding: 16px 20px !important;
      }

      body.dark-theme .notif-title {
        color: #f8fafc !important;
      }

      body.dark-theme .mark-all-btn {
        color: #93c5fd !important;
      }

      body.dark-theme .notif-list {
        background: #111827 !important;
        background-color: #111827 !important;
        padding: 10px !important;
      }

      body.dark-theme .notif-item {
        background: #0f172a !important;
        background-color: #0f172a !important;
        border-color: #263244 !important;
      }

      body.dark-theme .notif-item:hover {
        background: #1e293b !important;
        background-color: #1e293b !important;
        border-color: #334155 !important;
      }

      body.dark-theme .notif-item.unread {
        background: rgba(67, 97, 238, 0.16) !important;
        background-color: rgba(67, 97, 238, 0.16) !important;
        border-color: rgba(96, 165, 250, 0.35) !important;
        border-left: 4px solid #60a5fa !important;
      }

      body.dark-theme .notif-icon {
        background: rgba(255, 255, 255, 0.06) !important;
        background-color: rgba(255, 255, 255, 0.06) !important;
        color: #f8fafc !important;
      }

      body.dark-theme .notif-message {
        color: #f8fafc !important;
      }

      body.dark-theme .notif-date,
      body.dark-theme .notif-empty {
        color: #94a3b8 !important;
      }

      body.dark-theme .read-btn {
        background: rgba(16, 185, 129, 0.14) !important;
        background-color: rgba(16, 185, 129, 0.14) !important;
        color: #34d399 !important;
      }

      body.dark-theme .notif-footer {
        background: #111827 !important;
        background-color: #111827 !important;
        border-top: 1px solid #263244 !important;
        padding: 14px 16px !important;
      }

      body.dark-theme .notif-footer a {
        color: #93c5fd !important;
        font-weight: 900 !important;
      }
    `;
  }

  logout(): void {
    this.authService.logout();
  }

  getUserName(): string {
    return `${this.userPrenom} ${this.userNom}`.trim() || this.userEmail || 'Utilisateur';
  }

  getUserInitials(): string {
    if (this.userPrenom && this.userNom) {
      return `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase();
    }

    if (this.userEmail) {
      return this.userEmail.charAt(0).toUpperCase();
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
    if (path.includes('/employee/competences')) return 'Mes compétences';
    if (path.includes('/employee/mes-evaluations')) return 'Mes évaluations';
    if (path.includes('/employee/notifications')) return 'Notifications';

    return 'Espace employé';
  }

  private getNormalizedRoles(): string[] {
    return this.keycloakInit.getUserRoles().map(role => role.toLowerCase());
  }

  canGoToAdminSpace(): boolean {
    const roles = this.getNormalizedRoles();

    return roles.includes('admin') ||
      roles.includes('admin_rh') ||
      roles.includes('rh');
  }

  canGoToManagerSpace(): boolean {
    const roles = this.getNormalizedRoles();

    return roles.includes('manager') && !this.canGoToAdminSpace();
  }

  goToRoleSpace(): void {
    if (this.canGoToAdminSpace()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    if (this.canGoToManagerSpace()) {
      this.router.navigate(['/manager/dashboard']);
    }
  }

  isRouteActive(path: string): boolean {
    return this.router.url.startsWith(path);
  }
}