export type StatutOffreRecrutement = 'OUVERTE' | 'FERMEE' | 'BROUILLON';

export interface OffreRecrutement {
  id?: number;
  titrePoste: string;
  description: string;
  departement?: string;
  typeContrat?: string;
  localisation?: string;
  competencesRequises: string[];
  technologiesRequises: string[];
  experienceMin?: number;
  niveauEtude?: string;
  statut?: StatutOffreRecrutement;
  datePublication?: string;
  dateExpiration?: string;
  salairePropose?: number;
  nombreCandidatures?: number;
  meilleurScore?: number;
}

export interface CreateOffreRecrutementRequest {
  titrePoste: string;
  description: string;
  departement?: string;
  typeContrat?: string;
  localisation?: string;
  competencesRequises: string[];
  technologiesRequises: string[];
  experienceMin?: number;
  niveauEtude?: string;
  dateExpiration?: string;
  salairePropose?: number;
}

// export interface UpdateOffreRecrutementRequest extends CreateOffreRecrutementRequest {
//   statut?: StatutOffreRecrutement;
// }

export interface UpdateOffreRecrutementRequest {
  titrePoste: string;
  description: string;
  departement?: string;
  typeContrat?: string;
  localisation?: string;
  competencesRequises: string[];
  technologiesRequises: string[];
  experienceMin?: number;
  niveauEtude?: string;
  dateExpiration?: string;
  salairePropose?: number;
}