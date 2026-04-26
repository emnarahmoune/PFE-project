// admin-layout.component.ts
import { Component, OnInit, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();
  userRole = 'admin';
  userNom = '';
  userPrenom = '';
  userEmail = '';
  darkMode = false;

  menuItems = [
    { path: '/admin/dashboard', icon: '◪', label: 'Dashboard' },
    { path: '/admin/employes', icon: '◌', label: 'Employés' },
    { path: '/admin/competences', icon: '◈', label: 'Compétences' },
    { path: '/admin/formations', icon: '◔', label: 'Formations' },
    { path: '/admin/conges', icon: '◍', label: 'Congés' },
    { path: '/admin/indicateurs', icon: '◕', label: 'Indicateurs' },
    { path: '/admin/scores', icon: '◬', label: 'Scores risque' },
    { path: '/admin/managers', icon: '◧', label: 'Managers & équipes' }
  ];

  // 🔓 Rendre public pour l'utiliser dans le template
  constructor(
    private authService: AuthService,
    public router: Router,              // ← public pour template
    private keycloakService: KeycloakInitService,
    private renderer: Renderer2
  ) {}

  ngOnInit() {
    const roles = this.keycloakService.getUserRoles();
    if (roles.includes('manager')) {
      this.router.navigate(['/manager/dashboard']);
      return;
    }
    this.loadUserInfo();
    this.loadThemePreference();
  }

  loadUserInfo(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userNom = user.nom || '';
      this.userPrenom = user.prenom || '';
      this.userEmail = user.email || '';
      this.userRole = user.role || user.typeUtilisateur || 'ADMIN_RH';
    }
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout() {
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
      '/admin/indicateurs': 'Indicateurs RH',
      '/admin/scores': 'Scores de risque',
      '/admin/managers': 'Managers & équipes'
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

  // Méthode utilitaire pour vérifier si une route est active
  isRouteActive(path: string): boolean {
    return this.router.isActive(path, false);
  }
}