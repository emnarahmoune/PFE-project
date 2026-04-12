// src/app/app.routes.ts

import { Routes } from '@angular/router';
import { AuthLayoutComponent } from './shared/layouts/auth-layout/auth-layout.component';
import { AdminLayoutComponent } from './shared/layouts/admin-layout/admin-layout.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { ManagerLayoutComponent } from './shared/layouts/manager-layout/manager-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },

  // ✅ ROUTES PUBLIQUES
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ✅ ROUTES ADMIN
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard-admin/dashboard-admin.component')
          .then(m => m.DashboardAdminComponent)
      },
      {
        path: 'employes',
        loadChildren: () => import('./features/admin/gestion-employes/employes.routes')
          .then(m => m.employesRoutes)
      },
      {
        path: 'competences',
        loadChildren: () => import('./features/admin/gestion-competences/competences.routes')
          .then(m => m.competencesRoutes)
      },
      {
        path: 'formations',
        loadChildren: () => import('./features/admin/gestion-formations/formations.routes')
          .then(m => m.formationsRoutes)
      },
      // ✅ NOUVEAU - Validation RH des congés (>10 jours)
      {
        path: 'conges/validation-rh',
        loadComponent: () => import('./features/admin/gestion-conges/pages/validation-rh/validation-rh.component')
          .then(m => m.ValidationRhComponent)
      },
      // ✅ NOUVEAU - Assignation des managers
      {
        path: 'manager-assignment',
        loadComponent: () => import('./features/admin/gestion-employes/pages/employe-list/employe-list.component')
          .then(m => m.EmployeListComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // ✅ ROUTES MANAGER
  {
    path: 'manager',
    component: ManagerLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['manager'] },
    children: [
      { 
        path: 'dashboard', 
        loadComponent: () => import('./features/manager/dashboard-manager/dashboard-manager.component')
          .then(m => m.DashboardManagerComponent) 
      },
      { 
        path: 'equipe', 
        loadComponent: () => import('./features/manager/equipe/equipe.component')
          .then(m => m.EquipeComponent) 
      },
      // ✅ MODIFIÉ - Utiliser le nouveau composant d'approbation
      { 
        path: 'conges', 
        loadComponent: () => import('./features/manager/conges/approbation-conge/approbation-conge.component')
          .then(m => m.ApprobationCongeComponent) 
      },
      { 
        path: 'stats', 
        loadComponent: () => import('./features/manager/stats/stats.component')
          .then(m => m.StatsComponent) 
      },
      { 
        path: 'alertes', 
        loadComponent: () => import('./features/manager/alertes/alertes.component')
          .then(m => m.AlertesComponent) 
      },
      { 
        path: 'indicateurs', 
        loadComponent: () => import('./features/manager/indicateurs/indicateurs.component')
          .then(m => m.IndicateursComponent) 
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // ✅ ROUTES EMPLOYEE
  {
    path: 'employee',
    canActivate: [authGuard],
    loadChildren: () => import('./features/employee/employee.routes')
      .then(m => m.employeeRoutes)
  },

  // Redirection par défaut
  { path: '**', redirectTo: '/auth/login' }
];