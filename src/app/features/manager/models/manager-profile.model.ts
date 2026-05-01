// src/app/features/manager/models/manager-profile.model.ts
export interface ManagerProfile {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  poste: string;
  departement: string;
  dateEmbauche: string;
  dateNomination: string;
  actif: boolean;
  role: string;
  soldeConges: number;
  statutCompte: string;
  anciennete: number;
  ancienneteManager: number;
  nombreEmployesGeres: number;
  nomComplet: string;
  photoUrl?: string;   // ← propriété optionnelle
}

export interface UpdateProfileData {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  poste: string;
  departement: string;
}

export interface ChangePasswordData {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}