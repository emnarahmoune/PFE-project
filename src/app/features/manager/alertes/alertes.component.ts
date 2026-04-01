
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
@Component({
  standalone: true,
   imports: [CommonModule],
  template: `
    <h2>⚠️ Alertes</h2>

    <div class="card">
      Risque de départ élevé - Employé Ali
    </div>

    <div class="card">
      Absences répétées - Sara
    </div>
  `
})
export class AlertesComponent {}