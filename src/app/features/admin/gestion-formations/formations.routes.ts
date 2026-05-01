import { Routes } from '@angular/router';
import { FormationListComponent } from './pages/formation-list/formation-list.component';
import { FormationFormComponent } from './pages/formation-form/formation-form.component';
import { FormationDetailComponent } from './pages/formation-detail/formation-detail.component';
import { FormationParticipantsComponent } from './pages/formation-participants/formation-participants.component';
export const formationsRoutes: Routes = [

  // 🔥 CREATE (TOUJOURS EN PREMIER)
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/formation-form/formation-form.component')
        .then(m => m.FormationFormComponent)
  },

  // 🔥 EDIT
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./pages/formation-form/formation-form.component')
        .then(m => m.FormationFormComponent)
  },

  // 🔥 PARTICIPANTS
  {
    path: ':id/participants',
    loadComponent: () =>
      import('./pages/formation-participants/formation-participants.component')
        .then(m => m.FormationParticipantsComponent)
  },

  // 🔥 DETAIL
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/formation-detail/formation-detail.component')
        .then(m => m.FormationDetailComponent)
  },

  // 🔥 LIST (TOUJOURS EN DERNIER OU VIDE)
  {
    path: '',
    loadComponent: () =>
      import('./pages/formation-list/formation-list.component')
        .then(m => m.FormationListComponent)
  }

];