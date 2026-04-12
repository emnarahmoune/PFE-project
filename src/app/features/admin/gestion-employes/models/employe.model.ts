export interface Employe {
  id?: number;
  matricule?: string;
  nom?: string;
  prenom?: string;
  email?: string;
  telephone?: string;
  dateEmbauche?: string;
  poste?: string;
  salaire?: number;
  departement?: string;
  statut: string;
  soldeConges?: number;
  serviceId?: number;
  managerId?: number | null;      // peut être null
  role?: string;                  // "user", "manager", "admin_rh"
  createdAt?: string;
  managerNom?: string; 
  updatedAt?: string;
}

export interface EmployeResponse {
  success: boolean;
  message?: string;
  data: Employe | Employe[] | any;
  timestamp?: string;
  statusCode?: number;
}