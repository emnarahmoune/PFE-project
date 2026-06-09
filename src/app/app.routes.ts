// src/app/app.routes.ts

import { Routes } from '@angular/router';

import { AuthLayoutComponent } from './shared/layouts/auth-layout/auth-layout.component';
import { AppLayoutComponent } from './shared/layouts/app/app-layout.component';

import { LoginComponent } from './features/login/login.component';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { ScoresComponent } from './features/scores/scores.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/auth/login',
    pathMatch: 'full'
  },

  // ==================== AUTH ====================
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      {
        path: 'login',
        component: LoginComponent
      },
      {
        path: '',
        redirectTo: 'login',
        pathMatch: 'full'
      }
    ]
  },

  // ==================== ADMIN ====================
  {
    path: 'admin',
    component: AppLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: {
      roles: ['ADMIN_RH', 'admin_rh', 'admin', 'rh'],
      layoutRole: 'ADMIN'
    },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },

      // ==================== EMPLOYÉS ====================
      {
        path: 'employes',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'ADMIN_LISTE' }
      },
      {
        path: 'employes/nouveau',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'ADMIN_NOUVEAU' }
      },
      {
        path: 'employes/:id/edit',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'ADMIN_MODIFIER' }
      },
      {
        path: 'employes/:id',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'ADMIN_DETAIL' }
      },

      // ==================== COMPÉTENCES ====================
      {
        path: 'competences',
        loadComponent: () =>
          import('./features/competences/competences.component')
            .then(m => m.CompetencesComponent),
        data: { competenceMode: 'ADMIN_LISTE' }
      },
      {
        path: 'competences/nouveau',
        loadComponent: () =>
          import('./features/competences/competences.component')
            .then(m => m.CompetencesComponent),
        data: { competenceMode: 'ADMIN_NOUVEAU' }
      },
      {
        path: 'competences/:id/edit',
        loadComponent: () =>
          import('./features/competences/competences.component')
            .then(m => m.CompetencesComponent),
        data: { competenceMode: 'ADMIN_MODIFIER' }
      },
      {
        path: 'competences/:id',
        loadComponent: () =>
          import('./features/competences/competences.component')
            .then(m => m.CompetencesComponent),
        data: { competenceMode: 'ADMIN_DETAIL' }
      },

      // ==================== FORMATIONS ====================
      {
        path: 'formations',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'ADMIN_LISTE' }
      },
      {
        path: 'formations/new',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'ADMIN_NOUVEAU' }
      },
      {
        path: 'formations/:id/edit',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'ADMIN_MODIFIER' }
      },
      {
        path: 'formations/:id/participants',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'ADMIN_PARTICIPANTS' }
      },
      {
        path: 'formations/:id',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'ADMIN_DETAIL' }
      },

      // ==================== CONGÉS ADMIN RH ====================
      {
        path: 'conges',
        redirectTo: 'conges/validation-rh',
        pathMatch: 'full'
      },
      {
        path: 'conges/validation-rh',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'ADMIN_RH_VALIDATION' }
      },

      // ==================== SCORES ====================
  
{
  path: 'scores',
  component: ScoresComponent
},
{
  path: 'scores/:id',
  component: ScoresComponent
},


      // ==================== RECRUTEMENT INTERNE ====================
      {
        path: 'recrutement',
        loadComponent: () =>
          import('./features/recrutement/recrutement.component')
            .then(m => m.RecrutementComponent),
        data: { recrutementMode: 'ADMIN_RECRUTEMENT' }
      },

      // ==================== ASSIGNATION MANAGER ====================
      {
        path: 'manager-assignment',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'ADMIN_LISTE' }
      },

      // ==================== MANAGERS / ÉQUIPES ====================
      {
        path: 'managers',
        loadComponent: () =>
          import('./features/managers/managers.component')
            .then(m => m.ManagersComponent),
        data: { managersMode: 'ADMIN_MANAGER_LISTE' }
      },
      {
        path: 'managers/:id',
        loadComponent: () =>
          import('./features/managers/managers.component')
            .then(m => m.ManagersComponent),
        data: { managersMode: 'ADMIN_MANAGER_DETAIL' }
      },

      // ==================== PROFIL ADMIN ====================
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profil/profil.component')
            .then(m => m.ProfilComponent),
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN_RH', 'admin_rh', 'rh', 'admin'],
          profilMode: 'ADMIN_PROFIL'
        }
      },

      // ==================== ÉVALUATIONS ADMIN ====================
      {
        path: 'evaluations',
        loadComponent: () =>
          import('./features/evaluations/evaluations.component')
            .then(m => m.EvaluationsComponent),
        data: { evaluationMode: 'ADMIN_LISTE' }
      }
    ]
  },

  // ==================== MANAGER ====================
  {
    path: 'manager',
    component: AppLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: {
      roles: ['manager', 'MANAGER'],
      layoutRole: 'MANAGER'
    },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },

      // ==================== ÉQUIPE MANAGER ====================
      {
        path: 'equipe',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'MANAGER_EQUIPE' }
      },

      // ==================== CONGÉS MANAGER ====================
      {
        path: 'conges',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'MANAGER_VALIDATION' }
      },
      {
        path: 'employe/:id/conges',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'MANAGER_HISTORIQUE_EMPLOYE' }
      },

      // ==================== DÉTAIL EMPLOYÉ MANAGER ====================
      {
        path: 'employe/:id',
        loadComponent: () =>
          import('./features/employes/employes.component')
            .then(m => m.EmployesComponent),
        data: { employeMode: 'MANAGER_DETAIL' }
      },

      // ==================== PROFIL MANAGER ====================
      {
        path: 'profil',

        loadComponent: () =>
          import('./features/profil/profil.component')
            .then(m => m.ProfilComponent),
        canActivate: [roleGuard],
        data: {
          roles: ['manager', 'MANAGER'],
          profilMode: 'MANAGER_PROFIL'
        }
      },

      // ==================== ÉVALUATIONS MANAGER ====================
      {
        path: 'evaluations',
        loadComponent: () =>
          import('./features/evaluations/evaluations.component')
            .then(m => m.EvaluationsComponent),
        data: { evaluationMode: 'MANAGER_LISTE' }
      }
    ]
  },

  // ==================== EMPLOYEE ====================
  {
    path: 'employee',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    data: {
      layoutRole: 'EMPLOYE'
    },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
        data: {
          forceRole: 'EMPLOYE'
        }
      },

      {
        path: 'espace-personnel',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
        data: {
          forceRole: 'EMPLOYE'
        }
      },

      // ==================== PROFIL EMPLOYÉ ====================
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./features/profil/profil.component')
            .then(m => m.ProfilComponent),
        data: {
          profilMode: 'EMPLOYE_PROFIL'
        }
      },

      // ==================== CONGÉS EMPLOYÉ ====================
      {
        path: 'mes-conges',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'EMPLOYE_LISTE' }
      },
      {
        path: 'mes-conges/nouveau',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'EMPLOYE_NOUVEAU' }
      },
      {
        path: 'mes-conges/:id/modifier',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'EMPLOYE_MODIFIER' }
      },
      {
        path: 'mes-conges/:id',
        loadComponent: () =>
          import('./features/conges/conges.component')
            .then(m => m.CongesComponent),
        data: { congeMode: 'EMPLOYE_DETAIL' }
      },

      // ==================== FORMATIONS EMPLOYÉ ====================
      {
        path: 'mes-formations',
        loadComponent: () =>
          import('./features/formations/formations.component')
            .then(m => m.FormationsComponent),
        data: { formationMode: 'EMPLOYE_MES_FORMATIONS' }
      },

      // ==================== NOTIFICATIONS EMPLOYÉ ====================
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.component')
            .then(m => m.NotificationsComponent),
        data: {
          notificationMode: 'EMPLOYE_NOTIFICATIONS'
        }
      },

      // ==================== COMPÉTENCES EMPLOYÉ ====================
      {
        path: 'competences',
        loadComponent: () =>
          import('./features/competences/competences.component')
            .then(m => m.CompetencesComponent),
        data: { competenceMode: 'EMPLOYE_MES_COMPETENCES' }
      },

      // ==================== RECRUTEMENT INTERNE EMPLOYÉ ====================
      {
        path: 'offres-internes',
        loadComponent: () =>
          import('./features/recrutement/recrutement.component')
            .then(m => m.RecrutementComponent),
        data: { recrutementMode: 'EMPLOYE_RECRUTEMENT' }
      },

      // ==================== ÉVALUATIONS EMPLOYÉ ====================
      {
        path: 'mes-evaluations',
        loadComponent: () =>
          import('./features/evaluations/evaluations.component')
            .then(m => m.EvaluationsComponent),
        data: { evaluationMode: 'EMPLOYE_MES_EVALUATIONS' }
      }
    ]
  },

  // ==================== FALLBACK ====================
  {
    path: '**',
    redirectTo: '/auth/login'
  }
];