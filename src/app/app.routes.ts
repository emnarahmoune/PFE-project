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
  path: 'manager',
  component: AdminLayoutComponent, // 🔥 même layout
  canActivate: [authGuard, roleGuard(['manager'])],
  children: [
    {
      path: 'dashboard',
      loadComponent: () =>
        import('./features/manager/dashboard-manager/dashboard-manager.component')
          .then(m => m.DashboardManagerComponent)
    },
    {
      path: 'equipe',
      loadComponent: () =>
        import('./features/manager/equipe/equipe.component')
          .then(m => m.EquipeComponent)
    },
    {
      path: 'conges',
      loadComponent: () =>
        import('./features/manager/conges/conges.component')
          .then(m => m.CongesComponent)
    }
  ]
},
{
  path: 'redirect',
  loadComponent: () => import('./core/redirect/redirect.component')
    .then(m => m.RedirectComponent)
},
{
  path: 'admin',
  component: AdminLayoutComponent,
  canActivate: [authGuard],
  canActivateChild: [roleGuard(['admin'])],
  children: [

    {
      path: 'dashboard',
      loadComponent: () =>
        import('./features/admin/dashboard-admin/dashboard-admin.component')
          .then(m => m.DashboardAdminComponent)
    },

    {
      path: 'employes',
      loadChildren: () =>
        import('./features/admin/gestion-employes/employes.routes')
          .then(m => m.employesRoutes)
    },

    {
      path: 'competences',
      loadChildren: () =>
        import('./features/admin/gestion-competences/competences.routes')
          .then(m => m.competencesRoutes)
    },

    {
      path: 'formations',
      loadChildren: () =>
        import('./features/admin/gestion-formations/formations.routes')
          .then(m => m.formationsRoutes)
    },

    // {
    //   path: 'conges',
    //   loadChildren: () =>
    //     import('./features/admin/gestion-conges/conges.routes')
    //       .then(m => m.congesRoutes)
    // },

    // {
    //   path: 'indicateurs',
    //   loadComponent: () =>
    //     import('./features/admin/indicateurs/indicateurs.component')
    //       .then(m => m.IndicateursComponent)
    // },

    // {
    //   path: 'scores',
    //   loadComponent: () =>
    //     import('./features/admin/scores/scores.component')
    //       .then(m => m.ScoresComponent)
    // }

  ]
},
  // WILDCARD → LOGIN PUBLIC
  { path: '**', redirectTo: '/auth/login' }
];