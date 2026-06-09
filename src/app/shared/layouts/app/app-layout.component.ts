import {
  Component,
  OnInit,
  OnDestroy,
  Renderer2,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import {
  RouterModule,
  RouterOutlet,
  Router,
  ActivatedRoute,
  NavigationEnd
} from '@angular/router';

import { filter, Subscription } from 'rxjs';

import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';
import { PictureService } from '../../../core/services/picture.service';

import { AdminProfileService } from '../../../core/services/AdminProfile.service';
import { ManagerProfileService } from '../../../core/services/manager-profile.service';
import { EmployeeProfileService } from '../../../core/services/employe-profile.service';

import {
  NotificationService,
  AppNotification
} from '../../../core/services/notification.service';

type LayoutRole = 'ADMIN' | 'MANAGER' | 'EMPLOYE';

interface MenuItem {
  path: string;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatIconModule,
    MatMenuModule,
    MatButtonModule
  ],
  templateUrl: './app-layout.component.html',
  styleUrls: ['./app-layout.component.scss']
})
export class AppLayoutComponent implements OnInit, OnDestroy {
  layoutRole: LayoutRole = 'EMPLOYE';

  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  userNom = '';
  userPrenom = '';
  userEmail = '';
  userRole = '';
  userPhotoUrl: string | null = null;

  darkMode = false;

  menuItems: MenuItem[] = [];

  returnSpace: 'admin' | 'manager' | null = null;

  notificationCount = 0;
  recentNotifications: AppNotification[] = [];

  private pictureSubscription?: Subscription;
  private unreadCountSub?: Subscription;
  private notificationsSub?: Subscription;
  private routerSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private renderer: Renderer2,
    private cdr: ChangeDetectorRef,

    private authService: AuthService,
    private keycloakInit: KeycloakInitService,
    private pictureService: PictureService,

    private adminProfileService: AdminProfileService,
    private managerProfileService: ManagerProfileService,
    private employeeProfileService: EmployeeProfileService,

    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.layoutRole = this.route.snapshot.data['layoutRole'] || 'EMPLOYE';
    this.detectReturnSpace();

    if (window.innerWidth <= 1024) {
    this.isSidebarOpen = false;
  }

    this.initMenu();
    this.loadUserInfo();
    this.loadFullProfile();
    this.loadThemePreference();
    this.subscribeToPicture();

    if (this.isEmployeeLayout()) {
      this.initNotifications();
    }

    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.detectReturnSpace();
        this.cdr.detectChanges();
      });
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
    this.unreadCountSub?.unsubscribe();
    this.notificationsSub?.unsubscribe();
    this.routerSub?.unsubscribe();
  }

  // =========================
  // ROLE / MENU
  // =========================

  isAdminLayout(): boolean {
    return this.layoutRole === 'ADMIN';
  }

  isManagerLayout(): boolean {
    return this.layoutRole === 'MANAGER';
  }

  isEmployeeLayout(): boolean {
    return this.layoutRole === 'EMPLOYE';
  }

  private initMenu(): void {
    if (this.isAdminLayout()) {
      this.menuItems = [
        { path: '/admin/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
        { path: '/admin/employes', icon: 'groups', label: 'Employés' },
        { path: '/admin/competences', icon: 'psychology', label: 'Compétences' },
        { path: '/admin/formations', icon: 'school', label: 'Formations' },
        { path: '/admin/conges', icon: 'event', label: 'Congés' },
        { path: '/admin/scores', icon: 'warning', label: 'Scores risque' },
        { path: '/admin/recrutement', icon: 'work', label: 'Recrutement' },
        { path: '/admin/managers', icon: 'supervisor_account', label: 'Managers & équipes' },
        { path: '/admin/evaluations', icon: 'assessment', label: 'Évaluations' },
        { path: '/admin/profil', icon: 'person', label: 'Mon profil' }
      ];

      return;
    }

    if (this.isManagerLayout()) {
      this.menuItems = [
        { path: '/manager/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
        { path: '/manager/equipe', icon: 'groups', label: 'Mon équipe' },
        { path: '/manager/conges', icon: 'event', label: 'Demandes de congé' },
        { path: '/manager/evaluations', icon: 'assignment', label: 'Évaluations' },
        { path: '/manager/profil', icon: 'person', label: 'Mon profil' }
      ];

      return;
    }

    this.menuItems = [
      { path: '/employee/espace-personnel', icon: 'dashboard', label: 'Tableau de bord' },
      { path: '/employee/mes-conges', icon: 'event', label: 'Mes congés' },
      { path: '/employee/mes-formations', icon: 'school', label: 'Mes formations' },
      { path: '/employee/competences', icon: 'psychology', label: 'Mes compétences' },
      { path: '/employee/offres-internes', icon: 'work', label: 'Offres internes' },
      { path: '/employee/mes-evaluations', icon: 'star', label: 'Mes évaluations' },
      { path: '/employee/mon-profil', icon: 'person', label: 'Mon profil' }
    ];
  }

  getVisibleMenuItems(): MenuItem[] {
    return this.menuItems;
  }

  // =========================
  // USER INFO
  // =========================

  private loadUserInfo(): void {
    const userFromKeycloak = this.keycloakInit.getUser?.();
    const userFromAuth = this.authService.getCurrentUser?.();

    const user = userFromKeycloak || userFromAuth;

    this.userNom = user?.nom || '';
    this.userPrenom = user?.prenom || '';
    this.userEmail = user?.email || '';

    if (this.isAdminLayout()) {
      this.userRole = user?.role || 'ADMIN_RH';
    } else if (this.isManagerLayout()) {
      this.userRole = user?.role || 'MANAGER';
    } else {
      this.userRole = user?.role || user?.typeUtilisateur || 'EMPLOYE';
    }
  }

  private loadFullProfile(): void {
    if (this.isAdminLayout()) {
      this.adminProfileService.getProfile().subscribe({
        next: response => {
          if (response?.success && response.data) {
            this.applyProfileData(response.data);
          }
        },
        error: err => console.error('Erreur chargement profil admin', err)
      });

      return;
    }

    if (this.isManagerLayout()) {
      this.managerProfileService.getProfile().subscribe({
        next: response => {
          if (response?.success && response.data) {
            this.applyProfileData(response.data);
          }
        },
        error: err => console.error('Erreur chargement profil manager', err)
      });

      return;
    }

    this.employeeProfileService.getProfile().subscribe({
      next: response => {
        const data = response?.data || response;

        if (data) {
          this.applyProfileData(data);
        }
      },
      error: err => console.error('Erreur chargement profil employé', err)
    });
  }

  private applyProfileData(data: any): void {
    this.userNom = data.nom || this.userNom;
    this.userPrenom = data.prenom || this.userPrenom;
    this.userEmail = data.email || this.userEmail;
    this.userRole = data.role || data.typeUtilisateur || this.userRole;

    this.pictureService.setPicture(data.photoUrl || null);
  }

  private subscribeToPicture(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(url => {
      this.userPhotoUrl = url;
      this.cdr.detectChanges();
    });
  }

  // =========================
  // NOTIFICATIONS EMPLOYE
  // =========================

  private initNotifications(): void {
    this.notificationService.loadNotifications();
    this.notificationService.loadUnreadCount();

    this.unreadCountSub = this.notificationService.unreadCount$.subscribe(count => {
      this.notificationCount = count;
      this.cdr.detectChanges();
    });

    this.notificationsSub = this.notificationService.notifications$.subscribe(notifications => {
      this.recentNotifications = (notifications || []).slice(0, 5);
      this.cdr.detectChanges();
    });
  }

  markNotificationRead(id: number): void {
    this.notificationService.markAsRead(id);
  }

  markAllNotificationsRead(): void {
    this.notificationService.markAllAsRead();
  }

  goToNotifications(): void {
    this.router.navigate(['/employee/notifications']);
  }

  // =========================
  // UI
  // =========================

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  toggleTheme(): void {
    this.darkMode = !this.darkMode;
    localStorage.setItem('theme', this.darkMode ? 'dark' : 'light');
    this.applyBodyTheme();
  }

  private loadThemePreference(): void {
    this.darkMode = localStorage.getItem('theme') === 'dark';
    this.applyBodyTheme();
  }

  private applyBodyTheme(): void {
    if (this.darkMode) {
      this.renderer.addClass(document.body, 'dark-theme');
    } else {
      this.renderer.removeClass(document.body, 'dark-theme');
    }
  }

  logout(): void {
    this.keycloakInit.logout();
  }

  goToProfile(): void {
    if (this.isAdminLayout()) {
      this.router.navigate(['/admin/profil']);
      return;
    }

    if (this.isManagerLayout()) {
      this.router.navigate(['/manager/profil']);
      return;
    }

    this.router.navigate(['/employee/mon-profil']);
  }

  getUserName(): string {
    const fullName = `${this.userPrenom || ''} ${this.userNom || ''}`.trim();

    if (fullName) {
      return fullName;
    }

    if (this.isAdminLayout()) return 'Administrateur';
    if (this.isManagerLayout()) return 'Manager';

    return 'Employé';
  }

  getUserInitials(): string {
    const p = this.userPrenom?.charAt(0) || '';
    const n = this.userNom?.charAt(0) || '';

    const initials = `${p}${n}`.toUpperCase();

    if (initials) {
      return initials;
    }

    if (this.isAdminLayout()) return 'AD';
    if (this.isManagerLayout()) return 'MG';

    return 'EM';
  }

  isRouteActive(path: string): boolean {
    return this.router.url === path || this.router.url.startsWith(path + '/');
  }

  getCurrentPageTitle(): string {
    const currentUrl = this.router.url.split('?')[0];

    const current = this.menuItems.find(item =>
      currentUrl === item.path || currentUrl.startsWith(item.path + '/')
    );

    return current?.label || 'Portail-RH';
  }


 goToPersonalSpace(): void {
  if (this.isAdminLayout()) {
    localStorage.setItem('returnSpace', 'admin');

    this.router.navigate(['/employee/espace-personnel'], {
      queryParams: { from: 'admin' }
    });

    return;
  }

  if (this.isManagerLayout()) {
    localStorage.setItem('returnSpace', 'manager');

    this.router.navigate(['/employee/espace-personnel'], {
      queryParams: { from: 'manager' }
    });

    return;
  }

  this.router.navigate(['/employee/espace-personnel']);
}

private detectReturnSpace(): void {
  const currentUrl = this.router.url.split('?')[0];
  const queryString = this.router.url.split('?')[1] || '';
  const queryParams = new URLSearchParams(queryString);
  const from = queryParams.get('from');

  if (from === 'admin' || from === 'manager') {
    this.returnSpace = from;
    localStorage.setItem('returnSpace', from);
    return;
  }

  if (currentUrl.startsWith('/employee')) {
    const storedReturnSpace = localStorage.getItem('returnSpace');

    if (storedReturnSpace === 'admin' || storedReturnSpace === 'manager') {
      this.returnSpace = storedReturnSpace;
      return;
    }
  }

  if (currentUrl.startsWith('/admin') || currentUrl.startsWith('/manager')) {
    this.returnSpace = null;
    localStorage.removeItem('returnSpace');
    return;
  }

  this.returnSpace = null;
}
shouldShowReturnSpaceButton(): boolean {
  return this.isEmployeeLayout() &&
    (this.returnSpace === 'admin' || this.returnSpace === 'manager');
}

  shouldShowPersonalSpaceItem(): boolean {
    return this.isAdminLayout() || this.isManagerLayout();
  }

 shouldShowReturnSpaceItem(): boolean {
  return this.isEmployeeLayout() &&
    (this.returnSpace === 'admin' || this.returnSpace === 'manager');
}

  getReturnSpaceLabel(): string {
    if (this.returnSpace === 'admin') {
      return 'Retour espace admin';
    }

    if (this.returnSpace === 'manager') {
      return 'Retour espace manager';
    }

    return '';
  }

goBackToRoleSpace(): void {
  const target = this.returnSpace;

  localStorage.removeItem('returnSpace');
  this.returnSpace = null;

  if (target === 'admin') {
    this.router.navigate(['/admin/dashboard']);
    return;
  }

  if (target === 'manager') {
    this.router.navigate(['/manager/dashboard']);
    return;
  }

  this.router.navigate(['/employee/espace-personnel']);
}



isSimpleEmployee(): boolean {
  const role = String(this.userRole || '').toUpperCase();

  return (
    this.isEmployeeLayout() &&
    !role.includes('ADMIN') &&
    !role.includes('MANAGER')
  );
}

closeSidebarOnMobile(): void {
  if (window.innerWidth <= 1024) {
    this.isSidebarOpen = false;
  }
}







}