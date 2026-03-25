import { Routes } from '@angular/router';
import { AuthLayoutComponent } from './shared/layouts/auth-layout/auth-layout.component';
import { AdminLayoutComponent } from './shared/layouts/admin-layout/admin-layout.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // REDIRECTION PAR DÉFAUT → LOGIN (PUBLIC)
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  
  // ✅ ROUTES PUBLIQUES - AUCUN GUARD
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },
  
  // ✅ ROUTES PROTÉGÉES - SEULEMENT ICI les guards
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],  // ✅ RoleGuard après AuthGuard seulement si nécessaire
    children: [
      // Dashboard
      { 
        path: 'dashboard', 
        loadComponent: () => import('./features/admin/dashboard-admin/dashboard-admin.component')
          .then(m => m.DashboardAdminComponent) 
      },
      
      // Gestion des employés
      { 
        path: 'employes', 
        loadChildren: () => import('./features/admin/gestion-employes/employes.routes')
          .then(m => m.employesRoutes) 
      },
      
      // Gestion des compétences
      { 
        path: 'competences', 
        loadChildren: () => import('./features/admin/gestion-competences/competences.routes')
          .then(m => m.competencesRoutes) 
      },
      
      // ✅ Gestion des formations - AJOUTÉ
      { 
        path: 'formations', 
        loadChildren: () => import('./features/admin/gestion-formations/formations.routes')
          .then(m => m.formationsRoutes) 
      },
      
      // Gestion des congés (à ajouter plus tard)
      // { 
      //   path: 'conges', 
      //   loadChildren: () => import('./features/admin/gestion-conges/conges.routes')
      //     .then(m => m.congesRoutes) 
      // },
      
      // Redirection par défaut
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  
  // WILDCARD → LOGIN PUBLIC
  { path: '**', redirectTo: '/auth/login' }
];