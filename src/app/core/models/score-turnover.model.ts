// src/app/core/models/score.model.ts

export interface ScoreTurnover {
  id: number;
  employeId: number;
  employeNom: string;
  employePrenom: string;
  employeMatricule?: string;
  employeDepartement?: string;
  score: number;
  niveauRisque: string;
  datePrediction: string;
  facteursPrincipaux?: string;
  actionRecommandee?: string;
  scoreAbsenteisme?: number;
  scoreAnciennete?: number;
  scoreFormation?: number;
  scorePerformance?: number;
  scoreSalaire?: number;
  periodePrediction?: string;
  versionModele?: string;
  confianceModele?: number;
  systemeBiId?: number;
}

export interface SousScoreItem {
  critere: string;
  valeur: number;
  max: number;
  contribution: number;
  couleur: string;
}

export interface FacteurItem {
  libelle: string;
  score: number;
  niveauImpact: string;
}

export interface ActionItem {
  action: string;
  type: string;
}

export interface HistoriqueLigne {
  date: string;
  scoreGlobal: number;
  niveau: string;
  anciennete: string;
  salaire: string;
  performance: string;
  formations: string;
  absenteisme: string;
  facteursMajeurs: string;
}

export interface InfoCalcul {
  periodeDebut: string;
  periodeFin: string;
  methode: string;
  source: string;
  dernierBatch: string;
  prochainBatch: string;
}

export interface EmployeScoreDetail {
  employeId: number;
  nom: string;
  prenom: string;
  matricule: string;
  poste: string;
  departement: string;
  dateEmbauche: string;
  anciennete: string;
  salaireAnnuel: number;
  managerNom?: string;
  photoUrl?: string;
  scoreActuel: ScoreTurnover;
  scorePrecedent?: ScoreTurnover;
  evolutionScore: number;
  evolutionPourcentage: number;
  rang: number;
  percentile: number;
  totalEmployes: number;
  sousScores: SousScoreItem[];
  facteursContributifs: FacteurItem[];
  actionsRecommandees: ActionItem[];
  historique: HistoriqueLigne[];
  infoCalcul: InfoCalcul;
}