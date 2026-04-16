// src/app/features/manager/pages/equipe/equipe.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ManagerService } from '../../../../core/services/manager.service';
import { Employe } from '../../../admin/gestion-employes/models/employe.model';

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="equipe-container">
      <h2>👥 Mon équipe</h2>
      <div class="cards-grid">
        <div class="employe-card" *ngFor="let emp of employes" [routerLink]="['/manager/employe', emp.id]">
          <div class="avatar">{{ (emp.prenom?.charAt(0) ?? '') + (emp.nom?.charAt(0) ?? '') }}</div>
          <h3>{{ emp.prenom }} {{ emp.nom }}</h3>
          <p>{{ emp.poste }}</p>
          <span class="dept">{{ emp.departement }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .equipe-container { padding: 2rem; }
    .cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px,1fr)); gap: 1.5rem; margin-top: 1.5rem; }
    .employe-card { background: white; border-radius: 1rem; padding: 1.5rem; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
    .employe-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
    .avatar { width: 56px; height: 56px; background: #6366f1; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; margin-bottom: 1rem; }
    h3 { margin: 0 0 0.25rem; font-size: 1.1rem; }
    p { margin: 0; color: #4b5563; font-size: 0.9rem; }
    .dept { display: inline-block; margin-top: 0.75rem; font-size: 0.75rem; background: #eef2ff; padding: 0.25rem 0.75rem; border-radius: 20px; color: #4f46e5; }
  `]
})
export class EquipeComponent implements OnInit {
  employes: Employe[] = [];

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getEquipe().subscribe({
      next: (res) => {
        if (res.success) this.employes = res.data as Employe[];
      },
      error: (err) => console.error(err)
    });
  }
}