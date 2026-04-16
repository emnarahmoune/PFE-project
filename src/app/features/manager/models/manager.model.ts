// src/app/core/models/manager.model.ts
export interface Manager {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  matricule?: string;
  departement?: string;
  actif?: boolean;
}