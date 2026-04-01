// indicateurs.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-indicateurs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './indicateurs.component.html',
  styleUrls: ['./indicateurs.component.css']
})
export class IndicateursComponent implements OnInit {
  
  // Propriétés du composant
  indicateurs: any[] = [];
  loading: boolean = false;
  
  constructor() {}
  
  ngOnInit(): void {
    this.loadIndicateurs();
  }
  
  loadIndicateurs(): void {
    // Logique pour charger les indicateurs
  }
}