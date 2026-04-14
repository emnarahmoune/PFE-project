// src/app/shared/shared-charts.module.ts
import { NgModule } from '@angular/core';
import { NgChartsModule } from 'ng2-charts';

@NgModule({
  imports: [NgChartsModule],
  exports: [NgChartsModule]   // rend disponible la directive baseChart
})
export class SharedChartsModule { }