export interface DemandeConge {
  id?: number;
  employeId: number;  // ← AJOUT OBLIGATOIRE
  employeNom?: string;  // ← AJOUT
  employePrenom?: string;  // ← AJOUT
  dateDebut: string | Date;
  dateFin: string | Date;
  type: 'ANNUEL' | 'MALADIE' | 'SANS_SOLDE' | 'MATERNITE' | 'PATERNITE';
  statut: 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE' | 'ANNULE';
  dateDemande?: string | Date;
  dateDecision?: string | Date;
  commentaire?: string;
  motifRefus?: string;
  joursOuvres?: number;
  urgent?: boolean;  // ← CORRIGÉ: 'urgent' au lieu de 'urgente'
  
  // Champs calculés
  nombreJours?: number;
  resume?: string;
  motif?: string;
}

export interface NouvelleDemandeRequest {
  dateDebut: string | Date;
  dateFin: string | Date;
  type: string;
  commentaire?: string;
}

export interface CongeResponse {
  success: boolean;
  message: string;
  data: DemandeConge | DemandeConge[] | any;
  timestamp: string;
  statusCode: number;
}

export interface SoldeConges {
  total: number;
  pris: number;
  restant: number;
  enAttente: number;
}