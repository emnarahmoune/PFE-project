import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManagerService } from '../manager.service';

@Component({
  selector: 'app-dashboard-manager',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-manager.component.html'
})
export class DashboardManagerComponent implements OnInit {

  stats: any = {};
  equipe: any[] = [];
  conges: any[] = [];

  loading = true;
  errorMessage = '';
  currentDate = new Date();

  constructor(private managerService: ManagerService) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.loading = true;

    this.managerService.getStats().subscribe({
      next: (data) => this.stats = data,
      error: () => this.errorMessage = 'Erreur stats'
    });

    this.managerService.getEquipe().subscribe({
      next: (data) => this.equipe = data,
      error: () => this.errorMessage = 'Erreur équipe'
    });

    this.managerService.getConges().subscribe({
      next: (data) => this.conges = data,
      error: () => this.errorMessage = 'Erreur congés'
    });

    setTimeout(() => this.loading = false, 500);
  }
}