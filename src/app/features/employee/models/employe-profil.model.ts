// employe-profil.model.ts

export interface EmployeProfil {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  poste: string;
  departement: string;
  dateEmbauche: Date;
  salaire: number;
  statut: string;
  soldeConges: number;
  managerNom?: string;
  dateCreation: Date;
  adresse?: string;
  photoUrl?: string | null;
  role?: string;
  typeUtilisateur?: string;
}

export type Employe = EmployeProfil;

export interface SoldeConges {
  total: number;
  pris: number;
  restant: number;
  enAttente: number;
}

export interface CompetenceEmploye {
  id: number;
  nom: string;
  categorie: string;
  niveau: string;
  certifie: boolean;
  dateAcquisition: Date;
  dateExpiration?: Date;
}

export interface FormationEmploye {
  id: number;
  titre: string;
  domaine: string;
  dateDebut?: Date;
  dateFin?: Date;
  statut: string;
  noteEvaluation?: number;
  progression: number;
}

export interface HistoriqueConge {
  id: number;
  dateDebut: Date;
  dateFin: Date;
  type: string;
  statut: string;
  joursOuvres: number;
  dateDemande: Date;
  dateDecision?: Date;
}

export interface UpdateProfilRequest {
  telephone?: string;
  adresse?: string;
  photo?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  statusCode: number;
}

export type EmployeProfilResponse = ApiResponse<EmployeProfil>;
export type SoldeCongesResponse = ApiResponse<SoldeConges>;
export type CompetencesEmployeResponse = ApiResponse<CompetenceEmploye[]>;
export type FormationsEmployeResponse = ApiResponse<FormationEmploye[]>;
export type HistoriqueCongesResponse = ApiResponse<HistoriqueConge[]>;

/**
 * Ancien type si tu l’utilises ailleurs.
 * À éviter dans mon-profil.component.ts.
 */
export type EmployeGenericResponse =
  ApiResponse<EmployeProfil | SoldeConges | CompetenceEmploye[] | FormationEmploye[] | HistoriqueConge[]>;