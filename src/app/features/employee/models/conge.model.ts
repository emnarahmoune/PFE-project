// conge.model.ts
export interface DemandeConge {
  id?: number;
  employeId?: number;  // ✅ Rendre optionnel (le backend le récupère du JWT)
  dateDebut: string;
  dateFin: string;
  type: string;
  statut?: string;  // ✅ Rendre optionnel (le backend le définit)
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

export interface CongeResponse {
  success: boolean;
  message?: string;
  data?: DemandeConge | DemandeConge[] | any;
  error?: string;
  timestamp?: string;
  statusCode?: number;
}