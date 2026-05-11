export type FormationRecommendationType = 'GAP_POSTE' | 'BOOST_COMPETENCES';

export interface FormationRecommendation {
  id: number;
  employeId: number;
  offreId?: number | null;

  poste?: string | null;
  type: FormationRecommendationType;

  formationTitle: string;
  provider?: string | null;
  url?: string | null;
  description?: string | null;

  score?: number | null;
  semanticScore?: number | null;
  skillScore?: number | null;

  matchedSkills?: string | null;
  reason?: string | null;
  videosJson?: string | null;

  dateCreation?: string | null;
}