export interface Formation {
  id?: number;
  titre: string;
  description: string;
  domaine: 'TECHNIQUE' | 'SOFT_SKILLS' | 'MANAGEMENT' | 'LANGUES' | 'SECURITE';
  dureeHeures: number;
  actif?: boolean;
  dateCreation?: Date;
  nombreParticipants?: number;
  participants?: EmployeFormation[];
}

export interface EmployeFormation {
  id: number;
  nom: string;
  prenom: string;
  matricule: string;
  email: string;
  poste: string;
  departement: string;
  dateInscription?: Date;
  statut?: 'INSCRIT' | 'EN_COURS' | 'TERMINE' | 'ABANDON';
  noteEvaluation?: number;
  progression?: number;
}

export interface FormationResponse {
  success: boolean;
  message: string;
  data: Formation | Formation[];
  timestamp: string;
  statusCode: number;
}

export interface FormationStats {
  totalFormations: number;
  formationsActives: number;
  dureeMoyenne: number;
  participantsMoyens: number;
  totalParticipants: number;
  formationsParDomaine: { [key: string]: number };
  formationsPopulaires: Formation[];
}

export interface InscriptionRequest {
  formationId: number;
  employeId: number;
}