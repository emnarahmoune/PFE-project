// src/app/features/admin/gestion-conges/pages/validation-rh/validation-rh.routes.ts

import { Routes } from '@angular/router';
import { ValidationRhComponent } from './validation-rh.component';

export const VALIDATION_RH_ROUTES: Routes = [
  {
    path: '',
    component: ValidationRhComponent,
    data: { title: 'Validation RH - Demandes de congé' }
  }
];