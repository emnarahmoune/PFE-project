// src/app/core/models/user.model.ts
export interface User {
  id?: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  actif?: boolean;
  typeUtilisateur?: string;
  dateCreation?: Date;
}