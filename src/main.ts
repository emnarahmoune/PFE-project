// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import 'chart.js/auto';

bootstrapApplication(AppComponent, appConfig)
  .then(() => console.log('✅ Application bootstrap réussie'))
  .catch((err) => console.error('❌ Erreur bootstrap:', err));