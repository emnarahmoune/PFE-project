import { Routes } from '@angular/router';
import { EmployeeLayoutComponent } from '../../shared/layouts/employee-layout/employee-layout.component';
import { DashboardEmployeeComponent } from './pages/dashboard-employee/dashboard-employee.component';
import { MonProfilComponent } from './pages/mon-profil/mon-profil.component';
import { ListeCongesComponent } from './pages/mes-conges/liste-conges/liste-conges.component';
import { DemandeCongeComponent } from './pages/mes-conges/demande-conge/demande-conge.component';
import { DetailCongeComponent } from './pages/mes-conges/detail-conge/detail-conge.component';
import { MesFormationsComponent } from './pages/mes-formations/mes-formations.component';

export const employeeRoutes: Routes = [
  {
    path: '',
    component: EmployeeLayoutComponent,
    children: [
      { path: 'dashboard', component: DashboardEmployeeComponent },
      { path: 'mon-profil', component: MonProfilComponent },
      { path: 'mes-conges', component: ListeCongesComponent },
      { path: 'mes-conges/nouveau', component: DemandeCongeComponent },
      { path: 'mes-conges/:id', component: DetailCongeComponent },
      { path: 'mes-formations', component: MesFormationsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];