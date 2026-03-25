// src/app/features/auth/layouts/auth-layout/auth-layout.component.ts
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './auth-layout.component.html',
  styleUrls: ['./auth-layout.component.css']
})
export class AuthLayoutComponent {
  currentYear = new Date().getFullYear();
  
  features = [
    {
      icon: '👥',
      title: 'Gestion des Employés',
      description: 'Gérez facilement les profils, contrats et informations de vos employés'
    },
    {
      icon: '📅',
      title: 'Suivi des Congés',
      description: 'Demandes, validations et historique des absences en temps réel'
    },
    {
      icon: '📚',
      title: 'Formations',
      description: 'Catalogue complet et inscriptions aux formations professionnelles'
    },
    {
      icon: '📊',
      title: 'Analyse Prédictive',
      description: 'Anticipez les risques de turnover avec l\'IA et le Machine Learning'
    }
  ];
}