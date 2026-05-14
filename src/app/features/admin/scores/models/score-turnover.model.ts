// score-turnover.model.ts
export interface ScoreTurnover {
  photoUrl: string | undefined;
  employeId: number;
  employeNom: string;
  employePrenom: string;
  employeMatricule?: string;
  employeDepartement?: string;
  employeEmail?: string;
  employePhotoUrl?: string;
  employePoste?: string;
  dateEmbauche?: string;
  salaireAnnuel?: number;
  score: number;
  niveauRisque: string;
  datePrediction: string;
  scoreAnciennete?: number;
  scoreSalaire?: number;
  scorePerformance?: number;
  scoreFormation?: number;
  scoreAbsenteisme?: number;
  
  facteursPrincipaux?: string;
  actionRecommandee?: string;
  confianceModele?: number;

}