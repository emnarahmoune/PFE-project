export interface Employe {
  id?: number;
  matricule?: string;
  nom?: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  dateEmbauche?: string;
  poste?: string;
  salaire?: number;
  departement?: string;
  statut: string;
  soldeConges?: number;
  serviceId?: number;
  managerId?: number | null;
  role?: string;
  createdAt?: string;
  managerNom?: string;
  updatedAt?: string;
  absenteisme?: number;
  absenteismeDate?: string;
  scoreTurnover?: number;
  scoreTurnoverNiveau?: string;

  competences?: Competence[];
  formations?: Formation[];
  conges?: HistoriqueConge[];
  evaluations?: EvaluationEmploye[];
}

export interface Competence {
  nom: string;
  niveau: string;
}

export interface Formation {
  id: number;
  titre: string;
  domaine?: string;
  statut?: string;
  progression: number;
  dateDebut?: string;
  dateFin?: string;
}

export interface HistoriqueConge {
  id: number;
  dateDebut: string;
  dateFin: string;
  type: string;
  statut: string;
  joursOuvres?: number;
  dateDemande?: string;
  dateDecision?: string;
}

export interface EvaluationEmploye {
  id: number;
  employeId?: number;
  employeNom?: string;
  employePrenom?: string;
  dateEvaluation: string;
  note: number;
  objectifsAtteints?: number;
  commentaire?: string;
  evaluateurId?: number;
  evaluateurNom?: string;
}

export interface EmployeResponse {
  success: boolean;
  message?: string;
  data: Employe | Employe[] | any;
  timestamp?: string;
  statusCode?: number;
}