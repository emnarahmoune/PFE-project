export interface Formation {
  id?: number;
  titre: string;
  description: string;
  domaine: 'TECHNIQUE' | 'SOFT_SKILLS' | 'MANAGEMENT' | 'LANGUES' | 'SECURITE' | 'INFORMATIQUE';
  dureeHeures: number;
  actif?: boolean;
  dateCreation?: Date;

  urlVideo?: string;
  pdfPath?: string;
  participants?: EmployeFormation[];
  nombreParticipants?: number;
totalEmployes?: number;

  videos?: FormationVideo[];
  supports?: FormationSupport[];
}


export interface FormationVideo {
  id?: number;
  titre: string;
  urlYoutube: string;
  ordre?: number;
}

export interface FormationSupport {
  id?: number;
  titre: string;
  fichierUrl: string;
  ordre?: number;
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