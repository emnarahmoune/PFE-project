// src/app/features/employee/models/conge.model.ts

export interface DemandeConge {
  id?: number;
  employeId?: number;
  dateDebut: string;
  dateFin: string;
  type: string;
  statut?: string;
  dateDemande?: string;
  dateDecision?: string;
  commentaire?: string;
  motifRefus?: string;
  joursOuvres?: number;
  urgente?: boolean;
  employeMatricule?: string;
  employeNom?: string;
  employePrenom?: string;
  managerId?: number;
  managerNom?: string;
  nombreJours?: number;
  resume?: string;
  managerEmail?: string;
  adminEmail?: string;
  managerApprouve?: boolean;
  rhApprouve?: boolean;
  processInstanceId?: string;
}

// ✅ Interface pour le solde de congés (retournée par /mon-solde-conges)
export interface SoldeConges {
congesPris: any;
soldePrecedent: any;
soldeActuel: any;
  total: number;      // solde total (initial)
  pris: number;       // jours déjà pris (somme des jours approuvés)
  restant: number;    // solde restant
  enAttente: number;  // nombre de demandes en attente (utile pour l'affichage)
}

export interface CongeResponse {
  success: boolean;
  message?: string;
  data?: DemandeConge | DemandeConge[] | SoldeConges | any;
  error?: string;
  timestamp?: string;
  statusCode?: number;
}