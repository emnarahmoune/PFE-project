import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';
import { KeycloakInitService } from '../../../core/services/keycloak-init.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-manager-layout',
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
    MatDividerModule
  ],
  templateUrl: './manager-layout.component.html',
  styleUrls: ['./manager-layout.component.css']
})
export class ManagerLayoutComponent implements OnInit {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();
  
  menuItems = [
    { path: '/manager/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { path: '/manager/equipe', icon: 'people', label: 'Mon équipe' },
    { path: '/manager/conges', icon: 'event', label: 'Demandes de congé' },
    { path: '/manager/stats', icon: 'analytics', label: 'Statistiques' }
  ];

  constructor(
    private authService: AuthService,
    private keycloakService: KeycloakInitService,
    public router: Router
  ) {}

  ngOnInit() {}

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout() {
    this.keycloakService.logout();
  }

  isRouteActive(path: string): boolean {
  return this.router.url.startsWith(path);
}

  getUserName(): string {
    const user = this.authService.getCurrentUser();
    if (user?.prenom && user?.nom) {
      return `${user.prenom} ${user.nom}`.trim();
    }
    return 'Manager';
  }

  getUserInitials(): string {
    const user = this.authService.getCurrentUser();
    if (user?.prenom && user?.nom) {
      return `${user.prenom.charAt(0)}${user.nom.charAt(0)}`.toUpperCase();
    }
    return 'MG';
  }

getCurrentPageTitle(): string {
  const path = this.router.url;

  if (path.includes('/manager/dashboard')) return 'Tableau de bord';
  if (path.includes('/manager/equipe')) return 'Mon équipe';
  if (path.includes('/manager/conges')) return 'Demandes de congé';
  if (path.includes('/manager/stats')) return 'Statistiques';

  return 'Espace Manager';
}
}