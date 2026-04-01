import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManagerService } from '../../../core/services/manager.service';

@Component({
  selector: 'app-conges',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>📅 Congés en attente</h2>

    <div *ngFor="let c of conges">
      {{ c.nom }} - {{ c.type }}

      <button (click)="valider(c)">✔</button>
      <button (click)="refuser(c)">❌</button>
    </div>
  `
})
export class CongesComponent implements OnInit {

  conges: any[] = [];

  constructor(private managerService: ManagerService) {}

  ngOnInit(): void {
    this.managerService.getConges().subscribe((data: any[]) => {
      this.conges = data;
    });
  }

  valider(c: any): void {
    alert('Validé ' + c.nom);
  }

  refuser(c: any): void {
    alert('Refusé ' + c.nom);
  }
}