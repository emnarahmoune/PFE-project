import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>📊 Statistiques</h2>

    <div class="card">Absentéisme : 4%</div>
    <div class="card">Turnover : 2%</div>
  `
})
export class StatsComponent {}