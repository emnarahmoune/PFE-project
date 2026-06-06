export type FormationRecommendationType = 'GAP_POSTE' | 'BOOST_COMPETENCES';

export interface FormationRecommendation {
  id?: number | null;           // ← optionnel (null pour les externes)
  employeId?: number | null;
  offreId?: number | null;

  poste?: string | null;
  type?: FormationRecommendationType | null;

  // Titre — Python retourne "formation" ou "title"
  formationTitle?: string | null;
  formation?: string | null;    // ← ajouté
  title?: string | null;        // ← ajouté

  provider?: string | null;
  url?: string | null;          // ← lien Coursera/Udemy pour les externes
  description?: string | null;
  level?: string | null;        // ← ajouté

  score?: number | null;
  semanticScore?: number | null;
  skillScore?: number | null;
  priorityScore?: number | null; // ← ajouté

  matchedSkills?: string[] | null; // ← corrigé : c'est un tableau, pas une string
  reason?: string | null;
  videosJson?: string | null;
  videos?: any[] | null;           // ← ajouté : Python retourne un tableau

  source?: 'INTERNE' | 'EXTERNE' | null;  // ← ajouté : détecte si externe
  catalogueEpuise?: boolean | null;        // ← ajouté : catalogue épuisé

  dateCreation?: string | null;
}