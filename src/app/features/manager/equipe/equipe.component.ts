import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManagerService } from '../../../core/services/manager.service';

@Component({
  selector: 'app-equipe',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>👥 Mon équipe</h2>

    <div *ngFor="let emp of employes">
      {{ emp.nom }} - {{ emp.poste }}
    </div>
  `
})
export class EquipeComponent implements OnInit {

  employes: any[] = [];

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getEquipe().subscribe((data: any[]) => {
      this.employes = data;
    });
  }
}