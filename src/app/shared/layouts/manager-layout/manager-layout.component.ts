import { Component, OnInit, Renderer2, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';
import { PictureService } from '../../../core/services/picture.service';
import { ManagerProfileService } from '../../../core/services/manager-profile.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-manager-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  templateUrl: './manager-layout.component.html',
  styleUrls: ['./manager-layout.component.scss']
})
export class ManagerLayoutComponent implements OnInit, OnDestroy {

  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  userRole = 'manager';
  userNom = '';
  userPrenom = '';
  userEmail = '';

  userPhotoUrl: string | null = null;
  darkMode = false;

  private pictureSubscription: Subscription | null = null;

  menuItems = [
    { path: '/manager/dashboard', icon: '◪', label: 'Dashboard' },
    { path: '/manager/equipe', icon: '◌', label: 'Mon équipe' },
    { path: '/manager/conges', icon: '◍', label: 'Demandes de congé' },
    { path: '/manager/indicateurs', icon: '◕', label: 'Indicateurs' },
    { path: '/manager/profil', icon: '👤', label: 'Mon profil' }
  ];

  constructor(
    private authService: AuthService,
    private keycloakService: KeycloakInitService,
    public router: Router,
    private renderer: Renderer2,
    private pictureService: PictureService,
    private managerProfileService: ManagerProfileService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const roles = this.keycloakService.getUserRoles();

    if (roles.includes('admin')) {
      this.router.navigate(['/admin/dashboard']);
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
      this.userRole = user.role || 'manager';
    }
  }

  private loadFullProfile(): void {
    this.managerProfileService.getProfile().subscribe({
      next: (response) => {
        if (response.success && response.data) {

          this.userNom = response.data.nom || this.userNom;
          this.userPrenom = response.data.prenom || this.userPrenom;
          this.userEmail = response.data.email || this.userEmail;
          this.userRole = response.data.role || this.userRole;

          this.pictureService.setPicture(response.data.photoUrl || null);
        }
      },
      error: (err) => console.error(err)
    });
  }

  subscribeToPicture(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(url => {
      this.userPhotoUrl = PictureService.buildDisplayUrl(url);
      this.cdr.detectChanges();
    });
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout(): void {
    this.keycloakService.logout();
  }

  getUserName(): string {
    return this.userPrenom && this.userNom
      ? `${this.userPrenom} ${this.userNom}`
      : 'Manager';
  }

  getUserInitials(): string {
    return this.userPrenom && this.userNom
      ? `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase()
      : 'MG';
  }

  /** ✅ CORRIGÉ + ajout MON PROFIL */
  getCurrentPageTitle(): string {
    const path = this.router.url;

    if (path.includes('/manager/dashboard')) return 'Tableau de bord';
    if (path.includes('/manager/equipe')) return 'Mon équipe';
    if (path.includes('/manager/conges')) return 'Demandes de congé';
    if (path.includes('/manager/indicateurs')) return 'Indicateurs';
    if (path.includes('/manager/profil')) return 'Mon profil';

    return 'Espace Manager';
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
    }
  }

  isRouteActive(path: string): boolean {
    return this.router.url.startsWith(path);
  }
}