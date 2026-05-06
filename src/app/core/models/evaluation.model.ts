export type EvaluationStatut =
  | 'BROUILLON'
  | 'PUBLIEE'
  | 'VALIDEE'
  | 'ARCHIVEE';

export interface Evaluation {
  id?: number;

  employeId?: number;
  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
  employePoste?: string;
  employeDepartement?: string;
  employePhotoUrl?: string;
  employePhotoProfil?: string;

  evaluateurId?: number;
  evaluateurNom?: string;
  evaluateurPrenom?: string;
  evaluateurEmail?: string;
  evaluateurRole?: string;

  managerId?: number;
  managerNom?: string;
  managerPrenom?: string;
  managerEmail?: string;

  periode?: string;
  dateEvaluation?: string;

  note?: number;
  noteGlobale?: number;
  noteTechnique?: number;
  noteCommunication?: number;
  noteLeadership?: number;
  notePonctualite?: number;
  noteProductivite?: number;

  objectifsAtteints?: number;
  objectifs?: string;

  commentaire?: string;
  pointsForts?: string;
  axesAmelioration?: string;
  commentaireManager?: string;
  commentaireEmploye?: string;

  statut?: EvaluationStatut;

  createdAt?: string;
  updatedAt?: string;
}

export interface EvaluationRequest {
  employeId: number;
  periode: string;
  dateEvaluation?: string;

  note: number;
  noteGlobale?: number;

  noteTechnique?: number;
  noteCommunication?: number;
  noteLeadership?: number;
  notePonctualite?: number;
  noteProductivite?: number;

  objectifsAtteints?: number;
  objectifs?: string;

  commentaire?: string;
  pointsForts?: string;
  axesAmelioration?: string;
  commentaireManager?: string;

  statut?: EvaluationStatut;
}

export interface EvaluationStats {
  totalEvaluations?: number;
  moyenneGlobale?: number;
  moyenneNote?: number;

  moyenneTechnique?: number;
  moyenneCommunication?: number;
  moyenneLeadership?: number;
  moyennePonctualite?: number;
  moyenneProductivite?: number;

  moyenneObjectifs?: number;
  meilleureNote?: number;
  plusFaibleNote?: number;

  evaluationsPubliees?: number;
  evaluationsBrouillon?: number;
  evaluationsValidees?: number;

  progression?: number;
}

export interface EmployeEquipeEvaluation {
  id: number;
  nom?: string;
  prenom?: string;
  email?: string;
  poste?: string;
  fonction?: string;
  departement?: string;

  photoUrl?: string;
  photo_url?: string;
  photo?: string;
  imageUrl?: string;
  avatarUrl?: string;
  photoProfil?: string;
}