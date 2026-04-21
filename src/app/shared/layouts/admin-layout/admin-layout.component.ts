import { Component, OnInit } from '@angular/core';
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
  userRole: string = 'admin';
  userNom = '';
  userPrenom = '';
  userEmail = '';

  menuItems = [
    { path: '/admin/dashboard', icon: '📊', label: 'Dashboard' },
    { path: '/admin/employes', icon: '👥', label: 'Employés' },
    { path: '/admin/competences', icon: '🎓', label: 'Compétences' },
    { path: '/admin/formations', icon: '📚', label: 'Formations' },
    { path: '/admin/conges', icon: '🏖️', label: 'Congés' },
    { path: '/admin/indicateurs', icon: '📈', label: 'Indicateurs' },
    { path: '/admin/scores', icon: '⚠️', label: 'Scores risque' },
    // ✅ NOUVEAU : gestion des managers et équipes
    { path: '/admin/managers', icon: '👨‍💼', label: 'Managers & équipes' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private keycloakService: KeycloakInitService
  ) {}

  ngOnInit() {
    const roles = this.keycloakService.getUserRoles();
    
    // Si c'est un manager, rediriger vers manager/dashboard
    if (roles.includes('manager')) {
      this.router.navigate(['/manager/dashboard']);
      return;
    }
    
    this.loadUserInfo();
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
    if (this.userPrenom && this.userNom) {
      return `${this.userPrenom} ${this.userNom}`;
    }
    return 'Administrateur';
  }

  getUserInitials(): string {
    if (this.userPrenom && this.userNom) {
      return `${this.userPrenom.charAt(0)}${this.userNom.charAt(0)}`.toUpperCase();
    }
    return 'AD';
  }

  getCurrentPageTitle(): string {
    const path = window.location.pathname;
    if (path.includes('/admin/dashboard')) return 'Tableau de bord';
    if (path.includes('/admin/employes')) return 'Gestion des employés';
    if (path.includes('/admin/competences')) return 'Compétences';
    if (path.includes('/admin/formations')) return 'Formations';
    if (path.includes('/admin/conges')) return 'Congés';
    if (path.includes('/admin/indicateurs')) return 'Indicateurs RH';
    if (path.includes('/admin/scores')) return 'Scores de risque';
    if (path.includes('/admin/managers')) return 'Managers & équipes';
    return 'Administration';
  }
}