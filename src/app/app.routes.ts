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

  // ==================== ROUTES PUBLIQUES ====================
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ==================== ROUTES ADMIN ====================
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

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
      // Gestion des congés (validation RH)
      {
        path: 'conges',
        redirectTo: 'conges/validation-rh',
        pathMatch: 'full'
      },
      {
        path: 'conges/validation-rh',
        loadComponent: () => import('./features/admin/gestion-conges/pages/validation-rh/validation-rh.component')
          .then(m => m.ValidationRhComponent)
      },
         {
  path: 'scores',
  loadComponent: () => import('./features/admin/scores/admin-scores.component')
    .then(m => m.AdminScoresComponent)
},
      // Assignation des managers
      {
        path: 'manager-assignment',
        loadComponent: () => import('./features/admin/gestion-employes/pages/employe-list/employe-list.component')
          .then(m => m.EmployeListComponent)
      },
      // Liste des managers avec équipes
      {
        path: 'managers',
        loadComponent: () => import('./features/admin/gestion-employes/pages/manager-list/manager-list.component')
          .then(m => m.ManagerListComponent)
      },
      // Détail d'un manager
      {
        path: 'managers/:id',
        loadComponent: () => import('./features/admin/gestion-employes/pages/manager-detail/manager-detail.component')
          .then(m => m.ManagerDetailComponent)
      }
    ]
  },

  // ==================== ROUTES MANAGER ====================
  {
    path: 'manager',
    component: ManagerLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['manager'] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'dashboard',
        loadComponent: () => import('./features/manager/page/dashboard-manager/dashboard-manager.component')
          .then(m => m.DashboardManagerComponent)
      },
      {
        path: 'equipe',
        loadComponent: () => import('./features/manager/page/equipe/equipe.component')
          .then(m => m.EquipeComponent)
      },
      {
        path: 'conges',
        loadComponent: () => import('./features/manager/page/conges/approbation-conge/approbation-conge.component')
          .then(m => m.ApprobationCongeComponent)
      },
      {
        path: 'stats',
        loadComponent: () => import('./features/manager/page/stats/stats.component')
          .then(m => m.StatsComponent)
      },
      {
        path: 'alertes',
        loadComponent: () => import('./features/manager/page/alertes/alertes.component')
          .then(m => m.AlertesComponent)
      },
      {
        path: 'employe/:id',
        loadComponent: () => import('./features/manager/page/employe-detail/employe-detail.component')
          .then(m => m.EmployeDetailComponent)
      },
      {
        path: 'employe/:id/conges',
        loadComponent: () => import('./features/manager/page/employe-conges/employe-conges.component')
          .then(m => m.EmployeCongesComponent)
      },
      {
        path: 'indicateurs',
        loadComponent: () => import('./features/manager/page/indicateurs/indicateurs.component')
          .then(m => m.IndicateursComponent)
      }
    ]
  },

  // ==================== ROUTES EMPLOYEE ====================
  {
    path: 'employee',
    canActivate: [authGuard],
    loadChildren: () => import('./features/employee/employee.routes')
      .then(m => m.employeeRoutes)
  },

  // Redirection par défaut (page non trouvée)
  { path: '**', redirectTo: '/auth/login' }
];