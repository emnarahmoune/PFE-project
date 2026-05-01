// features/admin/layout/admin-layout.component.ts
import { Component, OnInit, Renderer2, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';
import { PictureService } from '../../../core/services/picture.service';
import { AdminProfileService } from '../../../core/services/AdminProfileService';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();
  userRole = 'admin';
  userNom = '';
  userPrenom = '';
  userEmail = '';
  userPhotoUrl: string | null = null;
  darkMode = false;
  private pictureSubscription: Subscription | null = null;

  menuItems = [
    { path: '/admin/dashboard', icon: '◪', label: 'Dashboard' },
    { path: '/admin/employes', icon: '◌', label: 'Employés' },
    { path: '/admin/competences', icon: '◈', label: 'Compétences' },
    { path: '/admin/formations', icon: '◔', label: 'Formations' },
    { path: '/admin/conges', icon: '◍', label: 'Congés' },
    { path: '/admin/indicateurs', icon: '◕', label: 'Indicateurs' },
    { path: '/admin/scores', icon: '◬', label: 'Scores risque' },
    { path: '/admin/managers', icon: '◧', label: 'Managers & équipes' },
    { path: '/admin/profil', icon: '👤', label: 'Mon profil' }
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
      next: (response) => {
        if (response.success && response.data) {
          if (response.data.nom) this.userNom = response.data.nom;
          if (response.data.prenom) this.userPrenom = response.data.prenom;
          if (response.data.email) this.userEmail = response.data.email;
          this.userRole = response.data.role || this.userRole;
          this.pictureService.setPicture(response.data.photoUrl || null);
        }
      },
      error: (err) => console.error('Erreur chargement profil complet', err)
    });
  }

  private subscribeToPicture(): void {
    this.pictureSubscription = this.pictureService.picture$.subscribe(urlWithTimestamp => {
      this.userPhotoUrl = urlWithTimestamp;
      this.cdr.detectChanges();
    });
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
    if (window.innerWidth <= 1024) {
      const sidebar = document.querySelector('.sidebar');
      if (sidebar) {
        if (this.isSidebarOpen) sidebar.classList.add('open');
        else sidebar.classList.remove('open');
      }
    }
  }

  logout(): void {
    this.keycloakService.logout();
  }

  getUserName(): string {
    return this.userPrenom && this.userNom ? `${this.userPrenom} ${this.userNom}` : 'Administrateur';
  }

  getUserInitials(): string {
    return this.userPrenom && this.userNom
      ? `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase()
      : 'AD';
  }

  getCurrentPageTitle(): string {
    const path = this.router.url;
    const titles: Record<string, string> = {
      '/admin/dashboard': 'Tableau de bord',
      '/admin/employes': 'Gestion des employés',
      '/admin/competences': 'Compétences',
      '/admin/formations': 'Formations',
      '/admin/conges': 'Congés',
      '/admin/scores': 'Scores de risque',
      '/admin/managers': 'Managers & équipes',
      '/admin/indicateurs': 'Indicateurs',
      '/admin/profil': 'Mon profil'
    };
    return titles[path] || 'Administration';
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
      this.renderer.removeClass(document.body, 'dark-theme');
    }
  }

  isRouteActive(path: string): boolean {
    return this.router.isActive(path, false);
  }
}