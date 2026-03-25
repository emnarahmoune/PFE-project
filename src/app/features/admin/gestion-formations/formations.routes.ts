import { Routes } from '@angular/router';
import { FormationListComponent } from './pages/formation-list/formation-list.component';
import { FormationFormComponent } from './pages/formation-form/formation-form.component';
import { FormationDetailComponent } from './pages/formation-detail/formation-detail.component';

export const formationsRoutes: Routes = [
  { path: '', component: FormationListComponent },
  { path: 'nouveau', component: FormationFormComponent },
  { path: ':id', component: FormationDetailComponent },
  { path: ':id/edit', component: FormationFormComponent },
  { path: ':id/participants', component: FormationDetailComponent }
];