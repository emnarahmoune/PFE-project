import { OffreRecrutement } from './offre-recrutement.model';
import { CvAnalyse } from './cv-analyse.model';
import { RecrutementScore } from './recrutement-score.model';

export type StatutCandidature =
  | 'SOUMISE'
  | 'EN_ANALYSE'
  | 'ANALYSEE'
  | 'ACCEPTEE'
  | 'REFUSEE';

export interface Candidature {
  id?: number;

  offreId: number;
  employeId?: number;

  employeNom?: string;
  employePrenom?: string;
  employeEmail?: string;
  employePosteActuel?: string;
  employeDepartement?: string;

  employePhotoUrl?: string;
  motivation?: string;

  cvFileName?: string;
  cvUrl?: string;

  statut: StatutCandidature;

  dateSoumission?: string;
  dateDecision?: string;

  decisionCommentaire?: string;

  offre?: OffreRecrutement;
  analyseCv?: CvAnalyse;
  score?: RecrutementScore;
}

export interface CreateCandidatureRequest {
  offreId: number;
  motivation?: string;
  cv: File;
}

export interface DecisionCandidatureRequest {
  commentaire?: string;
}

export interface TopCandidature {
  candidature: Candidature;
  scoreGlobal: number;
  niveauCompatibilite: string;
}