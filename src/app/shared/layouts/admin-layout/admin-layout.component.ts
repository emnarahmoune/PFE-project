// features/admin/layout/admin-layout.component.ts

import { Component, OnInit, Renderer2, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';
import { PictureService } from '../../../core/services/picture.service';
import { AdminProfileService } from '../../../core/services/AdminProfileService';

import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatIconModule,
    MatMenuModule,
    MatButtonModule
  ],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  userRole = 'ADMIN_RH';
  userNom = '';
  userPrenom = '';
  userEmail = '';
  userPhotoUrl: string | null = null;
  darkMode = false;

  private pictureSubscription: Subscription | null = null;

  menuItems = [
    { path: '/admin/dashboard', icon: 'dashboard', label: 'Tableau de bord' },
    { path: '/admin/employes', icon: 'groups', label: 'Employés' },
    { path: '/admin/competences', icon: 'psychology', label: 'Compétences' },
    { path: '/admin/formations', icon: 'school', label: 'Formations' },
    { path: '/admin/conges', icon: 'event', label: 'Congés' },
    { path: '/admin/indicateurs', icon: 'analytics', label: 'Indicateurs RH' },
    { path: '/admin/scores', icon: 'warning', label: 'Scores risque' },
    { path: '/admin/managers', icon: 'supervisor_account', label: 'Managers & équipes' },
    {path:'/admin/evaluations',icon: 'assessment',label: 'Évaluations'},
    { path: '/admin/profil', icon: 'person', label: 'Mon profil' ,
}
  ];

  constructor(
    private authService: AuthService,
    public router: Router,
    private keycloakService: KeycloakInitService,
    private renderer: Renderer2,
    private pictureService: PictureService,
    private adminProfileService: AdminProfileService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const roles = this.keycloakService.getUserRoles();

    if (roles.includes('manager')) {
      this.router.navigate(['/manager/dashboard']);
      return;
    }

    this.loadUserInfo();
    this.loadFullProfile();
    this.loadThemePreference();
    this.subscribeToPicture();
  }

  ngOnDestroy(): void {
    this.pictureSubscription?.unsubscribe();
  }

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();

    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userEmail = user.email || '';
      this.userRole = user.role || 'ADMIN_RH';
    }
  }

  private loadFullProfile(): void {
    this.adminProfileService.getProfile().subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.userNom = response.data.nom || this.userNom;
          this.userPrenom = response.data.prenom || this.userPrenom;
          this.userEmail = response.data.email || this.userEmail;
          this.userRole = response.data.role || this.userRole;

          this.pictureService.setPicture(response.data.photoUrl || null);
        }
      },
      error: (err: any) => {
        console.error('Erreur chargement profil complet', err);
      }
    });
  }

  private subscribeToPicture(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(rawUrl => {
       this.userPhotoUrl = rawUrl;
      this.cdr.detectChanges();
    });
  }

  private normalizePhotoUrl(url?: string | null): string | null {
    if (!url) {
      return null;
    }

    let cleanUrl = String(url).trim();

    if (!cleanUrl) {
      return null;
    }

    cleanUrl = cleanUrl.split('?')[0];

    if (cleanUrl.startsWith('data:image')) {
      return cleanUrl;
    }

    if (cleanUrl.startsWith('http://') || cleanUrl.startsWith('https://')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('/api/')) {
      return `${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('api/')) {
      return `/${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('/uploads/')) {
      return `/api${cleanUrl}?t=${Date.now()}`;
    }

    if (cleanUrl.startsWith('uploads/')) {
      return `/api/${cleanUrl}?t=${Date.now()}`;
    }

    return `/api/uploads/profile-photos/${cleanUrl}?t=${Date.now()}`;
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  toggleTheme(): void {
    this.darkMode = !this.darkMode;

    if (this.darkMode) {
      this.renderer.addClass(document.body, 'dark-theme');
      localStorage.setItem('theme', 'dark');
    } else {
      this.renderer.removeClass(document.body, 'dark-theme');
      localStorage.setItem('theme', 'light');
    }
  }

  private loadThemePreference(): void {
    const savedTheme = localStorage.getItem('theme');

    if (savedTheme === 'dark') {
      this.darkMode = true;
      this.renderer.addClass(document.body, 'dark-theme');
    } else {
      this.darkMode = false;
      this.renderer.removeClass(document.body, 'dark-theme');
    }
  }

  logout(): void {
    this.keycloakService.logout();
  }

  goToProfile(): void {
    this.router.navigate(['/admin/profil']);
  }

  goToPersonalSpace(): void {
    this.router.navigate(['/employee/dashboard']);
  }

  getUserName(): string {
    return this.userPrenom && this.userNom
      ? `${this.userPrenom} ${this.userNom}`
      : 'Administrateur';
  }

  getUserInitials(): string {
    return this.userPrenom && this.userNom
      ? `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase()
      : 'AD';
  }

  getCurrentPageTitle(): string {
    const path = this.router.url;

    if (path.includes('/admin/dashboard')) return 'Tableau de bord';
    if (path.includes('/admin/employes')) return 'Gestion des employés';
    if (path.includes('/admin/competences')) return 'Compétences';
    if (path.includes('/admin/formations')) return 'Formations';
    if (path.includes('/admin/conges')) return 'Congés';
    if (path.includes('/admin/scores')) return 'Scores de risque';
    if (path.includes('/admin/managers')) return 'Managers & équipes';
    if (path.includes('/admin/indicateurs')) return 'Indicateurs';
    if (path.includes('/admin/profil')) return 'Mon profil';

    return 'Administration';
  }

  isRouteActive(path: string): boolean {
    return this.router.url.startsWith(path);
  }
}