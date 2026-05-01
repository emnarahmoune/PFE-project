import { Routes } from '@angular/router';
import { CompetenceListComponent } from './pages/competence-list/competence-list.component';
import { CompetenceFormComponent } from './pages/competence-form/competence-form.component';
import { CompetenceDetailComponent } from './pages/competence-detail/competence-detail.component';

export const competencesRoutes: Routes = [
  { path: '', component: CompetenceListComponent },
  { path: 'nouveau', component: CompetenceFormComponent },
  { path: ':id', component: CompetenceDetailComponent }, // 👁️ DETAILS
  { path: ':id/edit', component: CompetenceFormComponent }
];