// src/app/features/employee/employee.routes.ts

import { Routes } from '@angular/router';
import { EmployeeLayoutComponent } from './layouts/employee-layout/employee-layout.component';
import { DashboardEmployeeComponent } from './pages/dashboard-employee/dashboard-employee.component';
import { MonProfilComponent } from './pages/mon-profil/mon-profil.component';
import { ListeCongesComponent } from './pages/mes-conges/liste-conges/liste-conges.component';
import { DemandeCongeComponent } from './pages/mes-conges/demande-conge/demande-conge.component';
import { DetailCongeComponent } from './pages/mes-conges/detail-conge/detail-conge.component';
import { MesFormationsComponent } from './pages/mes-formations/mes-formations.component';
import { MesNotificationsComponent } from './pages/mes-notifications/mes-notifications.component';

export const employeeRoutes: Routes = [
  {
    path: '',
    component: EmployeeLayoutComponent,
    children: [
      // Dashboard
      { path: 'dashboard', component: DashboardEmployeeComponent },

      // Mon profil
      { path: 'mon-profil', component: MonProfilComponent },

      // Mes congés
      { path: 'mes-conges', component: ListeCongesComponent },
      { path: 'mes-conges/nouveau', component: DemandeCongeComponent },
      { path: 'mes-conges/:id', component: DetailCongeComponent },

      // Mes formations
      { path: 'mes-formations', component: MesFormationsComponent },

      // ✅ NOUVEAU - Mes notifications
      { path: 'notifications', component: MesNotificationsComponent },
       

{
  path: 'mes-conges/:id/modifier',
  loadComponent: () => import('../employee/pages/mes-conges/ModifierCongeComponent/ModifierCongeComponent')
    .then(m => m.ModifierCongeComponent)
},
 
 
      // Redirection par défaut
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];