export interface DemandeConge {
  id?: number;
  dateDebut: Date;
  dateFin: Date;
  type: 'ANNUEL' | 'MALADIE' | 'SANS_SOLDE' | 'MATERNITE' | 'PATERNITE';
  statut: 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE' | 'ANNULE';
  commentaire?: string;
  motifRefus?: string;
  joursOuvres?: number;
  dateDemande?: Date;
  dateDecision?: Date;
  urgente?: boolean;
}

export interface CongeResponse {
  success: boolean;
  message: string;
  data: DemandeConge | DemandeConge[];
  timestamp: string;
  statusCode: number;
}