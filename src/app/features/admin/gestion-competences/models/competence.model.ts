export interface Competence {
  id?: number;
  nom: string;
  description: string;
  categorie: 'TECHNIQUE' | 'SOFT_SKILL' | 'LINGUISTIQUE' | 'MANAGEMENT';
  nombreEmployes?: number;
  niveauMoyen?: number;
}

export interface CompetenceResponse {
  success: boolean;
  message: string;
  data: Competence | Competence[];
  timestamp: string;
  statusCode: number;
}

export interface CompetenceStats {
  totalCompetences: number;
  competencesParCategorie: { [key: string]: number };
  topCompetences: Competence[];
  competencesNonAttribuees: number;
}