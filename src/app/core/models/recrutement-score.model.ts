export type NiveauCompatibilite =
  | 'EXCELLENT'
  | 'TRES_BON'
  | 'BON'
  | 'MOYEN'
  | 'FAIBLE';

export interface RecrutementScore {
  id?: number;

  candidatureId?: number;
  offreId: number;
  employeId: number;

  scoreGlobal: number;

  scoreCompetences?: number;
  scoreTechnologies?: number;
  scoreExperience?: number;
  scoreFormation?: number;

  niveauCompatibilite: NiveauCompatibilite;

  competencesCorrespondantes: string[];
  competencesManquantes: string[];

  technologiesCorrespondantes: string[];
  technologiesManquantes: string[];

  justificationIa?: string;
  recommandationIa?: string;

  dateCalcul?: string;
}

export interface MatchingResult {
  analyse: {
    competencesDetectees: string[];
    technologiesDetectees: string[];
    anneesExperienceEstimees?: number;
    resumeProfil?: string;
  };

  score: RecrutementScore;
}