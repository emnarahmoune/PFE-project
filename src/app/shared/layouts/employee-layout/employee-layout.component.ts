import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet, Router } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';

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
    MatDividerModule
  ],
  templateUrl: './employee-layout.component.html',
  styleUrls: ['./employee-layout.component.css']
})
export class EmployeeLayoutComponent {
  isSidebarOpen = true;
  currentYear = new Date().getFullYear();

  menuItems = [
    { path: '/employee/dashboard', icon: 'dashboard', label: 'Dashboard' },
    { path: '/employee/mon-profil', icon: 'person', label: 'Mon profil' },
    { path: '/employee/mes-conges', icon: 'event', label: 'Mes congés' },
    { path: '/employee/mes-formations', icon: 'school', label: 'Mes formations' }
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
    return user ? `${user.prenom} ${user.nom}` : 'Employé';
  }

  getUserInitials(): string {
    const user = this.authService.getCurrentUser();
    if (user) {
      return `${user.prenom?.charAt(0) || ''}${user.nom?.charAt(0) || ''}`;
    }
    return 'E';
  }
}