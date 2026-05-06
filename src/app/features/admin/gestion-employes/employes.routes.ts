import { Routes } from '@angular/router';
import { EmployeListComponent } from './pages/employe-list/employe-list.component';
import { EmployeFormComponent } from './pages/employe-form/employe-form.component';
import { EmployeDetailComponent } from './pages/employe-detail/employe-list/employe-detail.component';

export const employesRoutes: Routes = [
  { path: '', component: EmployeListComponent },
  { path: 'nouveau', component: EmployeFormComponent },
  { path: ':id', component: EmployeDetailComponent },
  { path: ':id/edit', component: EmployeFormComponent },
];