import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';

@Component({
  selector: 'app-admin-layout',
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
    MatBadgeModule,
    MatDividerModule
  ],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent implements OnInit {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();
  userRole: string = 'admin';
  menuItems: any[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private keycloakService: KeycloakInitService
  ) {}

  ngOnInit() {
    this.loadUserRole();
  }

  loadUserRole() {
    const roles = this.keycloakService.getUserRoles();
    
    if (roles.includes('admin')) {
      this.userRole = 'admin';
      this.menuItems = [
        { path: '/admin/dashboard', icon: 'dashboard', label: 'Dashboard' },
        { path: '/admin/employes', icon: 'people', label: 'Employés' },
        { path: '/admin/competences', icon: 'school', label: 'Compétences' },
        { path: '/admin/formations', icon: 'menu_book', label: 'Formations' },
        { path: '/admin/conges', icon: 'event', label: 'Congés' },
        { path: '/admin/indicateurs', icon: 'analytics', label: 'Indicateurs' },
        { path: '/admin/scores', icon: 'warning', label: 'Scores risque' }
      ];
    } else if (roles.includes('manager')) {
      this.userRole = 'manager';
      this.menuItems = [
        { path: '/manager/dashboard', icon: 'dashboard', label: 'Dashboard' },
        { path: '/manager/equipe', icon: 'people', label: 'Mon équipe' },
        { path: '/manager/conges', icon: 'event', label: 'Congés' },
        { path: '/manager/stats', icon: 'analytics', label: 'Statistiques' }
      ];
    } else {
      this.userRole = 'user';
      this.menuItems = [
        { path: '/employee/dashboard', icon: 'dashboard', label: 'Dashboard' },
        { path: '/employee/profil', icon: 'person', label: 'Mon profil' },
        { path: '/employee/conges', icon: 'event', label: 'Mes congés' },
        { path: '/employee/formations', icon: 'menu_book', label: 'Mes formations' }
      ];
    }
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout() {
    this.keycloakService.logout();
  }

  getUserName(): string {
    const user = this.authService.getCurrentUser();
    if (user?.prenom && user?.nom) {
      return `${user.prenom} ${user.nom}`.trim();
    }
    if (this.userRole === 'admin') return 'Administrateur';
    if (this.userRole === 'manager') return 'Manager';
    return 'Employé';
  }

  getUserInitials(): string {
    const user = this.authService.getCurrentUser();
    if (user?.prenom && user?.nom) {
      return `${user.prenom.charAt(0)}${user.nom.charAt(0)}`.toUpperCase();
    }
    if (this.userRole === 'admin') return 'AD';
    if (this.userRole === 'manager') return 'MG';
    return 'EM';
  }

  getCurrentPageTitle(): string {
    const path = window.location.pathname;
    
    // Admin pages
    if (path.includes('/admin/dashboard')) return 'Dashboard';
    if (path.includes('/admin/employes')) return 'Gestion des employés';
    if (path.includes('/admin/competences')) return 'Gestion des compétences';
    if (path.includes('/admin/formations')) return 'Gestion des formations';
    if (path.includes('/admin/conges')) return 'Gestion des congés';
    if (path.includes('/admin/indicateurs')) return 'Indicateurs RH';
    if (path.includes('/admin/scores')) return 'Scores de risque';
    
    // Manager pages
    if (path.includes('/manager/dashboard')) return 'Dashboard Manager';
    if (path.includes('/manager/equipe')) return 'Mon équipe';
    if (path.includes('/manager/conges')) return 'Gestion des congés';
    if (path.includes('/manager/stats')) return 'Statistiques';
    
    // Employee pages
    if (path.includes('/employee/dashboard')) return 'Mon tableau de bord';
    if (path.includes('/employee/profil')) return 'Mon profil';
    if (path.includes('/employee/conges')) return 'Mes congés';
    if (path.includes('/employee/formations')) return 'Mes formations';
    
    return 'RH Platform';
  }
}