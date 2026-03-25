import { Component } from '@angular/core';
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
export class AdminLayoutComponent {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  menuItems = [
    { path: '/admin/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { path: '/admin/employes', icon: 'people', label: 'Employés' },
    { path: '/admin/competences', icon: 'school', label: 'Compétences' },
    { path: '/admin/formations', icon: 'menu_book', label: 'Formations' },
    { path: '/admin/conges', icon: 'event', label: 'Congés' },
    { path: '/admin/indicateurs', icon: 'analytics', label: 'Indicateurs' },
    { path: '/admin/scores', icon: 'warning', label: 'Scores risque' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }

  getUserName(): string {
    const user = this.authService.getCurrentUser();
    return user ? `${user.prenom || ''} ${user.nom || ''}`.trim() || 'Admin' : 'Admin';
  }

  getUserInitials(): string {
    const user = this.authService.getCurrentUser();
    if (user?.prenom && user?.nom) {
      return `${user.prenom.charAt(0)}${user.nom.charAt(0)}`.toUpperCase();
    }
    return 'A';
  }

  getCurrentPageTitle(): string {
    const path = window.location.pathname;
    if (path.includes('dashboard')) return 'Dashboard';
    if (path.includes('employes')) return 'Gestion des employés';
    if (path.includes('competences')) return 'Gestion des compétences';
    if (path.includes('formations')) return 'Gestion des formations';
    if (path.includes('conges')) return 'Gestion des congés';
    if (path.includes('indicateurs')) return 'Indicateurs RH';
    if (path.includes('scores')) return 'Scores de risque';
    return 'RH Platform';
  }
}