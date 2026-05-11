// src/app/features/admin/scores/models/score-turnover.model.ts
export interface ScoreTurnover {
  id: number;
  employeId: number;
  employeNom: string;
  employePrenom: string;
  employeMatricule?: string;
  employeDepartement?: string;
  score: number;
  niveauRisque: string; // FAIBLE, MOYEN, ELEVE, CRITIQUE
  datePrediction: string;
  facteursPrincipaux?: string;
  actionRecommandee?: string;
}